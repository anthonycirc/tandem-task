package dev.thingscompanion.cloud

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * A change made on the phone and kept until Things Cloud has it. Each one states the end result
 * wanted, not a diff: [build] compares it with the current to-dos and returns the history item
 * to commit, or null when Things already shows it. So a change can be shown at once (applied on
 * top of the cache) and sent later, and sending it again after an uncertain attempt is harmless.
 */
@Serializable
sealed interface PendingChange {
    /** Identifies this change in the queue. */
    val opId: String

    /**
     * The objects to commit, by id, or null when nothing is left to send.
     * Throws [IllegalArgumentException] or [NoSuchElementException] when the change can't apply.
     */
    fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>?

    /**
     * What this change sets, e.g. a to-do's Trash state: a newer change with the same key makes a
     * queued one pointless (and sending both would let the older win if confirmed out of order).
     * Null for creates and deletes, which are never replaced.
     */
    val replaces: String? get() = null

    /** A Schedule in a storable form. */
    @Serializable
    data class When(val kind: String, val day: Long? = null) {
        fun toSchedule(): Schedule {
            val date = day?.let(LocalDate::ofEpochDay)
            return when (kind) {
                "today" -> Schedule.Today(date!!)
                "evening" -> Schedule.Evening(date!!)
                "date" -> Schedule.OnDate(date!!)
                "anytime" -> Schedule.Anytime
                "someday" -> Schedule.Someday
                else -> Schedule.Inbox
            }
        }

        companion object {
            fun of(schedule: Schedule) = when (schedule) {
                Schedule.Inbox -> When("inbox")
                is Schedule.Today -> When("today", schedule.day.toEpochDay())
                is Schedule.Evening -> When("evening", schedule.day.toEpochDay())
                is Schedule.OnDate -> When("date", schedule.day.toEpochDay())
                Schedule.Anytime -> When("anytime")
                Schedule.Someday -> When("someday")
            }
        }
    }

