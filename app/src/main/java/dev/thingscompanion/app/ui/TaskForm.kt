package dev.thingscompanion.app.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import dev.thingscompanion.cloud.TitleEmoji
import androidx.compose.material3.AssistChip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.thingscompanion.app.R
import dev.thingscompanion.cloud.Schedule
import dev.thingscompanion.cloud.ThingsDay
import dev.thingscompanion.cloud.ThingsIds
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal val dateFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
internal val timeFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

/** Values of the to-do form: a new to-do, or the current values of the one being edited. */
data class TaskDraft(
    val title: String = "",
    val notes: String = "",
    val schedule: Schedule = Schedule.Inbox,
    val reminder: LocalTime? = null,
    val checklist: List<ChecklistDraft> = emptyList(),
    val deadline: LocalDate? = null,
)

private val checklistSaver = listSaver<SnapshotStateList<ChecklistDraft>, String>(
    save = { rows -> rows.flatMap { listOf(it.id, it.title, it.done.toString()) } },
    restore = { flat -> flat.chunked(3).map { ChecklistDraft(it[0], it[1], it[2].toBoolean()) }.toMutableStateList() },
)

private fun <T> List<T>.toMutableStateList() = mutableStateListOf<T>().also { it.addAll(this) }

/**
 * Full-screen form shared by Add and Edit, with Save in the top bar so the keyboard never hides
 * it. Tapping outside a field or the keyboard's Done key hides the keyboard; leaving takes the
 * close button or Back, never a stray tap. [allowSchedule] is false for repeating to-dos (When
 * and reminder stay as Things manages them); [allowInbox] is false for to-dos filed in a project
 * or area; [allowChecklist] is false when Things keeps the checklist (repeating templates).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskFormScreen(
    initial: TaskDraft,
    @StringRes heading: Int,
    @StringRes confirm: Int,
    allowSchedule: Boolean,
    allowInbox: Boolean,
    allowChecklist: Boolean,
    allowDeadline: Boolean,
    onSubmit: (title: String, notes: String, schedule: Schedule, reminder: LocalTime?, checklist: List<ChecklistDraft>, deadline: LocalDate?) -> Unit,
    allowReminder: Boolean = true,
    place: Pair<ListKind, String>? = null,
    onPickPlace: (() -> Unit)? = null,
    /** The tags picked for a new item (titles), and how to pick them; null where the form doesn't offer tags. */
    tags: List<String>? = null,
    onPickTags: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val today = ThingsDay.today()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    fun hideKeyboard() {
        focusManager.clearFocus()
        keyboard?.hide()
    }

    var title by rememberSaveable { mutableStateOf(initial.title) }
    var pickingEmoji by remember { mutableStateOf(false) }
    var notes by rememberSaveable { mutableStateOf(initial.notes) }
    val checklist = rememberSaveable(saver = checklistSaver) { initial.checklist.toMutableStateList() }
    var whenChoice by rememberSaveable { mutableStateOf(WhenChoice.of(initial.schedule)) }
    // Moving the new to-do in or out of a project or area: the Inbox and Anytime follow, as in
    // Things (a filed to-do is never in the Inbox; one taken out of its project goes back there).
    var wasInboxAllowed by rememberSaveable { mutableStateOf(allowInbox) }
    LaunchedEffect(allowInbox) {
        if (allowInbox != wasInboxAllowed) {
            if (!allowInbox && whenChoice == WhenChoice.INBOX) whenChoice = WhenChoice.ANYTIME
            if (allowInbox && whenChoice == WhenChoice.ANYTIME) whenChoice = WhenChoice.INBOX
            wasInboxAllowed = allowInbox
        }
    }
    // The day of the initial schedule: kept when the choice is unchanged (an overdue to-do stays on its day).
    var pickedDay by rememberSaveable { mutableStateOf(initial.schedule.date?.toEpochDay()) }
    var pickingDate by remember { mutableStateOf(false) }
    var reminderMinute by rememberSaveable { mutableStateOf(initial.reminder?.let { it.hour * 60 + it.minute }) }
    var pickingTime by remember { mutableStateOf(false) }
    var deadlineDay by rememberSaveable { mutableStateOf(initial.deadline?.toEpochDay()) }
    var pickingDeadline by remember { mutableStateOf(false) }
    // One tap on Save: the form closes at once, a second tap must not queue the change twice.
    var submitted by remember { mutableStateOf(false) }
    var focusRowId by remember { mutableStateOf<String?>(null) }

    BackHandler(onBack = onDismiss)

    // A new to-do starts with the keyboard on its title.
    val titleFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (initial.title.isEmpty()) titleFocus.requestFocus() }

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
    if (pickingDeadline) {
        DayPickerDialog(
            today = today,
            onPicked = { day ->
                pickingDeadline = false
                deadlineDay = day.toEpochDay()
            },
            onDismiss = { pickingDeadline = false },
        )
    }
    if (pickingDate) {
        DayPickerDialog(
            today = today,
            onPicked = { day ->
                pickingDate = false
                // Today picked in the calendar means Today.
                whenChoice = if (day == today) WhenChoice.TODAY else WhenChoice.DATE
                pickedDay = day.toEpochDay()
            },
            onDismiss = { pickingDate = false },
        )
    }

    fun submit() {
        if (submitted || title.isBlank()) return
        submitted = true
        hideKeyboard()
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
        val reminder = if (!allowSchedule || !allowReminder) initial.reminder?.takeIf { schedule.date != null } else
            reminderMinute?.takeIf { schedule.date != null }?.let { LocalTime.of(it / 60, it % 60) }
        val deadline = if (allowDeadline) deadlineDay?.let(LocalDate::ofEpochDay) else initial.deadline
        onSubmit(title, notes, schedule, reminder, checklist.toList(), deadline)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(heading)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel)) }
                },
                actions = {
                    TextButton(onClick = ::submit, enabled = title.isNotBlank() && !submitted) {
                        Text(stringResource(confirm), style = MaterialTheme.typography.titleMedium)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .pointerInput(Unit) { detectTapGestures { hideKeyboard() } }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.title)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                trailingIcon = {
                    IconButton(onClick = {
                        hideKeyboard()
                        pickingEmoji = true
                    }) { Text(TitleEmoji.leading(title) ?: "☺", fontSize = 20.sp) }
                },
                modifier = Modifier.fillMaxWidth().focusRequester(titleFocus),
            )
            if (pickingEmoji) {
                EmojiDialog(
                    current = TitleEmoji.leading(title),
                    onPick = {
                        pickingEmoji = false
                        title = TitleEmoji.apply(title, it)
                    },
                    onDismiss = { pickingEmoji = false },
                )
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.notes)) },
                minLines = 3,
                // Markdown styled as you type, as in Things.
                visualTransformation = MarkdownTransformation(markdownColors()),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            if (allowChecklist) {
                Text(stringResource(R.string.checklist), style = MaterialTheme.typography.labelLarge)
                checklist.forEachIndexed { index, row ->
                    // A row just added takes the keyboard.
                    val rowFocus = remember(row.id) { FocusRequester() }
                    LaunchedEffect(row.id, focusRowId) { if (row.id == focusRowId) rowFocus.requestFocus() }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ThingsCheckbox(checked = row.done, onCheckedChange = {}, enabled = false, size = 18.dp)
                        TextField(
                            value = row.title,
                            onValueChange = { checklist[index] = row.copy(title = it) },
                            placeholder = { Text(stringResource(R.string.new_checklist_item)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { hideKeyboard() }),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier.weight(1f).focusRequester(rowFocus),
                        )
                        IconButton(onClick = { checklist.removeAt(index) }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.delete_item))
                        }
                    }
                }
                TextButton(onClick = {
                    val row = ChecklistDraft(ThingsIds.random(), "")
                    checklist.add(row)
                    focusRowId = row.id
                }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(stringResource(R.string.add_checklist_item), Modifier.padding(start = 8.dp))
                }
            }

            if (place != null && onPickPlace != null) {
                Text(stringResource(R.string.place_label), style = MaterialTheme.typography.labelLarge)
                AssistChip(
                    onClick = {
                        hideKeyboard()
                        onPickPlace()
                    },
                    label = { Text(place.second, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { ListIcon(place.first, size = 16.dp) },
                )
            }

            if (tags != null && onPickTags != null) {
                Text(stringResource(R.string.tags_edit), style = MaterialTheme.typography.labelLarge)
                AssistChip(
                    onClick = {
                        hideKeyboard()
                        onPickTags()
                    },
                    label = {
                        Text(
                            tags.joinToString(", ").ifEmpty { stringResource(R.string.tags_empty) },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = { TagGlyph(MaterialTheme.colorScheme.onSurfaceVariant, size = 16.dp) },
                )
            }

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
                            onClick = {
                                hideKeyboard()
                                if (choice == WhenChoice.DATE) pickingDate = true else whenChoice = choice
                            },
                            label = { Text(label) },
                            leadingIcon = choice.icon?.let { kind -> { ListIcon(kind, size = 16.dp) } },
                        )
                    }
                }
                // A reminder needs a day: only offered once Today, This Evening or a date is chosen.
                if (allowReminder && whenChoice.hasDay) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = reminderMinute != null,
                            onClick = {
                                hideKeyboard()
                                pickingTime = true
                            },
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
                                onClick = { reminderMinute = null },
                                label = { Text(stringResource(R.string.no_reminder)) },
                            )
                        }
                    }
                }
            }

            if (allowDeadline) {
                Text(stringResource(R.string.deadline), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = deadlineDay != null,
                        onClick = {
                            hideKeyboard()
                            pickingDeadline = true
                        },
                        label = { Text(deadlineDay?.let { LocalDate.ofEpochDay(it).format(dateFormat) } ?: stringResource(R.string.add_deadline)) },
                        leadingIcon = { DeadlineFlag() },
                    )
                    if (deadlineDay != null) {
                        FilterChip(selected = false, onClick = { deadlineDay = null }, label = { Text(stringResource(R.string.no_deadline)) })
                    }
                }
            }
        }
    }
}

