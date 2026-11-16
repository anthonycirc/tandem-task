package dev.thingscompanion.app.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.thingscompanion.app.R
import dev.thingscompanion.app.data.NotSignedInException
import dev.thingscompanion.app.data.SyncOutcome
import dev.thingscompanion.app.data.ThingsRepository
import dev.thingscompanion.app.sync.BackgroundSync
import dev.thingscompanion.app.widget.ListWidget
import dev.thingscompanion.app.data.writeWidgetSnapshot
import dev.thingscompanion.cloud.DemoData
import dev.thingscompanion.cloud.PendingChange
import dev.thingscompanion.cloud.RepeatRule
import dev.thingscompanion.cloud.TaskStatus
import dev.thingscompanion.cloud.Schedule
import dev.thingscompanion.cloud.TaskFilter
import dev.thingscompanion.cloud.ThingsCloudException
import dev.thingscompanion.cloud.ThingsIds
import dev.thingscompanion.cloud.ThingsDay
import dev.thingscompanion.cloud.ThingsLibrary
import dev.thingscompanion.cloud.WidgetSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime

/** A message for the snackbar, resolved against the app's (localized) resources. */
data class UiMessage(@StringRes val res: Int, val arg: Any? = null, val undo: PendingChange? = null)

data class UiState(
    val signedIn: Boolean = false,
    val library: ThingsLibrary? = null,
    /** Signing in: the only wait the user sees. */
    val loading: Boolean = false,
    /** A sync runs in the background; nothing waits for it. */
    val syncing: Boolean = false,
    /** Changes kept on the phone until Things Cloud has them. */
    val pending: Int = 0,
    val message: UiMessage? = null,
    /** Requests from a widget tap, consumed by the screen. */
    val launchOpenAdd: Boolean = false,
    val launchTaskId: String? = null,
    /** Display filter; the show-completed choice is remembered across launches. */
    val filter: TaskFilter = TaskFilter(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Sample data for screenshots: no network, no writes, the real cache untouched. */
    val demo: Boolean = false,
)

/** App appearance: follow the system, or force light or dark. Remembered across launches. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** A checklist row of the to-do form: an existing item (by id) or a new one (a fresh id). */
data class ChecklistDraft(val id: String, val title: String, val done: Boolean = false)

class ThingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ThingsRepository(application)
    private val displayPrefs = application.getSharedPreferences("display", android.content.Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(
        UiState(
            signedIn = repository.isSignedIn,
            filter = TaskFilter(showClosed = displayPrefs.getBoolean(KEY_SHOW_CLOSED, true)),
            themeMode = displayPrefs.getString(KEY_THEME, null)
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
        ),
    )
    val state: StateFlow<UiState> = _state

    /** Sync requests, merged while one runs; one consumer, so syncs never overlap or cancel each other. */
    private val syncRequests = Channel<Unit>(Channel.CONFLATED)
    private var syncLoop: Job? = null

    /** Initial cache load; cancelled when the demo starts so real data never replaces the demo. */
    private var startupJob: Job? = null

    init {
        if (repository.isSignedIn) start()
    }

    private fun start() {
        BackgroundSync.schedule(getApplication())
        startSyncLoop()
        startupJob = viewModelScope.launch {
            val cached = repository.cached()
            if (!_state.value.demo) {
                cached?.let(::show)
                requestSync()
            }
        }
    }

    private fun startSyncLoop() {
        if (syncLoop != null) return
        syncLoop = viewModelScope.launch {
            for (request in syncRequests) {
                if (_state.value.demo || !repository.isSignedIn) continue
                _state.update { it.copy(syncing = true) }
                try {
                    val outcome = repository.sync()
                    if (!_state.value.demo) show(outcome)
                    outcome.failed.firstOrNull()?.let { (_, error) ->
                        _state.update { it.copy(message = UiMessage(R.string.change_refused, messageText(error))) }
                    }
                    runCatching { ListWidget.refreshAll(getApplication()) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Offline: the changes wait on the phone and go out with the network.
                    if (_state.value.pending > 0) BackgroundSync.sendSoon(getApplication())
                    val quiet = e is IOException && _state.value.pending == 0
                    if (!quiet) _state.update { it.copy(message = messageFor(e)) }
                } finally {
                    _state.update { it.copy(syncing = false) }
                }
            }
        }
    }

    /** Sends queued changes and pulls, in the background; never blocks the screen. */
    fun refresh() {
        if (!_state.value.demo) requestSync()
    }

    private fun requestSync() {
        syncRequests.trySend(Unit)
    }

    private var sendJob: Job? = null

    /**
     * A tap only queues its change; sending (and the pull that follows) waits until taps stop for
     * [SEND_DELAY_MS], so a burst of changes goes out in one sync, in the background.
     */
    private fun sendLater() {
        sendJob?.cancel()
        sendJob = viewModelScope.launch {
            delay(SEND_DELAY_MS)
            requestSync()
        }
    }

    private fun show(outcome: SyncOutcome) = _state.update { it.copy(library = outcome.library, pending = outcome.pending) }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            try {
                val library = repository.signIn(email, password)
                _state.update { it.copy(signedIn = true, library = library) }
                start()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(message = messageFor(e)) }
            } finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun enterDemo() {
        startupJob?.cancel()
        val library = ThingsLibrary.from(DemoData.state(ThingsDay.today(), System.currentTimeMillis() / 1000.0))
        _state.update { it.copy(signedIn = true, demo = true, library = library, loading = false, pending = 0) }
        // The widget shows the demo lists too, until the next real sync.
        viewModelScope.launch {
            writeWidgetSnapshot(getApplication(), WidgetSnapshot.from(library, ThingsDay.today(), System.currentTimeMillis()))
            runCatching { ListWidget.refreshAll(getApplication()) }
        }
    }

    fun exitDemo() {
        _state.update { it.copy(demo = false, signedIn = repository.isSignedIn, library = null) }
        if (repository.isSignedIn) {
            startSyncLoop()
            viewModelScope.launch {
                repository.cached()?.let(::show)
                requestSync()
            }
        }
    }

    /**
     * Keeps the change on the phone, shows it at once, then sends it in the background.
     * Started undispatched so changes made in a row are queued in that order.
     */
    private fun change(change: PendingChange, @StringRes done: Int? = null, undo: PendingChange? = null) {
        if (blockedInDemo()) return
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                show(repository.enqueue(change))
                done?.let { res -> _state.update { it.copy(message = UiMessage(res, undo = undo)) } }
                runCatching { ListWidget.refreshAll(getApplication()) }
                sendLater()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(message = messageFor(e)) }
            }
        }
    }

    private fun newId() = ThingsIds.random()

    fun addTask(
        title: String,
        notes: String,
        schedule: Schedule,
        reminder: LocalTime?,
        checklist: List<ChecklistDraft> = emptyList(),
        deadline: LocalDate? = null,
        projectId: String? = null,
        areaId: String? = null,
    ) {
        val taskId = newId()
        change(
            PendingChange.AddTask(newId(), taskId, title, notes, PendingChange.When.of(schedule), reminder?.toSecondOfDay(), projectId = projectId, areaId = areaId),
            // Name the Inbox when that is where it went; elsewhere (Today, a date…) just confirm.
            done = if (schedule == Schedule.Inbox) R.string.added_to_inbox else R.string.added,
        )
        checklist.filter { it.title.isNotBlank() }.forEach { change(PendingChange.AddChecklistItem(newId(), it.id, taskId, it.title)) }
        deadline?.let { change(PendingChange.SetDeadline(newId(), taskId, it.toEpochDay())) }
    }

    /** Saves the form: the to-do's fields, then the checklist rows added, renamed or removed. */
    fun editTask(
        id: String,
        title: String,
        notes: String,
        schedule: Schedule,
        reminder: LocalTime?,
        checklist: List<ChecklistDraft>? = null,
        deadline: LocalDate? = null,
    ) {
        val task = _state.value.library?.task(id)
        change(PendingChange.EditTask(newId(), id, title, notes, PendingChange.When.of(schedule), reminder?.toSecondOfDay()), done = R.string.saved)
        if (task != null && deadline != task.deadline) change(PendingChange.SetDeadline(newId(), id, deadline?.toEpochDay()))
        if (checklist == null) return
        val before = _state.value.library?.checklist(id).orEmpty().associateBy { it.id }
        val kept = checklist.filter { it.title.isNotBlank() }
        before.keys.filter { old -> kept.none { it.id == old } }.forEach { change(PendingChange.DeleteChecklistItem(newId(), it)) }
        kept.forEach { row ->
            val existing = before[row.id]
            when {
                existing == null -> change(PendingChange.AddChecklistItem(newId(), row.id, id, row.title))
                existing.title != row.title.trim() -> change(PendingChange.RenameChecklistItem(newId(), row.id, row.title))
            }
        }
    }

    fun addProject(title: String, notes: String, schedule: Schedule, deadline: LocalDate?, areaId: String?) {
        val id = newId()
        change(PendingChange.AddProject(newId(), id, title, notes, PendingChange.When.of(schedule), areaId), done = R.string.added)
        deadline?.let { change(PendingChange.SetDeadline(newId(), id, it.toEpochDay())) }
    }

    fun addHeading(projectId: String, title: String) = change(PendingChange.AddHeading(newId(), newId(), projectId, title))

    fun addArea(title: String) = change(PendingChange.AddArea(newId(), newId(), title), done = R.string.added)

    fun renameArea(id: String, title: String) = change(PendingChange.RenameArea(newId(), id, title))

    /** A heading keeps its place and status; only its title changes. */
    fun renameHeading(id: String, title: String) {
        val heading = _state.value.library?.task(id) ?: return
        change(PendingChange.EditTask(newId(), id, title, heading.notes.orEmpty(), PendingChange.When.of(Schedule.of(heading, ThingsDay.today()))))
    }

    fun moveTask(id: String, to: PendingChange.Destination) = change(PendingChange.MoveTask(newId(), id, to), done = R.string.moved)

    fun moveProject(id: String, areaId: String?) = change(PendingChange.MoveProject(newId(), id, areaId), done = R.string.moved)

    /** A list's new order after a drag (see [PendingChange.Reorder]). */
    fun reorder(id: String, order: List<String>, listKey: String, todayOrder: Boolean = false, evening: Boolean? = null) =
        change(PendingChange.Reorder(newId(), id, order, listKey, todayOrder, evening))

    /** Dropped in a project: filed under the section it landed in, then placed in its order. */
    fun dropInProject(projectId: String, id: String, headingId: String?, order: List<String>) {
        val task = _state.value.library?.task(id) ?: return
        if (task.headingId != headingId) {
            change(PendingChange.MoveTask(newId(), id, if (headingId != null) PendingChange.Destination(headingId = headingId) else PendingChange.Destination(projectId = projectId)))
        }
        reorder(id, order, "project:$projectId:${headingId.orEmpty()}")
    }

    /** A project dropped under an area (or none): moved there if needed, then placed in its order. */
    fun dropProject(id: String, areaId: String?, order: List<String>) {
        val project = _state.value.library?.task(id) ?: return
        if (project.areaId != areaId) change(PendingChange.MoveProject(newId(), id, areaId))
        reorder(id, order, "projects:${areaId.orEmpty()}")
    }

    fun deleteArea(id: String) = change(PendingChange.DeleteArea(newId(), id), done = R.string.area_deleted)

    /** The to-do becomes the first occurrence of a new repeat (see [PendingChange.MakeRepeating]). */
    fun makeRepeating(id: String, rule: RepeatRule) {
        val checklist = _state.value.library?.checklist(id).orEmpty()
        change(PendingChange.MakeRepeating(newId(), id, newId(), rule, checklist.map { newId() }), done = R.string.now_repeats)
    }

    fun setProjectStatus(id: String, status: TaskStatus) = change(
        PendingChange.SetProjectStatus(newId(), id, status),
        done = when (status) {
            TaskStatus.COMPLETED -> R.string.project_completed
            TaskStatus.CANCELLED -> R.string.project_cancelled
            TaskStatus.OPEN -> R.string.project_reopened
        },
    )

    fun setDone(id: String, done: Boolean) =
        change(PendingChange.SetDone(newId(), id, done, day = ThingsDay.today().toEpochDay()), if (done) R.string.marked_done else R.string.marked_open)

    /** Swipe to reschedule: the to-do's own title and notes, a new When; the reminder stays when there is still a day. */
    fun reschedule(id: String, schedule: Schedule) {
        val task = _state.value.library?.task(id) ?: return
        val reminder = task.reminder?.takeIf { schedule.date != null }
        change(
            PendingChange.EditTask(newId(), id, task.title, task.notes.orEmpty(), PendingChange.When.of(schedule), reminder?.toSecondOfDay()),
            done = R.string.rescheduled,
        )
    }

    fun setChecklistItemDone(itemId: String, done: Boolean) = change(PendingChange.SetChecklistItemDone(newId(), itemId, done))

    fun deleteChecklistItem(itemId: String) = change(PendingChange.DeleteChecklistItem(newId(), itemId))

    fun addChecklistItem(taskId: String, title: String) {
        if (title.isNotBlank()) change(PendingChange.AddChecklistItem(newId(), newId(), taskId, title))
    }

    /** Moves the to-do to the Trash; the snackbar offers to put it back. */
    fun trash(id: String) = change(
        PendingChange.SetTrashed(newId(), id, true),
        done = R.string.moved_to_trash,
        undo = PendingChange.SetTrashed(newId(), id, false),
    )

    fun restore(id: String) = change(PendingChange.SetTrashed(newId(), id, false), done = R.string.restored)

    /** The snackbar's Undo. */
    fun undo(change: PendingChange) = change(change)

    fun discardPending() {
        viewModelScope.launch {
            repository.discardPending()?.let(::show)
            requestSync()
        }
    }

    /** The app goes to the background: anything still queued is sent by WorkManager. */
    fun onAppStopped() {
        if (_state.value.pending > 0 && !_state.value.demo) BackgroundSync.sendSoon(getApplication())
    }

    fun handleLaunch(openAdd: Boolean, taskId: String?) =
        _state.update { it.copy(launchOpenAdd = openAdd, launchTaskId = taskId) }

    fun consumeLaunch() = _state.update { it.copy(launchOpenAdd = false, launchTaskId = null) }

    fun setThemeMode(mode: ThemeMode) {
        displayPrefs.edit().putString(KEY_THEME, mode.name).apply()
        _state.update { it.copy(themeMode = mode) }
    }

    fun setFilter(filter: TaskFilter) {
        displayPrefs.edit().putBoolean(KEY_SHOW_CLOSED, filter.showClosed).apply()
        _state.update { it.copy(filter = filter) }
    }

    fun signOut() {
        // The sync lock makes a sync still running finish before the cache is cleared.
        syncLoop?.cancel()
        syncLoop = null
        BackgroundSync.cancel(getApplication())
        _state.value = UiState()
        viewModelScope.launch {
            repository.signOut()
            runCatching { ListWidget.refreshAll(getApplication()) }
        }
    }

    private fun blockedInDemo(): Boolean {
        if (!_state.value.demo) return false
        _state.update { it.copy(message = UiMessage(R.string.demo_no_writes)) }
        return true
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    private companion object {
        const val SEND_DELAY_MS = 8_000L

        const val KEY_SHOW_CLOSED = "show_closed"
        const val KEY_THEME = "theme"
    }

    private fun messageText(e: Exception): String {
        val message = messageFor(e)
        val app = getApplication<Application>()
        return if (message.arg != null) app.getString(message.res, message.arg) else app.getString(message.res)
    }

    private fun messageFor(e: Exception): UiMessage = when (e) {
        is ThingsCloudException ->
            if (e.status == 401 || e.status == 403) UiMessage(R.string.error_wrong_credentials) else UiMessage(R.string.error_http, e.status)
        is IOException -> UiMessage(R.string.error_network)
        is NotSignedInException -> UiMessage(R.string.error_not_signed_in)
        is NoSuchElementException -> UiMessage(R.string.error_todo_gone)
        is IllegalArgumentException -> UiMessage(R.string.error_not_allowed)
        else -> UiMessage(R.string.error_generic, e.message ?: e.javaClass.simpleName)
    }
}