    @Serializable
    @SerialName("add")
    data class AddTask(
        override val opId: String,
        val taskId: String,
        val title: String,
        val notes: String,
        val schedule: When,
        val reminderSecond: Int? = null,
        /** Where it is filed, when added from a project, heading or area; at most one. */
        val projectId: String? = null,
        val headingId: String? = null,
        val areaId: String? = null,
    ) : PendingChange {
        val reminder: LocalTime? get() = reminderSecond?.let { LocalTime.ofSecondOfDay(it.toLong()) }

        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            if (library.task(taskId) != null) return null
            val schedule = schedule.toSchedule()
            val heading = headingId?.let { library.task(it) ?: throw NoSuchElementException("The heading no longer exists.") }
            val project = (projectId ?: heading?.projectId)?.let { library.task(it) ?: throw NoSuchElementException("The project no longer exists.") }
            val index = when {
                project != null -> library.nextIndexInProject(project.id)
                areaId != null -> library.nextIndexInArea(areaId)
                else -> library.nextTopIndex(schedule.start)
            }
            // Under a heading, Things files the to-do by its heading only (agr, empty pr).
            val payload = InboxTask.payload(
                title, notes, index, now, schedule, reminder,
                projectId = projectId.takeIf { headingId == null }, areaId = areaId, headingId = headingId,
            )
            return mapOf(taskId to payload)
        }
    }

    @Serializable
    @SerialName("project_add")
    data class AddProject(
        override val opId: String,
        val projectId: String,
        val title: String,
        val notes: String,
        val schedule: When,
        val areaId: String? = null,
    ) : PendingChange {
        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            if (library.task(projectId) != null) return null
            areaId?.let { library.area(it) ?: throw NoSuchElementException("The area no longer exists.") }
            val schedule = schedule.toSchedule().takeIf { it != Schedule.Inbox } ?: Schedule.Anytime
            return mapOf(projectId to InboxTask.payload(title, notes, library.nextProjectIndex(areaId), now, schedule, type = 1, areaId = areaId))
        }
    }

    @Serializable
    @SerialName("heading_add")
    data class AddHeading(override val opId: String, val headingId: String, val projectId: String, val title: String) : PendingChange {
        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            if (library.task(headingId) != null) return null
            val project = library.task(projectId) ?: throw NoSuchElementException("The project no longer exists.")
            require(library.canEditProject(project)) { "Headings can only be added to a project." }
            val index = library.nextIndexInProject(projectId)
            return mapOf(headingId to InboxTask.payload(title, "", index, now, Schedule.Anytime, type = 2, projectId = projectId))
        }
    }

    @Serializable
    @SerialName("area_add")
    data class AddArea(override val opId: String, val areaId: String, val title: String) : PendingChange {
        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            if (library.area(areaId) != null) return null
            return mapOf(areaId to TaskEdits.newArea(title, library.nextAreaIndex()))
        }
    }

    @Serializable
    @SerialName("area_rename")
    data class RenameArea(override val opId: String, val areaId: String, val title: String) : PendingChange {
        override val replaces get() = "area:$areaId"

        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            val area = library.area(areaId) ?: throw NoSuchElementException("The area no longer exists.")
            return TaskEdits.renameArea(area, title, now)?.let { mapOf(areaId to it) }
        }
    }

    @Serializable
    @SerialName("edit")
    data class EditTask(
        override val opId: String,
        val taskId: String,
        val title: String,
        val notes: String,
        val schedule: When,
        val reminderSecond: Int? = null,
    ) : PendingChange {
        override val replaces get() = "edit:$taskId"

        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            val task = library.task(taskId) ?: throw NoSuchElementException("The to-do no longer exists.")
            val reminder = reminderSecond?.let { LocalTime.ofSecondOfDay(it.toLong()) }
            return TaskEdits.edit(task, title, notes, schedule.toSchedule(), reminder, now)?.let { mapOf(taskId to it) }
        }
    }

    @Serializable
    @SerialName("done")
    data class SetDone(
        override val opId: String,
        val taskId: String,
        val done: Boolean,
        /** The local day of the tap (epoch day): an "after completion" repeat counts from it, even if sent later. */
        val day: Long? = null,
    ) : PendingChange {
        override val replaces get() = "done:$taskId"

        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            val task = library.task(taskId) ?: throw NoSuchElementException("The to-do no longer exists.")
            if ((task.status == TaskStatus.COMPLETED) == done) return null
            require(library.canComplete(task)) { "This to-do can't be checked from the app." }
            val completion = mapOf(taskId to TaskEdits.complete(task, done, now))
            // An "after completion" repeat also moves its template on, in the same item.
            val (template, rule) = library.afterCompletionRule(task) ?: return completion
            val completedOn = day?.let(LocalDate::ofEpochDay) ?: ThingsDay.today(Instant.ofEpochMilli((now * 1000).toLong()))
            return completion + (template.id to TaskEdits.afterCompletion(template, rule, completedOn))
        }
    }

    /** A project completed, cancelled or reopened; closing it closes its open to-dos and headings too. */
    @Serializable
    @SerialName("project_status")
    data class SetProjectStatus(override val opId: String, val projectId: String, val status: TaskStatus) : PendingChange {
        override val replaces get() = "done:$projectId"

        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            val project = library.task(projectId) ?: throw NoSuchElementException("The project no longer exists.")
            require(library.canEditProject(project)) { "This project can't be changed from the app." }
            if (project.status == status) return null
            val closing = if (status == TaskStatus.OPEN) emptyList() else library.openChildren(project)
            return (listOf(project) + closing).associate { it.id to TaskEdits.setStatus(it, status, now) }
        }
    }

    @Serializable
    @SerialName("deadline")
    data class SetDeadline(override val opId: String, val taskId: String, val day: Long?) : PendingChange {
        override val replaces get() = "deadline:$taskId"

        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            val task = library.task(taskId) ?: throw NoSuchElementException("The to-do no longer exists.")
            require(library.canSetDeadline(task)) { "This deadline can't be changed from the app." }
            return TaskEdits.setDeadline(task, day?.let(LocalDate::ofEpochDay), now)?.let { mapOf(taskId to it) }
        }
    }

    /** Where a to-do goes: a project, a heading, an area, the Inbox, or (all null) out of any container. */
    @Serializable
    data class Destination(
        val projectId: String? = null,
        val headingId: String? = null,
        val areaId: String? = null,
        val inbox: Boolean = false,
    )

    /** Files a to-do elsewhere, as things3-cloud's `edit --move` (and Things, for headings) writes it. */
    @Serializable
    @SerialName("move")
    data class MoveTask(override val opId: String, val taskId: String, val to: Destination) : PendingChange {
        override val replaces get() = "move:$taskId"

        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            val task = library.task(taskId) ?: throw NoSuchElementException("The to-do no longer exists.")
            require(library.canMove(task)) { "This to-do can't be moved from the app." }
            to.headingId?.let { require(library.task(it)?.type == TaskType.HEADING) { "The heading no longer exists." } }
            to.projectId?.let { require(library.task(it)?.type == TaskType.PROJECT) { "The project no longer exists." } }
            to.areaId?.let { library.area(it) ?: throw NoSuchElementException("The area no longer exists.") }
            return TaskEdits.move(task, to, now)?.let { mapOf(taskId to it) }
        }
    }

    @Serializable
    @SerialName("project_move")
    data class MoveProject(override val opId: String, val projectId: String, val areaId: String?) : PendingChange {
        override val replaces get() = "move:$projectId"

        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            val project = library.task(projectId) ?: throw NoSuchElementException("The project no longer exists.")
            require(library.canEditProject(project)) { "This project can't be moved from the app." }
            areaId?.let { library.area(it) ?: throw NoSuchElementException("The area no longer exists.") }
            return TaskEdits.moveProject(project, areaId, now)?.let { mapOf(projectId to it) }
        }
    }

    /**
     * A list's order after a drag: [order] is the ids as the list should read, with [taskId] where
     * it was dropped (in Today, the ids of the section it was dropped in). The index (`ix`, or `ti` in Today) is computed when sending, halfway between
     * the neighbours, renumbering the list in the same commit when they leave no room. In Today,
     * [evening] says which section it was dropped in.
     */
    @Serializable
    @SerialName("reorder")
    data class Reorder(
        override val opId: String,
        val taskId: String,
        val order: List<String>,
        val listKey: String,
        val todayOrder: Boolean = false,
        val evening: Boolean? = null,
    ) : PendingChange {
        // Per to-do: a later drag of the same one replaces this; drags of others in the list stay.
        override val replaces get() = "order:$taskId"

        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            val task = library.task(taskId) ?: throw NoSuchElementException("The to-do no longer exists.")
            require(library.canReorder(task)) { "This can't be reordered from the app." }
            val siblings = order.mapNotNull(library::task).filter(library::canReorder)
            val indexes = TaskEdits.placeIndexes(siblings, task, if (todayOrder) Task::todayIndex else Task::index)
            val key = if (todayOrder) "ti" else "ix"
            val sections = if (todayOrder && evening != null && evening != task.evening) mapOf(taskId to evening) else emptyMap()
            val ids = indexes.keys + sections.keys
            if (ids.isEmpty()) return null
            return ids.associateWith { id ->
                val target = library.task(id)!!
                TaskEdits.reorder(target, indexes[id]?.let { key to it }, sections[id], now)
            }
        }
    }

    /**
     * Deletes an area (`t:2`, as things3-cloud does), first moving its projects and own to-dos to
     * the Trash in the same item, so nothing is left filed in an area that is gone.
     */
    @Serializable
    @SerialName("area_delete")
    data class DeleteArea(override val opId: String, val areaId: String) : PendingChange {
        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            val area = library.area(areaId) ?: return null
            val trashed = library.areaContents(areaId).associate { it.id to TaskEdits.setTrashed(it, true, now) }
            return trashed + (areaId to TaskEdits.delete(area.entity))
        }
    }

    /**
     * Makes a dated to-do repeat, as Things 3 does: a new template copied from it (with copies of
     * its checklist items, [checklistIds] in checklist order) and the to-do linked to it as its
     * first occurrence. Things clients create the next occurrences from the template.
     */
    @Serializable
    @SerialName("make_repeating")
    data class MakeRepeating(
        override val opId: String,
        val taskId: String,
        val templateId: String,
        val unit: RepeatUnit,
        val every: Int,
        val afterCompletion: Boolean,
        val checklistIds: List<String>,
        /** ISO day numbers (1 Monday .. 7 Sunday) of a fixed weekly rule. */
        val weekdays: List<Int> = emptyList(),
    ) : PendingChange {
        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            val task = library.task(taskId) ?: throw NoSuchElementException("The to-do no longer exists.")
            if (templateId in task.recurrenceTemplateIds) return null
            require(library.canMakeRepeating(task)) { "This to-do can't be made to repeat from the app." }
            val rule = RepeatRule(unit, every, afterCompletion, weekdays.map(DayOfWeek::of))
            require(TaskEdits.canWriteRule(rule, task.startDate!!)) { "This repeat can't be set from the app yet." }
            val checklist = library.checklist(taskId)
            require(checklist.size <= checklistIds.size) { "The checklist changed; try again." }
            return TaskEdits.makeRepeating(task, checklist, templateId, checklistIds, rule, now)
        }

        companion object {
            operator fun invoke(opId: String, taskId: String, templateId: String, rule: RepeatRule, checklistIds: List<String>) =
                MakeRepeating(opId, taskId, templateId, rule.unit, rule.every, rule.afterCompletion, checklistIds, rule.weekdays.map { it.value })
        }
    }

    @Serializable
    @SerialName("trash")
    data class SetTrashed(override val opId: String, val taskId: String, val trashed: Boolean) : PendingChange {
        override val replaces get() = "trash:$taskId"

        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            val task = library.task(taskId) ?: throw NoSuchElementException("The to-do no longer exists.")
            if (task.trashed == trashed) return null
            require(if (trashed) library.canTrash(task) else library.canRestore(task)) { "This to-do can't be moved from the app." }
            return mapOf(taskId to TaskEdits.setTrashed(task, trashed, now))
        }
    }

    @Serializable
    @SerialName("checklist_add")
    data class AddChecklistItem(
        override val opId: String,
        val itemId: String,
        val taskId: String,
        val title: String,
    ) : PendingChange {
        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            if (library.checklistItem(itemId) != null) return null
            val task = library.task(taskId) ?: throw NoSuchElementException("The to-do no longer exists.")
            require(library.canEditChecklist(task)) { "This checklist can't be changed from the app." }
            val index = (library.checklist(taskId).maxOfOrNull { it.index } ?: -1) + 1
            return mapOf(itemId to TaskEdits.newChecklistItem(taskId, title, index, now))
        }
    }

    @Serializable
    @SerialName("checklist_rename")
    data class RenameChecklistItem(override val opId: String, val itemId: String, val title: String) : PendingChange {
        override val replaces get() = "rename:$itemId"

        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            val item = library.checklistItem(itemId) ?: throw NoSuchElementException("The checklist item no longer exists.")
            requireEditable(library, item)
            return TaskEdits.renameChecklistItem(item, title, now)?.let { mapOf(itemId to it) }
        }
    }

    @Serializable
    @SerialName("checklist_done")
    data class SetChecklistItemDone(override val opId: String, val itemId: String, val done: Boolean) : PendingChange {
        override val replaces get() = "checklist_done:$itemId"

        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            val item = library.checklistItem(itemId) ?: throw NoSuchElementException("The checklist item no longer exists.")
            requireEditable(library, item)
            if (item.done == done) return null
            return mapOf(itemId to TaskEdits.checkChecklistItem(item, done, now))
        }
    }

    @Serializable
    @SerialName("checklist_delete")
    data class DeleteChecklistItem(override val opId: String, val itemId: String) : PendingChange {
        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            val item = library.checklistItem(itemId) ?: return null
            requireEditable(library, item)
            return mapOf(itemId to TaskEdits.delete(item.entity))
        }
    }
}

