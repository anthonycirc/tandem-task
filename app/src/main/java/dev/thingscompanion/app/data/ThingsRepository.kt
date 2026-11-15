package dev.thingscompanion.app.data

import android.content.Context
import dev.thingscompanion.app.reminders.ReminderScheduler
import dev.thingscompanion.cloud.HistoryCache
import dev.thingscompanion.cloud.HistoryState
import dev.thingscompanion.cloud.Schedule
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
import java.time.LocalTime

class NotSignedInException : Exception("Not signed in")

/** Android adapter: credentials in the Keystore, history cached in app files, use cases in [ThingsSync]. */
class ThingsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val secureStore = SecureStore(context)
    private val cache = FileHistoryCache(context)
    private val client = ThingsCloudClient(appInstanceId = secureStore.appInstanceId)
    private val sync = ThingsSync(client, cache)

    val isSignedIn: Boolean get() = secureStore.historyKey() != null

    suspend fun signIn(email: String, password: String): ThingsLibrary = locked {
        val historyKey = client.authenticate(email.trim(), password)
        secureStore.saveHistoryKey(historyKey)
        cache.clear()
        sync.sync(historyKey, abortIfCancelled()).library.also(::scheduleReminders)
    }

    suspend fun cached(): ThingsLibrary? = withContext(Dispatchers.IO) {
        cache.load()?.first?.let(ThingsLibrary::from)
    }

    suspend fun refresh(): ThingsLibrary = locked {
        sync.sync(historyKey(), abortIfCancelled()).library.also(::scheduleReminders)
    }

    suspend fun addTask(id: String, title: String, notes: String, schedule: Schedule, reminder: LocalTime?): ThingsLibrary = locked {
        sync.addInboxTask(historyKey(), id, title, notes, abortIfCancelled(), schedule, reminder).also(::scheduleReminders)
    }

    suspend fun completeTask(id: String, done: Boolean): ThingsLibrary = locked {
        sync.completeTask(historyKey(), id, done, abortIfCancelled()).also(::scheduleReminders)
    }

    suspend fun editTask(id: String, title: String, notes: String, schedule: Schedule, reminder: LocalTime?): ThingsLibrary = locked {
        sync.editTask(historyKey(), id, title, notes, schedule, reminder, abortIfCancelled()).also(::scheduleReminders)
    }

    /** Whether an earlier attempt for this draft id reached Things. */
    suspend fun landed(id: String): Boolean = locked {
        sync.landed(historyKey(), id, abortIfCancelled())
    }

    /** Under the sync lock, so no sync (UI or worker) can write the cache back after it is cleared. */
    suspend fun signOut() = locked {
        secureStore.clear()
        cache.clear()
        ReminderScheduler.cancelAll(appContext)
    }

    /** Every sync refreshes the phone's own reminder alarms. */
    private fun scheduleReminders(library: ThingsLibrary) = ReminderScheduler.schedule(appContext, library)

    private fun historyKey() = secureStore.historyKey() ?: throw NotSignedInException()

    /** The app and the background worker share the cache files: one sync at a time, off the main thread. */
    private suspend fun <T> locked(block: suspend () -> T): T = syncLock.withLock { withContext(Dispatchers.IO) { block() } }

    private companion object {
        val syncLock = Mutex()
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

/** The lists the home-screen widget shows, refreshed after every sync. */
fun widgetSnapshotFile(context: Context) = File(context.filesDir, "widget_snapshot.json")

fun readWidgetSnapshot(context: Context): WidgetSnapshot? = widgetSnapshotFile(context)
    .takeIf { it.exists() }
    ?.let { runCatching { WidgetSnapshot.fromJson(it.readText()) }.getOrNull() }

/** Writes a snapshot for the widget outside a sync (demo mode). */
fun writeWidgetSnapshot(context: Context, snapshot: WidgetSnapshot) {
    widgetSnapshotFile(context).writeText(snapshot.toJson())
}
