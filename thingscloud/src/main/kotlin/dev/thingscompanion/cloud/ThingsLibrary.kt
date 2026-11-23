package dev.thingscompanion.cloud

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class TaskType { TODO, PROJECT, HEADING }
enum class TaskStatus { OPEN, CANCELLED, COMPLETED }
enum class StartList { INBOX, ANYTIME, SOMEDAY }

data class Task(
    val id: String,
    val title: String,
    val notes: String?,
    val type: TaskType,
    val status: TaskStatus,
    val start: StartList,
    val startDate: LocalDate?,
    val deadline: LocalDate?,
    /** Effective project: the task's own, or its heading's. */
    val projectId: String?,
    /** Effective area: the task's own, its project's, or its heading's. */
    val areaId: String?,
    val headingId: String?,
    val tagIds: List<String>,
    val index: Int,
    val todayIndex: Int,
    val evening: Boolean,
    val trashed: Boolean,
    val isRecurrenceTemplate: Boolean,
    /** Reminder time on [startDate], local time (`ato`, seconds from midnight). */
    val reminder: LocalTime? = null,
    /** When a device last handled this reminder, epoch seconds (`lai`). */
    val lastAlarmInteraction: Double? = null,
    /** When it was completed or cancelled (`sp`). */
    val stopDate: Instant? = null,
    /** For a repeating instance: its template (`rt`). */
    val recurrenceTemplateIds: List<String> = emptyList(),
    /** For a template: the repeat type (`rr.tp`): 0 fixed schedule, 1 after completion. */
    val repeatType: Int? = null,
    /** For a template: its repeat rule as stored (`rr`). */
    internal val repeatRuleJson: JsonObject? = null,
    /** Last change (`md`), epoch seconds. */
    val modifiedAt: Double? = null,
    /** For a template: the day of its next occurrence, not created yet (`tir`). */
    val nextOccurrence: LocalDate? = null,
    /** For a template: how many occurrences Things made of it (`icc`). */
    val instanceCount: Int? = null,
    internal val entity: String?,
) {
    val isOpen get() = status == TaskStatus.OPEN

    val isRepeating get() = isRecurrenceTemplate || repeatType != null || recurrenceTemplateIds.isNotEmpty()

    /** The day it is filed under in Upcoming: its start date, or for an undated item its deadline, as Things does. */
    val upcomingDay: LocalDate? get() = startDate ?: deadline
}

/** The numbers Things shows next to its lists: open to-dos in the Inbox and Today, and overdue items. */
data class ListCounts(val inbox: Int, val today: Int, val overdue: Int)

/** A heading of a project and its to-dos; [heading] is null for the to-dos under no heading. */
data class ProjectSection(val heading: Task?, val tasks: List<Task>)

data class Area(val id: String, val title: String, val index: Int, internal val entity: String? = "Area3")

data class Tag(val id: String, val title: String, val index: Int = 0)

data class SearchResult(val tasks: List<Task>, val areas: List<Area>)

data class ChecklistItem(
    val id: String,
    val title: String,
    val done: Boolean,
    val index: Int,
    val taskId: String = "",
    internal val entity: String? = null,
)