/** A template's checklist is left to Things, as in the app's forms. */
private fun requireEditable(library: ThingsLibrary, item: ChecklistItem) {
    val owner = library.task(item.taskId) ?: return
    require(library.canEditChecklist(owner)) { "This checklist can't be changed from the app." }
}

object PendingChanges {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(PendingChange.serializer())

    /** Adds [change] after the queued ones, dropping those it makes pointless (see [PendingChange.replaces]). */
    fun enqueue(queue: List<PendingChange>, change: PendingChange): List<PendingChange> {
        val replaced: Set<String> = when (change) {
            is PendingChange.DeleteChecklistItem -> setOf("rename:${change.itemId}", "checklist_done:${change.itemId}")
            else -> setOfNotNull(change.replaces)
        }
        return queue.filterNot { it.replaces in replaced } + change
    }

    fun toJson(changes: List<PendingChange>): String = json.encodeToString(serializer, changes)

    fun fromJson(text: String): List<PendingChange> = json.decodeFromString(serializer, text)

    /**
     * The to-dos as they will be once [changes] reach Things: each change applied in order on a
     * copy of [state]. A change that can't apply is left out here; sending it reports why.
     */
    fun overlay(state: HistoryState, changes: List<PendingChange>, now: Double): ThingsLibrary = overlayState(state, changes, now).library

