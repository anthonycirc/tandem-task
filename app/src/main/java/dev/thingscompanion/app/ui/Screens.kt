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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import dev.thingscompanion.cloud.ListLayout
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import dev.thingscompanion.cloud.Sharing
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import dev.thingscompanion.cloud.ListCounts
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.runtime.DisposableEffect
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.Surface
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.draw.clip
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.thingscompanion.app.R
import dev.thingscompanion.cloud.Schedule
import dev.thingscompanion.cloud.Task
import dev.thingscompanion.cloud.TaskFilter
import dev.thingscompanion.cloud.TaskStatus
import dev.thingscompanion.cloud.TaskType
import dev.thingscompanion.cloud.ThingsDay
import dev.thingscompanion.cloud.ThingsLibrary
import dev.thingscompanion.cloud.TitleEmoji
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class ListTab(@StringRes val label: Int, val kind: ListKind) {
    TODAY(R.string.list_today, ListKind.TODAY),
    INBOX(R.string.list_inbox, ListKind.INBOX),
    UPCOMING(R.string.list_upcoming, ListKind.UPCOMING),
    // Things' Deadlines list: everything with a deadline, overdue first.
    DEADLINES(R.string.list_deadlines, ListKind.DEADLINES),
    ANYTIME(R.string.list_anytime, ListKind.ANYTIME),
    SOMEDAY(R.string.list_someday, ListKind.SOMEDAY),
    // Projects and areas in one list: each area folds open or closed over its projects.
    PROJECTS(R.string.list_projects, ListKind.PROJECT),
    // Things' Logbook: what was completed or cancelled, by the day it was closed, for the review.
    LOGBOOK(R.string.list_logbook, ListKind.LOGBOOK),
    TRASH(R.string.list_trash, ListKind.TRASH),
}

