package dev.thingscompanion.app.data

import android.content.Context
import dev.thingscompanion.app.reminders.ReminderScheduler
import dev.thingscompanion.cloud.HistoryCache
import dev.thingscompanion.cloud.HistoryState
import dev.thingscompanion.cloud.PendingChange
import dev.thingscompanion.cloud.PendingChanges
import dev.thingscompanion.cloud.ThingsCloudClient
import dev.thingscompanion.cloud.ThingsLibrary
import dev.thingscompanion.cloud.ThingsSync
import dev.thingscompanion.cloud.WidgetSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class NotSignedInException : Exception("Not signed in")

/** The to-dos to show (cache plus changes not sent yet), how many changes wait, and those that could not apply. */
class SyncOutcome(
    val library: ThingsLibrary,
    val pending: Int,
    val failed: List<Pair<PendingChange, Exception>> = emptyList(),
)

/** Android adapter: credentials in the Keystore, history cached in app files, use cases in [ThingsSync]. */
class ThingsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val secureStore = SecureStore(context)
    private val cache = FileHistoryCache(context)
    private val client = ThingsCloudClient(appInstanceId = secureStore.appInstanceId)
    private val outbox = Outbox(context)
    private val sync = ThingsSync(client, cache, pending = outbox::read)

    val isSignedIn: Boolean get() = secureStore.historyKey() != null

    suspend fun signIn(email: String, password: String): ThingsLibrary = locked {
        val historyKey = client.authenticate(email.trim(), password)
        secureStore.saveHistoryKey(historyKey)
        cache.clear()
        sync.sync(historyKey, abortIfCancelled()).library.also(::scheduleReminders)
    }

    /** The cached to-dos with the changes not sent yet; no network. */
    suspend fun cached(): SyncOutcome? = withContext(Dispatchers.IO) {
        val state = cache.load()?.first ?: return@withContext null
        val pending = outbox.read()
        SyncOutcome(PendingChanges.overlay(state, pending, now()), pending.size)
    }

    /**
     * Keeps [change] on the phone and returns the to-dos as they will be once it is sent.
     * Takes the outbox lock before switching threads, so changes keep the order they were made in.
     */
    suspend fun enqueue(change: PendingChange): SyncOutcome = outboxLock.withLock {
        withContext(Dispatchers.IO) {
            val pending = PendingChanges.enqueue(outbox.read(), change)
            outbox.write(pending)
            val state = cache.load()?.first ?: HistoryState()
            val library = PendingChanges.overlay(state, pending, now())
            writeWidgetSnapshot(appContext, WidgetSnapshot.from(library, dev.thingscompanion.cloud.ThingsDay.today(), System.currentTimeMillis()))
            scheduleReminders(library)
            SyncOutcome(library, pending.size)
        }
    }

    /** Sends the queued changes, then pulls; the cache and the widget are refreshed either way. */
    suspend fun sync(): SyncOutcome = locked {
        val pending = outbox.read()
        val result = sync.flush(historyKey(), pending, abortIfCancelled())
        // Changes queued meanwhile stay; only those settled by this flush leave the queue.
        val settled = (result.done + result.failed.map { it.first.opId }).toSet()
        val remaining = outboxLock.withLock { outbox.read().filterNot { it.opId in settled }.also(outbox::write) }
        val library = PendingChanges.overlay(result.synced.state, remaining, now())
        scheduleReminders(library)
        SyncOutcome(library, remaining.size, result.failed)
    }

    suspend fun pendingCount(): Int = withContext(Dispatchers.IO) { outbox.read().size }

    /** Drops the changes not sent yet (a change Things keeps refusing would otherwise block the queue). */
    suspend fun discardPending(): SyncOutcome? {
        outboxLock.withLock { withContext(Dispatchers.IO) { outbox.write(emptyList()) } }
        return cached()
    }

    /** Under the sync lock, so no sync (UI or worker) can write the cache back after it is cleared. */
    suspend fun signOut() = locked {
        secureStore.clear()
        cache.clear()
        outboxLock.withLock { outbox.write(emptyList()) }
        ReminderScheduler.cancelAll(appContext)
    }

    private fun now() = System.currentTimeMillis() / 1000.0

    /** Every sync refreshes the phone's own reminder alarms. */
    private fun scheduleReminders(library: ThingsLibrary) = ReminderScheduler.schedule(appContext, library)

    private fun historyKey() = secureStore.historyKey() ?: throw NotSignedInException()

    /** The app and the background worker share the cache files: one sync at a time, off the main thread. */
    private suspend fun <T> locked(block: suspend () -> T): T = syncLock.withLock { withContext(Dispatchers.IO) { block() } }

    private companion object {
        val syncLock = Mutex()

        /** Guards the outbox file; never held across network calls, so queuing a change is instant. */
        val outboxLock = Mutex()
    }

    /** Pulls block and ignore cancellation; skip the cache write if the caller was cancelled (e.g. signed out). */
    private suspend fun abortIfCancelled(): () -> Unit {
        val context = currentCoroutineContext()
        return { context.ensureActive() }
    }
}