    /** [overlay], also returning the state it built, so a later change can be applied alone with [applyOne]. */
    fun overlayState(state: HistoryState, changes: List<PendingChange>, now: Double): Shown {
        var shown = Shown(HistoryState(state.objects), ThingsLibrary.from(state))
        for (change in changes) shown = applyOne(shown, change, now)
        return shown
    }

    /**
     * Whether [latest] can be applied alone on what [previous] showed: only when the queue just
     * grew by it. When it also dropped a superseded change, the changes after that one were
     * judged against a state that no longer holds, so the whole queue must be replayed.
     */
    fun canApplyAlone(previous: List<PendingChange>, pending: List<PendingChange>, latest: PendingChange?): Boolean =
        latest != null && pending == previous + latest

    /** [shown] with one more change: a copy of its state, one rebuild of the lists. */
    fun applyOne(shown: Shown, change: PendingChange, now: Double): Shown {
        val payload = try {
            change.build(shown.library, now)
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: NoSuchElementException) {
            null
        } ?: return shown
        val copy = HistoryState(shown.state.objects)
        copy.apply(WireParser.parseItem(JsonObject(payload)))
        return Shown(copy, ThingsLibrary.from(copy))
    }

    /** A state with queued changes applied, and the lists built from it; never mutated. */
    class Shown(val state: HistoryState, val library: ThingsLibrary)
}
