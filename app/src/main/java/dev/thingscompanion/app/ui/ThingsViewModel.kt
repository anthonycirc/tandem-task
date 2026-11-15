package dev.thingscompanion.app.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.thingscompanion.app.R
import dev.thingscompanion.app.data.NotSignedInException
import dev.thingscompanion.app.data.ThingsRepository
import dev.thingscompanion.app.sync.BackgroundSync
import dev.thingscompanion.app.widget.ListWidget
import dev.thingscompanion.app.data.writeWidgetSnapshot
import dev.thingscompanion.cloud.DemoData
import dev.thingscompanion.cloud.EarlierAttemptLandedException
import dev.thingscompanion.cloud.Schedule
import dev.thingscompanion.cloud.TaskFilter
import dev.thingscompanion.cloud.TaskNotConfirmedException
import dev.thingscompanion.cloud.ThingsCloudException
import dev.thingscompanion.cloud.ThingsIds
import dev.thingscompanion.cloud.ThingsDay
import dev.thingscompanion.cloud.ThingsLibrary
import dev.thingscompanion.cloud.WidgetSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalTime

/** A message for the snackbar, resolved against the app's (localized) resources. */
data class UiMessage(@StringRes val res: Int, val arg: Any? = null)

data class UiState(
    val signedIn: Boolean = false,
    val library: ThingsLibrary? = null,
    val loading: Boolean = false,
    val message: UiMessage? = null,
    /** Set once an add or edit is confirmed (the message to show), until the screen consumes it. Lost with the process, never replayed. */
    @StringRes val formDone: Int? = null,
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
    private var loadJob: Job? = null

    /** Initial cache load; cancelled when the demo starts so real data never replaces the demo. */
    private var startupJob: Job? = null

    init {
        if (repository.isSignedIn) {
            BackgroundSync.schedule(application)
            startupJob = viewModelScope.launch {
                val cached = repository.cached()
                if (!_state.value.demo) {
                    _state.update { it.copy(library = cached) }
                    refresh()
                }
            }
        }
    }

    fun signIn(email: String, password: String) = load {
        val library = repository.signIn(email, password)
        BackgroundSync.schedule(getApplication())
        _state.update { it.copy(signedIn = true, library = library) }
    }

    fun enterDemo() {
        startupJob?.cancel()
        loadJob?.cancel()
        val library = ThingsLibrary.from(DemoData.state(ThingsDay.today(), System.currentTimeMillis() / 1000.0))
        _state.update { it.copy(signedIn = true, demo = true, library = library, loading = false) }
        // The widget shows the demo lists too, until the next real sync.
        viewModelScope.launch {
            writeWidgetSnapshot(getApplication(), WidgetSnapshot.from(library, ThingsDay.today(), System.currentTimeMillis()))
            runCatching { ListWidget.refreshAll(getApplication()) }
        }
    }

    fun exitDemo() {
        _state.update { it.copy(demo = false, signedIn = repository.isSignedIn, library = null) }
        if (repository.isSignedIn) {
            viewModelScope.launch {
                _state.update { it.copy(library = repository.cached()) }
                refresh()
            }
        }
    }

    fun refresh() = if (_state.value.demo) Unit else load {
        val library = repository.refresh()
        _state.update { it.copy(library = library) }
    }

    /** One id per draft: a retry after a failure reuses it, so the to-do can't be created twice. */
    private var draftId: String? = null

    /** An attempt for [draftId] failed or was interrupted, so it may still have reached Things. */
    private var draftFailed = false

    fun addTask(title: String, notes: String, schedule: Schedule, reminder: LocalTime?) {
        if (blockedInDemo()) return
        val id = draftId ?: ThingsIds.random().also { draftId = it }
        load {
            try {
                val library = repository.addTask(id, title, notes, schedule, reminder)
                draftId = null
                draftFailed = false
                // Name the Inbox when that is where it went; elsewhere (Today, a date…) just confirm.
                val done = if (schedule == Schedule.Inbox) R.string.added_to_inbox else R.string.added
                _state.update { it.copy(library = library, formDone = done) }
            } catch (e: EarlierAttemptLandedException) {
                // That to-do exists; the next Add is a new one.
                draftId = null
                draftFailed = false
                throw e
            } catch (e: Throwable) {
                draftFailed = true
                throw e
            }
        }
    }

    /**
     * The user cancels the form. If an attempt failed, it may have landed anyway: check, and say
     * so, rather than letting the user re-enter it and create a duplicate.
     */
    fun discardDraft() {
        val id = draftId
        val failed = draftFailed
        draftId = null
        draftFailed = false
        if (id != null && failed) {
            load {
                val landed = try {
                    repository.landed(id)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Can't tell (e.g. offline): warn before the user re-enters it and makes a duplicate.
                    _state.update { it.copy(message = UiMessage(R.string.earlier_attempt_unknown)) }
                    return@load
                }
                if (landed) _state.update { it.copy(message = UiMessage(R.string.earlier_attempt_landed)) }
                _state.update { it.copy(library = repository.cached()) }
            }
        }
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

    fun consumeFormDone() = _state.update { it.copy(formDone = null) }

    fun editTask(id: String, title: String, notes: String, schedule: Schedule, reminder: LocalTime?) = if (blockedInDemo()) Unit else load {
        val library = repository.editTask(id, title, notes, schedule, reminder)
        _state.update { it.copy(library = library, formDone = R.string.saved) }
    }

    fun setDone(id: String, done: Boolean) = if (blockedInDemo()) Unit else load {
        val library = repository.completeTask(id, done)
        _state.update { it.copy(library = library, message = UiMessage(if (done) R.string.marked_done else R.string.marked_open)) }
    }

    fun signOut() {
        // A sync still running would write the cache back after it is cleared.
        loadJob?.cancel()
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

    private fun load(block: suspend () -> Unit) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            try {
                block()
                // A widget that fails to redraw must not turn a good sync into an error.
                runCatching { ListWidget.refreshAll(getApplication()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(message = messageFor(e)) }
            } finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    private companion object {
        const val KEY_SHOW_CLOSED = "show_closed"
        const val KEY_THEME = "theme"
    }

    private fun messageFor(e: Exception): UiMessage = when (e) {
        is ThingsCloudException ->
            if (e.status == 401 || e.status == 403) UiMessage(R.string.error_wrong_credentials) else UiMessage(R.string.error_http, e.status)
        is TaskNotConfirmedException -> UiMessage(R.string.error_not_confirmed)
        is EarlierAttemptLandedException -> UiMessage(R.string.error_earlier_attempt, e.savedTitle)
        is IOException -> UiMessage(R.string.error_network)
        is NotSignedInException -> UiMessage(R.string.error_not_signed_in)
        is NoSuchElementException -> UiMessage(R.string.error_todo_gone)
        is IllegalArgumentException -> UiMessage(R.string.error_not_allowed)
        else -> UiMessage(R.string.error_generic, e.message ?: e.javaClass.simpleName)
    }
}
