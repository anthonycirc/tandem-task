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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import dev.thingscompanion.cloud.RepeatUnit
import dev.thingscompanion.cloud.TaskEdits
import dev.thingscompanion.cloud.RepeatRule
import dev.thingscompanion.cloud.Area
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.foundation.lazy.rememberLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState
import sh.calvin.reorderable.ReorderableItem
import dev.thingscompanion.cloud.StartList
import dev.thingscompanion.cloud.PendingChange
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
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
import java.time.DayOfWeek
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
    AREAS(R.string.list_areas, ListKind.AREA),
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
    var areaId by rememberSaveable { mutableStateOf<String?>(null) }
    var taskId by rememberSaveable { mutableStateOf<String?>(null) }
    var schedulingId by rememberSaveable { mutableStateOf<String?>(null) }
    var addMenu by remember { mutableStateOf(false) }
    var addingProject by rememberSaveable { mutableStateOf(false) }
    var naming by remember { mutableStateOf<NameRequest?>(null) }
    var movingId by rememberSaveable { mutableStateOf<String?>(null) }
    var repeatingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingAreaId by rememberSaveable { mutableStateOf<String?>(null) }

    // A widget tap asked to open the add form or a to-do.
    LaunchedEffect(state.launchOpenAdd, state.launchTaskId) {
        if (state.launchOpenAdd) adding = true
        if (state.launchTaskId != null) taskId = state.launchTaskId
        if (state.launchOpenAdd || state.launchTaskId != null) viewModel.consumeLaunch()
    }

    // The forms take the whole screen: Save stays in the top bar, above the keyboard.
    // Added from a project or an area, the to-do is filed there.
    val addInProject = projectId?.takeIf { library?.task(it) != null }
    val addInArea = areaId?.takeIf { addInProject == null && library?.area(it) != null }
    if (adding) {
        val filed = addInProject != null || addInArea != null
        TaskFormScreen(
            initial = TaskDraft(
                schedule = when {
                    filed -> Schedule.Anytime
                    tab == ListTab.TODAY -> Schedule.Today(ThingsDay.today())
                    else -> Schedule.Inbox
                },
            ),
            heading = R.string.new_todo_title,
            confirm = R.string.add,
            allowSchedule = true,
            allowInbox = !filed,
            allowChecklist = true,
            allowDeadline = true,
            onSubmit = { title, notes, schedule, reminder, checklist, deadline ->
                adding = false
                viewModel.addTask(title, notes, schedule, reminder, checklist, deadline, projectId = addInProject, areaId = addInArea)
            },
            onDismiss = { adding = false },
        )
        return
    }
    if (addingProject) {
        TaskFormScreen(
            initial = TaskDraft(schedule = Schedule.Anytime),
            heading = R.string.new_project_title,
            confirm = R.string.add,
            allowSchedule = true,
            allowInbox = false,
            allowChecklist = false,
            allowDeadline = true,
            allowReminder = false,
            onSubmit = { title, notes, schedule, _, _, deadline ->
                addingProject = false
                viewModel.addProject(title, notes, schedule, deadline, areaId = addInArea)
            },
            onDismiss = { addingProject = false },
        )
        return
    }
    val repeating = repeatingId?.let { library?.task(it) }
    val repeatStart = repeating?.startDate
    if (repeating != null && repeatStart != null) {
        RepeatDialog(
            start = repeatStart,
            onPick = { rule ->
                repeatingId = null
                viewModel.makeRepeating(repeating.id, rule)
            },
            onDismiss = { repeatingId = null },
        )
    }
    val moving = movingId?.let { library?.task(it) }
    if (moving != null && library != null) {
        MoveDialog(
            library = library,
            item = moving,
            onPick = { destination ->
                movingId = null
                if (moving.type == TaskType.PROJECT) viewModel.moveProject(moving.id, destination.areaId) else viewModel.moveTask(moving.id, destination)
            },
            onDismiss = { movingId = null },
        )
    }
    val deletingArea = deletingAreaId?.let { library?.area(it) }
    if (deletingArea != null && library != null) {
        val count = library.areaContents(deletingArea.id).size
        AlertDialog(
            onDismissRequest = { deletingAreaId = null },
            title = { Text(stringResource(R.string.delete_area_title, deletingArea.title)) },
            text = { Text(pluralStringResource(R.plurals.delete_area_contents, count, count)) },
            confirmButton = {
                TextButton(onClick = {
                    deletingAreaId = null
                    areaId = null
                    viewModel.deleteArea(deletingArea.id)
                }) { Text(stringResource(R.string.delete_area)) }
            },
            dismissButton = { TextButton(onClick = { deletingAreaId = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    naming?.let { request ->
        NameDialog(
            title = stringResource(request.title),
            initial = request.initial,
            onConfirm = { name ->
                naming = null
                request.onName(name)
            },
            onDismiss = { naming = null },
        )
    }
    val edited = editingId?.let { library?.task(it) }
    if (edited != null && library != null && edited.type == TaskType.PROJECT) {
        // A project: title, notes, When and deadline; no checklist, reminder or Inbox.
        TaskFormScreen(
            initial = TaskDraft(edited.title, edited.notes.orEmpty(), Schedule.of(edited, ThingsDay.today()), edited.reminder, deadline = edited.deadline),
            heading = R.string.edit_project_title,
            confirm = R.string.save,
            allowSchedule = library.canEditProject(edited),
            allowInbox = false,
            allowChecklist = false,
            allowDeadline = library.canSetDeadline(edited),
            allowReminder = false,
            onSubmit = { title, notes, schedule, reminder, _, deadline ->
                editingId = null
                viewModel.editTask(edited.id, title, notes, schedule, reminder, deadline = deadline)
            },
            onDismiss = { editingId = null },
        )
        return
    }
    if (edited != null && library != null) {
        TaskFormScreen(
            initial = TaskDraft(
                edited.title,
                edited.notes.orEmpty(),
                Schedule.of(edited, ThingsDay.today()),
                edited.reminder,
                library.checklist(edited.id).map { ChecklistDraft(it.id, it.title, it.done) },
                edited.deadline,
            ),
            heading = R.string.edit_todo_title,
            confirm = R.string.save,
            allowSchedule = library.canReschedule(edited),
            allowInbox = edited.projectId == null && edited.areaId == null,
            allowChecklist = library.canEditChecklist(edited),
            allowDeadline = library.canSetDeadline(edited),
            onSubmit = { title, notes, schedule, reminder, checklist, deadline ->
                editingId = null
                viewModel.editTask(edited.id, title, notes, schedule, reminder, checklist.takeIf { library.canEditChecklist(edited) }, deadline)
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

    // Back closes the to-do, then the project, then the area it was opened from.
    fun back() {
        when {
            taskId != null -> taskId = null
            projectId != null -> projectId = null
            else -> areaId = null
        }
    }
    BackHandler(enabled = taskId != null || projectId != null || areaId != null) { back() }

    val today = ThingsDay.today()
    val openTask = taskId?.let { library?.task(it) }
    val openProject = projectId?.let { library?.task(it) }
    val openArea = areaId?.let { library?.area(it) }
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
                // In a project, an area or Projects, + offers what can be created there, as in Things.
                val choices = buildList {
                    val project = projectId?.let { library.task(it) }
                    when {
                        project != null -> {
                            add(R.string.new_todo_title to { adding = true })
                            if (library.canEditProject(project)) {
                                add(R.string.new_heading_title to {
                                    naming = NameRequest(R.string.new_heading_title) { viewModel.addHeading(project.id, it) }
                                })
                            }
                        }
                        areaId != null -> {
                            add(R.string.new_todo_title to { adding = true })
                            add(R.string.new_project_title to { addingProject = true })
                        }
                        tab == ListTab.AREAS -> add(R.string.new_area_title to { naming = NameRequest(R.string.new_area_title) { viewModel.addArea(it) } })
                        tab == ListTab.PROJECTS -> {
                            add(R.string.new_project_title to { addingProject = true })
                            add(R.string.new_area_title to { naming = NameRequest(R.string.new_area_title) { viewModel.addArea(it) } })
                        }
                    }
                }
                Box {
                    FloatingActionButton(
                        onClick = { if (choices.isEmpty()) adding = true else addMenu = true },
                        containerColor = ListColors.checked,
                        contentColor = Color.White,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_to_inbox))
                    }
                    DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                        choices.forEach { (label, action) ->
                            DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = {
                                addMenu = false
                                action()
                            })
                        }
                    }
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    if (taskId == null && projectId == null && areaId == null) {
                        Text(stringResource(R.string.app_name), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    if (taskId != null || projectId != null || areaId != null) {
                        IconButton(onClick = ::back) {
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
                        if (library.canMakeRepeating(openTask)) {
                            val repeatLabel = stringResource(R.string.repeat_title)
                            IconButton(onClick = { repeatingId = openTask.id }, modifier = Modifier.semantics { contentDescription = repeatLabel }) {
                                RepeatGlyph(LocalContentColor.current, size = 22.dp)
                            }
                        }
                        if (library.canMove(openTask)) {
                            IconButton(onClick = { movingId = openTask.id }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.move))
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
                    onDeleteChecklistItem = viewModel::deleteChecklistItem,
                    onRestore = viewModel::restore,
                )
                openProject != null -> {
                    if (!filter.isEmpty) ActiveFilters(library, filter, viewModel::setFilter)
                    TaskList(
                        library = library,
                        header = {
                            ProjectHeader(
                                library = library,
                                project = openProject,
                                onEdit = { editingId = openProject.id },
                                onMove = { movingId = openProject.id },
                                onTrash = {
                                    projectId = null
                                    viewModel.trash(openProject.id)
                                },
                                onStatus = { viewModel.setProjectStatus(openProject.id, it) },
                            )
                        },
                        // Things' headings; with a filter on, headings left empty are hidden.
                        groups = library.projectSections(openProject.id, closedOn = today)
                            .map { section -> (section.heading?.let { Group.Heading(it.id, it.title) } ?: Group.None) to filter.apply(section.tasks) }
                            .filter { (group, tasks) -> filter.isEmpty || group !is Group.Heading || tasks.isNotEmpty() },
                        emptyText = stringResource(R.string.no_open_todos),
                        actions = rowActions,
                        onHeading = { heading ->
                            naming = NameRequest(R.string.rename_heading, heading.title) { viewModel.renameHeading(heading.id, it) }
                        },
                        // Dragging needs the whole list: off while a filter hides rows.
                        onDrop = { task: Task, section: Group, order: List<String> ->
                            viewModel.dropInProject(openProject.id, task.id, (section as? Group.Heading)?.id, order)
                        }.takeIf { filter.isEmpty },
                        onHeadingsDrop = { headingId: String, order: List<String> -> viewModel.reorder(headingId, order, "headings:${openProject.id}") }
                            .takeIf { filter.isEmpty },
                    )
                }
                openArea != null -> {
                    if (!filter.isEmpty) ActiveFilters(library, filter, viewModel::setFilter)
                    TaskList(
                        library = library,
                        header = {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.weight(1f)) { ListHeader(ListKind.AREA, openArea.title) }
                                    IconButton(onClick = {
                                        naming = NameRequest(R.string.rename_area, openArea.title) { viewModel.renameArea(openArea.id, it) }
                                    }) { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.rename_area)) }
                                    IconButton(onClick = { deletingAreaId = openArea.id }) {
                                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_area))
                                    }
                                }
                                library.areaProjects(openArea.id).forEach { project ->
                                    SwipeActions(
                                        canReschedule = false,
                                        canTrash = library.canTrash(project),
                                        onReschedule = {},
                                        onTrash = { viewModel.trash(project.id) },
                                    ) { ProjectRow(library, project) { projectId = it.id } }
                                }
                            }
                        },
                        groups = listOf(Group.None to filter.apply(library.areaTasks(openArea.id, closedOn = today))),
                        emptyText = "",
                        actions = rowActions,
                        onDrop = { task: Task, _: Group, order: List<String> -> viewModel.reorder(task.id, order, "area:${openArea.id}") }.takeIf { filter.isEmpty },
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
                    if (!filter.isEmpty && tab != ListTab.PROJECTS && tab != ListTab.AREAS && tab != ListTab.TRASH) ActiveFilters(library, filter, viewModel::setFilter)
                    val title = stringResource(tab.label)
                    when (tab) {
                        ListTab.PROJECTS -> ProjectList(
                            library = library,
                            title = title,
                            onOpenArea = { areaId = it },
                            onDrop = { project, area, order -> viewModel.dropProject(project.id, area, order) },
                            onTrashProject = { viewModel.trash(it.id) },
                            onDeleteArea = { deletingAreaId = it.id },
                        ) { projectId = it.id }
                        ListTab.AREAS -> AreaList(library, title, onOpen = { areaId = it.id }, onDelete = { deletingAreaId = it.id })
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
                                // Upcoming is ordered by date; dragging needs the whole list, so not while filtering.
                                onDrop = { task: Task, section: Group, order: List<String> ->
                                    if (tab == ListTab.TODAY) {
                                        viewModel.reorder(task.id, order, "today:${section == Group.Evening}", todayOrder = true, evening = section == Group.Evening)
                                    } else {
                                        viewModel.reorder(task.id, order, tab.name)
                                    }
                                }.takeIf { filter.isEmpty && tab != ListTab.UPCOMING },
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

/**
 * Where to move a to-do (Inbox, a project, one of its headings, an area) or a project (an area,
 * or none). The current place is marked.
 */
@Composable
private fun MoveDialog(library: ThingsLibrary, item: Task, onPick: (PendingChange.Destination) -> Unit, onDismiss: () -> Unit) {
    val isProject = item.type == TaskType.PROJECT
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    @Composable
    fun Choice(kind: ListKind?, label: String, indent: Int, current: Boolean, destination: PendingChange.Destination) {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = !current) { onPick(destination) }.padding(start = (8 + indent * 20).dp, top = 10.dp, bottom = 10.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (kind != null) ListIcon(kind, size = 18.dp) else Spacer(Modifier.width(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, color = if (current) muted else Color.Unspecified, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (current) Text("✓", color = muted)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isProject) R.string.move_to_area else R.string.move)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                if (isProject) {
                    item { Choice(null, stringResource(R.string.no_area), 0, item.areaId == null, PendingChange.Destination()) }
                    items(library.areas(), key = { it.id }) { area ->
                        Choice(ListKind.AREA, area.title, 0, item.areaId == area.id, PendingChange.Destination(areaId = area.id))
                    }
                } else {
                    item {
                        Choice(ListKind.INBOX, stringResource(R.string.list_inbox), 0, item.start == StartList.INBOX && item.projectId == null && item.areaId == null, PendingChange.Destination(inbox = true))
                    }
                    fun LazyListScope.project(project: Task, indent: Int) {
                        item(key = project.id) {
                            Choice(ListKind.PROJECT, project.title, indent, item.projectId == project.id && item.headingId == null, PendingChange.Destination(projectId = project.id))
                        }
                        library.projectSections(project.id).mapNotNull { it.heading }.forEach { heading ->
                            item(key = heading.id) {
                                Choice(null, heading.title, indent + 1, item.headingId == heading.id, PendingChange.Destination(headingId = heading.id))
                            }
                        }
                    }
                    library.projects().filter { it.areaId == null }.forEach { project(it, 0) }
                    library.areas().forEach { area ->
                        item(key = area.id) {
                            Choice(ListKind.AREA, area.title, 0, item.projectId == null && item.areaId == area.id, PendingChange.Destination(areaId = area.id))
                        }
                        library.areaProjects(area.id).forEach { project(it, 1) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * How a to-do repeats, from the app: every N days or weeks, on a fixed schedule (weekly on
 * chosen days) or after completion. The rule starts on the to-do's day, [start], which a weekly
 * schedule always includes; months and years are set in Things.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RepeatDialog(start: LocalDate, onPick: (RepeatRule) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    var unit by rememberSaveable { mutableStateOf(RepeatUnit.DAY) }
    var every by rememberSaveable { mutableIntStateOf(1) }
    var afterCompletion by rememberSaveable { mutableStateOf(false) }
    var days by rememberSaveable { mutableStateOf(listOf(start.dayOfWeek.value)) }
    val weekly = unit == RepeatUnit.WEEK && !afterCompletion
    // Several days only every week: which week Things counts from is unknown.
    val chosen = if (every > 1) listOf(start.dayOfWeek.value) else days
    val rule = RepeatRule(unit, every, afterCompletion, weekdays = if (weekly) chosen.sorted().map(DayOfWeek::of) else emptyList())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.repeat_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = unit == RepeatUnit.DAY, onClick = { unit = RepeatUnit.DAY }, label = { Text(stringResource(R.string.repeat_unit_days)) })
                    FilterChip(selected = unit == RepeatUnit.WEEK, onClick = { unit = RepeatUnit.WEEK }, label = { Text(stringResource(R.string.repeat_unit_weeks)) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { if (every > 1) every-- }, enabled = every > 1) { Text("−") }
                    Text(every.toString(), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { if (every < 30) every++ }) { Text("+") }
                }
                if (weekly) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DayOfWeek.entries.forEach { day ->
                            val isStart = day == start.dayOfWeek
                            FilterChip(
                                selected = day.value in chosen,
                                onClick = { days = if (day.value in days) days - day.value else days + day.value },
                                enabled = !isStart && every == 1,
                                label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.repeat_after_completion_toggle), modifier = Modifier.weight(1f))
                    Switch(checked = afterCompletion, onCheckedChange = { afterCompletion = it })
                }
                Text(describeRepeat(context, rule), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.repeat_app_limits), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(rule) }, enabled = TaskEdits.canWriteRule(rule, start)) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** A name to ask for: a new heading or area, or a new name for one. */
private class NameRequest(@StringRes val title: Int, val initial: String = "", val onName: (String) -> Unit)

@Composable
private fun NameDialog(title: String, initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onConfirm(name) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** A section of a list: Today's This Evening, a day of Upcoming, a project's heading, or none. */
private sealed interface Group {
    data object None : Group
    data class Heading(val id: String, val title: String) : Group
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
private fun GroupHeader(group: Group, onHeading: (Group.Heading) -> Unit = {}) {
    when (group) {
        Group.None -> Unit
        is Group.Heading -> Column(Modifier.clickable { onHeading(group) }.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 4.dp)) {
            Text(group.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ListColors.heading)
            HorizontalDivider(Modifier.padding(top = 6.dp), color = ListColors.heading.copy(alpha = 0.4f))
        }
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

/** A row of a [TaskList]: a section header or a to-do. */
private sealed interface ListRow {
    val key: String
}

private data class HeaderRow(val group: Group, override val key: String) : ListRow

private data class TaskItemRow(val task: Task) : ListRow {
    override val key get() = task.id
}

/**
 * A list of to-dos in sections. With [onDrop], a long press lifts a to-do to drag it: dropped, it
 * reports its section and that section's new order. With [onHeadingsDrop], headings are dragged
 * the same way and report the headings' new order.
 */
@Composable
private fun TaskList(
    library: ThingsLibrary,
    header: @Composable () -> Unit,
    groups: List<Pair<Group, List<Task>>>,
    emptyText: String,
    actions: RowActions,
    showWhen: Boolean = true,
    onHeading: (Group.Heading) -> Unit = {},
    onDrop: ((task: Task, section: Group, order: List<String>) -> Unit)? = null,
    onHeadingsDrop: ((headingId: String, order: List<String>) -> Unit)? = null,
) {
    val built = remember(groups) {
        groups.withIndex().flatMap { (index, entry) ->
            val (group, tasks) = entry
            // A heading shows even when empty, as in Things.
            if (tasks.isEmpty() && group !is Group.Heading) emptyList()
            else listOf(HeaderRow(group, "group-" + ((group as? Group.Heading)?.id ?: index.toString()))) + tasks.map(::TaskItemRow)
        }
    }
    // The rows as dragged; rebuilt from the lists whenever they change.
    val rows = remember(built) { built.toMutableStateList() }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = rows.indexOfFirst { it.key == from.key }
        val toIndex = rows.indexOfFirst { it.key == to.key }
        // Nothing goes above the first section's header.
        if (fromIndex < 0 || toIndex <= 0) return@rememberReorderableLazyListState
        rows.add(toIndex, rows.removeAt(fromIndex))
    }
    fun dropped(key: String) {
        val index = rows.indexOfFirst { it.key == key }
        when (val row = rows.getOrNull(index)) {
            is TaskItemRow -> {
                val headerIndex = (index downTo 0).first { rows[it] is HeaderRow }
                val section = (rows[headerIndex] as HeaderRow).group
                val order = rows.drop(headerIndex + 1).takeWhile { it is TaskItemRow }.map { it.key }
                onDrop?.invoke(row.task, section, order)
            }
            is HeaderRow -> {
                val heading = row.group as? Group.Heading ?: return
                onHeadingsDrop?.invoke(heading.id, rows.mapNotNull { ((it as? HeaderRow)?.group as? Group.Heading)?.id })
            }
            null -> Unit
        }
    }
    fun draggable(row: ListRow) = when (row) {
        is TaskItemRow -> onDrop != null && row.task.isOpen && library.canReorder(row.task)
        is HeaderRow -> onHeadingsDrop != null && row.group is Group.Heading
    }
    LazyColumn(Modifier.fillMaxSize(), state = listState) {
        item(key = "header") { header() }
        if (groups.all { it.second.isEmpty() && it.first !is Group.Heading }) {
            item(key = "empty") {
                Text(emptyText, Modifier.padding(20.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(rows, key = { it.key }) { row ->
            ReorderableItem(reorderState, key = row.key, enabled = draggable(row)) { dragging ->
                val handle = if (draggable(row)) Modifier.longPressDraggableHandle(onDragStopped = { dropped(row.key) }) else Modifier
                val lifted = if (dragging) Modifier.shadow(8.dp) else Modifier
                Box(handle.then(lifted)) {
                    when (row) {
                        is HeaderRow -> {
                            // A heading swiped right goes to the Trash with its to-dos.
                            val heading = (row.group as? Group.Heading)?.let { library.task(it.id) }
                            SwipeActions(
                                canReschedule = false,
                                canTrash = heading != null && library.canTrash(heading),
                                onReschedule = {},
                                onTrash = { heading?.let { actions.onTrash(it.id) } },
                            ) {
                                Box(Modifier.background(MaterialTheme.colorScheme.surface)) { GroupHeader(row.group, onHeading) }
                            }
                        }
                        is TaskItemRow -> {
                            val task = row.task
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
                }
            }
        }
        // Room for the add button over the last row.
        item(key = "footer") { Spacer(Modifier.height(88.dp)) }
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

/**
 * Projects without an area, then each area with its projects, as in Things. A long press lifts a
 * project: dropped under another area it moves there, and its area's new order is reported.
 */
@Composable
private fun ProjectList(
    library: ThingsLibrary,
    title: String,
    onOpenArea: (String) -> Unit,
    onDrop: ((project: Task, areaId: String?, order: List<String>) -> Unit)?,
    onTrashProject: (Task) -> Unit,
    onDeleteArea: (Area) -> Unit,
    onOpen: (Task) -> Unit,
) {
    val projects = library.projects()
    val areas = library.areas()
    // Area headers (the first, with no area, draws nothing) and projects, in display order.
    val built = remember(library) {
        listOf<Pair<String, Any?>>("area-none" to null) + projects.filter { it.areaId == null }.map { it.id to it } +
            areas.flatMap { area -> listOf("area-${area.id}" to area) + library.areaProjects(area.id).map { it.id to it } }
    }
    val rows = remember(built) { built.toMutableStateList() }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = rows.indexOfFirst { it.first == from.key }
        val toIndex = rows.indexOfFirst { it.first == to.key }
        if (fromIndex < 0 || toIndex <= 0) return@rememberReorderableLazyListState
        rows.add(toIndex, rows.removeAt(fromIndex))
    }
    fun dropped(project: Task) {
        val index = rows.indexOfFirst { it.first == project.id }
        if (index < 0) return
        val headerIndex = (index downTo 0).first { rows[it].second !is Task }
        val area = rows[headerIndex].second as? Area
        val order = rows.drop(headerIndex + 1).takeWhile { it.second is Task }.map { it.first }
        onDrop?.invoke(project, area?.id, order)
    }
    LazyColumn(Modifier.fillMaxSize(), state = listState) {
        item(key = "header") { ListHeader(ListKind.PROJECT, title) }
        if (projects.isEmpty() && areas.isEmpty()) {
            item(key = "empty") { Text(stringResource(R.string.nothing_here), Modifier.padding(20.dp)) }
        }
        items(rows, key = { it.first }) { (key, value) ->
            val project = value as? Task
            val canDrag = project != null && onDrop != null && library.canEditProject(project)
            ReorderableItem(reorderState, key = key, enabled = canDrag) { dragging ->
                val handle = if (canDrag) Modifier.longPressDraggableHandle(onDragStopped = { dropped(project!!) }) else Modifier
                Box(handle.then(if (dragging) Modifier.shadow(8.dp) else Modifier).background(MaterialTheme.colorScheme.surface)) {
                    when (value) {
                        is Task -> SwipeActions(
                            canReschedule = false,
                            canTrash = library.canTrash(value),
                            onReschedule = {},
                            onTrash = { onTrashProject(value) },
                        ) { ProjectRow(library, value, onOpen) }
                        // Every area, even one without projects, opens on its own to-dos and projects as in Things.
                        // Swiped right, an area asks before it is deleted.
                        is Area -> SwipeActions(canReschedule = false, canTrash = true, onReschedule = {}, onTrash = { onDeleteArea(value) }) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { onOpenArea(value.id) }
                                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ListIcon(ListKind.AREA, size = 20.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(value.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        else -> Spacer(Modifier.height(1.dp))
                    }
                }
            }
        }
        item(key = "footer") { Spacer(Modifier.height(88.dp)) }
    }
}

/**
 * An open project's title with Things' actions: its circle completes it (asking first when
 * to-dos are still open, since they are completed with it), the pencil edits it, and the menu
 * cancels or reopens it.
 */
@Composable
private fun ProjectHeader(
    library: ThingsLibrary,
    project: Task,
    onEdit: () -> Unit,
    onMove: () -> Unit,
    onTrash: () -> Unit,
    onStatus: (TaskStatus) -> Unit,
) {
    val editable = library.canEditProject(project)
    var confirming by remember { mutableStateOf<TaskStatus?>(null) }
    var menu by remember { mutableStateOf(false) }
    val openCount = library.openChildren(project).count { it.type == TaskType.TODO }
    fun request(status: TaskStatus) {
        if (status != TaskStatus.OPEN && openCount > 0) confirming = status else onStatus(status)
    }
    confirming?.let { status ->
        AlertDialog(
            onDismissRequest = { confirming = null },
            title = { Text(stringResource(if (status == TaskStatus.COMPLETED) R.string.complete_project else R.string.cancel_project)) },
            text = {
                Text(pluralStringResource(if (status == TaskStatus.COMPLETED) R.plurals.complete_project_todos else R.plurals.cancel_project_todos, openCount, openCount))
            },
            confirmButton = {
                TextButton(onClick = {
                    confirming = null
                    onStatus(status)
                }) { Text(stringResource(if (status == TaskStatus.COMPLETED) R.string.complete_project else R.string.cancel_project)) }
            },
            dismissButton = { TextButton(onClick = { confirming = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ThingsCheckbox(
                checked = project.status == TaskStatus.COMPLETED,
                onCheckedChange = { request(if (it) TaskStatus.COMPLETED else TaskStatus.OPEN) },
                enabled = editable,
                size = 26.dp,
                contentDescription = stringResource(R.string.complete_project),
            )
            Text(
                project.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (project.isOpen) null else TextDecoration.LineThrough,
                modifier = Modifier.weight(1f),
            )
            if (editable) {
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit)) }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.move_to_area)) }, onClick = {
                            menu = false
                            onMove()
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.delete_project)) }, onClick = {
                            menu = false
                            onTrash()
                        })
                        if (project.status == TaskStatus.OPEN) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.cancel_project)) }, onClick = {
                                menu = false
                                request(TaskStatus.CANCELLED)
                            })
                        } else {
                            DropdownMenuItem(text = { Text(stringResource(R.string.reopen_project)) }, onClick = {
                                menu = false
                                onStatus(TaskStatus.OPEN)
                            })
                        }
                    }
                }
            }
        }
        // When and deadline, as under a to-do's title.
        val facts = listOfNotNull(
            project.startDate?.format(dateFormat),
            project.deadline?.let { stringResource(R.string.deadline) + " " + it.format(dateFormat) },
        )
        if (facts.isNotEmpty()) {
            Text(facts.joinToString("  ·  "), Modifier.padding(start = 44.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Every area, as in Things' sidebar: tap to open, swipe right to delete (after a confirmation). */
@Composable
private fun AreaList(library: ThingsLibrary, title: String, onOpen: (Area) -> Unit, onDelete: (Area) -> Unit) {
    val areas = library.areas()
    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "header") { ListHeader(ListKind.AREA, title) }
        if (areas.isEmpty()) {
            item(key = "empty") { Text(stringResource(R.string.nothing_here), Modifier.padding(20.dp)) }
        }
        items(areas, key = { it.id }) { area ->
            SwipeActions(canReschedule = false, canTrash = true, onReschedule = {}, onTrash = { onDelete(area) }) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onOpen(area) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ListIcon(ListKind.AREA, size = 20.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(area.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    val count = library.areaProjects(area.id).size + library.areaTasks(area.id).size
                    if (count > 0) Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item(key = "footer") { Spacer(Modifier.height(88.dp)) }
    }
}

/** A project in a list: its circle, title and count of open to-dos. */
@Composable
private fun ProjectRow(library: ThingsLibrary, project: Task, onOpen: (Task) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).clickable { onOpen(project) }.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ListIcon(ListKind.PROJECT, size = 20.dp)
        Spacer(Modifier.width(12.dp))
        Text(project.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        val open = library.projectTasks(project.id).size
        if (open > 0) Text(open.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onDeleteChecklistItem: (String) -> Unit,
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
                val notes = task.notes
                if (notes == null) {
                    Text(stringResource(R.string.no_notes), style = MaterialTheme.typography.bodyLarge, color = muted)
                } else {
                    val colors = markdownColors()
                    Text(remember(notes, colors) { markdown(notes, colors, clickableLinks = true) }, style = MaterialTheme.typography.bodyLarge)
                }
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
                            modifier = Modifier.weight(1f),
                        )
                        if (editable) {
                            IconButton(onClick = { onDeleteChecklistItem(item.id) }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.delete_item), Modifier.size(18.dp), tint = muted)
                            }
                        }
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
