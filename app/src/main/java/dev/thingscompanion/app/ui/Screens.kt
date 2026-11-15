package dev.thingscompanion.app.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberDatePickerState
import dev.thingscompanion.cloud.Schedule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.InputChip
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Switch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import dev.thingscompanion.app.R
import dev.thingscompanion.cloud.Task
import dev.thingscompanion.cloud.TaskFilter
import dev.thingscompanion.cloud.TaskStatus
import dev.thingscompanion.cloud.TaskType
import dev.thingscompanion.cloud.ThingsDay
import dev.thingscompanion.cloud.ThingsLibrary

enum class ListTab(@StringRes val label: Int) {
    TODAY(R.string.list_today),
    INBOX(R.string.list_inbox),
    UPCOMING(R.string.list_upcoming),
    ANYTIME(R.string.list_anytime),
    SOMEDAY(R.string.list_someday),
    PROJECTS(R.string.list_projects),
}

@Composable
fun ThingsApp(viewModel: ThingsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(state.message) {
        state.message?.let {
            viewModel.dismissMessage()
            snackbar.showSnackbar(if (it.arg != null) context.getString(it.res, it.arg) else context.getString(it.res))
        }
    }

    if (!state.signedIn) {
        LoginScreen(loading = state.loading, snackbar = snackbar, onSignIn = viewModel::signIn)
    } else {
        LibraryScreen(
            library = state.library,
            loading = state.loading,
            formDone = state.formDone,
            snackbar = snackbar,
            onRefresh = viewModel::refresh,
            onSignOut = viewModel::signOut,
            onAddTask = viewModel::addTask,
            onDiscardDraft = viewModel::discardDraft,
            onFormDoneShown = viewModel::consumeFormDone,
            onEditTask = viewModel::editTask,
            onSetDone = viewModel::setDone,
            filter = state.filter,
            onFilterChange = viewModel::setFilter,
            themeMode = state.themeMode,
            onThemeModeChange = viewModel::setThemeMode,
            demo = state.demo,
            onExitDemo = viewModel::exitDemo,
            launchOpenAdd = state.launchOpenAdd,
            launchTaskId = state.launchTaskId,
            onLaunchHandled = viewModel::consumeLaunch,
        )
    }
}