private enum class WhenChoice(@StringRes val label: Int, val hasDay: Boolean, val icon: ListKind?) {
    INBOX(R.string.list_inbox, false, ListKind.INBOX),
    TODAY(R.string.list_today, true, ListKind.TODAY),
    EVENING(R.string.this_evening, true, ListKind.EVENING),
    DATE(R.string.pick_date, true, ListKind.UPCOMING),
    ANYTIME(R.string.list_anytime, false, ListKind.ANYTIME),
    SOMEDAY(R.string.list_someday, false, ListKind.SOMEDAY);

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

/**
 * Calendar returning the picked calendar day; limited to today and later unless [futureOnly] is false.
 * It opens on [initial] when that day can be picked, and [onClear] adds a button taking the date off.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPickerDialog(
    today: LocalDate,
    onPicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    futureOnly: Boolean = true,
    initial: LocalDate? = null,
    onClear: Pair<String, () -> Unit>? = null,
) {
    val todayUtcMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val initialMillis = initial?.takeIf { !futureOnly || !it.isBefore(today) }?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis ?: if (futureOnly) todayUtcMillis + 86_400_000L else todayUtcMillis,
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
        dismissButton = {
            Row {
                onClear?.let { (label, clear) -> TextButton(onClick = clear) { Text(label) } }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    ) {
        DatePicker(state = state)
    }
}

/** A reminder's time; with [onClear], a button takes the reminder off. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickerDialog(initial: LocalTime, onPicked: (LocalTime) -> Unit, onDismiss: () -> Unit, onClear: (() -> Unit)? = null) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onPicked(LocalTime.of(state.hour, state.minute)) }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            Row {
                if (onClear != null) TextButton(onClick = onClear) { Text(stringResource(R.string.no_reminder)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

/** A grid of emoji to lead the title with, and a choice to take it off. */
@Composable
internal fun EmojiDialog(current: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.emoji_title)) },
        text = {
            LazyVerticalGrid(columns = GridCells.Adaptive(48.dp), modifier = Modifier.heightIn(max = 360.dp)) {
                items(TitleEmoji.CHOICES) { emoji ->
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (emoji == current) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                            .clickable { onPick(emoji) },
                        contentAlignment = Alignment.Center,
                    ) { Text(emoji, fontSize = 24.sp) }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Row {
                if (current != null) TextButton(onClick = { onPick(null) }) { Text(stringResource(R.string.emoji_none)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