@Composable
fun ThingsApp(viewModel: ThingsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    // Clearing the message restarts this effect: the snackbar runs in its own scope so that restart
    // does not cancel it (it used to vanish at once, Undo with it).
    // A newer message replaces the one showing, so an older Undo never comes back after it.
    val snackbarScope = rememberCoroutineScope()
    var showing by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        viewModel.dismissMessage()
        showing?.cancel()
        snackbar.currentSnackbarData?.dismiss()
        showing = snackbarScope.launch {
            val text = when {
                message.extra != null -> context.resources.getQuantityString(message.extra.first, message.extra.second, message.extra.second) +
                    " · " + context.resources.getQuantityString(message.res, message.arg as Int, message.arg)
                message.plural -> context.resources.getQuantityString(message.res, message.arg as Int, message.arg)
                message.arg != null -> context.getString(message.res, message.arg)
                else -> context.getString(message.res)
            }
            val undoLabel = context.getString(R.string.undo).takeIf { message.undo.isNotEmpty() }
            val result = snackbar.showSnackbar(text, actionLabel = undoLabel, withDismissAction = false)
            if (result == SnackbarResult.ActionPerformed) viewModel.undo(message.undo)
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
    // The tabs in the order chosen in Settings, without the hidden ones (still reachable from the search).
    val allTabs = ListTab.entries.map { it.name }
    val shownTabs = ListLayout.shown(allTabs, state.tabOrder, state.hiddenTabs).map(ListTab::valueOf)
    var tab by rememberSaveable { mutableStateOf(shownTabs.first()) }
    var adding by rememberSaveable { mutableStateOf(false) }
    // A reminder changed from a to-do's sheet.
    var reminderId by rememberSaveable { mutableStateOf<String?>(null) }
    var filtering by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var projectId by rememberSaveable { mutableStateOf<String?>(null) }
    var areaId by rememberSaveable { mutableStateOf<String?>(null) }
    var taskId by rememberSaveable { mutableStateOf<String?>(null) }
    var schedulingId by rememberSaveable { mutableStateOf<String?>(null) }
    // A deadline tapped in the details or a project's header: changed there, without the form.
    var deadlineId by rememberSaveable { mutableStateOf<String?>(null) }
    var addMenu by remember { mutableStateOf(false) }
    var addingProject by rememberSaveable { mutableStateOf(false) }
    var naming by remember { mutableStateOf<NameRequest?>(null) }
    var movingId by rememberSaveable { mutableStateOf<String?>(null) }
    var repeatingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingAreaId by rememberSaveable { mutableStateOf<String?>(null) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var taggingId by rememberSaveable { mutableStateOf<String?>(null) }
    // Selecting several to-dos or projects: null when not selecting.
    var selection by rememberSaveable { mutableStateOf<List<String>?>(null) }
    var selectionWhen by remember { mutableStateOf(false) }
    var selectionMove by remember { mutableStateOf(false) }
    fun toggleSelected(id: String) {
        selection = selection?.let { if (id in it) it - id else it + id }
    }
    var query by rememberSaveable { mutableStateOf("") }
    // To-dos to delete for good, frozen when asked: the confirmation sends exactly these.
    var deletingForever by rememberSaveable { mutableStateOf<List<String>?>(null) }
    // Where a new to-do goes: the project or area it is added from, or one picked in the form.
    var placeProjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var placeAreaId by rememberSaveable { mutableStateOf<String?>(null) }
    // A heading of that project, when added from its "New to-do" row.
    var placeHeadingId by rememberSaveable { mutableStateOf<String?>(null) }
    var pickingPlace by remember { mutableStateOf(false) }
    // Tags picked for a new to-do or project, sent with its creation.
    var newTagIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var pickingTags by remember { mutableStateOf(false) }
    // Title and notes a new to-do starts with: what another app shared, or nothing.
    var addTitle by rememberSaveable { mutableStateOf("") }
    var addNotes by rememberSaveable { mutableStateOf("") }
    fun startAdding(shared: Sharing.Draft? = null, headingId: String? = null) {
        placeProjectId = projectId?.takeIf { library?.task(it) != null }
        placeAreaId = areaId?.takeIf { placeProjectId == null && library?.area(it) != null }
        placeHeadingId = headingId?.takeIf { placeProjectId != null && library?.task(it)?.projectId == placeProjectId }
        newTagIds = emptyList()
        addTitle = shared?.title.orEmpty()
        addNotes = shared?.notes.orEmpty()
        adding = true
    }
    @Composable
    fun NewTagsDialog() {
        if (pickingTags && library != null) {
            TagsDialog(
                library = library,
                selected = newTagIds.toSet(),
                onSave = {
                    pickingTags = false
                    newTagIds = it
                },
                onDismiss = { pickingTags = false },
            )
        }
    }
    val newTagTitles = newTagIds.mapNotNull { id -> library?.tags()?.firstOrNull { it.id == id }?.title }

    // A widget tap asked to open the add form or a to-do; another app shared text to add.
    LaunchedEffect(state.launchOpenAdd, state.launchTaskId, state.launchShare) {
        val shared = state.launchShare
        if (state.launchOpenAdd || shared != null) startAdding(shared)
        if (state.launchTaskId != null) taskId = state.launchTaskId
        if (state.launchOpenAdd || state.launchTaskId != null || shared != null) viewModel.consumeLaunch()
    }

    // The forms take the whole screen: Save stays in the top bar, above the keyboard.
    // Added from a project or an area, the to-do is filed there.
    val addInProject = placeProjectId?.takeIf { library?.task(it) != null }
    val addInArea = placeAreaId?.takeIf { addInProject == null && library?.area(it) != null }
    val addUnderHeading = placeHeadingId?.takeIf { addInProject != null && library?.task(it)?.projectId == addInProject }
    if (adding) {
        val filed = addInProject != null || addInArea != null
        if (pickingPlace && library != null) {
            PlaceDialog(
                library = library,
                isProject = false,
                current = when {
                    addInProject != null -> PendingChange.Destination(projectId = addInProject)
                    addInArea != null -> PendingChange.Destination(areaId = addInArea)
                    else -> PendingChange.Destination(inbox = true)
                },
                title = R.string.place_label,
                headings = false,
                onPick = {
                    pickingPlace = false
                    placeProjectId = it.projectId
                    placeAreaId = it.areaId
                    placeHeadingId = null
                },
                onDismiss = { pickingPlace = false },
            )
        }
        TaskFormScreen(
            initial = TaskDraft(
                title = addTitle,
                notes = addNotes,
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
            place = when {
                addInProject != null -> ListKind.PROJECT to listOfNotNull(library?.task(addInProject)?.title, addUnderHeading?.let { library?.task(it)?.title }).joinToString(" › ")
                addInArea != null -> ListKind.AREA to library?.area(addInArea)?.title.orEmpty()
                else -> ListKind.INBOX to stringResource(R.string.list_inbox)
            },
            onPickPlace = { pickingPlace = true },
            tags = newTagTitles,
            onPickTags = { pickingTags = true },
            onSubmit = { title, notes, schedule, reminder, checklist, deadline ->
                adding = false
                // A to-do filed in a project or area is never in the Inbox.
                val filedSchedule = if (filed && schedule == Schedule.Inbox) Schedule.Anytime else schedule
                viewModel.addTask(title, notes, filedSchedule, reminder, checklist, deadline, projectId = addInProject, areaId = addInArea, tagIds = newTagIds, headingId = addUnderHeading)
            },
            onDismiss = { adding = false },
        )
        NewTagsDialog()
        return
    }
    if (addingProject) {
        NewTagsDialog()
        TaskFormScreen(
            initial = TaskDraft(schedule = Schedule.Anytime),
            heading = R.string.new_project_title,
            confirm = R.string.add,
            allowSchedule = true,
            allowInbox = false,
            allowChecklist = false,
            allowDeadline = true,
            allowReminder = false,
            tags = newTagTitles,
            onPickTags = { pickingTags = true },
            onSubmit = { title, notes, schedule, _, _, deadline ->
                addingProject = false
                viewModel.addProject(title, notes, schedule, deadline, areaId = addInArea, tagIds = newTagIds)
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
    val deadlineOf = deadlineId?.let { library?.task(it) }
    if (deadlineOf != null) {
        DayPickerDialog(
            today = ThingsDay.today(),
            initial = deadlineOf.deadline,
            onPicked = { day ->
                deadlineId = null
                viewModel.setDeadline(deadlineOf.id, day)
            },
            onDismiss = { deadlineId = null },
            onClear = stringResource(R.string.no_deadline) to {
                deadlineId = null
                viewModel.setDeadline(deadlineOf.id, null)
            },
        )
    }
    val reminderOf = reminderId?.let { library?.task(it) }
    if (reminderOf != null) {
        TimePickerDialog(
            initial = reminderOf.reminder ?: LocalTime.of(9, 0),
            onPicked = {
                reminderId = null
                viewModel.setReminder(reminderOf.id, it)
            },
            onDismiss = { reminderId = null },
            onClear = {
                reminderId = null
                viewModel.setReminder(reminderOf.id, null)
            }.takeIf { reminderOf.reminder != null },
        )
    }
    val tagging = taggingId?.let { library?.task(it) }
    if (tagging != null && library != null) {
        TagsDialog(
            library = library,
            selected = tagging.tagIds.toSet(),
            onSave = { ids ->
                taggingId = null
                viewModel.setTags(tagging.id, ids)
            },
            onDismiss = { taggingId = null },
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
    deletingForever?.let { ids ->
        AlertDialog(
            onDismissRequest = { deletingForever = null },
            title = { Text(pluralStringResource(R.plurals.delete_forever_title, ids.size, ids.size)) },
            text = { Text(stringResource(R.string.delete_forever_text)) },
            confirmButton = {
                TextButton(onClick = {
                    deletingForever = null
                    if (taskId in ids) taskId = null
                    viewModel.deleteForever(ids)
                }) { Text(stringResource(R.string.delete_forever), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingForever = null }) { Text(stringResource(R.string.cancel)) } },
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
            onDelete = request.onDelete?.let { delete ->
                {
                    naming = null
                    delete()
                }
            },
        )
    }
    if (searching && library != null) {
        SearchScreen(
            library = library,
            snackbar = snackbar,
            query = query,
            onQuery = { query = it },
            actions = RowActions(
                onOpen = {
                    searching = false
                    if (it.type == TaskType.PROJECT) projectId = it.id else taskId = it.id
                },
                onSetDone = viewModel::setDone,
                onTrash = viewModel::trash,
                onRestore = viewModel::restore,
                onReschedule = { schedulingId = it.id },
                onSetCancelled = viewModel::setCancelled,
            ),
            onOpenArea = {
                searching = false
                areaId = it.id
            },
            onOpenList = {
                searching = false
                taskId = null
                projectId = null
                areaId = null
                tab = it
            },
            onDismiss = { searching = false },
        )
        // A swipe to the left on a result reschedules it, as in the lists.
        schedulingId?.let { library.task(it) }?.let { found ->
            WhenDialog(
                task = found,
                onPick = {
                    schedulingId = null
                    viewModel.reschedule(found.id, it)
                },
                onDismiss = { schedulingId = null },
            )
        }
        return
    }

    if (settingsOpen) {
        SettingsDialog(
            themeMode = state.themeMode,
            onThemeModeChange = viewModel::setThemeMode,
            navLayout = state.navLayout,
            onNavLayoutChange = viewModel::setNavLayout,
            tabOrder = ListLayout.order(ListTab.entries.map { it.name }, state.tabOrder),
            hiddenTabs = state.hiddenTabs,
            onTabsChange = viewModel::setTabs,
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
    // Back, or going to another list, ends a selection.
    BackHandler(enabled = selection != null) { selection = null }
    // Only a real change of place: rotating the screen keeps the selection.
    var selectionPlace by rememberSaveable { mutableStateOf("$tab|$projectId|$areaId") }
    LaunchedEffect(tab, projectId, areaId) {
        val place = "$tab|$projectId|$areaId"
        if (place != selectionPlace) {
            selectionPlace = place
            selection = null
        }
    }

    val today = ThingsDay.today()
    val openTask = taskId?.let { library?.task(it) }
    val openProject = projectId?.let { library?.task(it) }
    val openArea = areaId?.let { library?.area(it) }
    val rowActions = RowActions(
        selected = selection?.toSet(),
        // A coming occurrence of a repeat (its template) is not picked: nothing can be done to it from the app.
        onOpen = { if (selection == null) taskId = it.id else if (!it.isRecurrenceTemplate) toggleSelected(it.id) },
        onSetDone = viewModel::setDone,
        onTrash = viewModel::trash,
        onRestore = viewModel::restore,
        onReschedule = { schedulingId = it.id },
        onSetCancelled = viewModel::setCancelled,
        onCompleteAhead = { occurrence -> occurrence.startDate?.let { viewModel.completeAhead(occurrence.id, it) } },
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

    val picked = selection.orEmpty().mapNotNull { library?.task(it) }
    if (selectionWhen && library != null) {
        picked.firstOrNull { it.type == TaskType.TODO }?.let { first ->
            WhenDialog(
                task = first,
                onPick = { schedule ->
                    selectionWhen = false
                    viewModel.rescheduleAll(picked.map { it.id }, schedule)
                    selection = null
                },
                onDismiss = { selectionWhen = false },
            )
        } ?: run { selectionWhen = false }
    }
    if (selectionMove && library != null) {
        PlaceDialog(
            library = library,
            isProject = picked.isNotEmpty() && picked.all { it.type == TaskType.PROJECT },
            current = null,
            title = R.string.move,
            onPick = { destination ->
                selectionMove = false
                viewModel.moveAll(picked.map { it.id }, destination)
                selection = null
            },
            onDismiss = { selectionMove = false },
        )
    }

    // Areas folded closed, shared by the Projects list and the side menu.
    var foldedAreas by rememberSaveable { mutableStateOf(emptyList<String>()) }
    // The numbers Things shows next to its lists.
    val counts = remember(library) { library?.counts(ThingsDay.today()) }
    val drawer = state.navLayout == NavLayout.DRAWER
    val bottom = state.navLayout == NavLayout.BOTTOM
    var moreOpen by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    fun openList(list: ListTab) {
        taskId = null
        projectId = null
        areaId = null
        tab = list
    }
    BackHandler(enabled = drawer && drawerState.isOpen) { drawerScope.launch { drawerState.close() } }
    val screen = @Composable {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (bottom && library != null) {
                // The lists of Settings > Lists in a bar; past five, the rest sit behind More.
                val shown = (if (tab in shownTabs) shownTabs else shownTabs + tab).map { it.name }
                val split = ListLayout.bottomBar(shown, current = tab.name)
                val atList = projectId == null && areaId == null
                NavigationBar {
                    split.bar.map(ListTab::valueOf).forEach { list ->
                        NavigationBarItem(
                            selected = atList && list == tab,
                            onClick = { openList(list) },
                            icon = { CountedListIcon(list, counts, size = 22.dp) },
                            label = { Text(stringResource(list.label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                    if (split.more.isNotEmpty()) {
                        NavigationBarItem(
                            selected = false,
                            onClick = { moreOpen = true },
                            icon = { Icon(Icons.Filled.MoreVert, contentDescription = null) },
                            label = { Text(stringResource(R.string.more), maxLines = 1) },
                        )
                    }
                }
                if (moreOpen) {
                    ModalBottomSheet(onDismissRequest = { moreOpen = false }) {
                        Column(Modifier.padding(bottom = 24.dp)) {
                            split.more.map(ListTab::valueOf).forEach { list ->
                                NavigationDrawerItem(
                                    label = { Text(stringResource(list.label)) },
                                    icon = { ListIcon(list.kind, size = 22.dp) },
                                    selected = false,
                                    onClick = {
                                        moreOpen = false
                                        openList(list)
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (library != null && taskId == null && tab != ListTab.TRASH && tab != ListTab.LOGBOOK && selection == null) {
                // In a project, an area or Projects, + offers what can be created there, as in Things.
                val choices = buildList {
                    val project = projectId?.let { library.task(it) }
                    when {
                        project != null -> {
                            add(R.string.new_todo_title to { startAdding() })
                            if (library.canEditProject(project)) {
                                add(R.string.new_heading_title to {
                                    naming = NameRequest(R.string.new_heading_title) { viewModel.addHeading(project.id, it) }
                                })
                            }
                        }
                        areaId != null -> {
                            add(R.string.new_todo_title to { startAdding() })
                            add(R.string.new_project_title to { newTagIds = emptyList(); addingProject = true })
                        }
                        tab == ListTab.PROJECTS -> {
                            add(R.string.new_project_title to { newTagIds = emptyList(); addingProject = true })
                            add(R.string.new_area_title to { naming = NameRequest(R.string.new_area_title) { viewModel.addArea(it) } })
                        }
                    }
                }
                // A tap creates the first choice (a to-do wherever one fits); a long press offers them all.
                val addLabel = stringResource(choices.firstOrNull()?.first ?: R.string.add_to_inbox)
                val moreLabel = stringResource(R.string.add_choices)
                Box {
                    Surface(
                        shape = FloatingActionButtonDefaults.shape,
                        color = ListColors.checked,
                        contentColor = Color.White,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(56.dp)
                            .semantics {
                                contentDescription = addLabel
                                if (choices.size > 1) onLongClick(moreLabel) { addMenu = true; true }
                            }
                            .clip(FloatingActionButtonDefaults.shape)
                            .combinedClickable(
                                onClick = { choices.firstOrNull()?.second?.invoke() ?: startAdding() },
                                onLongClick = { if (choices.size > 1) addMenu = true },
                            ),
                    ) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, contentDescription = null) }
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
            val selecting = selection
            if (selecting != null && library != null) {
                TopAppBar(
                    title = { Text(pluralStringResource(R.plurals.selection_count, selecting.size, selecting.size)) },
                    navigationIcon = {
                        IconButton(onClick = { selection = null }) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.end_selection)) }
                    },
                    actions = {
                        val any = picked.isNotEmpty()
                        IconButton(onClick = { selectionWhen = true }, enabled = picked.any { library.canReschedule(it) && it.isOpen }) {
                            Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.reschedule))
                        }
                        IconButton(onClick = { selectionMove = true }, enabled = any && picked.any(library::canMove)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.move))
                        }
                        IconButton(onClick = {
                            viewModel.completeAll(picked.map { it.id })
                            selection = null
                        }, enabled = picked.any { it.type == TaskType.TODO && it.isOpen && library.canComplete(it) }) {
                            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.mark_done))
                        }
                        IconButton(onClick = {
                            viewModel.trashAll(picked.map { it.id })
                            selection = null
                        }, enabled = picked.any(library::canTrash)) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.move_to_trash))
                        }
                    },
                )
                return@Scaffold
            }
            TopAppBar(
                title = {
                    if (projectId == null && areaId == null) {
                        Text(stringResource(R.string.app_name), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    if (projectId != null || areaId != null) {
                        IconButton(onClick = ::back) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    } else if (drawer) {
                        IconButton(onClick = { drawerScope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.open_menu))
                        }
                    }
                },
                actions = {
                    if (state.pending > 0) {
                        TextButton(onClick = viewModel::refresh) {
                            Text(stringResource(R.string.pending_count, state.pending), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    run {
                        if (tab != ListTab.TRASH || projectId != null || areaId != null) {
                            val selectLabel = stringResource(R.string.select)
                            IconButton(onClick = { selection = emptyList() }, modifier = Modifier.semantics { contentDescription = selectLabel }) {
                                SelectGlyph(LocalContentColor.current)
                            }
                        }
                        IconButton(onClick = {
                            query = ""
                            searching = true
                        }) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
                        }
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
                openProject != null -> {
                    if (!filter.isEmpty) ActiveFilters(library, filter, viewModel::setFilter)
                    TaskList(
                        library = library,
                        header = {
                            ProjectHeader(
                                library = library,
                                project = openProject,
                                onRename = { title, notes -> viewModel.rename(openProject.id, title, notes) },
                                onMove = { movingId = openProject.id },
                                onTrash = {
                                    projectId = null
                                    viewModel.trash(openProject.id)
                                },
                                onStatus = { viewModel.setProjectStatus(openProject.id, it) },
                                onTags = { taggingId = openProject.id },
                                onReschedule = { schedulingId = openProject.id },
                                onDeadline = { deadlineId = openProject.id },
                            )
                        },
                        // Things' headings; with a filter on, headings left empty are hidden.
                        groups = library.projectSections(openProject.id, closedOn = today)
                            .map { section -> (section.heading?.let { Group.Heading(it.id, it.title) } ?: Group.None) to filter.apply(section.tasks) }
                            .filter { (group, tasks) -> filter.isEmpty || group !is Group.Heading || tasks.isNotEmpty() },
                        emptyText = stringResource(R.string.no_open_todos),
                        actions = rowActions,
                        // A "New to-do" row ends each section, filing the to-do under its heading.
                        onAdd = { group: Group -> startAdding(headingId = (group as? Group.Heading)?.id) }.takeIf { library.canEditProject(openProject) && selection == null },
                        onHeading = { heading ->
                            // Deleting a heading moves it to the Trash with its to-dos, as its swipe does.
                            val canDelete = library.task(heading.id)?.let(library::canTrash) == true
                            naming = NameRequest(
                                R.string.rename_heading,
                                heading.title,
                                onDelete = { viewModel.trash(heading.id) }.takeIf { canDelete },
                            ) { viewModel.renameHeading(heading.id, it) }
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
                                    Box(Modifier.weight(1f)) { ListHeader(ListKind.AREA, openArea.title, count = library.areaTasks(openArea.id).size) }
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
                        onAdd = { _: Group -> startAdding() }.takeIf { selection == null },
                        onDrop = { task: Task, _: Group, order: List<String> -> viewModel.reorder(task.id, order, "area:${openArea.id}") }.takeIf { filter.isEmpty },
                    )
                }
                else -> Column(Modifier.nestedScroll(pullToSearch {
                    query = ""
                    searching = true
                })) {
                    // A hidden list opened from the search shows as a tab while it is open.
                    val tabs = if (tab in shownTabs) shownTabs else shownTabs + tab
                    if (!drawer && !bottom) {
                        PrimaryScrollableTabRow(selectedTabIndex = tabs.indexOf(tab), edgePadding = 8.dp) {
                            tabs.forEach { entry ->
                                Tab(
                                    selected = tab == entry,
                                    onClick = { tab = entry },
                                    icon = { CountedListIcon(entry, counts, size = 20.dp) },
                                    text = { Text(stringResource(entry.label), maxLines = 1) },
                                )
                            }
                        }
                    }
                    if (!filter.isEmpty && tab != ListTab.PROJECTS && tab != ListTab.TRASH && tab != ListTab.LOGBOOK) ActiveFilters(library, filter, viewModel::setFilter)
                    val title = stringResource(tab.label)
                    when (tab) {
                        ListTab.PROJECTS -> ProjectList(
                            library = library,
                            title = title,
                            onOpenArea = { areaId = it },
                            onDrop = { project, area, order -> viewModel.dropProject(project.id, area, order) },
                            onTrashProject = { viewModel.trash(it.id) },
                            onDeleteArea = { deletingAreaId = it.id },
                            selected = selection?.toSet(),
                            folded = foldedAreas,
                            onFold = { foldedAreas = it },
                        ) { if (selection != null) toggleSelected(it.id) else projectId = it.id }
                        ListTab.TRASH -> TaskList(
                            library = library,
                            header = {
                                Column {
                                    ListHeader(ListKind.TRASH, title)
                                    val trash = library.trash()
                                    val deletable = trash.filter(library::canDeleteForever).map { it.id }
                                    if (deletable.isNotEmpty()) {
                                        TextButton(onClick = { deletingForever = deletable }, modifier = Modifier.padding(horizontal = 8.dp)) {
                                            Text(stringResource(R.string.empty_trash), color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            },
                            groups = listOf(Group.None to library.trash()),
                            emptyText = stringResource(R.string.trash_empty),
                            actions = rowActions,
                        )
                        // Filters leave it whole: they hide what is closed, which is all it holds.
                        ListTab.LOGBOOK -> TaskList(
                            library = library,
                            header = { ListHeader(tab.kind, title) },
                            groups = library.logbook()
                                .groupBy { it.stopDate!!.atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
                                .map { (day, items) -> Group.Day(day) to items },
                            emptyText = stringResource(R.string.logbook_empty),
                            actions = rowActions,
                        )
                        else -> {
                            val listed = when (tab) {
                                // To-dos closed today stay in place, struck through, as in Things.
                                ListTab.TODAY -> library.today(today, closedOn = today)
                                ListTab.INBOX -> library.inbox(closedOn = today)
                                // With the coming occurrences of repeating to-dos, as Things shows them.
                                ListTab.UPCOMING -> (library.upcoming(today, closedOn = today) + library.repeatsAhead(today)).sortedBy { it.upcomingDay }
                                ListTab.DEADLINES -> library.deadlines(closedOn = today)
                                ListTab.ANYTIME -> library.anytime(today, closedOn = today)
                                else -> library.someday(closedOn = today)
                            }
                            val tasks = filter.apply(listed)
                            // Classed by project: a group per project or area, under the section it shows in.
                            val grouped = filter.groupByProject && tab != ListTab.UPCOMING && tab != ListTab.DEADLINES
                            fun byProject(section: Group, items: List<Task>): List<Pair<Group, List<Task>>> {
                                if (!grouped) return listOf(section to items)
                                val places = library.byProject(items)
                                val loose = places.firstOrNull { it.first == null }?.second.orEmpty()
                                return listOf(section to loose) + places.mapNotNull { (id, items) ->
                                    val project = id?.let(library::task)
                                    val area = id?.let(library::area)
                                    when {
                                        project != null -> Group.Place(project.id, project.title, ListKind.PROJECT) to items
                                        area != null -> Group.Place(area.id, area.title, ListKind.AREA) to items
                                        else -> null
                                    }
                                }
                            }
                            val groups = when (tab) {
                                ListTab.TODAY -> {
                                    val (evening, day) = tasks.partition { it.evening }
                                    byProject(Group.None, day) + if (evening.isEmpty()) emptyList() else byProject(Group.Evening, evening)
                                }
                                // Undated items due later are filed under their deadline day, as in Things.
                                ListTab.UPCOMING -> tasks.groupBy { it.upcomingDay }.map { (date, items) ->
                                    (date?.let(Group::Day) ?: Group.None) to items
                                }
                                // Overdue first, then each deadline day, as in Things.
                                ListTab.DEADLINES -> {
                                    val (overdue, coming) = tasks.partition { it.deadline!!.isBefore(today) }
                                    listOf(Group.Overdue to overdue) + coming.groupBy { it.deadline!! }.map { (day, items) -> Group.Day(day) to items }
                                }
                                else -> byProject(Group.None, tasks)
                            }
                            TaskList(
                                library = library,
                                header = { ListHeader(tab.kind, title, count = tasks.count { it.isOpen && it.type == TaskType.TODO && !it.isRecurrenceTemplate }) },
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
                                }.takeIf { filter.isEmpty && !grouped && tab != ListTab.UPCOMING && tab != ListTab.DEADLINES },
                                // In Today only for its deadline: no Today order to change.
                                canDrag = { tab != ListTab.TODAY || it.startDate != null },
                            )
                        }
                    }
                }
            }
        }
    }
    }
    if (drawer) {
        // The side menu: the lists in the order of Settings > Lists. Opened from the menu button only, so a
        // swipe to the right on a row still means the Trash; a swipe to the left closes it.
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen,
            drawerContent = {
                ModalDrawerSheet {
                    SideMenu(
                        library = library,
                        counts = counts,
                        lists = if (tab in shownTabs) shownTabs else shownTabs + tab,
                        current = tab.takeIf { projectId == null && areaId == null },
                        currentPlace = projectId ?: areaId,
                        folded = foldedAreas,
                        onFold = { foldedAreas = it },
                        onOpen = { list ->
                            openList(list)
                            drawerScope.launch { drawerState.close() }
                        },
                        onOpenArea = { id ->
                            taskId = null
                            projectId = null
                            areaId = id
                            drawerScope.launch { drawerState.close() }
                        },
                        onOpenProject = { id ->
                            taskId = null
                            projectId = id
                            drawerScope.launch { drawerState.close() }
                        },
                    )
                }
            },
        ) { screen() }
    } else {
        screen()
    }

    // A to-do opens in a sheet sliding up over the list, as in Todoist; swiping it down or Back closes it.
    if (openTask != null && library != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { taskId = null }, sheetState = sheetState) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
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
                if (openTask.type == TaskType.TODO) {
                    val context = LocalContext.current
                    val chooserTitle = stringResource(R.string.share)
                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, openTask.title)
                            putExtra(Intent.EXTRA_TEXT, Sharing.text(openTask.title, openTask.notes, library.checklist(openTask.id)))
                        }
                        context.startActivity(Intent.createChooser(send, chooserTitle))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = chooserTitle)
                    }
                }
            }
            TaskDetail(
                library = library,
                task = openTask,
                onSetDone = viewModel::setDone,
                onChecklistDone = viewModel::setChecklistItemDone,
                onAddChecklistItem = viewModel::addChecklistItem,
                onDeleteChecklistItem = viewModel::deleteChecklistItem,
                onRenameChecklistItem = viewModel::renameChecklistItem,
                onRename = viewModel::rename,
                onReminder = { reminderId = it },
                onRestore = viewModel::restore,
                onDeleteForever = { deletingForever = listOf(it) },
                onTags = { taggingId = it },
                onReschedule = { schedulingId = it },
                onDeadline = { deadlineId = it },
                onMove = { movingId = it },
                onSetCancelled = viewModel::setCancelled,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            )
        }
    }
}

/**
 * Pulling a list down past its top, by about a row's height, opens the search, as in Things.
 * A fling or a scroll back up starts the count again.
 */
@Composable
private fun pullToSearch(onPull: () -> Unit): NestedScrollConnection {
    val threshold = with(LocalDensity.current) { 96.dp.toPx() }
    val pull by rememberUpdatedState(onPull)
    return remember(threshold) {
        object : NestedScrollConnection {
            var pulled = 0f

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    pulled += available.y
                    if (pulled > threshold) {
                        pulled = Float.NEGATIVE_INFINITY
                        pull()
                    }
                } else if (consumed.y != 0f || available.y < 0f) {
                    pulled = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pulled = 0f
                return Velocity.Zero
            }
        }
    }
}

/** Quick Find over to-dos, projects and areas; a result opens where it is. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreen(
    library: ThingsLibrary,
    snackbar: SnackbarHostState,
    query: String,
    onQuery: (String) -> Unit,
    actions: RowActions,
    onOpenArea: (Area) -> Unit,
    onOpenList: (ListTab) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val result = remember(library, query) { library.search(query) }
    // Lists by name, hidden ones included: the way to open a list taken out of the tabs.
    val listNames = ListTab.entries.associateWith { stringResource(it.label) }
    val lists = remember(query, listNames) {
        val wanted = foldForSearch(query.trim())
        if (wanted.isEmpty()) emptyList() else ListTab.entries.filter { foldForSearch(listNames.getValue(it)).contains(wanted) }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
                title = {
                    TextField(
                        value = query,
                        onValueChange = onQuery,
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                },
                actions = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQuery("") }) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear_search)) }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding()) {
            items(lists, key = { "list-" + it.name }) { list ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpenList(list) }.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ListIcon(list.kind, size = 20.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(listNames.getValue(list), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
            }
            if (query.isNotBlank() && lists.isEmpty() && result.tasks.isEmpty() && result.areas.isEmpty()) {
                item(key = "none") {
                    Text(stringResource(R.string.search_none), Modifier.padding(20.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(result.areas, key = { "area-" + it.id }) { area ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpenArea(area) }.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ListIcon(ListKind.AREA, size = 20.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(area.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            items(result.tasks, key = { it.id }) { task ->
                // The same swipes as in the lists: left reschedules, right moves to the Trash.
                SwipeActions(
                    canReschedule = task.type == TaskType.TODO && library.canReschedule(task) && task.isOpen,
                    canTrash = library.canTrash(task),
                    onReschedule = { actions.onReschedule(task) },
                    onTrash = { actions.onTrash(task.id) },
                ) {
                    if (task.type == TaskType.PROJECT) ProjectRow(library, task, onOpen = actions.onOpen) else TaskRow(library, task, actions, showWhen = true)
                }
            }
        }
    }
}

private class RowActions(
    /** While selecting: the ids picked; a tap picks or drops a row instead of opening it. */
    val selected: Set<String>? = null,
    val onOpen: (Task) -> Unit,
    val onSetDone: (String, Boolean) -> Unit,
    val onTrash: (String) -> Unit,
    val onRestore: (String) -> Unit,
    val onReschedule: (Task) -> Unit,
    val onSetCancelled: (String, Boolean) -> Unit = { _, _ -> },
    /** A repeat's coming occurrence in Upcoming (its template, dated) checked before Things created it. */
    val onCompleteAhead: (Task) -> Unit = {},
)

/**
 * A to-do's checkbox: a tap checks it (or reopens it when cancelled), a long press offers to
 * complete, cancel or reopen it, as in Things.
 */
@Composable
private fun TodoCheckbox(
    library: ThingsLibrary,
    task: Task,
    onSetDone: (String, Boolean) -> Unit,
    onSetCancelled: (String, Boolean) -> Unit,
    size: androidx.compose.ui.unit.Dp = 22.dp,
) {
    val canCheck = library.canComplete(task)
    // A long press cancels, repeats included ("after completion" ones move their template on, as checking them does).
    val canCancel = canCheck
    val cancelled = task.status == TaskStatus.CANCELLED
    var menu by remember { mutableStateOf(false) }
    Box {
        ThingsCheckbox(
            checked = task.status == TaskStatus.COMPLETED,
            cancelled = cancelled,
            onCheckedChange = { if (cancelled) onSetCancelled(task.id, false) else onSetDone(task.id, it) },
            enabled = canCheck,
            size = size,
            onLongClick = { menu = true }.takeIf { canCancel },
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            if (task.status != TaskStatus.COMPLETED) {
                DropdownMenuItem(text = { Text(stringResource(R.string.complete_todo)) }, onClick = {
                    menu = false
                    onSetDone(task.id, true)
                })
            }
            if (!cancelled) {
                DropdownMenuItem(text = { Text(stringResource(R.string.cancel_todo)) }, onClick = {
                    menu = false
                    onSetCancelled(task.id, true)
                })
            }
            if (!task.isOpen) {
                DropdownMenuItem(text = { Text(stringResource(R.string.reopen_todo)) }, onClick = {
                    menu = false
                    if (cancelled) onSetCancelled(task.id, false) else onSetDone(task.id, false)
                })
            }
        }
    }
}

/**
 * Where to move a to-do (Inbox, a project, one of its headings, an area) or a project (an area,
 * or none). The current place is marked.
 */
@Composable
private fun MoveDialog(library: ThingsLibrary, item: Task, onPick: (PendingChange.Destination) -> Unit, onDismiss: () -> Unit) {
    val current = when {
        item.headingId != null -> PendingChange.Destination(headingId = item.headingId)
        item.projectId != null -> PendingChange.Destination(projectId = item.projectId)
        item.areaId != null -> PendingChange.Destination(areaId = item.areaId)
        item.start == StartList.INBOX -> PendingChange.Destination(inbox = true)
        else -> null
    }
    PlaceDialog(library, item.type == TaskType.PROJECT, current, R.string.move, onPick, onDismiss)
}

/**
 * Inbox, projects with their headings, and areas with their projects, for a to-do; areas for a
 * project ([isProject]). [current] is marked and cannot be picked.
 */
@Composable
private fun PlaceDialog(
    library: ThingsLibrary,
    isProject: Boolean,
    current: PendingChange.Destination?,
    @StringRes title: Int,
    onPick: (PendingChange.Destination) -> Unit,
    onDismiss: () -> Unit,
    headings: Boolean = true,
) {
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
    // Every choice in the tree's order; a search lists the matching ones flat, a heading with its project.
    class Entry(val kind: ListKind?, val label: String, val indent: Int, val isCurrent: Boolean, val destination: PendingChange.Destination, val searchLabel: String = label)
    val inboxLabel = stringResource(R.string.list_inbox)
    val noAreaLabel = stringResource(R.string.no_area)
    val entries = remember(library, isProject, current, headings) {
        buildList {
            if (isProject) {
                add(Entry(null, noAreaLabel, 0, current?.areaId == null, PendingChange.Destination()))
                library.areas().forEach { area -> add(Entry(ListKind.AREA, area.title, 0, current?.areaId == area.id, PendingChange.Destination(areaId = area.id))) }
            } else {
                add(Entry(ListKind.INBOX, inboxLabel, 0, current?.inbox == true, PendingChange.Destination(inbox = true)))
                fun project(project: Task, indent: Int) {
                    add(Entry(ListKind.PROJECT, project.title, indent, current?.projectId == project.id, PendingChange.Destination(projectId = project.id)))
                    if (headings) library.projectSections(project.id).mapNotNull { it.heading }.forEach { heading ->
                        add(Entry(null, heading.title, indent + 1, current?.headingId == heading.id, PendingChange.Destination(headingId = heading.id), "${project.title} › ${heading.title}"))
                    }
                }
                library.projects().filter { it.areaId == null }.forEach { project(it, 0) }
                library.areas().forEach { area ->
                    add(Entry(ListKind.AREA, area.title, 0, current?.areaId == area.id, PendingChange.Destination(areaId = area.id)))
                    library.areaProjects(area.id).forEach { project(it, 1) }
                }
            }
        }
    }
    var query by rememberSaveable { mutableStateOf("") }
    val wanted = foldForSearch(query.trim())
    val shown = if (wanted.isEmpty()) entries else entries.filter { foldForSearch(it.searchLabel).contains(wanted) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isProject) R.string.move_to_area else title)) },
        text = {
            Column {
                SearchField(query, onQuery = { query = it }, placeholder = stringResource(R.string.place_search))
                if (shown.isEmpty()) {
                    Text(stringResource(R.string.search_none), Modifier.padding(vertical = 8.dp), color = muted)
                }
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(shown, key = { it.destination.toString() }) { entry ->
                        val label = if (wanted.isEmpty()) entry.label else entry.searchLabel
                        Choice(entry.kind, label, if (wanted.isEmpty()) entry.indent else 0, entry.isCurrent, entry.destination)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * How a to-do repeats, from the app: every N days, weeks, months or years, on a fixed schedule
 * (weekly on chosen days; monthly or yearly on the to-do's day, its weekday's rank, or the last
 * day of its month) or after completion. The rule starts on the to-do's day, [start], which a
 * fixed schedule always includes.
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
    // Monthly: 0 on the day of the month, 1 on the weekday's rank, 2 on the last day.
    var monthPattern by rememberSaveable { mutableIntStateOf(0) }
    val weekly = unit == RepeatUnit.WEEK && !afterCompletion
    val monthly = (unit == RepeatUnit.MONTH || unit == RepeatUnit.YEAR) && !afterCompletion
    val yearMonth = start.month.takeIf { unit == RepeatUnit.YEAR }
    val rank = (start.dayOfMonth - 1) / 7 + 1
    val isLastDay = start.dayOfMonth == start.lengthOfMonth()
    // Several days only every week: which week Things counts from is unknown.
    val chosen = if (every > 1) listOf(start.dayOfWeek.value) else days
    val rule = when {
        weekly -> RepeatRule(unit, every, false, weekdays = chosen.sorted().map(DayOfWeek::of))
        monthly && monthPattern == 1 -> RepeatRule(unit, every, false, weekdays = listOf(start.dayOfWeek), month = yearMonth, weekdayOrdinal = rank)
        monthly && monthPattern == 2 -> RepeatRule(unit, every, false, monthDay = -1, month = yearMonth)
        monthly -> RepeatRule(unit, every, false, monthDay = start.dayOfMonth, month = yearMonth)
        else -> RepeatRule(unit, every, afterCompletion)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.repeat_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = unit == RepeatUnit.DAY, onClick = { unit = RepeatUnit.DAY }, label = { Text(stringResource(R.string.repeat_unit_days)) })
                    FilterChip(selected = unit == RepeatUnit.WEEK, onClick = { unit = RepeatUnit.WEEK }, label = { Text(stringResource(R.string.repeat_unit_weeks)) })
                    FilterChip(selected = unit == RepeatUnit.MONTH, onClick = { unit = RepeatUnit.MONTH }, label = { Text(stringResource(R.string.repeat_unit_months)) })
                    FilterChip(selected = unit == RepeatUnit.YEAR, onClick = { unit = RepeatUnit.YEAR }, label = { Text(stringResource(R.string.repeat_unit_years)) })
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
                if (monthly) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val ofMonth = yearMonth?.let { " " + ofMonth(context, it, locale) }.orEmpty()
                        val dayLabel = if (yearMonth != null) {
                            stringResource(R.string.repeat_pick_text, start.format(DateTimeFormatter.ofPattern(stringResource(R.string.repeat_month_day_pattern), locale)))
                        } else stringResource(R.string.repeat_pick_day, start.dayOfMonth)
                        FilterChip(selected = monthPattern == 0, onClick = { monthPattern = 0 }, label = { Text(dayLabel) })
                        // A fifth weekday is not in every month.
                        if (rank <= 4) FilterChip(
                            selected = monthPattern == 1,
                            onClick = { monthPattern = 1 },
                            label = { Text(stringResource(R.string.repeat_pick_nth, ordinalName(context, rank), start.dayOfWeek.getDisplayName(TextStyle.FULL, locale)) + ofMonth) },
                        )
                        if (isLastDay) FilterChip(selected = monthPattern == 2, onClick = { monthPattern = 2 }, label = { Text(stringResource(R.string.repeat_pick_last) + ofMonth) })
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
/** A name to type; with [onDelete], the dialog also offers to delete what is named. */
private class NameRequest(@StringRes val title: Int, val initial: String = "", val onDelete: (() -> Unit)? = null, val onName: (String) -> Unit)

@Composable
private fun NameDialog(title: String, initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null) {
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
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

/** A section of a list: Today's This Evening, a day of Upcoming, a project's heading, or none. */
private sealed interface Group {
    data object None : Group
    data class Heading(val id: String, val title: String) : Group
    data object Evening : Group
    data class Day(val date: LocalDate) : Group
    /** The Deadlines list's first section: what is past its deadline. */
    data object Overdue : Group
    /** To-dos of one project or area, when a list is grouped by project. */
    data class Place(val id: String, val title: String, val kind: ListKind) : Group
}

/** The list's icon and name in large type, as Things opens each list; [count] open to-dos after it, when there are any. */
@Composable
private fun ListHeader(kind: ListKind, title: String, count: Int? = null) {
    Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        ListIcon(kind, size = 28.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        OpenCount(count)
    }
}

/** How many open to-dos a list or project holds, in grey after its name; nothing when there are none. */
@Composable
private fun OpenCount(count: Int?) {
    if (count == null || count <= 0) return
    val label = pluralStringResource(R.plurals.open_todos_count, count, count)
    Text(
        count.toString(),
        Modifier.padding(start = 10.dp).semantics { contentDescription = label },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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
        is Group.Place -> Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ListIcon(group.kind, size = 16.dp)
                Spacer(Modifier.width(8.dp))
                Text(group.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            HorizontalDivider(Modifier.padding(top = 6.dp))
        }
        Group.Overdue -> Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp)) {
            Text(stringResource(R.string.overdue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ListColors.deadline)
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
        today.minusDays(1) -> stringResource(R.string.yesterday)
        else -> {
            val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            // Within a week either way the weekday is enough; further off, the Logbook's past days too, the month says which.
            if (date.isBefore(today.plusDays(7)) && date.isAfter(today.minusDays(7))) weekday else "$weekday · ${date.format(DateTimeFormatter.ofPattern("MMM yyyy"))}"
        }
    }.replaceFirstChar { it.titlecase() }
}

/** A row of a [TaskList]: a section header or a to-do. */
private sealed interface ListRow {
    val key: String
}

private data class HeaderRow(val group: Group, override val key: String) : ListRow

private data class TaskItemRow(val task: Task) : ListRow {
    // A repeat's coming occurrences are copies of its template, one per day.
    override val key get() = if (task.isRecurrenceTemplate) "${task.id}@${task.startDate}" else task.id
}

/** The "New to-do" row ending a section. */
private data class AddRow(val group: Group, override val key: String) : ListRow

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
    canDrag: (Task) -> Boolean = { true },
    /** With it, each section ends with a "New to-do" row adding there. */
    onAdd: ((Group) -> Unit)? = null,
) {
    val adding = onAdd != null
    val built = remember(groups, adding) {
        groups.withIndex().flatMap { (index, entry) ->
            val (group, tasks) = entry
            val id = (group as? Group.Heading)?.id ?: index.toString()
            val add = if (adding) listOf(AddRow(group, "add-$id")) else emptyList()
            // A heading shows even when empty, as in Things; so does This Evening when project groups follow it.
            val keep = group is Group.Heading || (group == Group.Evening && groups.getOrNull(index + 1)?.first is Group.Place)
            if (tasks.isEmpty() && !keep) add
            else listOf(HeaderRow(group, "group-$id")) + tasks.map(::TaskItemRow) + add
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
                // The section's to-dos up to the next header; its "New to-do" row may sit among them mid-drag.
                val order = rows.drop(headerIndex + 1).takeWhile { it !is HeaderRow }.filterIsInstance<TaskItemRow>().map { it.key }
                onDrop?.invoke(row.task, section, order)
            }
            is HeaderRow -> {
                val heading = row.group as? Group.Heading ?: return
                onHeadingsDrop?.invoke(heading.id, rows.mapNotNull { ((it as? HeaderRow)?.group as? Group.Heading)?.id })
            }
            is AddRow, null -> Unit
        }
    }
    fun draggable(row: ListRow) = actions.selected == null && when (row) {
        is TaskItemRow -> onDrop != null && row.task.isOpen && library.canReorder(row.task) && canDrag(row.task)
        is HeaderRow -> onHeadingsDrop != null && row.group is Group.Heading
        is AddRow -> false
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
                        is AddRow -> NewTodoRow { onAdd?.invoke(row.group) }
                        is TaskItemRow -> {
                            val task = row.task
                            SwipeActions(
                                canReschedule = actions.selected == null && library.canReschedule(task) && task.isOpen,
                                canTrash = actions.selected == null && library.canTrash(task),
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

/** "+ New to-do" in grey at the end of a section, where the to-do will be added. */
@Composable
private fun NewTodoRow(onClick: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(start = 20.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, Modifier.size(18.dp), tint = ListColors.checked)
        Spacer(Modifier.width(14.dp))
        Text(stringResource(R.string.new_todo_title), style = MaterialTheme.typography.bodyMedium, color = muted)
    }
}

/** How far a row must be dragged to the right, as a share of its width, before it goes to the Trash. */
private const val TRASH_SWIPE_SHARE = 0.5f

/**
 * Row swipes: to the left opens When to reschedule (the more frequent action gets the easier
 * gesture), to the right moves the to-do to the Trash (the snackbar offers Undo). The Trash needs
 * the row dragged at least halfway across: a short fling slides back instead of deleting.
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
    var width by remember { mutableIntStateOf(0) }
    // The state is read by its own confirmation, for how far the row was dragged.
    val holder = remember { arrayOfNulls<androidx.compose.material3.SwipeToDismissBoxState>(1) }
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
                        val dragged = holder[0]?.let { state -> runCatching { state.requireOffset() }.getOrNull() } ?: 0f
                        if (width > 0 && dragged >= width * TRASH_SWIPE_SHARE) {
                            onTrash()
                            true
                        } else {
                            false
                        }
                    }
                    SwipeToDismissBoxValue.Settled -> true
                }
            },
        ).also { holder[0] = it }
    }
    LaunchedEffect(swipe.currentValue) {
        if (swipe.currentValue != SwipeToDismissBoxValue.Settled) {
            delay(1500)
            generation++
        }
    }
    SwipeToDismissBox(
        state = swipe,
        modifier = Modifier.onSizeChanged { width = it.width },
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
        if (task.type == TaskType.TODO && task.projectId == null && task.areaId == null) add(Triple(ListKind.INBOX, stringResource(R.string.list_inbox)) { onPick(Schedule.Inbox) })
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
    // A repeat's coming occurrence, not created yet: in grey, as Things shows it.
    val ahead = task.isRecurrenceTemplate
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable { actions.onOpen(task) }
            .padding(start = 8.dp, end = 16.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val selected = actions.selected
        if (selected != null) {
            SelectionMark(task.id in selected)
        } else if (ahead) {
            // Checked ahead as in Things 3.23: the next occurrence only, the following ones once it is done.
            ThingsCheckbox(
                checked = false,
                onCheckedChange = { if (it) actions.onCompleteAhead(task) },
                enabled = library.canCompleteAhead(task),
            )
        } else {
            TodoCheckbox(library, task, actions.onSetDone, actions.onSetCancelled)
        }
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(
                task.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (closed) TextDecoration.LineThrough else null,
                color = if (closed || task.trashed || ahead) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
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
            // As in Things: how many days are left, or how late it is.
            Row(verticalAlignment = Alignment.CenterVertically) {
                DeadlineFlag()
                Spacer(Modifier.width(2.dp))
                Text(deadlineText(LocalContext.current, deadline, ThingsDay.today()), style = small, color = ListColors.deadline)
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
    selected: Set<String>? = null,
    folded: List<String> = emptyList(),
    onFold: (List<String>) -> Unit = {},
    onOpen: (Task) -> Unit,
) {
    val projects = library.projects()
    val areas = library.areas()
    // Areas folded closed ([folded]): their projects are hidden under the header.
    // Area headers (the first, with no area, draws nothing) and projects, in display order.
    val built = remember(library, folded) {
        listOf<Pair<String, Any?>>("area-none" to null) + projects.filter { it.areaId == null }.map { it.id to it } +
            areas.flatMap { area ->
                listOf("area-${area.id}" to area) + if (area.id in folded) emptyList() else library.areaProjects(area.id).map { it.id to it }
            }
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
        val shown = rows.drop(headerIndex + 1).takeWhile { it.second is Task }.map { it.first }
        // Dropped on a folded area: first, before its hidden projects.
        val hidden = area?.takeIf { it.id in folded }?.let { library.areaProjects(it.id).map(Task::id).filter { id -> id !in shown } }.orEmpty()
        onDrop?.invoke(project, area?.id, shown + hidden)
    }
    LazyColumn(Modifier.fillMaxSize(), state = listState) {
        item(key = "header") { ListHeader(ListKind.PROJECT, title) }
        if (projects.isEmpty() && areas.isEmpty()) {
            item(key = "empty") { Text(stringResource(R.string.nothing_here), Modifier.padding(20.dp)) }
        }
        items(rows, key = { it.first }) { (key, value) ->
            val project = value as? Task
            val canDrag = selected == null && project != null && onDrop != null && library.canEditProject(project)
            ReorderableItem(reorderState, key = key, enabled = canDrag) { dragging ->
                val handle = if (canDrag) Modifier.longPressDraggableHandle(onDragStopped = { dropped(project!!) }) else Modifier
                Box(handle.then(if (dragging) Modifier.shadow(8.dp) else Modifier).background(MaterialTheme.colorScheme.surface)) {
                    when (value) {
                        is Task -> SwipeActions(
                            canReschedule = false,
                            canTrash = selected == null && library.canTrash(value),
                            onReschedule = {},
                            onTrash = { onTrashProject(value) },
                        ) { ProjectRow(library, value, selected = selected?.let { value.id in it }, onOpen = onOpen) }
                        // Every area, even one without projects, opens on its own to-dos and projects as in Things;
                        // its arrow folds its projects away or back. Swiped right, an area asks before it is deleted.
                        is Area -> SwipeActions(canReschedule = false, canTrash = true, onReschedule = {}, onTrash = { onDeleteArea(value) }) {
                            val isFolded = value.id in folded
                            Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenArea(value.id) }
                                        .padding(start = 20.dp, top = 12.dp, bottom = 0.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    ListIcon(ListKind.AREA, size = 20.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        value.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    // How much the area holds, shown when folded, as Todoist counts a section.
                                    val count = library.areaProjects(value.id).size + library.areaTasks(value.id).size
                                    if (count > 0) {
                                        Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { onFold(if (isFolded) folded - value.id else folded + value.id) }) {
                                        Icon(
                                            if (isFolded) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowDown,
                                            contentDescription = stringResource(if (isFolded) R.string.unfold_area else R.string.fold_area),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                HorizontalDivider(Modifier.padding(start = 20.dp, end = 20.dp))
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
    onRename: (title: String, notes: String) -> Unit,
    onMove: () -> Unit,
    onTrash: () -> Unit,
    onStatus: (TaskStatus) -> Unit,
    onTags: () -> Unit = {},
    onReschedule: () -> Unit = {},
    onDeadline: () -> Unit = {},
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
            // A tap completes it; a long press cancels it (or reopens it once closed), as in Things.
            ThingsCheckbox(
                checked = project.status == TaskStatus.COMPLETED,
                cancelled = project.status == TaskStatus.CANCELLED,
                onCheckedChange = { request(if (it && project.status == TaskStatus.OPEN) TaskStatus.COMPLETED else TaskStatus.OPEN) },
                onLongClick = { if (project.status == TaskStatus.OPEN) request(TaskStatus.CANCELLED) else onStatus(TaskStatus.OPEN) },
                enabled = editable,
                size = 26.dp,
                contentDescription = stringResource(R.string.complete_project),
            )
            // A tap on the title or the notes changes them in place.
            val titleStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            InlineEditable(
                value = project.title,
                onSave = { title: String -> onRename(title, project.notes.orEmpty()) }.takeIf { editable },
                singleLine = true,
                textStyle = titleStyle,
                withEmoji = true,
                modifier = Modifier.weight(1f, fill = false),
            ) { tap ->
                Text(
                    project.title,
                    style = titleStyle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (project.isOpen) null else TextDecoration.LineThrough,
                    modifier = tap,
                )
            }
            OpenCount(library.projectTasks(project.id).size)
            Spacer(Modifier.weight(1f))
            if (editable) {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.move_to_area)) }, onClick = {
                            menu = false
                            onMove()
                        })
                        if (project.deadline == null && project.isOpen) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.add_deadline)) }, onClick = {
                                menu = false
                                onDeadline()
                            })
                        }
                        if (library.canTag(project)) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.tags_edit)) }, onClick = {
                                menu = false
                                onTags()
                            })
                        }
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
        // When, area and deadline, as in a to-do's details: a tap on When or the area changes it.
        val muted = MaterialTheme.colorScheme.onSurfaceVariant
        val small = MaterialTheme.typography.labelLarge
        FlowRow(Modifier.padding(start = 36.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.Center) {
            val (whenKind, whenLabel) = whenFact(project)
            Row(
                Modifier.clip(MaterialTheme.shapes.small)
                    .clickable(enabled = editable, onClickLabel = stringResource(R.string.reschedule), onClick = onReschedule)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ListIcon(whenKind, size = 14.dp)
                Spacer(Modifier.width(4.dp))
                Text(whenLabel, style = small, color = muted)
            }
            Row(
                Modifier.clip(MaterialTheme.shapes.small)
                    .clickable(enabled = editable, onClickLabel = stringResource(R.string.move_to_area), onClick = onMove)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val area = project.areaId?.let { library.area(it)?.title }
                ListIcon(ListKind.AREA, size = 14.dp)
                Spacer(Modifier.width(4.dp))
                Text(area ?: stringResource(R.string.no_area), style = small, color = muted)
            }
            project.deadline?.let {
                Row(
                    Modifier.clip(MaterialTheme.shapes.small)
                        .clickable(enabled = editable && project.isOpen, onClickLabel = stringResource(R.string.deadline), onClick = onDeadline)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DeadlineFlag()
                    Spacer(Modifier.width(4.dp))
                    Text(it.format(dateFormat), style = small, color = ListColors.deadline)
                }
            }
        }
        val tags = library.tagTitles(project)
        if (tags.isNotEmpty()) {
            FlowRow(
                Modifier.padding(start = 44.dp, top = 4.dp).clickable(enabled = library.canTag(project), onClickLabel = stringResource(R.string.tags_edit)) { onTags() },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) { tags.forEach { TagPill(it) } }
        }
        // The project's notes, as Things shows them under its title; "No notes" only when they can be added.
        if (!project.notes.isNullOrBlank() || editable) {
            val notesStyle = MaterialTheme.typography.bodyMedium
            InlineEditable(
                value = project.notes.orEmpty(),
                onSave = { notes: String -> onRename(project.title, notes) }.takeIf { editable },
                singleLine = false,
                textStyle = notesStyle,
                allowBlank = true,
                markdown = true,
                modifier = Modifier.padding(start = 44.dp, end = 12.dp, top = 6.dp),
            ) { tap -> NotesText(project.notes, notesStyle, tap, selectable = !editable) }
        }
    }
}

/** A project in a list: its circle, title and count of open to-dos. */
@Composable
private fun ProjectRow(library: ThingsLibrary, project: Task, selected: Boolean? = null, onOpen: (Task) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).clickable { onOpen(project) }.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected != null) SelectionMark(selected, Modifier.padding(end = 4.dp))
        ListIcon(ListKind.PROJECT, size = 20.dp, progress = library.projectProgress(project.id))
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
    onDeleteForever: (String) -> Unit,
    onRenameChecklistItem: (String, String) -> Unit = { _, _ -> },
    onRename: (id: String, title: String, notes: String) -> Unit = { _, _, _ -> },
    onReminder: (String) -> Unit = {},
    onTags: (String) -> Unit = {},
    onReschedule: (String) -> Unit = {},
    onDeadline: (String) -> Unit = {},
    onMove: (String) -> Unit = {},
    onSetCancelled: (String, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val focusManager = LocalFocusManager.current
    // A repeating template opened from Upcoming stays as Things manages it.
    val textEditable = task.type == TaskType.TODO && !task.trashed && !task.isRecurrenceTemplate
    Column(
        modifier.imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (task.trashed) {
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.in_trash), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                if (library.canDeleteForever(task)) {
                    TextButton(onClick = { onDeleteForever(task.id) }) { Text(stringResource(R.string.delete_forever), color = MaterialTheme.colorScheme.error) }
                }
                if (library.canRestore(task)) TextButton(onClick = { onRestore(task.id) }) { Text(stringResource(R.string.restore)) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (task.type == TaskType.TODO) {
                TodoCheckbox(library, task, onSetDone, onSetCancelled, size = 24.dp)
            } else {
                ListIcon(ListKind.PROJECT, Modifier.padding(10.dp), size = 24.dp, progress = library.projectProgress(task.id))
            }
            // Title and notes change in place, where the edit form allowed it: an untrashed to-do.
            val titleStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
            InlineEditable(
                value = task.title,
                onSave = { title: String -> onRename(task.id, title, task.notes.orEmpty()) }.takeIf { textEditable },
                singleLine = true,
                textStyle = titleStyle,
                withEmoji = true,
                modifier = Modifier.weight(1f),
            ) { tap ->
                Text(
                    task.title,
                    style = titleStyle,
                    textDecoration = if (task.isOpen) null else TextDecoration.LineThrough,
                    modifier = tap,
                )
            }
        }
        if (task.type == TaskType.TODO && !library.canComplete(task) && !task.trashed && task.isRepeating) {
            Text(stringResource(R.string.repeating_locked), Modifier.padding(start = 12.dp), style = MaterialTheme.typography.labelMedium, color = muted)
        }

        Column(Modifier.padding(start = 12.dp, end = 12.dp)) {
            val notesStyle = MaterialTheme.typography.bodyLarge
            InlineEditable(
                value = task.notes.orEmpty(),
                onSave = { notes: String -> onRename(task.id, task.title, notes) }.takeIf { textEditable },
                singleLine = false,
                textStyle = notesStyle,
                allowBlank = true,
                markdown = true,
            ) { tap ->
                NotesText(task.notes, notesStyle, tap, selectable = !textEditable)
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
                        val itemStyle = MaterialTheme.typography.bodyMedium
                        InlineEditable(
                            value = item.title,
                            onSave = { title: String -> onRenameChecklistItem(item.id, title) }.takeIf { editable },
                            singleLine = true,
                            textStyle = itemStyle,
                            modifier = Modifier.weight(1f),
                        ) { tap ->
                            Text(
                                item.title,
                                style = itemStyle,
                                textDecoration = if (item.done) TextDecoration.LineThrough else null,
                                color = if (item.done) muted else Color.Unspecified,
                                modifier = tap,
                            )
                        }
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
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        HorizontalDivider(Modifier.padding(horizontal = 12.dp))
        DetailFacts(
            library,
            task,
            onTags = { onTags(task.id) }.takeIf { library.canTag(task) },
            onWhen = { onReschedule(task.id) }.takeIf { library.canReschedule(task) && task.isOpen },
            onDeadline = { onDeadline(task.id) }.takeIf { library.canSetDeadline(task) && task.isOpen },
            onPlace = { onMove(task.id) }.takeIf { library.canMove(task) },
            // A reminder needs a day, as in the form.
            onReminder = { onReminder(task.id) }.takeIf { task.type == TaskType.TODO && library.canReschedule(task) && task.isOpen && task.startDate != null },
        )
    }
}

/**
 * Text that a tap turns into a field, when [onSave] is set; [display] draws it, with the tap
 * modifier it is given. The change is saved on the keyboard's Done (one line), when the field
 * loses focus, or when it leaves the screen (the sheet closed); a blank title is not saved.
 * [withEmoji] adds the emoji picker that leads a title; [markdown] styles notes as they are typed.
 */
@Composable
private fun InlineEditable(
    value: String,
    onSave: ((String) -> Unit)?,
    singleLine: Boolean,
    textStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    allowBlank: Boolean = false,
    withEmoji: Boolean = false,
    markdown: Boolean = false,
    display: @Composable (tap: Modifier) -> Unit,
) {
    var editing by rememberSaveable(value) { mutableStateOf(false) }
    if (!editing || onSave == null) {
        Box(modifier) { display(if (onSave != null) Modifier.clickable(onClickLabel = stringResource(R.string.edit)) { editing = true } else Modifier) }
        return
    }
    var text by rememberSaveable(value) { mutableStateOf(value) }
    var pickingEmoji by remember { mutableStateOf(false) }
    // Saved once per edit, whichever of Done, focus loss or leaving comes first.
    val saved = remember { booleanArrayOf(false) }
    val latest by rememberUpdatedState(text)
    val save by rememberUpdatedState(onSave)
    fun commit() {
        if (saved[0]) return
        saved[0] = true
        val changed = latest.trim() != value.trim()
        if (changed && (allowBlank || latest.isNotBlank())) save(latest)
        editing = false
    }
    DisposableEffect(Unit) { onDispose { commit() } }
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    if (pickingEmoji) {
        EmojiDialog(
            current = TitleEmoji.leading(text),
            onPick = {
                pickingEmoji = false
                text = TitleEmoji.apply(text, it)
                commit()
            },
            onDismiss = {
                pickingEmoji = false
                commit()
            },
        )
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.text.BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = singleLine,
            textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = if (markdown) MarkdownTransformation(markdownColors()) else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = if (singleLine) ImeAction.Done else ImeAction.Default),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            modifier = Modifier.weight(1f).focusRequester(focus).onFocusChanged {
                // The emoji picker takes the focus away: its choice saves instead.
                if (it.isFocused) focused = true else if (focused && !pickingEmoji) commit()
            },
        )
        if (withEmoji) {
            IconButton(onClick = { pickingEmoji = true }) { Text(TitleEmoji.leading(text) ?: "☺", fontSize = 20.sp) }
        }
    }
}

/** A to-do's notes in Markdown, links included (an Obsidian link without the app shows a short message); "No notes" when empty. */
@Composable
private fun NotesText(notes: String?, style: androidx.compose.ui.text.TextStyle, modifier: Modifier, selectable: Boolean) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    if (notes.isNullOrBlank()) {
        Text(stringResource(R.string.no_notes), modifier, style = style, color = muted)
        return
    }
    val colors = markdownColors()
    val context = LocalContext.current
    val noApp = stringResource(R.string.link_no_app)
    val links = remember(context, noApp) {
        object : UriHandler {
            override fun openUri(uri: String) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, noApp, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    CompositionLocalProvider(LocalUriHandler provides links) {
        val text = @Composable { Text(remember(notes, colors) { markdown(notes, colors, clickableLinks = true) }, modifier.fillMaxWidth(), style = style) }
        // Notes that can't be edited can still be selected to copy; editable ones are copied from their field.
        if (selectable) SelectionContainer { text() } else text()
    }
}

/** Where a to-do or project is scheduled, as the icon and name of its list (a later day by its date). */
@Composable
private fun whenFact(task: Task): Pair<ListKind, String> {
    val today = ThingsDay.today()
    // A repeating template: the day of its next occurrence.
    val startDate = task.startDate ?: task.nextOccurrence.takeIf { task.isRecurrenceTemplate }
    return when {
        task.start == StartList.INBOX && startDate == null -> ListKind.INBOX to stringResource(R.string.list_inbox)
        startDate == null -> if (task.start == StartList.SOMEDAY) ListKind.SOMEDAY to stringResource(R.string.list_someday)
        else ListKind.ANYTIME to stringResource(R.string.list_anytime)
        startDate.isAfter(today) -> ListKind.UPCOMING to startDate.format(dateFormat)
        task.evening -> ListKind.EVENING to stringResource(R.string.this_evening)
        else -> ListKind.TODAY to stringResource(R.string.list_today)
    }
}

/**
 * When, reminder, deadline, place and tags, each with its icon. When, deadline, place and tags open
 * their picker on a tap, when the item can take the change.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailFacts(
    library: ThingsLibrary,
    task: Task,
    onTags: (() -> Unit)? = null,
    onWhen: (() -> Unit)? = null,
    onDeadline: (() -> Unit)? = null,
    onPlace: (() -> Unit)? = null,
    onReminder: (() -> Unit)? = null,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val (whenKind, whenLabel) = whenFact(task)
        Fact({ ListIcon(whenKind, size = 18.dp) }, whenLabel, onClick = onWhen, clickLabel = stringResource(R.string.reschedule))
        // Reminder and deadline: shown when set, offered in grey when they can be.
        val reminder = task.reminder
        if (reminder != null || onReminder != null) {
            Fact(
                { Icon(Icons.Filled.Notifications, null, Modifier.size(18.dp), tint = muted) },
                reminder?.format(timeFormat) ?: stringResource(R.string.add_reminder),
                color = if (reminder == null) muted else Color.Unspecified,
                onClick = onReminder,
                clickLabel = stringResource(R.string.reminder),
            )
        }
        library.repeatRule(task)?.let { rule ->
            Fact({ RepeatGlyph(muted, size = 18.dp) }, describeRepeat(LocalContext.current, rule))
        }
        val deadline = task.deadline
        if (deadline != null || onDeadline != null) {
            Fact(
                { DeadlineFlag(size = 16.dp) },
                deadline?.let { stringResource(R.string.due_date, it.format(dateFormat)) } ?: stringResource(R.string.add_deadline),
                if (deadline != null) ListColors.deadline else muted,
                onClick = onDeadline,
                clickLabel = stringResource(R.string.deadline),
            )
        }
        val project = task.projectId?.let { library.task(it) }
        val place = project?.title ?: task.areaId?.let { library.area(it)?.title }
        if (place != null || onPlace != null) {
            Fact(
                { ListIcon(if (project != null || place == null) ListKind.PROJECT else ListKind.AREA, size = 18.dp) },
                place ?: stringResource(R.string.place_none),
                color = if (place == null) muted else Color.Unspecified,
                onClick = onPlace,
                clickLabel = stringResource(R.string.move),
            )
        }
        val tags = library.tagTitles(task)
        if (tags.isNotEmpty() || onTags != null) {
            // Tapping the tags (or "Tags" when there are none) picks them.
            Row(
                Modifier.fillMaxWidth()
                    .then(if (onTags != null) Modifier.clickable(onClickLabel = stringResource(R.string.tags_edit)) { onTags() } else Modifier)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { TagGlyph(muted, size = 18.dp) }
                Spacer(Modifier.width(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    tags.forEach { TagPill(it) }
                    if (tags.isEmpty()) Text(stringResource(R.string.tags_edit), style = MaterialTheme.typography.bodyMedium, color = muted)
                }
            }
        }
    }
}

@Composable
private fun Fact(
    icon: @Composable () -> Unit,
    text: String,
    color: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    clickLabel: String? = null,
) {
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClickLabel = clickLabel, onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

/** A read-only field naming the current choice; a tap opens its picker. */
@Composable
private fun PickerField(label: String, value: String, onClick: () -> Unit) {
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        // Over the field, which would otherwise take the tap for its text.
        Box(Modifier.matchParentSize().padding(top = 8.dp).clip(MaterialTheme.shapes.extraSmall).clickable(onClickLabel = label, onClick = onClick))
    }
}

/** The filter's project or area: all, or one, picked from the areas with their projects, then those with no area; a field narrows them. */
@Composable
private fun FilterPlaceDialog(library: ThingsLibrary, current: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    class Entry(val id: String?, val kind: ListKind?, val label: String, val indent: Int, val strong: Boolean = false)
    val allLabel = stringResource(R.string.filter_all)
    val entries = remember(library) {
        val areas = library.areas()
        val projects = library.projects()
        buildList {
            add(Entry(null, null, allLabel, 0))
            // Projects under their area, as the Projects list shows them; then those with no area.
            areas.forEach { area ->
                add(Entry(area.id, ListKind.AREA, area.title, 0, strong = true))
                projects.filter { it.areaId == area.id }.forEach { add(Entry(it.id, ListKind.PROJECT, it.title, 1)) }
            }
            projects.filter { project -> areas.none { it.id == project.areaId } }.forEach { add(Entry(it.id, ListKind.PROJECT, it.title, 0)) }
        }
    }
    var query by rememberSaveable { mutableStateOf("") }
    val wanted = foldForSearch(query.trim())
    val shown = if (wanted.isEmpty()) entries else entries.filter { it.id != null && foldForSearch(it.label).contains(wanted) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.project_or_area)) },
        text = {
            Column {
                SearchField(query, onQuery = { query = it }, placeholder = stringResource(R.string.place_search))
                if (shown.isEmpty()) Text(stringResource(R.string.search_none), Modifier.padding(vertical = 8.dp), color = muted)
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(shown, key = { it.id ?: "all" }) { entry ->
                        val picked = entry.id == current
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(entry.id) }
                                .padding(start = (8 + (if (wanted.isEmpty()) entry.indent else 0) * 20).dp, top = 10.dp, bottom = 10.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (entry.kind != null) ListIcon(entry.kind, size = 18.dp) else Spacer(Modifier.width(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                entry.label,
                                fontWeight = if (entry.strong) FontWeight.SemiBold else null,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (picked) Text("✓", color = muted)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Show-completed option and display filters: tags (any), one project or area, one day. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterDialog(library: ThingsLibrary, filter: TaskFilter, onChange: (TaskFilter) -> Unit, onDismiss: () -> Unit) {
    var pickingDay by remember { mutableStateOf(false) }
    var pickingTags by remember { mutableStateOf(false) }
    var pickingPlace by remember { mutableStateOf(false) }
    if (pickingTags) {
        TagsDialog(
            library = library,
            selected = filter.tagIds,
            onSave = {
                pickingTags = false
                onChange(filter.copy(tagIds = it.toSet()))
            },
            onDismiss = { pickingTags = false },
        )
    }
    if (pickingPlace) {
        FilterPlaceDialog(
            library = library,
            current = filter.containerId,
            onPick = {
                pickingPlace = false
                onChange(filter.copy(containerId = it))
            },
            onDismiss = { pickingPlace = false },
        )
    }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.group_by_project), Modifier.weight(1f))
                    Switch(checked = filter.groupByProject, onCheckedChange = { onChange(filter.copy(groupByProject = it)) })
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
                // Each opens a picker with a search field.
                val tags = library.tags()
                if (tags.isNotEmpty()) {
                    val chosen = tags.filter { it.id in filter.tagIds }
                    PickerField(
                        label = stringResource(R.string.tags),
                        value = chosen.joinToString(", ") { it.title }.ifEmpty { stringResource(R.string.filter_all) },
                        onClick = { pickingTags = true },
                    )
                }
                val containerTitle = filter.containerId?.let { id -> library.area(id)?.title ?: library.task(id)?.title }
                PickerField(
                    label = stringResource(R.string.project_or_area),
                    value = containerTitle ?: stringResource(R.string.filter_all),
                    onClick = { pickingPlace = true },
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
        dismissButton = {
            TextButton(onClick = { onChange(TaskFilter(showClosed = filter.showClosed, groupByProject = filter.groupByProject)) }, enabled = !filter.isEmpty) {
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
    navLayout: NavLayout,
    onNavLayoutChange: (NavLayout) -> Unit,
    tabOrder: List<String>,
    hiddenTabs: Set<String>,
    onTabsChange: (List<String>, Set<String>) -> Unit,
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
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                // How the lists are reached.
                Text(stringResource(R.string.navigation_title), style = MaterialTheme.typography.labelLarge)
                // Each choice with a sketch of the screen it gives.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NavLayout.entries.forEach { layout ->
                        NavLayoutChoice(
                            layout = layout,
                            selected = navLayout == layout,
                            onClick = { onNavLayoutChange(layout) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                HorizontalDivider()
                // The tabs: their order (arrows) and which ones show (switch); a hidden list stays in the search.
                Text(stringResource(R.string.lists_title), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.lists_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column {
                    tabOrder.forEachIndexed { index, name ->
                        val entry = ListTab.valueOf(name)
                        val shown = name !in hiddenTabs
                        val lastShown = shown && tabOrder.count { it !in hiddenTabs } == 1
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ListIcon(entry.kind, size = 18.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(entry.label), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = { onTabsChange(ListLayout.move(tabOrder, name, -1), hiddenTabs) }, enabled = index > 0) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.move_up))
                            }
                            IconButton(onClick = { onTabsChange(ListLayout.move(tabOrder, name, 1), hiddenTabs) }, enabled = index < tabOrder.lastIndex) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.move_down))
                            }
                            // The last list shown can't be hidden: the screen needs one.
                            Switch(
                                checked = shown,
                                enabled = !lastShown,
                                onCheckedChange = { on -> onTabsChange(tabOrder, if (on) hiddenTabs - name else hiddenTabs + name) },
                            )
                        }
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

/**
 * Picks a to-do's or project's tags among Things' tags, in Things' order. The field narrows the list
 * by name (accents and case aside); tags already picked stay picked while hidden.
 */
@Composable
private fun TagsDialog(library: ThingsLibrary, selected: Set<String>, onSave: (List<String>) -> Unit, onDismiss: () -> Unit) {
    val tags = remember(library) { library.tags() }
    var picked by rememberSaveable { mutableStateOf(selected.filter { id -> tags.any { it.id == id } }) }
    var query by rememberSaveable { mutableStateOf("") }
    val wanted = foldForSearch(query.trim())
    val shown = if (wanted.isEmpty()) tags else tags.filter { foldForSearch(it.title).contains(wanted) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tags_edit)) },
        text = {
            if (tags.isEmpty()) {
                Text(stringResource(R.string.tags_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else Column {
                SearchField(query, onQuery = { query = it }, placeholder = stringResource(R.string.tags_search))
                if (shown.isEmpty()) {
                    Text(stringResource(R.string.tags_no_match), Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(shown, key = { it.id }) { tag ->
                        val on = tag.id in picked
                        Row(
                            Modifier.fillMaxWidth().clickable { picked = if (on) picked - tag.id else picked + tag.id }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = on, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Text(tag.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(picked) }, enabled = tags.isNotEmpty()) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** A round mark for a picked (filled, with a check) or unpicked row while selecting. */
@Composable
private fun SelectionMark(picked: Boolean, modifier: Modifier = Modifier) {
    val color = ListColors.checked
    val label = stringResource(if (picked) R.string.selected else R.string.not_selected)
    Box(modifier.size(48.dp).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (picked) color else Color.Transparent)
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (picked) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

/** The field that narrows a picker's list as one types. */
@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    )
}

/** Text compared for search: lower case, without accents. */
private fun foldForSearch(text: String): String =
    java.text.Normalizer.normalize(text.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

/**
 * The side menu's content, as Things' sidebar: the app's name, the lists, then the projects with no
 * area and each area with its projects (an area folds away under its arrow). The open place is highlighted.
 */
@Composable
private fun SideMenu(
    library: ThingsLibrary?,
    counts: ListCounts?,
    lists: List<ListTab>,
    current: ListTab?,
    currentPlace: String?,
    folded: List<String>,
    onFold: (List<String>) -> Unit,
    onOpen: (ListTab) -> Unit,
    onOpenArea: (String) -> Unit,
    onOpenProject: (String) -> Unit,
) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
        Text(
            stringResource(R.string.app_name),
            Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        lists.forEach { list ->
            NavigationDrawerItem(
                label = { Text(stringResource(list.label)) },
                icon = { ListIcon(list.kind, size = 22.dp) },
                badge = {
                    listCount(list, counts)?.let { (count, late) ->
                        Text(count.toString(), color = if (late) ListColors.deadline else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    }
                },
                selected = list == current,
                onClick = { onOpen(list) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        if (library == null) return@Column
        @Composable
        fun ProjectItem(project: Task) {
            NavigationDrawerItem(
                label = { Text(project.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                icon = { ListIcon(ListKind.PROJECT, size = 20.dp, progress = library.projectProgress(project.id)) },
                selected = project.id == currentPlace,
                onClick = { onOpenProject(project.id) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 8.dp))
        library.projects().filter { it.areaId == null }.forEach { ProjectItem(it) }
        library.areas().forEach { area ->
            val isFolded = area.id in folded
            NavigationDrawerItem(
                label = { Text(area.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                icon = { ListIcon(ListKind.AREA, size = 20.dp) },
                badge = {
                    IconButton(onClick = { onFold(if (isFolded) folded - area.id else folded + area.id) }) {
                        Icon(
                            if (isFolded) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(if (isFolded) R.string.unfold_area else R.string.fold_area),
                        )
                    }
                },
                selected = area.id == currentPlace,
                onClick = { onOpenArea(area.id) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            if (!isFolded) library.areaProjects(area.id).forEach { ProjectItem(it) }
        }
    }
}

/** The number shown next to a list, as Things does (Inbox, Today, overdue deadlines in red), or null when none. */
private fun listCount(list: ListTab, counts: ListCounts?): Pair<Int, Boolean>? {
    counts ?: return null
    val (count, late) = when (list) {
        ListTab.INBOX -> counts.inbox to false
        ListTab.TODAY -> counts.today to false
        ListTab.DEADLINES -> counts.overdue to true
        else -> return null
    }
    return (count to late).takeIf { count > 0 }
}

/** A list's icon with its count as a small badge, for the tabs and the bottom bar. */
@Composable
private fun CountedListIcon(list: ListTab, counts: ListCounts?, size: androidx.compose.ui.unit.Dp) {
    val count = listCount(list, counts)
    BadgedBox(
        badge = {
            if (count != null) {
                Badge(
                    containerColor = if (count.second) ListColors.deadline else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (count.second) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                ) { Text(if (count.first > 99) "99+" else count.first.toString()) }
            }
        },
    ) { ListIcon(list.kind, size = size) }
}

/** A Navigation choice in Settings: a small sketch of the screen (tabs, side menu or bottom bar) and its name. */
@Composable
private fun NavLayoutChoice(layout: NavLayout, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val line = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val label = stringResource(when (layout) {
        NavLayout.TABS -> R.string.navigation_tabs
        NavLayout.DRAWER -> R.string.navigation_drawer
        NavLayout.BOTTOM -> R.string.navigation_bottom
    })
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .border(if (selected) 2.dp else 1.dp, if (selected) accent else line, MaterialTheme.shapes.medium)
            .clickable(onClickLabel = label, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(width = 44.dp, height = 72.dp)) {
            val w = size.width
            val h = size.height
            val stroke = 1.5.dp.toPx()
            drawRoundRect(line, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
            // Rows of the list.
            val left = if (layout == NavLayout.DRAWER) w * 0.5f else w * 0.18f
            val top = if (layout == NavLayout.TABS) h * 0.3f else h * 0.18f
            repeat(4) { i ->
                val y = top + i * h * 0.12f
                drawLine(line, Offset(left, y), Offset(w * 0.82f, y), strokeWidth = stroke, cap = StrokeCap.Round)
            }
            when (layout) {
                NavLayout.TABS -> repeat(3) { i ->
                    val x = w * (0.18f + i * 0.24f)
                    drawLine(if (i == 0) accent else line, Offset(x, h * 0.14f), Offset(x + w * 0.16f, h * 0.14f), strokeWidth = stroke * 2, cap = StrokeCap.Round)
                }
                NavLayout.DRAWER -> {
                    drawRect(accent.copy(alpha = 0.18f), topLeft = Offset(stroke, stroke), size = androidx.compose.ui.geometry.Size(w * 0.4f, h - 2 * stroke))
                    repeat(4) { i ->
                        val y = h * (0.18f + i * 0.12f)
                        drawLine(accent, Offset(w * 0.1f, y), Offset(w * 0.32f, y), strokeWidth = stroke, cap = StrokeCap.Round)
                    }
                }
                NavLayout.BOTTOM -> {
                    drawLine(line, Offset(0f, h * 0.84f), Offset(w, h * 0.84f), strokeWidth = stroke)
                    repeat(4) { i ->
                        drawCircle(if (i == 0) accent else line, radius = 2.5.dp.toPx(), center = Offset(w * (0.2f + i * 0.2f), h * 0.92f))
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2)
    }
}