private class FileHistoryCache(context: Context) : HistoryCache {
    private val stateFile = File(context.filesDir, "history_state.json")
    private val indexFile = File(context.filesDir, "history_index")
    private val snapshotFile = widgetSnapshotFile(context)

    /** Resume only when both the state and its index are readable. */
    override fun load(): Pair<HistoryState, Long>? {
        val state = stateFile.readTextOrNull()?.let { runCatching { HistoryState.fromJson(it) }.getOrNull() }
        val index = indexFile.readTextOrNull()?.toLongOrNull()
        return if (state != null && index != null) state to index else null
    }

    override fun save(state: HistoryState, nextStartIndex: Long, snapshot: WidgetSnapshot) {
        stateFile.writeAtomically(state.toJson())
        indexFile.writeAtomically(nextStartIndex.toString())
        snapshotFile.writeAtomically(snapshot.toJson())
    }

    fun clear() {
        stateFile.delete()
        indexFile.delete()
        snapshotFile.delete()
    }

    private fun File.readTextOrNull(): String? = if (exists()) readText() else null

    /** Readers (e.g. the widget) never see a half-written file. */
    private fun File.writeAtomically(text: String) {
        val tmp = File(parentFile, "$name.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(this)) {
            tmp.delete()
            writeText(text)
        }
    }
}

/** Changes made on the phone and not yet in Things Cloud, in the order they were made. */
private class Outbox(context: Context) {
    private val file = File(context.filesDir, "pending_changes.json")

    /** An unreadable file is kept aside rather than silently emptied. */
    fun read(): List<PendingChange> {
        if (!file.exists()) return emptyList()
        return runCatching { PendingChanges.fromJson(file.readText()) }.getOrElse {
            file.renameTo(File(file.parentFile, "${file.name}.unreadable"))
            emptyList()
        }
    }

    fun write(changes: List<PendingChange>) {
        if (changes.isEmpty()) {
            file.delete()
            return
        }
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(PendingChanges.toJson(changes))
        if (!tmp.renameTo(file)) {
            tmp.delete()
            file.writeText(PendingChanges.toJson(changes))
        }
    }
}

/** The lists the home-screen widget shows, refreshed after every sync. */
fun widgetSnapshotFile(context: Context) = File(context.filesDir, "widget_snapshot.json")

fun readWidgetSnapshot(context: Context): WidgetSnapshot? = widgetSnapshotFile(context)
    .takeIf { it.exists() }
    ?.let { runCatching { WidgetSnapshot.fromJson(it.readText()) }.getOrNull() }

/** Writes a snapshot for the widget outside a sync (demo mode). */
fun writeWidgetSnapshot(context: Context, snapshot: WidgetSnapshot) {
    val file = widgetSnapshotFile(context)
    val tmp = File(file.parentFile, "${file.name}.tmp2")
    tmp.writeText(snapshot.toJson())
    if (!tmp.renameTo(file)) {
        tmp.delete()
        file.writeText(snapshot.toJson())
    }
}
