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
    suspend fun cached(): SyncOutcome? = outboxLock.withLock {
        withContext(Dispatchers.IO) {
            if (!cache.exists()) return@withContext null
            val pending = outbox.read()
            SyncOutcome(show(pending, latest = null), pending.size)
        }
    }

    /**
     * Keeps [change] on the phone and returns the to-dos as they will be once it is sent.
     * Takes the outbox lock before switching threads, so changes keep the order they were made in.
     */
    suspend fun enqueue(change: PendingChange): SyncOutcome = outboxLock.withLock {
        withContext(Dispatchers.IO) {
            val pending = PendingChanges.enqueue(outbox.read(), change)
            outbox.write(pending)
            val library = show(pending, latest = change)
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

    /**
     * The lists with [pending] applied. Called under the outbox lock. When only [latest] is new
     * since the last call and the cache has not changed, it is applied alone: a tap costs one
     * rebuild of the lists, not a replay of the whole queue.
     */
    private fun show(pending: List<PendingChange>, latest: PendingChange?): ThingsLibrary {
        // Read the version before the state: a sync saving in between only causes a full replay next time.
        val version = FileHistoryCache.version
        val last = shown
        val next = if (last != null && last.cacheVersion == version && PendingChanges.canApplyAlone(last.pending, pending, latest)) {
            PendingChanges.applyOne(last.shown, latest!!, now())
        } else {
            PendingChanges.overlayState(cache.load()?.first ?: HistoryState(), pending, now())
        }
        shown = Shown(version, pending, next)
        return next.library
    }

    /** What [show] last built, from which cache version and queue. */
    private class Shown(val cacheVersion: Long, val pending: List<PendingChange>, val shown: PendingChanges.Shown)

    /** Every sync refreshes the phone's own reminder alarms. */
    private fun scheduleReminders(library: ThingsLibrary) = ReminderScheduler.schedule(appContext, library)

    private fun historyKey() = secureStore.historyKey() ?: throw NotSignedInException()

    /** The app and the background worker share the cache files: one sync at a time, off the main thread. */
    private suspend fun <T> locked(block: suspend () -> T): T = syncLock.withLock { withContext(Dispatchers.IO) { block() } }

    private companion object {
        val syncLock = Mutex()

        /** Guards the outbox file; never held across network calls, so queuing a change is instant. */
        val outboxLock = Mutex()

        /** Shared by the app, the widget and the worker, which all run in the app's process. */
        @Volatile
        private var shown: Shown? = null
    }

    /** Pulls block and ignore cancellation; skip the cache write if the caller was cancelled (e.g. signed out). */
    private suspend fun abortIfCancelled(): () -> Unit {
        val context = currentCoroutineContext()
        return { context.ensureActive() }
    }
}

/**
 * The history in app files, also kept in memory once read: parsing the whole history took most
 * of the time of every tap. Callers get a copy, so they may change it freely.
 */
private class FileHistoryCache(context: Context) : HistoryCache {
    // v2: notes edited in Things are rebuilt from their patches. The first files kept only the last
    // patch, the text it applied to lost, so they are dropped and the history pulled again once.
    private val stateFile = File(context.filesDir, "history_state_v2.json")
    private val indexFile = File(context.filesDir, "history_index_v2")

    init {
        File(context.filesDir, "history_state.json").delete()
        File(context.filesDir, "history_index").delete()
    }
    private val snapshotFile = widgetSnapshotFile(context)

    fun exists(): Boolean = memory != null || (stateFile.exists() && indexFile.exists())

    /** Resume only when both the state and its index are readable. */
    override fun load(): Pair<HistoryState, Long>? {
        memory?.let { (state, index) -> return HistoryState(state.objects) to index }
        val versionRead = version
        val index = indexFile.readTextOrNull()?.toLongOrNull()
        val state = stateFile.readTextOrNull()?.let { runCatching { HistoryState.fromJson(it) }.getOrNull() }
        if (state == null || index == null) return null
        // A save or clear meanwhile may have paired another state with this index: keep it out of memory.
        synchronized(FileHistoryCache) {
            if (version == versionRead && memory == null) {
                memory = HistoryState(state.objects) to index
                version++
            }
        }
        return HistoryState(state.objects) to index
    }

    override fun save(state: HistoryState, nextStartIndex: Long, snapshot: WidgetSnapshot) {
        stateFile.writeAtomically(state.toJson())
        indexFile.writeAtomically(nextStartIndex.toString())
        snapshotFile.writeAtomically(snapshot.toJson())
        remember(state, nextStartIndex)
    }

    fun clear() {
        stateFile.delete()
        indexFile.delete()
        snapshotFile.delete()
        synchronized(FileHistoryCache) {
            memory = null
            version++
        }
    }

    private fun remember(state: HistoryState, index: Long) = synchronized(FileHistoryCache) {
        memory = HistoryState(state.objects) to index
        version++
    }

    companion object {
        @Volatile
        private var memory: Pair<HistoryState, Long>? = null

        /** Changes each time the cached history does. */
        @Volatile
        var version = 0L
            private set
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