@Composable
private fun LoginScreen(loading: Boolean, snackbar: SnackbarHostState, onSignIn: (String, String) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.sign_in_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.sign_in_note),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onSignIn(email, password) },
                enabled = !loading && email.isNotBlank() && password.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(if (loading) R.string.signing_in else R.string.sign_in)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryScreen(
    library: ThingsLibrary?,
    loading: Boolean,
    formDone: Int?,
    snackbar: SnackbarHostState,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
    onAddTask: (String, String, Schedule, LocalTime?) -> Unit,
    onDiscardDraft: () -> Unit,
    onFormDoneShown: () -> Unit,
    onEditTask: (String, String, String, Schedule, LocalTime?) -> Unit,
    onSetDone: (String, Boolean) -> Unit,
    filter: TaskFilter,
    onFilterChange: (TaskFilter) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    demo: Boolean,
    onExitDemo: () -> Unit,
    launchOpenAdd: Boolean,
    launchTaskId: String?,
    onLaunchHandled: () -> Unit,
) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(ListTab.TODAY) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var filtering by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    if (settingsOpen) {
        SettingsDialog(
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            demo = demo,
            onExitDemo = {
                settingsOpen = false
                onExitDemo()
            },
            onSignOut = {
                settingsOpen = false
                onSignOut()
            },
            onDismiss = { settingsOpen = false },
        )
    }
    if (filtering && library != null) {
        FilterDialog(library, filter, onChange = onFilterChange, onDismiss = { filtering = false })
    }
    var projectId by rememberSaveable { mutableStateOf<String?>(null) }
    var taskId by rememberSaveable { mutableStateOf<String?>(null) }
    // Close a form only on a confirmed write; the event is consumed once and does not survive process death.
    // The snackbar runs in its own scope: consuming the event changes this effect's key and would cancel it.
    val scope = rememberCoroutineScope()
    LaunchedEffect(formDone) {
        if (formDone != null) {
            adding = false
            editing = false
            val text = context.getString(formDone)
            scope.launch { snackbar.showSnackbar(text) }
            onFormDoneShown()
        }
    }
    if (adding) {
        TaskFormDialog(
            initial = TaskDraft(),
            heading = R.string.new_todo_title,
            confirm = R.string.add,
            busy = R.string.adding,
            loading = loading,
            allowSchedule = true,
            allowInbox = true,
            onSubmit = onAddTask,
            onDismiss = {
                adding = false
                onDiscardDraft()
            },
        )
    }
    val editedTask = taskId?.let { library?.task(it) }
    if (editing && editedTask != null && library != null) {
        TaskFormDialog(
            initial = TaskDraft(editedTask.title, editedTask.notes.orEmpty(), Schedule.of(editedTask, ThingsDay.today()), editedTask.reminder),
            heading = R.string.edit_todo_title,
            confirm = R.string.save,
            busy = R.string.saving,
            loading = loading,
            allowSchedule = library.canReschedule(editedTask),
            allowInbox = editedTask.projectId == null && editedTask.areaId == null,
            onSubmit = { title, notes, schedule, reminder -> onEditTask(editedTask.id, title, notes, schedule, reminder) },
            onDismiss = { editing = false },
        )
    }
    // A widget tap asked to open the Inbox form or a to-do.
    LaunchedEffect(launchOpenAdd, launchTaskId) {
        if (launchOpenAdd) adding = true
        if (launchTaskId != null) taskId = launchTaskId
        if (launchOpenAdd || launchTaskId != null) onLaunchHandled()
    }
    val today = ThingsDay.today()

    BackHandler(enabled = taskId != null || projectId != null) {
        if (taskId != null) taskId = null else projectId = null
    }

    val openTask = taskId?.let { library?.task(it) }
    val openProject = projectId?.let { library?.task(it) }
    val title = openTask?.title ?: openProject?.title ?: stringResource(R.string.app_name)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (library != null && taskId == null) {
                FloatingActionButton(onClick = { adding = true }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_to_inbox))
                }
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (taskId != null || projectId != null) {
                        IconButton(onClick = { if (taskId != null) taskId = null else projectId = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    if (taskId != null && library?.task(taskId!!)?.type == TaskType.TODO) {
                        IconButton(onClick = { editing = true }, enabled = !loading) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit))
                        }
                    }
                    if (taskId == null) {
                        IconButton(onClick = { filtering = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = stringResource(R.string.filters),
                                tint = if (filter.isEmpty) LocalContentColor.current else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    IconButton(onClick = onRefresh, enabled = !loading) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                    IconButton(onClick = { settingsOpen = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            when {
                library == null -> Text(stringResource(R.string.loading_lists), Modifier.padding(16.dp))
                openTask != null -> TaskDetail(library, openTask, loading, onSetDone)
                openProject != null -> {
                    if (!filter.isEmpty) ActiveFilters(library, filter, onFilterChange)
                    TaskList(library, filter.apply(library.projectTasks(openProject.id, closedOn = today)), stringResource(R.string.no_open_todos), loading, onSetDone) {
                        taskId = it.id
                    }
                }
                else -> {
                    PrimaryScrollableTabRow(selectedTabIndex = tab.ordinal) {
                        ListTab.entries.forEach { entry ->
                            Tab(selected = tab == entry, onClick = { tab = entry }, text = { Text(stringResource(entry.label)) })
                        }
                    }
                    if (!filter.isEmpty && tab != ListTab.PROJECTS) ActiveFilters(library, filter, onFilterChange)
                    val listed = when (tab) {
                        // To-dos closed today stay in place, struck through, as in Things.
                        ListTab.TODAY -> library.today(today, closedOn = today)
                        ListTab.INBOX -> library.inbox(closedOn = today)
                        ListTab.UPCOMING -> library.upcoming(today, closedOn = today)
                        ListTab.ANYTIME -> library.anytime(today, closedOn = today)
                        ListTab.SOMEDAY -> library.someday(closedOn = today)
                        ListTab.PROJECTS -> library.projects()
                    }
                    val tasks = if (tab == ListTab.PROJECTS) listed else filter.apply(listed)
                    TaskList(library, tasks, stringResource(R.string.nothing_here), loading, onSetDone) { task ->
                        if (tab == ListTab.PROJECTS) projectId = task.id else taskId = task.id
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskList(
    library: ThingsLibrary,
    tasks: List<Task>,
    emptyText: String,
    loading: Boolean,
    onSetDone: (String, Boolean) -> Unit,
    onOpen: (Task) -> Unit,
) {
    if (tasks.isEmpty()) {
        Text(emptyText, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(tasks, key = { it.id }) { task ->
            ListItem(
                headlineContent = {
                    Text(
                        task.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.isOpen) null else TextDecoration.LineThrough,
                        color = if (task.isOpen) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                supportingContent = subtitle(LocalContext.current, library, task)?.let { text -> { Text(text, maxLines = 1) } },
                leadingContent = if (task.type == TaskType.TODO) {
                    {
                        Checkbox(
                            checked = task.status == TaskStatus.COMPLETED,
                            onCheckedChange = { onSetDone(task.id, it) },
                            enabled = !loading && library.canComplete(task),
                        )
                    }
                } else null,
                modifier = Modifier.clickable { onOpen(task) },
            )
            HorizontalDivider()
        }
    }
}

private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

private fun subtitle(context: Context, library: ThingsLibrary, task: Task): String? = listOfNotNull(
    task.projectId?.let { library.task(it)?.title } ?: task.areaId?.let { library.area(it)?.title },
    task.startDate?.let { context.getString(R.string.when_date, it.format(dateFormat)) },
    task.reminder?.let { context.getString(R.string.reminder_at, it.format(timeFormat)) },
    task.deadline?.let { context.getString(R.string.due_date, it.format(dateFormat)) },
    if (task.evening) context.getString(R.string.this_evening) else null,
    if (task.notes != null) context.getString(R.string.notes) else null,
).joinToString(" · ").ifEmpty { null }

@Composable
private fun TaskDetail(library: ThingsLibrary, task: Task, loading: Boolean, onSetDone: (String, Boolean) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (task.type == TaskType.TODO) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = task.status == TaskStatus.COMPLETED,
                    onCheckedChange = { onSetDone(task.id, it) },
                    enabled = !loading && library.canComplete(task),
                )
                Text(
                    stringResource(if (library.canComplete(task)) R.string.completed else R.string.repeating_locked),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        subtitle(LocalContext.current, library, task)?.let { Text(it, style = MaterialTheme.typography.labelLarge) }
        library.tagTitles(task).takeIf { it.isNotEmpty() }?.let {
            Text(it.joinToString("  ") { tag -> "#$tag" }, style = MaterialTheme.typography.labelMedium)
        }
        val checklist = library.checklist(task.id)
        if (checklist.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                checklist.forEach { item -> Text("${if (item.done) "☑" else "☐"}  ${item.title}") }
            }
        }
        SelectionContainer {
            Text(task.notes ?: stringResource(R.string.no_notes), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** Values of the to-do form: a new to-do, or the current values of the one being edited. */
data class TaskDraft(
    val title: String = "",
    val notes: String = "",
    val schedule: Schedule = Schedule.Inbox,
    val reminder: LocalTime? = null,
)

/**
 * Form shared by Add and Edit. [allowSchedule] is false for repeating to-dos (When and reminder
 * stay as Things manages them); [allowInbox] is false for to-dos filed in a project or area.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskFormDialog(
    initial: TaskDraft,
    @StringRes heading: Int,
    @StringRes confirm: Int,
    @StringRes busy: Int,
    loading: Boolean,
    allowSchedule: Boolean,
    allowInbox: Boolean,
    onSubmit: (String, String, Schedule, LocalTime?) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = ThingsDay.today()
    var title by rememberSaveable { mutableStateOf(initial.title) }
    var notes by rememberSaveable { mutableStateOf(initial.notes) }
    var whenChoice by rememberSaveable { mutableStateOf(WhenChoice.of(initial.schedule)) }
    // The day of the initial schedule: kept when the choice is unchanged (an overdue to-do stays on its day).
    val initialDay = initial.schedule.date?.toEpochDay()
    var pickedDay by rememberSaveable { mutableStateOf(initialDay) }
    var pickingDate by remember { mutableStateOf(false) }
    var reminderMinute by rememberSaveable { mutableStateOf(initial.reminder?.let { it.hour * 60 + it.minute }) }
    var pickingTime by remember { mutableStateOf(false) }

    if (pickingTime) {
        TimePickerDialog(
            initial = reminderMinute?.let { LocalTime.of(it / 60, it % 60) } ?: LocalTime.of(9, 0),
            onPicked = {
                pickingTime = false
                reminderMinute = it.hour * 60 + it.minute
            },
            onDismiss = { pickingTime = false },
        )
    }

    if (pickingDate) {
        DayPickerDialog(
            today = today,
            onPicked = { day ->
                pickingDate = false
                // Today picked in the calendar means Today.
                if (day == today) {
                    whenChoice = WhenChoice.TODAY
                    pickedDay = today.toEpochDay()
                } else {
                    whenChoice = WhenChoice.DATE
                    pickedDay = day.toEpochDay()
                }
            },
            onDismiss = { pickingDate = false },
        )
    }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(stringResource(heading)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.title)) },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes)) },
                    minLines = 3,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (allowSchedule) {
                    Text(stringResource(R.string.when_label), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WhenChoice.entries.filter { allowInbox || it != WhenChoice.INBOX }.forEach { choice ->
                            val label = when (choice) {
                                WhenChoice.DATE -> pickedDay?.takeIf { whenChoice == WhenChoice.DATE }
                                    ?.let { LocalDate.ofEpochDay(it).format(dateFormat) } ?: stringResource(R.string.pick_date)
                                else -> stringResource(choice.label)
                            }
                            FilterChip(
                                selected = whenChoice == choice,
                                enabled = !loading,
                                onClick = { if (choice == WhenChoice.DATE) pickingDate = true else whenChoice = choice },
                                label = { Text(label) },
                            )
                        }
                    }
                    // A reminder needs a day: only offered once Today, This Evening or a date is chosen.
                    if (whenChoice.hasDay) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = reminderMinute != null,
                                enabled = !loading,
                                onClick = { pickingTime = true },
                                label = {
                                    Text(
                                        reminderMinute?.let {
                                            stringResource(R.string.reminder) + " " + LocalTime.of(it / 60, it % 60).format(timeFormat)
                                        } ?: stringResource(R.string.reminder),
                                    )
                                },
                            )
                            if (reminderMinute != null) {
                                FilterChip(
                                    selected = false,
                                    enabled = !loading,
                                    onClick = { reminderMinute = null },
                                    label = { Text(stringResource(R.string.no_reminder)) },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val schedule = if (!allowSchedule) initial.schedule else {
                // Today/This Evening keep the to-do's own day if it already had one on or before today.
                val keptDay = pickedDay?.let(LocalDate::ofEpochDay)?.takeIf { !it.isAfter(today) } ?: today
                when (whenChoice) {
                    WhenChoice.INBOX -> Schedule.Inbox
                    WhenChoice.TODAY -> Schedule.Today(keptDay)
                    WhenChoice.EVENING -> Schedule.Evening(keptDay)
                    WhenChoice.DATE -> pickedDay?.let { Schedule.OnDate(LocalDate.ofEpochDay(it)) } ?: Schedule.Inbox
                    WhenChoice.ANYTIME -> Schedule.Anytime
                    WhenChoice.SOMEDAY -> Schedule.Someday
                }
            }
            val reminder = if (!allowSchedule) initial.reminder else
                reminderMinute?.takeIf { schedule.date != null }?.let { LocalTime.of(it / 60, it % 60) }
            TextButton(onClick = { onSubmit(title, notes, schedule, reminder) }, enabled = !loading && title.isNotBlank()) {
                Text(stringResource(if (loading) busy else confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private enum class WhenChoice(@StringRes val label: Int, val hasDay: Boolean) {
    INBOX(R.string.list_inbox, false),
    TODAY(R.string.list_today, true),
    EVENING(R.string.this_evening, true),
    DATE(R.string.pick_date, true),
    ANYTIME(R.string.list_anytime, false),
    SOMEDAY(R.string.list_someday, false);

    companion object {
        fun of(schedule: Schedule) = when (schedule) {
            Schedule.Inbox -> INBOX
            is Schedule.Today -> TODAY
            is Schedule.Evening -> EVENING
            is Schedule.OnDate -> DATE
            Schedule.Anytime -> ANYTIME
            Schedule.Someday -> SOMEDAY
        }
    }
}

/** Calendar returning the picked calendar day; limited to today and later unless [futureOnly] is false. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPickerDialog(today: LocalDate, onPicked: (LocalDate) -> Unit, onDismiss: () -> Unit, futureOnly: Boolean = true) {
    val todayUtcMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = if (futureOnly) todayUtcMillis + 86_400_000L else todayUtcMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = !futureOnly || utcTimeMillis >= todayUtcMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                },
                enabled = state.selectedDateMillis != null,
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(initial: LocalTime, onPicked: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onPicked(LocalTime.of(state.hour, state.minute)) }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Show-completed option and display filters: tags (any), one project or area, one day. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterDialog(library: ThingsLibrary, filter: TaskFilter, onChange: (TaskFilter) -> Unit, onDismiss: () -> Unit) {
    var pickingDay by remember { mutableStateOf(false) }
    if (pickingDay) {
        DayPickerDialog(
            today = ThingsDay.today(),
            futureOnly = false,
            onPicked = {
                pickingDay = false
                onChange(filter.copy(day = it))
            },
            onDismiss = { pickingDay = false },
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filters)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.show_completed), Modifier.weight(1f))
                    Switch(checked = filter.showClosed, onCheckedChange = { onChange(filter.copy(showClosed = it)) })
                }
                Text(stringResource(R.string.date), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filter.day != null,
                        onClick = { pickingDay = true },
                        label = { Text(filter.day?.format(dateFormat) ?: stringResource(R.string.pick_date)) },
                    )
                    if (filter.day != null) {
                        FilterChip(selected = false, onClick = { onChange(filter.copy(day = null)) }, label = { Text(stringResource(R.string.any_date)) })
                    }
                }
                val tags = library.tags()
                if (tags.isNotEmpty()) {
                    Text(stringResource(R.string.tags), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.forEach { tag ->
                            val selected = tag.id in filter.tagIds
                            FilterChip(
                                selected = selected,
                                onClick = { onChange(filter.copy(tagIds = if (selected) filter.tagIds - tag.id else filter.tagIds + tag.id)) },
                                label = { Text(tag.title) },
                            )
                        }
                    }
                }
                Text(stringResource(R.string.project_or_area), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (library.areas().map { it.id to it.title } + library.projects().map { it.id to it.title }).forEach { (id, title) ->
                        val selected = filter.containerId == id
                        FilterChip(
                            selected = selected,
                            onClick = { onChange(filter.copy(containerId = if (selected) null else id)) },
                            label = { Text(title) },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
        dismissButton = {
            TextButton(onClick = { onChange(TaskFilter(showClosed = filter.showClosed)) }, enabled = !filter.isEmpty) {
                Text(stringResource(R.string.clear_filters))
            }
        },
    )
}

/** Active criteria under the tabs; tapping one removes it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveFilters(library: ThingsLibrary, filter: TaskFilter, onChange: (TaskFilter) -> Unit) {
    FlowRow(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        filter.day?.let { day ->
            InputChip(selected = true, onClick = { onChange(filter.copy(day = null)) }, label = { Text(day.format(dateFormat) + "  ✕") })
        }
        filter.containerId?.let { id ->
            val title = library.area(id)?.title ?: library.task(id)?.title ?: id
            InputChip(selected = true, onClick = { onChange(filter.copy(containerId = null)) }, label = { Text("$title  ✕") })
        }
        library.tags().filter { it.id in filter.tagIds }.forEach { tag ->
            InputChip(selected = true, onClick = { onChange(filter.copy(tagIds = filter.tagIds - tag.id)) }, label = { Text("#${tag.title}  ✕") })
        }
    }
}

@Composable
private fun SettingsDialog(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    demo: Boolean,
    onExitDemo: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.app_theme), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            label = {
                                Text(
                                    stringResource(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> R.string.theme_system
                                            ThemeMode.LIGHT -> R.string.theme_light
                                            ThemeMode.DARK -> R.string.theme_dark
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
                HorizontalDivider()
                if (demo) {
                    TextButton(onClick = onExitDemo) { Text(stringResource(R.string.exit_demo)) }
                } else {
                    TextButton(onClick = onSignOut) { Text(stringResource(R.string.sign_out)) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
    )
}
