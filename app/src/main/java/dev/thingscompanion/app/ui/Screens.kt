package dev.thingscompanion.app.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.thingscompanion.app.R
import dev.thingscompanion.cloud.Schedule
import dev.thingscompanion.cloud.Task
import dev.thingscompanion.cloud.TaskFilter
import dev.thingscompanion.cloud.TaskStatus
import dev.thingscompanion.cloud.TaskType
import dev.thingscompanion.cloud.ThingsDay
import dev.thingscompanion.cloud.ThingsLibrary
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class ListTab(@StringRes val label: Int, val kind: ListKind) {
    TODAY(R.string.list_today, ListKind.TODAY),
    INBOX(R.string.list_inbox, ListKind.INBOX),
    UPCOMING(R.string.list_upcoming, ListKind.UPCOMING),
    ANYTIME(R.string.list_anytime, ListKind.ANYTIME),
    SOMEDAY(R.string.list_someday, ListKind.SOMEDAY),
    PROJECTS(R.string.list_projects, ListKind.PROJECT),
    TRASH(R.string.list_trash, ListKind.TRASH),
}

@Composable
fun ThingsApp(viewModel: ThingsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(state.message) {
        state.message?.let {
            viewModel.dismissMessage()
            val text = if (it.arg != null) context.getString(it.res, it.arg) else context.getString(it.res)
            val result = snackbar.showSnackbar(text, actionLabel = it.undo?.let { context.getString(R.string.undo) }, withDismissAction = false)
            if (result == SnackbarResult.ActionPerformed) it.undo?.let(viewModel::undo)
        }
    }

    if (!state.signedIn) {
        LoginScreen(loading = state.loading, snackbar = snackbar, onSignIn = viewModel::signIn)
    } else {
        LibraryScreen(state, viewModel, snackbar)
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
            Text(stringResource(R.string.sign_in_note), style = MaterialTheme.typography.bodyMedium)
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
private fun LibraryScreen(state: UiState, viewModel: ThingsViewModel, snackbar: SnackbarHostState) {
    val library = state.library
    val filter = state.filter
    var tab by rememberSaveable { mutableStateOf(ListTab.TODAY) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var filtering by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var projectId by rememberSaveable { mutableStateOf<String?>(null) }
    var taskId by rememberSaveable { mutableStateOf<String?>(null) }
    var schedulingId by rememberSaveable { mutableStateOf<String?>(null) }

    // A widget tap asked to open the add form or a to-do.
    LaunchedEffect(state.launchOpenAdd, state.launchTaskId) {
        if (state.launchOpenAdd) adding = true
        if (state.launchTaskId != null) taskId = state.launchTaskId
        if (state.launchOpenAdd || state.launchTaskId != null) viewModel.consumeLaunch()
    }

    // The forms take the whole screen: Save stays in the top bar, above the keyboard.
    if (adding) {
        TaskFormScreen(
            initial = TaskDraft(schedule = if (tab == ListTab.TODAY) Schedule.Today(ThingsDay.today()) else Schedule.Inbox),
            heading = R.string.new_todo_title,
            confirm = R.string.add,
            allowSchedule = true,
            allowInbox = true,
            allowChecklist = true,
            onSubmit = { title, notes, schedule, reminder, checklist ->
                adding = false
                viewModel.addTask(title, notes, schedule, reminder, checklist)
            },
            onDismiss = { adding = false },
        )
        return
    }
    val edited = editingId?.let { library?.task(it) }
    if (edited != null && library != null) {
        TaskFormScreen(
            initial = TaskDraft(
                edited.title,
                edited.notes.orEmpty(),
                Schedule.of(edited, ThingsDay.today()),
                edited.reminder,
                library.checklist(edited.id).map { ChecklistDraft(it.id, it.title, it.done) },
            ),
            heading = R.string.edit_todo_title,
            confirm = R.string.save,
            allowSchedule = library.canReschedule(edited),
            allowInbox = edited.projectId == null && edited.areaId == null,
            allowChecklist = library.canEditChecklist(edited),
            onSubmit = { title, notes, schedule, reminder, checklist ->
                editingId = null
                viewModel.editTask(edited.id, title, notes, schedule, reminder, checklist.takeIf { library.canEditChecklist(edited) })
            },
            onDismiss = { editingId = null },
        )
        return
    }

    if (settingsOpen) {
        SettingsDialog(
            themeMode = state.themeMode,
            onThemeModeChange = viewModel::setThemeMode,
            pending = state.pending,
            onDiscardPending = viewModel::discardPending,
            demo = state.demo,
            onExitDemo = {
                settingsOpen = false
                viewModel.exitDemo()
            },
            onSignOut = {
                settingsOpen = false
                viewModel.signOut()
            },
            onDismiss = { settingsOpen = false },
        )
    }
    if (filtering && library != null) {
        FilterDialog(library, filter, onChange = viewModel::setFilter, onDismiss = { filtering = false })
    }

    BackHandler(enabled = taskId != null || projectId != null) {
        if (taskId != null) taskId = null else projectId = null
    }

    val today = ThingsDay.today()
    val openTask = taskId?.let { library?.task(it) }
    val openProject = projectId?.let { library?.task(it) }
    val rowActions = RowActions(
        onOpen = { taskId = it.id },
        onSetDone = viewModel::setDone,
        onTrash = viewModel::trash,
        onRestore = viewModel::restore,
        onReschedule = { schedulingId = it.id },
    )
    val scheduling = schedulingId?.let { library?.task(it) }
    if (scheduling != null) {
        WhenDialog(
            task = scheduling,
            onPick = {
                schedulingId = null
                viewModel.reschedule(scheduling.id, it)
            },
            onDismiss = { schedulingId = null },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (library != null && taskId == null && tab != ListTab.TRASH) {
                FloatingActionButton(onClick = { adding = true }, containerColor = ListColors.checked, contentColor = Color.White) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_to_inbox))
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    if (taskId == null && projectId == null) {
                        Text(stringResource(R.string.app_name), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    if (taskId != null || projectId != null) {
                        IconButton(onClick = { if (taskId != null) taskId = null else projectId = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    if (state.pending > 0) {
                        TextButton(onClick = viewModel::refresh) {
                            Text(stringResource(R.string.pending_count, state.pending), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (openTask != null && library != null) {
                        if (openTask.type == TaskType.TODO && !openTask.trashed) {
                            IconButton(onClick = { editingId = openTask.id }) {
                                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit))
                            }
                        }
                        if (library.canTrash(openTask)) {
                            IconButton(onClick = {
                                taskId = null
                                viewModel.trash(openTask.id)
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.move_to_trash))
                            }
                        }
                    } else {
                        IconButton(onClick = { filtering = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = stringResource(R.string.filters),
                                tint = if (filter.isEmpty) LocalContentColor.current else MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh))
                        }
                        IconButton(onClick = { settingsOpen = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // A thin bar while syncing in the background; the lists stay usable.
            if (state.syncing) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp)) else Spacer(Modifier.height(2.dp))
            when {
                library == null -> Text(stringResource(R.string.loading_lists), Modifier.padding(16.dp))
                openTask != null -> TaskDetail(
                    library = library,
                    task = openTask,
                    onSetDone = viewModel::setDone,
                    onChecklistDone = viewModel::setChecklistItemDone,
                    onAddChecklistItem = viewModel::addChecklistItem,
                    onRestore = viewModel::restore,
                )
                openProject != null -> {
                    if (!filter.isEmpty) ActiveFilters(library, filter, viewModel::setFilter)
                    TaskList(
                        library = library,
                        header = { ListHeader(ListKind.PROJECT, openProject.title) },
                        groups = listOf(Group.None to filter.apply(library.projectTasks(openProject.id, closedOn = today))),
                        emptyText = stringResource(R.string.no_open_todos),
                        actions = rowActions,
                    )
                }
                else -> {
                    PrimaryScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = 8.dp) {
                        ListTab.entries.forEach { entry ->
                            Tab(
                                selected = tab == entry,
                                onClick = { tab = entry },
                                icon = { ListIcon(entry.kind, size = 20.dp) },
                                text = { Text(stringResource(entry.label), maxLines = 1) },
                            )
                        }
                    }
                    if (!filter.isEmpty && tab != ListTab.PROJECTS && tab != ListTab.TRASH) ActiveFilters(library, filter, viewModel::setFilter)
                    val title = stringResource(tab.label)
                    when (tab) {
                        ListTab.PROJECTS -> ProjectList(library, title) { projectId = it.id }
                        ListTab.TRASH -> TaskList(
                            library = library,
                            header = { ListHeader(ListKind.TRASH, title) },
                            groups = listOf(Group.None to library.trash()),
                            emptyText = stringResource(R.string.trash_empty),
                            actions = rowActions,
                        )
                        else -> {
                            val listed = when (tab) {
                                // To-dos closed today stay in place, struck through, as in Things.
                                ListTab.TODAY -> library.today(today, closedOn = today)
                                ListTab.INBOX -> library.inbox(closedOn = today)
                                ListTab.UPCOMING -> library.upcoming(today, closedOn = today)
                                ListTab.ANYTIME -> library.anytime(today, closedOn = today)
                                else -> library.someday(closedOn = today)
                            }
                            val tasks = filter.apply(listed)
                            val groups = when (tab) {
                                ListTab.TODAY -> {
                                    val (evening, day) = tasks.partition { it.evening }
                                    listOf(Group.None to day, Group.Evening to evening)
                                }
                                ListTab.UPCOMING -> tasks.groupBy { it.startDate }.map { (date, items) ->
                                    (date?.let(Group::Day) ?: Group.None) to items
                                }
                                else -> listOf(Group.None to tasks)
                            }
                            TaskList(
                                library = library,
                                header = { ListHeader(tab.kind, title) },
                                groups = groups,
                                emptyText = stringResource(R.string.nothing_here),
                                actions = rowActions,
                                showWhen = tab != ListTab.TODAY && tab != ListTab.UPCOMING,
                            )
                        }
                    }
                }
            }
        }
    }
}

private class RowActions(
    val onOpen: (Task) -> Unit,
    val onSetDone: (String, Boolean) -> Unit,
    val onTrash: (String) -> Unit,
    val onRestore: (String) -> Unit,
    val onReschedule: (Task) -> Unit,
)

/** A section of a list: Today's This Evening, a day of Upcoming, or none. */
private sealed interface Group {
    data object None : Group
    data object Evening : Group
    data class Day(val date: LocalDate) : Group
}

/** The list's icon and name in large type, as Things opens each list. */
@Composable
private fun ListHeader(kind: ListKind, title: String) {
    Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        ListIcon(kind, size = 28.dp)
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun GroupHeader(group: Group) {
    when (group) {
        Group.None -> Unit
        Group.Evening -> Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ListIcon(ListKind.EVENING, size = 18.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.this_evening), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ListColors.evening)
            }
            HorizontalDivider(Modifier.padding(top = 6.dp))
        }
        is Group.Day -> Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(group.date.dayOfMonth.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(dayLabel(group.date), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun dayLabel(date: LocalDate): String {
    val today = ThingsDay.today()
    return when (date) {
        today.plusDays(1) -> stringResource(R.string.tomorrow)
        else -> {
            val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            if (date.isBefore(today.plusDays(7))) weekday else "$weekday · ${date.format(DateTimeFormatter.ofPattern("MMM yyyy"))}"
        }
    }.replaceFirstChar { it.titlecase() }
}

@Composable
private fun TaskList(
    library: ThingsLibrary,
    header: @Composable () -> Unit,
    groups: List<Pair<Group, List<Task>>>,
    emptyText: String,
    actions: RowActions,
    showWhen: Boolean = true,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "header") { header() }
        if (groups.all { it.second.isEmpty() }) {
            item(key = "empty") {
                Text(emptyText, Modifier.padding(20.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        groups.forEachIndexed { index, (group, tasks) ->
            if (tasks.isNotEmpty()) {
                item(key = "group-$index") { GroupHeader(group) }
                taskRows(library, tasks, actions, showWhen)
            }
        }
        // Room for the add button over the last row.
        item(key = "footer") { Spacer(Modifier.height(88.dp)) }
    }
}

private fun LazyListScope.taskRows(library: ThingsLibrary, tasks: List<Task>, actions: RowActions, showWhen: Boolean) {
    items(tasks, key = { it.id }) { task ->
        SwipeActions(
            canReschedule = library.canReschedule(task) && task.isOpen,
            canTrash = library.canTrash(task),
            onReschedule = { actions.onReschedule(task) },
            onTrash = { actions.onTrash(task.id) },
        ) {
            TaskRow(library, task, actions, showWhen)
        }
    }
}

/**
 * Row swipes: to the left opens When to reschedule (the more frequent action gets the easier
 * gesture), to the right moves the to-do to the Trash (the snackbar offers Undo).
 */
@Composable
private fun SwipeActions(
    canReschedule: Boolean,
    canTrash: Boolean,
    onReschedule: () -> Unit,
    onTrash: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (!canReschedule && !canTrash) {
        content()
        return
    }
    // A row that stays (the change was refused, or demo mode) gets a fresh swipe state: reset() leaves
    // material3 1.3 half-dismissed on the next tap.
    var generation by remember { mutableIntStateOf(0) }
    val swipe = key(generation) {
        rememberSwipeToDismissBoxState(
            confirmValueChange = {
                when (it) {
                    // Reschedule opens a picker: the row slides back.
                    SwipeToDismissBoxValue.EndToStart -> {
                        onReschedule()
                        false
                    }
                    SwipeToDismissBoxValue.StartToEnd -> {
                        onTrash()
                        true
                    }
                    SwipeToDismissBoxValue.Settled -> true
                }
            },
        )
    }
    LaunchedEffect(swipe.currentValue) {
        if (swipe.currentValue != SwipeToDismissBoxValue.Settled) {
            delay(1500)
            generation++
        }
    }
    SwipeToDismissBox(
        state = swipe,
        enableDismissFromStartToEnd = canTrash,
        enableDismissFromEndToStart = canReschedule,
        backgroundContent = {
            val trashing = swipe.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            Box(
                Modifier.fillMaxSize()
                    .background(if (trashing) ListColors.deadline else ListColors.today)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (trashing) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                if (trashing) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.move_to_trash), tint = Color.White)
                } else {
                    Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.reschedule), tint = Color.White)
                }
            }
        },
    ) { content() }
}

/** Things' When menu: pick where the to-do goes; a reminder is kept when the new choice has a day. */
@Composable
private fun WhenDialog(task: Task, onPick: (Schedule) -> Unit, onDismiss: () -> Unit) {
    val today = ThingsDay.today()
    var pickingDate by remember { mutableStateOf(false) }
    if (pickingDate) {
        DayPickerDialog(
            today = today,
            onPicked = {
                pickingDate = false
                onPick(if (it == today) Schedule.Today(today) else Schedule.OnDate(it))
            },
            onDismiss = { pickingDate = false },
        )
        return
    }
    val choices = buildList {
        add(Triple(ListKind.TODAY, stringResource(R.string.list_today)) { onPick(Schedule.Today(today)) })
        add(Triple(ListKind.EVENING, stringResource(R.string.this_evening)) { onPick(Schedule.Evening(today)) })
        add(Triple(ListKind.UPCOMING, stringResource(R.string.tomorrow)) { onPick(Schedule.OnDate(today.plusDays(1))) })
        add(Triple(ListKind.UPCOMING, stringResource(R.string.pick_date)) { pickingDate = true })
        add(Triple(ListKind.ANYTIME, stringResource(R.string.list_anytime)) { onPick(Schedule.Anytime) })
        add(Triple(ListKind.SOMEDAY, stringResource(R.string.list_someday)) { onPick(Schedule.Someday) })
        if (task.projectId == null && task.areaId == null) add(Triple(ListKind.INBOX, stringResource(R.string.list_inbox)) { onPick(Schedule.Inbox) })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.when_label)) },
        text = {
            Column {
                Text(task.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                choices.forEach { (kind, label, action) ->
                    Row(
                        Modifier.fillMaxWidth().clickable(onClick = action).padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ListIcon(kind, size = 20.dp)
                        Spacer(Modifier.width(14.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskRow(library: ThingsLibrary, task: Task, actions: RowActions, showWhen: Boolean) {
    val closed = !task.isOpen
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable { actions.onOpen(task) }
            .padding(start = 8.dp, end = 16.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThingsCheckbox(
            checked = task.status == TaskStatus.COMPLETED,
            onCheckedChange = { actions.onSetDone(task.id, it) },
            enabled = library.canComplete(task),
        )
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(
                task.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (closed) TextDecoration.LineThrough else null,
                color = if (closed || task.trashed) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
            )
            RowDetails(library, task, showWhen)
        }
        if (task.trashed && library.canRestore(task)) {
            TextButton(onClick = { actions.onRestore(task.id) }) { Text(stringResource(R.string.restore)) }
        }
    }
}

/** The grey line under a title: container, when, reminder, deadline, notes and checklist marks, tags. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RowDetails(library: ThingsLibrary, task: Task, showWhen: Boolean) {
    val container = task.projectId?.let { library.task(it)?.title } ?: task.areaId?.let { library.area(it)?.title }
    val checklist = library.checklist(task.id)
    val tags = library.tagTitles(task)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val small = MaterialTheme.typography.labelMedium
    val hasAny = container != null || task.isRepeating || (showWhen && task.startDate != null) || task.reminder != null ||
        task.deadline != null || task.notes != null || checklist.isNotEmpty() || tags.isNotEmpty()
    if (!hasAny) return
    FlowRow(
        Modifier.padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        if (container != null) Text(container, style = small, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        task.startDate?.takeIf { showWhen }?.let { date ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (task.evening) ListIcon(ListKind.EVENING, size = 12.dp)
                Text(date.format(dateFormat), style = small, color = muted)
            }
        }
        task.reminder?.let { time ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Notifications, contentDescription = null, Modifier.size(12.dp), tint = muted)
                Text(time.format(timeFormat), style = small, color = muted)
            }
        }
        task.deadline?.let { deadline ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                DeadlineFlag()
                Spacer(Modifier.width(2.dp))
                Text(deadline.format(dateFormat), style = small, color = ListColors.deadline)
            }
        }
        if (task.isRepeating) RepeatGlyph(muted, Modifier.padding(top = 3.dp))
        if (task.notes != null) NotesGlyph(muted)
        if (checklist.isNotEmpty()) Text("☑ ${checklist.count { it.done }}/${checklist.size}", style = small, color = muted)
        tags.forEach { TagPill(it) }
    }
}

/** The small "has notes" mark of a row. */
@Composable
private fun NotesGlyph(color: Color) {
    Canvas(Modifier.size(width = 12.dp, height = 14.dp)) {
        val w = size.width
        listOf(0.3f, 0.55f, 0.8f).forEachIndexed { i, y ->
            drawLine(color, Offset(0f, size.height * y), Offset(if (i == 2) w * 0.6f else w, size.height * y), strokeWidth = w * 0.13f, cap = StrokeCap.Round)
        }
    }
}

/** Projects grouped by area, as in the Things sidebar. */
@Composable
private fun ProjectList(library: ThingsLibrary, title: String, onOpen: (Task) -> Unit) {
    val projects = library.projects()
    val byArea = projects.groupBy { it.areaId }
    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "header") { ListHeader(ListKind.PROJECT, title) }
        if (projects.isEmpty()) {
            item(key = "empty") { Text(stringResource(R.string.nothing_here), Modifier.padding(20.dp)) }
        }
        val sections = listOf<String?>(null) + library.areas().map { it.id }
        sections.forEach { areaId ->
            val inArea = byArea[areaId].orEmpty()
            if (inArea.isEmpty()) return@forEach
            if (areaId != null) {
                item(key = "area-$areaId") {
                    Text(
                        library.area(areaId)?.title.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
                    )
                }
            }
            items(inArea, key = { it.id }) { project ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(project) }.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ListIcon(ListKind.PROJECT, size = 20.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(project.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    val open = library.projectTasks(project.id).size
                    if (open > 0) Text(open.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item(key = "footer") { Spacer(Modifier.height(88.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskDetail(
    library: ThingsLibrary,
    task: Task,
    onSetDone: (String, Boolean) -> Unit,
    onChecklistDone: (String, Boolean) -> Unit,
    onAddChecklistItem: (String, String) -> Unit,
    onRestore: (String) -> Unit,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val focusManager = LocalFocusManager.current
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (task.trashed) {
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.in_trash), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                if (library.canRestore(task)) TextButton(onClick = { onRestore(task.id) }) { Text(stringResource(R.string.restore)) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (task.type == TaskType.TODO) {
                ThingsCheckbox(
                    checked = task.status == TaskStatus.COMPLETED,
                    onCheckedChange = { onSetDone(task.id, it) },
                    enabled = library.canComplete(task),
                    size = 24.dp,
                )
            } else {
                ListIcon(ListKind.PROJECT, Modifier.padding(10.dp), size = 24.dp)
            }
            Text(
                task.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (task.isOpen) null else TextDecoration.LineThrough,
            )
        }
        if (task.type == TaskType.TODO && !library.canComplete(task) && !task.trashed && task.isRepeating) {
            Text(stringResource(R.string.repeating_locked), Modifier.padding(start = 12.dp), style = MaterialTheme.typography.labelMedium, color = muted)
        }

        Column(Modifier.padding(start = 12.dp, end = 12.dp)) {
            SelectionContainer {
                Text(task.notes ?: stringResource(R.string.no_notes), style = MaterialTheme.typography.bodyLarge, color = if (task.notes == null) muted else Color.Unspecified)
            }
        }

        val checklist = library.checklist(task.id)
        val editable = library.canEditChecklist(task)
        if (checklist.isNotEmpty() || editable) {
            Column {
                checklist.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ThingsCheckbox(
                            checked = item.done,
                            onCheckedChange = { onChecklistDone(item.id, it) },
                            enabled = editable,
                            size = 18.dp,
                        )
                        Text(
                            item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (item.done) TextDecoration.LineThrough else null,
                            color = if (item.done) muted else Color.Unspecified,
                        )
                    }
                }
                if (editable) {
                    var newItem by rememberSaveable(task.id) { mutableStateOf("") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, contentDescription = null, Modifier.padding(start = 12.dp, end = 12.dp).size(20.dp), tint = muted)
                        androidx.compose.material3.TextField(
                            value = newItem,
                            onValueChange = { newItem = it },
                            placeholder = { Text(stringResource(R.string.add_checklist_item)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (newItem.isNotBlank()) {
                                    onAddChecklistItem(task.id, newItem)
                                    newItem = ""
                                } else {
                                    focusManager.clearFocus()
                                }
                            }),
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        HorizontalDivider(Modifier.padding(horizontal = 12.dp))
        DetailFacts(library, task)
    }
}

/** When, reminder, deadline, project and tags, each with its icon. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailFacts(library: ThingsLibrary, task: Task) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val today = ThingsDay.today()
    Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val startDate = task.startDate
        val whenFact: Pair<ListKind, String>? = when {
            task.start == dev.thingscompanion.cloud.StartList.INBOX && startDate == null -> ListKind.INBOX to stringResource(R.string.list_inbox)
            startDate == null -> if (task.start == dev.thingscompanion.cloud.StartList.SOMEDAY) ListKind.SOMEDAY to stringResource(R.string.list_someday)
            else ListKind.ANYTIME to stringResource(R.string.list_anytime)
            startDate.isAfter(today) -> ListKind.UPCOMING to startDate.format(dateFormat)
            task.evening -> ListKind.EVENING to stringResource(R.string.this_evening)
            else -> ListKind.TODAY to stringResource(R.string.list_today)
        }
        whenFact?.let { (kind, label) -> Fact({ ListIcon(kind, size = 18.dp) }, label) }
        task.reminder?.let { Fact({ Icon(Icons.Filled.Notifications, null, Modifier.size(18.dp), tint = muted) }, it.format(timeFormat)) }
        library.repeatRule(task)?.let { rule ->
            Fact({ RepeatGlyph(muted, size = 18.dp) }, describeRepeat(LocalContext.current, rule))
        }
        task.deadline?.let { Fact({ DeadlineFlag(size = 16.dp) }, stringResource(R.string.due_date, it.format(dateFormat)), ListColors.deadline) }
        (task.projectId?.let { library.task(it)?.title } ?: task.areaId?.let { library.area(it)?.title })?.let {
            Fact({ ListIcon(ListKind.PROJECT, size = 18.dp) }, it)
        }
        val tags = library.tagTitles(task)
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                tags.forEach { TagPill(it) }
            }
        }
    }
}

@Composable
private fun Fact(icon: @Composable () -> Unit, text: String, color: Color = Color.Unspecified) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
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
    pending: Int,
    onDiscardPending: () -> Unit,
    demo: Boolean,
    onExitDemo: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmDiscard by remember { mutableStateOf(false) }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.discard_pending_title)) },
            text = { Text(stringResource(R.string.discard_pending_text, pending)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onDiscardPending()
                }) { Text(stringResource(R.string.discard)) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
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
                if (pending > 0) {
                    HorizontalDivider()
                    Text(stringResource(R.string.pending_explained, pending), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { confirmDiscard = true }) { Text(stringResource(R.string.discard_pending_title)) }
                }
                HorizontalDivider()
                if (demo) {
                    TextButton(onClick = onExitDemo) { Text(stringResource(R.string.exit_demo)) }
                } else {
                    TextButton(onClick = onSignOut) { Text(stringResource(R.string.sign_out)) }
                }
                val context = LocalContext.current
                val version = remember {
                    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
                }
                Text(
                    stringResource(R.string.app_version, stringResource(R.string.app_name), version),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
    )
}