/** Read-only view of a Things database, with the app's list rules. */
class ThingsLibrary private constructor(
    private val tasks: Map<String, Task>,
    private val areas: Map<String, Area>,
    private val tags: Map<String, Tag>,
    private val checklists: Map<String, List<ChecklistItem>>,
    /** Repeating templates' props as stored: Things copies them as they are onto an occurrence checked ahead. */
    private val templateProps: Map<String, JsonObject> = emptyMap(),
) {
    fun task(id: String): Task? = tasks[id]

    internal fun storedTemplate(id: String): JsonObject? = templateProps[id]

    fun area(id: String): Area? = areas[id]

    fun areas(): List<Area> = areas.values.sortedWith(compareBy<Area>({ it.index }, { it.id }))

    /** In Things' own tag order, as the Things apps show them. */
    fun tagTitles(task: Task): List<String> = task.tagIds.mapNotNull { tags[it] }.sortedWith(tagOrder).map { it.title }

    fun tags(): List<Tag> = tags.values.sortedWith(tagOrder)

    fun checklist(taskId: String): List<ChecklistItem> = checklists[taskId].orEmpty()

    fun checklistItem(id: String): ChecklistItem? = checklistItems[id]

    private val checklistItems by lazy { checklists.values.flatten().associateBy { it.id } }

    /**
     * Quick Find: to-dos and projects whose title or notes hold [query], and areas whose title
     * does, ignoring case and accents. Open items first, projects before to-dos, then Things'
     * order; completed and cancelled ones last. The Trash and repeating templates are left out.
     */
    fun search(query: String): SearchResult {
        val needle = fold(query.trim())
        if (needle.isEmpty()) return SearchResult(emptyList(), emptyList())
        fun matches(text: String?) = text != null && needle in fold(text)
        val found = tasks.values.filter {
            it.type != TaskType.HEADING && !it.trashed && !it.inTrashedContainer && it.isCurrentEntity &&
                !it.isRecurrenceTemplate && it.title.isNotBlank() && (matches(it.title) || matches(it.notes))
        }.sortedWith(compareBy<Task>({ !it.isOpen }, { it.type != TaskType.PROJECT }).then(byIndex))
        return SearchResult(found, areas().filter { matches(it.title) })
    }

    /** Trashed to-dos, most recently changed first (Things' Trash). */
    fun trash(): List<Task> = tasks.values
        .filter { it.trashed && it.isCurrentEntity && it.title.isNotBlank() }
        .sortedByDescending { it.modifiedAt ?: 0.0 }

    /** Things' Logbook: completed and cancelled to-dos and projects, most recently closed first. */
    fun logbook(): List<Task> = tasks.values
        .filter {
            !it.isOpen && it.type != TaskType.HEADING && !it.isRecurrenceTemplate && !it.trashed && !it.inTrashedContainer &&
                it.isCurrentEntity && it.title.isNotBlank()
        }
        .sortedWith(compareByDescending<Task> { it.stopDate }.thenBy { it.id })

    /** How a repeating to-do repeats: its own rule for a template, its template's for an instance. */
    fun repeatRule(task: Task): RepeatRule? {
        val template = if (task.repeatRuleJson != null) task else task.recurrenceTemplateIds.singleOrNull()?.let(tasks::get)
        return template?.repeatRuleJson?.let(RepeatRule::parse)
    }

    /** Same to-dos as check-off: moving a repeating template or an "after completion" repeat to the Trash is left to Things. */
    // An "after completion" instance is checkable, but trashing one is left to Things.
    fun canTrash(task: Task): Boolean =
        canComplete(task) || canEditProject(task) || canTrashHeading(task)

    /** A heading of a project the app may change; it goes to the Trash with its to-dos. */
    private fun canTrashHeading(task: Task): Boolean =
        task.type == TaskType.HEADING && !task.trashed && task.isCurrentEntity &&
            task.projectId?.let(tasks::get)?.let(::canEditProject) == true

    fun canRestore(task: Task): Boolean = task.trashed && task.isCurrentEntity

    /** Trashed to-dos (repeating templates included), projects and headings the app may delete for good. */
    /** A to-do or project in the lists (not a heading, not in the Trash, not a repeating template) takes tags. */
    fun canTag(task: Task): Boolean =
        (task.type == TaskType.TODO || task.type == TaskType.PROJECT) && !task.trashed && task.isCurrentEntity && !task.isRecurrenceTemplate

    fun canDeleteForever(task: Task): Boolean = task.trashed && task.isCurrentEntity

    /** Everything filed in a project (headings, and to-dos under it or its headings) or under a heading. */
    fun containedTasks(containerId: String): List<Task> = tasks.values.filter {
        it.isCurrentEntity && it.id != containerId && (it.projectId == containerId || it.headingId == containerId)
    }

    /** The to-dos generated from a repeating template. */
    fun instancesOf(templateId: String): List<Task> = tasks.values.filter { templateId in it.recurrenceTemplateIds }

    /** Checklist of plain to-dos and repeating instances; a template's checklist is left to Things. */
    fun canEditChecklist(task: Task): Boolean = task.isTodo && !task.trashed && !task.isRecurrenceTemplate && task.repeatType == null

    fun inbox(closedOn: LocalDate? = null, zone: ZoneId = ZoneId.systemDefault()): List<Task> = visible(closedOn, zone) { it.isTodo && it.start == StartList.INBOX && it.projectId == null && it.areaId == null }
        .sortedWith(byIndex)

    /** Dated to-dos up to today, and, as in Things, undated ones (Inbox included) once their deadline is today or past. */
    fun today(today: LocalDate, closedOn: LocalDate? = null, zone: ZoneId = ZoneId.systemDefault()): List<Task> = visible(closedOn, zone) {
        it.isTodo && !it.isRecurrenceTemplate && (
            (it.start != StartList.INBOX && it.startDate != null && !it.startDate.isAfter(today)) ||
                (it.startDate == null && it.deadline != null && !it.deadline.isAfter(today))
            )
    }.sortedWith(compareBy<Task>({ it.evening }, { it.todayIndex }, { it.index }, { it.id }))

    /** Items scheduled after today, and undated ones due after today, each under its [Task.upcomingDay]. */
    fun upcoming(today: LocalDate, closedOn: LocalDate? = null, zone: ZoneId = ZoneId.systemDefault()): List<Task> = visible(closedOn, zone) {
        it.type != TaskType.HEADING && !it.isRecurrenceTemplate && (
            it.startDate?.isAfter(today) == true ||
                // Undated: Anytime or Someday, not the Inbox, as in Things.
                (it.startDate == null && it.start != StartList.INBOX && it.deadline?.isAfter(today) == true)
            )
    }.sortedWith(compareBy<Task>({ it.upcomingDay }, { it.index }, { it.id }))

    /**
     * The coming occurrences of repeating to-dos, which Things shows in Upcoming before creating
     * them: each template's next day ([Task.nextOccurrence]) and, on a fixed schedule, the
     * following ones up to [horizonDays] after today, until the rule ends. Days already holding an
     * occurrence, and today or earlier, are left out; an "after completion" repeat shows its next
     * day only once no occurrence of it is open. Each is a copy of its template dated that day: read-only rows.
     * Not part of [upcoming], which the widget and the counts read.
     */
    fun repeatsAhead(today: LocalDate, horizonDays: Long = 28): List<Task> {
        val horizon = today.plusDays(horizonDays)
        val instances = tasks.values.filter { it.recurrenceTemplateIds.isNotEmpty() && !it.trashed }
        return tasks.values
            .filter {
                it.isTodo && it.isRecurrenceTemplate && it.isOpen && !it.trashed && !it.inTrashedContainer &&
                    it.isCurrentEntity && it.title.isNotBlank()
            }
            .flatMap { template ->
                val first = template.nextOccurrence?.takeIf { it.isAfter(today) } ?: return@flatMap emptyList()
                val rule = repeatRule(template) ?: return@flatMap emptyList()
                val mine = instances.filter { template.id in it.recurrenceTemplateIds }
                if (rule.afterCompletion && mine.any { it.isOpen }) return@flatMap emptyList()
                val taken = mine.mapNotNull { it.startDate }.toSet()
                // With a deadline lead the rule's days are deadlines, not starts: only the next start is known.
                if (rule.leadDays != 0) {
                    val ended = rule.endDate != null && first.isAfter(rule.endDate)
                    return@flatMap listOfNotNull(template.copy(startDate = first).takeIf { !ended && first !in mine.mapNotNull { it.startDate } })
                }
                val days = generateSequence(first) { day -> rule.nextAfter(day)?.takeIf { it.isAfter(day) } }
                    .takeWhile { it == first || !it.isAfter(horizon) }
                    .takeWhile { rule.endDate == null || !it.isAfter(rule.endDate) }
                    .take(MAX_REPEATS_AHEAD)
                days.filter { it !in taken }.map { template.copy(startDate = it) }.toList()
            }
            .sortedWith(compareBy<Task>({ it.startDate }, { it.index }, { it.id }))
    }

    /**
     * Whether a coming occurrence from [repeatsAhead] can be checked before Things creates it, as
     * Things 3.23+ does: the template's next one only, on a fixed schedule with no deadline lead,
     * no deadline and no checklist (the writes seen in the history; the others are left to Things).
     */
    fun canCompleteAhead(occurrence: Task): Boolean {
        val template = tasks[occurrence.id]?.takeIf { it.isRecurrenceTemplate } ?: return false
        val day = occurrence.startDate ?: return false
        val rule = repeatRule(template) ?: return false
        return template.isTodo && template.isOpen && !template.trashed && !template.inTrashedContainer && template.isCurrentEntity &&
            !rule.afterCompletion && rule.leadDays == 0 && template.deadline == null && checklist(template.id).isEmpty() &&
            day == template.nextOccurrence && rule.nextAfter(day)?.let { next -> next.isAfter(day) && (rule.endDate == null || !next.isAfter(rule.endDate)) } == true &&
            templateProps[template.id] != null &&
            instancesOf(template.id).none { it.startDate == day && !it.trashed }
    }

    /** What Things counts next to its lists (open items only). */
    fun counts(today: LocalDate): ListCounts = ListCounts(
        inbox = inbox().size,
        today = today(today).size,
        overdue = deadlines().count { it.deadline!!.isBefore(today) },
    )

    /** Things' Deadlines list: open to-dos and projects that have a deadline, earliest first. */
    fun deadlines(closedOn: LocalDate? = null, zone: ZoneId = ZoneId.systemDefault()): List<Task> = visible(closedOn, zone) {
        it.type != TaskType.HEADING && !it.isRecurrenceTemplate && it.deadline != null
    }.sortedWith(compareBy<Task>({ it.deadline }, { it.index }, { it.id }))

    fun anytime(today: LocalDate, closedOn: LocalDate? = null, zone: ZoneId = ZoneId.systemDefault()): List<Task> = visible(closedOn, zone) {
        it.isTodo && it.start == StartList.ANYTIME &&
            (it.startDate == null || !it.startDate.isAfter(today)) &&
            projectVisibleInAnytime(it.projectId, today)
    }.sortedWith(byIndex)

    fun someday(closedOn: LocalDate? = null, zone: ZoneId = ZoneId.systemDefault()): List<Task> = visible(closedOn, zone) {
        it.type != TaskType.HEADING && it.start == StartList.SOMEDAY && it.startDate == null &&
            !it.isRecurrenceTemplate && (it.type == TaskType.PROJECT || it.projectId == null)
    }.sortedWith(byIndex)

    /** Index that puts a new to-do on top of the Inbox: lowest open Inbox index minus 1, or 0 if empty. */
    fun nextInboxTopIndex(): Int = nextTopIndex(StartList.INBOX)

    /** The index after the last of [siblings]: where Things puts a new project, heading or area. */
    private fun bottomIndex(siblings: List<Int>) = (siblings.maxOrNull() ?: -1) + 1

    fun nextProjectIndex(areaId: String?): Int =
        bottomIndex(tasks.values.filter { it.type == TaskType.PROJECT && it.areaId == areaId && !it.trashed }.map { it.index })

    /** After the project's to-dos and headings. */
    fun nextIndexInProject(projectId: String): Int =
        bottomIndex(tasks.values.filter { it.projectId == projectId && it.type != TaskType.PROJECT && !it.trashed }.map { it.index })

    fun nextIndexInArea(areaId: String): Int =
        bottomIndex(tasks.values.filter { it.isTodo && it.areaId == areaId && it.projectId == null && !it.trashed }.map { it.index })

    fun nextAreaIndex(): Int = bottomIndex(areas.values.map { it.index })

    /** Same rule for unfiled to-dos of any start list (Anytime for Today/Evening, Someday for a later date). */
    fun nextTopIndex(start: StartList): Int = tasks.values
        .filter { it.isOpen && !it.trashed && it.isCurrentEntity && it.isTodo && it.start == start && it.projectId == null && it.areaId == null }
        .minOfOrNull { it.index }
        ?.minus(1) ?: 0

    /**
     * Whether the app may check or uncheck this to-do: plain to-dos, repeating instances whose
     * template repeats on a fixed schedule (as things3-cloud allows), and open instances of
     * "after completion" templates repeating by days, weeks, months or years (checked as the Things apps do).
     */
    fun canComplete(task: Task): Boolean {
        if (task.type != TaskType.TODO || task.trashed || task.isRecurrenceTemplate || task.repeatType != null) return false
        if (task.recurrenceTemplateIds.isEmpty()) return true
        val template = task.recurrenceTemplateIds.singleOrNull()?.let(tasks::get) ?: return false
        return when (template.repeatType) {
            0 -> true
            // After completion: checking only, and only for the units whose writes are known.
            1 -> task.isOpen && afterCompletionRule(task) != null
            else -> false
        }
    }

    /**
     * For an instance of an "after completion" template repeating by days, weeks, months or years: the template
     * and its rule, which checking the instance must move on (see [TaskEdits.afterCompletion]).
     */
    fun afterCompletionRule(task: Task): Pair<Task, RepeatRule>? {
        val template = task.recurrenceTemplateIds.singleOrNull()?.let(tasks::get) ?: return null
        val rule = repeatRule(template)?.takeIf { it.afterCompletion } ?: return null
        return template to rule
    }

    /** Of [ids], the tags this library knows, in Things' tag order: what a create may carry in `tg`. */
    fun knownTags(ids: List<String>): List<String> {
        val wanted = ids.toSet()
        return tags().filter { it.id in wanted }.map { it.id }
    }

    /** Projects the app may complete, cancel, reopen, date or edit: not repeating ones. */
    fun canEditProject(task: Task): Boolean =
        task.type == TaskType.PROJECT && !task.trashed && !task.isRepeating && task.isCurrentEntity

    /**
     * What completing or cancelling [project] also closes, as Things does: its open headings and
     * open to-dos (directly or under a heading), except repeats the app can't check.
     */
    fun openChildren(project: Task): List<Task> = tasks.values.filter {
        it.projectId == project.id && it.isOpen && !it.trashed && it.isCurrentEntity &&
            (it.type == TaskType.HEADING || (it.isTodo && canComplete(it)))
    }

    /**
     * To-dos the app may make repeat: open, dated (the rule starts on that day), not repeating
     * yet, and without a deadline (Things turns it into a lead time the app doesn't write).
     */
    fun canMakeRepeating(task: Task): Boolean =
        task.isTodo && task.isOpen && !task.trashed && task.isCurrentEntity && task.startDate != null && task.deadline == null &&
            !task.isRepeating

    /** To-dos the app may file elsewhere: not repeating templates, not trashed. */
    fun canMove(task: Task): Boolean =
        task.isTodo && !task.trashed && !task.isRecurrenceTemplate && task.repeatType == null && task.isCurrentEntity

    /** What Move takes in a selection: to-dos the app may file ([canMove]) and projects it may move to an area ([canEditProject]). */
    fun canMoveInSelection(task: Task): Boolean = canMove(task) || canEditProject(task)

    /** To-dos, projects and headings the app may reorder. */
    fun canReorder(task: Task): Boolean = !task.trashed && task.isCurrentEntity && !task.isRecurrenceTemplate

    /** Whether the deadline of this to-do or project can be set from the app. */
    fun canSetDeadline(task: Task): Boolean = (task.isTodo && canReschedule(task) && !task.isRepeating) || canEditProject(task)

    /** When and reminder of repeating to-dos stay as Things manages them. */
    /** A to-do, a repeating to-do's occurrence included (Things moves it alone); not a repeating template. */
    fun canReschedule(task: Task): Boolean = task.type == TaskType.TODO && !task.trashed && !task.isRecurrenceTemplate

    /** Open dated to-dos that carry a reminder time. */
    fun withReminders(): List<Task> = visible { it.isTodo && it.startDate != null && it.reminder != null }

    fun projects(): List<Task> = visible { it.type == TaskType.PROJECT && !it.isRecurrenceTemplate }
        .sortedWith(byIndex)

    fun projectTasks(projectId: String, closedOn: LocalDate? = null, zone: ZoneId = ZoneId.systemDefault()): List<Task> = visible(closedOn, zone) { it.isTodo && it.projectId == projectId }
        .sortedWith(byIndex)

    /** The share of a project's to-dos done (completed or cancelled), 0 to 1, for its filling circle; repeating templates are not to-dos. */
    fun projectProgress(projectId: String): Float {
        val all = tasks.values.filter {
            it.isTodo && it.projectId == projectId && !it.trashed && !it.inTrashedContainer && it.isCurrentEntity &&
                !it.isRecurrenceTemplate && it.title.isNotBlank()
        }
        return if (all.isEmpty()) 0f else all.count { !it.isOpen }.toFloat() / all.size
    }

    /**
     * [tasks] classed by project, as the lists show them grouped: to-dos in no project or area
     * first (key null), then the sidebar's order: projects with no area, then each area (its own
     * to-dos, then its projects'). Each group keeps [tasks]' order; places with none are left out.
     */
    fun byProject(tasks: List<Task>): List<Pair<String?, List<Task>>> {
        val groups = tasks.groupBy { it.projectId ?: it.areaId }
        val sidebar = listOf(null) + projects().filter { it.areaId == null }.map { it.id } +
            areas().flatMap { area -> listOf(area.id) + areaProjects(area.id).map { it.id } }
        // A place the sidebar no longer lists (a project closed today) goes last.
        val order = sidebar.filter { it in groups } + groups.keys.filter { it !in sidebar }
        return order.map { it to groups.getValue(it) }
    }

    /** An area's open projects, in Things' order. */
    fun areaProjects(areaId: String): List<Task> = projects().filter { it.areaId == areaId }

    /** To-dos filed in the area itself, outside any project. */
    fun areaTasks(areaId: String, closedOn: LocalDate? = null, zone: ZoneId = ZoneId.systemDefault()): List<Task> =
        visible(closedOn, zone) { it.isTodo && it.areaId == areaId && it.projectId == null }.sortedWith(byIndex)

    /**
     * A project's to-dos as Things shows them: those under no heading first, then each open
     * heading (empty ones too) with its to-dos. A to-do whose heading is archived or trashed
     * falls back to the first section.
     */
    fun projectSections(projectId: String, closedOn: LocalDate? = null, zone: ZoneId = ZoneId.systemDefault()): List<ProjectSection> {
        val headings = visible { it.type == TaskType.HEADING && it.projectId == projectId }.sortedWith(byIndex)
        val byHeading = projectTasks(projectId, closedOn, zone).groupBy { task -> task.headingId?.takeIf { id -> headings.any { it.id == id } } }
        val loose = ProjectSection(null, byHeading[null].orEmpty())
        return listOfNotNull(loose.takeIf { it.tasks.isNotEmpty() || headings.isEmpty() }) +
            headings.map { ProjectSection(it, byHeading[it.id].orEmpty()) }
    }

    private val Task.isTodo get() = type == TaskType.TODO

    /** Things' order within a list, ties broken by id so a refresh never swaps rows. */
    private val byIndex = compareBy<Task>({ it.index }, { it.id })

    /** Task6 and later (Task7 seen in 2026 accounts); Task3/Task4 are legacy rows the app no longer shows. */
    private val Task.isCurrentEntity
        get() = entity?.removePrefix("Task")?.toIntOrNull()?.let { it >= MIN_TASK_ENTITY_VERSION } ?: false

    /**
     * Untrashed to-dos of the current entity with a title that pass [predicate]: open ones, plus,
     * when [closedOn] is set, those completed or cancelled on that day (Things keeps them in place,
     * struck through, until they move to the Logbook).
     */
    private fun visible(
        closedOn: LocalDate? = null,
        zone: ZoneId = ZoneId.systemDefault(),
        predicate: (Task) -> Boolean,
    ): List<Task> = tasks.values.filter {
        val shown = it.isOpen || (closedOn != null && it.stopDate?.atZone(zone)?.toLocalDate() == closedOn)
        shown && !it.trashed && !it.inTrashedContainer && it.isCurrentEntity && it.title.isNotBlank() && predicate(it)
    }

    /** A to-do or heading of a project (or heading) in the Trash: hidden with it, as in Things. */
    private val Task.inTrashedContainer
        get() = projectId?.let(tasks::get)?.trashed == true || headingId?.let(tasks::get)?.trashed == true

    /** What deleting an area moves to the Trash: its projects and its own to-dos, not already there. */
    fun areaContents(areaId: String): List<Task> = tasks.values.filter {
        !it.trashed && it.isCurrentEntity && it.areaId == areaId &&
            ((it.type == TaskType.PROJECT) || (it.isTodo && it.projectId == null))
    }

    private fun projectVisibleInAnytime(projectId: String?, today: LocalDate): Boolean {
        val project = projectId?.let(tasks::get) ?: return true
        if (project.trashed || !project.isOpen || project.start == StartList.SOMEDAY) return false
        return project.startDate == null || !project.startDate.isAfter(today)
    }

    companion object {
        private const val MIN_TASK_ENTITY_VERSION = 6

        /** At most this many coming occurrences of one repeat (a daily one over four weeks). */
        private const val MAX_REPEATS_AHEAD = 31

        // Older areas and tags (Area2, Tag3) were replaced by Things' migration and are gone from its apps.
        private const val MIN_AREA_ENTITY_VERSION = 3
        private const val MIN_TAG_ENTITY_VERSION = 4

        private val MARKS = Regex("\\p{M}+")

        /** Lower case without accents, so "demenag" finds "Déménagement". */
        private fun fold(text: String) = MARKS.replace(java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD), "").lowercase()

        private val tagOrder = compareBy<Tag>({ it.index }, { it.title.lowercase() }, { it.id })

        private fun StoredObject.isCurrent(kind: String, min: Int) =
            entity?.takeIf { it.startsWith(kind) }?.removePrefix(kind)?.toIntOrNull()?.let { it >= min } ?: false

        fun from(state: HistoryState): ThingsLibrary {
            val raw = state.objects
            val rawTasks = raw.filterValues { it.entity?.startsWith("Task") == true }

            fun firstId(props: Map<String, JsonElement>, key: String): String? =
                (props[key] as? JsonArray)?.firstNotNullOfOrNull { it.stringOrNull() }

            fun directProject(id: String?) = id?.let { rawTasks[it]?.props }?.let { firstId(it, "pr") }
            fun directArea(id: String?) = id?.let { rawTasks[it]?.props }?.let { firstId(it, "ar") }

            // A project or to-do may still name an area deleted since: it then has none.
            val areaIds = raw.filterValues { it.isCurrent("Area", MIN_AREA_ENTITY_VERSION) }.keys
            val tasks = rawTasks.mapValues { (id, obj) ->
                val p = obj.props
                val heading = firstId(p, "agr")
                val project = firstId(p, "pr") ?: directProject(heading)
                val area = (firstId(p, "ar") ?: directArea(project) ?: directArea(heading))?.takeIf { it in areaIds }
                Task(
                    id = id,
                    title = p["tt"].stringOrNull().orEmpty(),
                    notes = Notes.toPlainText(p["nt"]),
                    type = when (p.int("tp")) { 1 -> TaskType.PROJECT; 2 -> TaskType.HEADING; else -> TaskType.TODO },
                    status = when (p.int("ss")) { 2 -> TaskStatus.CANCELLED; 3 -> TaskStatus.COMPLETED; else -> TaskStatus.OPEN },
                    start = when (p.int("st")) { 1 -> StartList.ANYTIME; 2 -> StartList.SOMEDAY; else -> StartList.INBOX },
                    startDate = p.day("sr"),
                    deadline = p.day("dd"),
                    projectId = project,
                    areaId = area,
                    headingId = heading,
                    tagIds = (p["tg"] as? JsonArray)?.mapNotNull { it.stringOrNull() }.orEmpty(),
                    index = p.int("ix") ?: 0,
                    todayIndex = p.int("ti") ?: 0,
                    evening = p.int("sb") == 1,
                    trashed = p["tr"].primitiveOrNull()?.booleanOrNull ?: false,
                    isRecurrenceTemplate = p["rr"] is JsonObject &&
                        (p["rt"] as? JsonArray).isNullOrEmpty(),
                    reminder = p.int("ato")?.takeIf { it in 0 until 86_400 }?.let { LocalTime.ofSecondOfDay(it.toLong()) },
                    lastAlarmInteraction = p["lai"].primitiveOrNull()?.doubleOrNull,
                    stopDate = p["sp"].primitiveOrNull()?.doubleOrNull?.let { Instant.ofEpochMilli((it * 1000).toLong()) },
                    recurrenceTemplateIds = (p["rt"] as? JsonArray)?.mapNotNull { it.stringOrNull() }.orEmpty(),
                    repeatType = (p["rr"] as? JsonObject)?.let { it.int("tp") ?: -1 },
                    repeatRuleJson = p["rr"] as? JsonObject,
                    modifiedAt = p["md"].primitiveOrNull()?.doubleOrNull,
                    nextOccurrence = p.day("tir"),
                    instanceCount = p.int("icc"),
                    entity = obj.entity,
                )
            }

            val areas = raw.filterValues { it.isCurrent("Area", MIN_AREA_ENTITY_VERSION) }.mapValues { (id, obj) ->
                Area(id, obj.props["tt"].stringOrNull().orEmpty(), obj.props.int("ix") ?: 0, obj.entity)
            }
            val tags = raw.filterValues { it.isCurrent("Tag", MIN_TAG_ENTITY_VERSION) }
                .mapValues { (id, obj) -> Tag(id, obj.props["tt"].stringOrNull().orEmpty(), obj.props.int("ix") ?: 0) }
            val checklists = raw.filterValues { it.entity?.startsWith("ChecklistItem") == true }
                .mapNotNull { (id, obj) ->
                    val owner = firstId(obj.props, "ts") ?: obj.props["ts"].stringOrNull() ?: return@mapNotNull null
                    owner to ChecklistItem(
                        id = id,
                        title = obj.props["tt"].stringOrNull().orEmpty(),
                        done = obj.props.int("ss") == 3,
                        index = obj.props.int("ix") ?: 0,
                        taskId = owner,
                        entity = obj.entity,
                    )
                }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, items) -> items.sortedBy { it.index } }

            val templateProps = rawTasks.filterKeys { tasks[it]?.isRecurrenceTemplate == true }.mapValues { (_, obj) -> JsonObject(obj.props) }
            return ThingsLibrary(tasks, areas, tags, checklists, templateProps)
        }

        private fun Map<String, JsonElement>.int(key: String): Int? = this[key].primitiveOrNull()?.intOrNull

        private fun Map<String, JsonElement>.day(key: String): LocalDate? {
            val value = this[key].primitiveOrNull() ?: return null
            val seconds = value.longOrNull ?: value.doubleOrNull?.toLong() ?: return null
            return ThingsDay.toLocalDate(seconds)
        }
    }
}
