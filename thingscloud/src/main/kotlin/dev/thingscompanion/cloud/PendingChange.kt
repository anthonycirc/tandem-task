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
import java.time.Month

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
        val tagIds: List<String> = emptyList(),
    ) : PendingChange {
        val reminder: LocalTime? get() = reminderSecond?.let { LocalTime.ofSecondOfDay(it.toLong()) }

        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            if (library.task(taskId) != null) return null
            val schedule = schedule.toSchedule()
            val heading = headingId?.let { library.task(it) ?: throw NoSuchElementException("The heading no longer exists.") }
            val project = (projectId ?: heading?.projectId)?.let { library.task(it) ?: throw NoSuchElementException("The project no longer exists.") }
            // Filing in an area Things no longer has made Things 3 for Mac crash on sync (2026-11-17).
            areaId?.let { library.area(it) ?: throw NoSuchElementException("The area no longer exists.") }
            val index = when {
                project != null -> library.nextIndexInProject(project.id)
                areaId != null -> library.nextIndexInArea(areaId)
                else -> library.nextTopIndex(schedule.start)
            }
            // Under a heading, Things files the to-do by its heading only (agr, empty pr).
            val payload = InboxTask.payload(
                title, notes, index, now, schedule, reminder,
                projectId = projectId.takeIf { headingId == null }, areaId = areaId, headingId = headingId,
                tagIds = library.knownTags(tagIds),
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
        val tagIds: List<String> = emptyList(),
    ) : PendingChange {
        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            if (library.task(projectId) != null) return null
            areaId?.let { library.area(it) ?: throw NoSuchElementException("The area no longer exists.") }
            val schedule = schedule.toSchedule().takeIf { it != Schedule.Inbox } ?: Schedule.Anytime
            return mapOf(projectId to InboxTask.payload(title, notes, library.nextProjectIndex(areaId), now, schedule, type = 1, areaId = areaId, tagIds = library.knownTags(tagIds)))
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
            // Unchecking also reopens a cancelled to-do.
            if (if (done) task.status == TaskStatus.COMPLETED else task.status == TaskStatus.OPEN) return null
            require(library.canComplete(task)) { "This to-do can't be checked from the app." }
            val completion = mapOf(taskId to TaskEdits.complete(task, done, now))
            // An "after completion" repeat also moves its template on, in the same item.
            val (template, rule) = library.afterCompletionRule(task) ?: return completion
            val completedOn = day?.let(LocalDate::ofEpochDay) ?: ThingsDay.today(Instant.ofEpochMilli((now * 1000).toLong()))
            return completion + (template.id to TaskEdits.afterCompletion(template, rule, completedOn))
        }
    }

    /**
     * A to-do cancelled, or reopened after a cancel, as Things writes it ({ss, sp, md}). Cancelling an
     * "after completion" occurrence moves its template on as checking it does: Things' own cancels
     * in the history carry the same {acrd, tir}. Reopening one is left to Things.
     */
    @Serializable
    @SerialName("cancel")
    data class SetCancelled(
        override val opId: String,
        val taskId: String,
        val cancelled: Boolean,
        /** The local day of the long press (epoch day): an "after completion" repeat counts from it, even if sent later. */
        val day: Long? = null,
    ) : PendingChange {
        override val replaces get() = "done:$taskId"

        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            val task = library.task(taskId) ?: throw NoSuchElementException("The to-do no longer exists.")
            // Reopening only undoes a cancel; a completed to-do is unchecked with SetDone.
            if (cancelled == (task.status == TaskStatus.CANCELLED) || (!cancelled && task.status != TaskStatus.CANCELLED)) return null
            require(library.canComplete(task) && (cancelled || library.afterCompletionRule(task) == null)) { "This to-do can't be cancelled from the app." }
            val change = mapOf(taskId to TaskEdits.setStatus(task, if (cancelled) TaskStatus.CANCELLED else TaskStatus.OPEN, now))
            val (template, rule) = library.afterCompletionRule(task) ?: return change
            val cancelledOn = day?.let(LocalDate::ofEpochDay) ?: ThingsDay.today(Instant.ofEpochMilli((now * 1000).toLong()))
            return change + (template.id to TaskEdits.afterCompletion(template, rule, cancelledOn))
        }
    }

    /**
     * A repeating to-do's coming occurrence checked in Upcoming before Things created it, as Things 3.23+
     * does: a completed copy of the template on [day] (epoch day), [copyId], and the template moved on.
     */
    @Serializable
    @SerialName("complete_ahead")
    data class CompleteAhead(override val opId: String, val templateId: String, val day: Long, val copyId: String) : PendingChange {
        override val replaces get() = "ahead:$templateId:$day"

        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            if (library.task(copyId) != null) return null
            val template = library.task(templateId) ?: throw NoSuchElementException("The repeating to-do no longer exists.")
            val on = LocalDate.ofEpochDay(day)
            require(library.canCompleteAhead(template.copy(startDate = on))) { "This occurrence can't be checked ahead from the app." }
            val next = requireNotNull(library.repeatRule(template)?.nextAfter(on))
            return TaskEdits.completeAhead(template, on, next, copyId, now)
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
            // Today shows undated to-dos for their deadline; Things never gives them a Today order or evening.
            require(!todayOrder || task.startDate != null) { "This to-do is in Today for its deadline only." }
            val siblings = order.mapNotNull(library::task).filter { library.canReorder(it) && (!todayOrder || it.startDate != null) }
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
            // Its projects and to-dos go to the Trash out of the area ({tr, ar: []}), as Things writes it.
            val trashed = library.areaContents(areaId).associate { it.id to TaskEdits.trashOutOfArea(it, now) }
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
        val monthDay: Int? = null,
        val weekdayOrdinal: Int? = null,
        /** 1 January .. 12 December, for a yearly rule. */
        val month: Int? = null,
    ) : PendingChange {
        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            val task = library.task(taskId) ?: throw NoSuchElementException("The to-do no longer exists.")
            if (templateId in task.recurrenceTemplateIds) return null
            require(library.canMakeRepeating(task)) { "This to-do can't be made to repeat from the app." }
            val rule = RepeatRule(unit, every, afterCompletion, weekdays.map(DayOfWeek::of), monthDay = monthDay, month = month?.let(Month::of), weekdayOrdinal = weekdayOrdinal)
            require(TaskEdits.canWriteRule(rule, task.startDate!!)) { "This repeat can't be set from the app yet." }
            val checklist = library.checklist(taskId)
            require(checklist.size <= checklistIds.size) { "The checklist changed; try again." }
            return TaskEdits.makeRepeating(task, checklist, templateId, checklistIds, rule, now)
        }

        companion object {
            operator fun invoke(opId: String, taskId: String, templateId: String, rule: RepeatRule, checklistIds: List<String>) =
                MakeRepeating(opId, taskId, templateId, rule.unit, rule.every, rule.afterCompletion, checklistIds, rule.weekdays.map { it.value }, rule.monthDay, rule.weekdayOrdinal, rule.month?.value)
        }
    }

    /**
     * Deletes trashed to-dos for good, as Things 3 does when the Trash is emptied: each to-do and
     * its checklist items `t:2`, live instances of a deleted template unlinked (`rt` []), and a
     * `Tombstone2` for each deleted plain to-do and checklist item (none for templates, as Things
     * wrote them). [taskIds] are frozen when the user confirms, so to-dos trashed later stay.
     */
    @Serializable
    @SerialName("delete_forever")
    data class DeleteForever(override val opId: String, val taskIds: List<String>) : PendingChange {
        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            val picked = taskIds.distinct().mapNotNull(library::task).filter(library::canDeleteForever)
            if (picked.isEmpty()) return null
            // A project or heading goes with everything filed in it, as Things empties its Trash.
            val tasks = (picked + picked.filter { it.type != TaskType.TODO }.flatMap { library.containedTasks(it.id) }).distinctBy { it.id }
            val deleted = tasks.map { it.id }.toSet()
            val payload = linkedMapOf<String, JsonObject>()
            for (task in tasks) {
                payload[task.id] = TaskEdits.deleteForever("Task7")
                // Projects and headings get no tombstone and have no checklist.
                if (task.type != TaskType.TODO) continue
                val checklist = library.checklist(task.id).map { it.id }
                checklist.forEach { payload[it] = TaskEdits.deleteForever("ChecklistItem3") }
                if (task.isRecurrenceTemplate) {
                    library.instancesOf(task.id).filter { it.id !in deleted }.forEach { payload[it.id] = TaskEdits.unlinkInstance() }
                } else {
                    (listOf(task.id) + checklist).forEach { payload[TaskEdits.tombstoneId(opId, it)] = TaskEdits.tombstone(it, now) }
                }
            }
            return payload
        }
    }

    @Serializable
    @SerialName("trash")
    data class SetTrashed(
        override val opId: String,
        val taskId: String,
        val trashed: Boolean,
        /** The local day of the tap (epoch day): trashing an "after completion" occurrence counts from it. */
        val day: Long? = null,
    ) : PendingChange {
        override val replaces get() = "trash:$taskId"

        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            val task = library.task(taskId) ?: throw NoSuchElementException("The to-do no longer exists.")
            if (task.trashed == trashed) return null
            require(if (trashed) library.canTrash(task) else library.canRestore(task)) { "This to-do can't be moved from the app." }
            val change = mapOf(taskId to TaskEdits.setTrashed(task, trashed, now))
            // An open "after completion" occurrence: Things moves its template on when it goes to the
            // Trash, as when it is checked, and clears the template's dates when it comes back.
            if (!task.isOpen) return change
            val (template, rule) = library.afterCompletionRule(task) ?: return change
            val templateChange = if (trashed) {
                val on = day?.let(LocalDate::ofEpochDay) ?: ThingsDay.today(Instant.ofEpochMilli((now * 1000).toLong()))
                TaskEdits.afterCompletion(template, rule, on)
            } else {
                TaskEdits.clearAfterCompletion(template)
            }
            return change + (template.id to templateChange)
        }
    }

    /** Sets a to-do's or project's known tags, in Things' tag order; tags the app does not know are kept. */
    @Serializable
    @SerialName("set_tags")
    data class SetTags(override val opId: String, val taskId: String, val tagIds: List<String>) : PendingChange {
        override val replaces get() = "tags:$taskId"

        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            val task = library.task(taskId) ?: throw NoSuchElementException("The to-do no longer exists.")
            require(library.canTag(task)) { "This item can't be tagged from the app." }
            val wanted = tagIds.toSet()
            val known = library.tags()
            val tags = known.filter { it.id in wanted }.map { it.id }
            if (tags == known.filter { it.id in task.tagIds }.map { it.id }) return null
            // Tags the app does not know (not synced yet, or from an older Things) stay as they were.
            val kept = task.tagIds.filter { id -> known.none { it.id == id } }
            return mapOf(taskId to TaskEdits.setTags(task, tags + kept, now))
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
