package dev.thingscompanion.cloud

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.LocalTime

/** Partial updates (`t:1`) of existing to-dos: only the changed properties plus `md`, on the to-do's own entity. */
object TaskEdits {
    fun complete(task: Task, done: Boolean, now: Double): JsonObject = update(
        task,
        mapOf(
            "ss" to JsonPrimitive(if (done) 3 else 0),
            "sp" to if (done) InboxTask.plainNumber(now) else JsonNull,
        ),
        now,
    )

    /**
     * The update turning [task] into the given values, or null when nothing changes.
     * Repeating to-dos may change title and notes only.
     */
    fun edit(task: Task, title: String, notes: String, schedule: Schedule, reminder: LocalTime?, now: Double): JsonObject? {
        val newTitle = title.trim()
        val newNotes = notes.trim()
        require(newTitle.isNotEmpty()) { "The title is empty." }
        require(reminder == null || schedule.date != null) { "A reminder needs a date." }

        val scheduleChanged = schedule.start != task.start || schedule.date != task.startDate ||
            schedule.evening != task.evening || reminder != task.reminder
        require(!scheduleChanged || !task.isRepeating) { "When and reminder of a repeating to-do can't be changed here." }

        val changes = linkedMapOf<String, JsonElement>()
        if (newTitle != task.title) changes["tt"] = JsonPrimitive(newTitle)
        if (newNotes != (task.notes ?: "")) changes["nt"] = InboxTask.notesJson(newNotes)
        if (schedule.start != task.start) changes["st"] = JsonPrimitive(InboxTask.startCode(schedule.start))
        if (schedule.date != task.startDate) {
            val day = schedule.date?.let { JsonPrimitive(it.toEpochDay() * InboxTask.SECONDS_PER_DAY) } ?: JsonNull
            changes["sr"] = day
            changes["tir"] = day
        }
        if (schedule.evening != task.evening) changes["sb"] = JsonPrimitive(if (schedule.evening) 1 else 0)
        if (reminder != task.reminder) changes["ato"] = reminder?.let { JsonPrimitive(it.toSecondOfDay()) } ?: JsonNull
        return if (changes.isEmpty()) null else update(task, changes, now)
    }

    /**
     * The template update the Things apps write with the completion of an "after completion"
     * instance: `acrd` the day it was completed, `tir` that day plus the interval (when the next
     * instance starts; a Things client creates it then). No `md`, as they write it.
     */
    fun afterCompletion(template: Task, rule: RepeatRule, completedOn: LocalDate): JsonObject {
        val days = when (rule.unit) {
            RepeatUnit.DAY -> rule.every.toLong()
            RepeatUnit.WEEK -> 7L * rule.every
            else -> throw IllegalArgumentException("Only daily and weekly repeats are checked from the app.")
        }
        fun midnight(date: LocalDate) = JsonPrimitive(date.toEpochDay() * InboxTask.SECONDS_PER_DAY)
        return buildJsonObject {
            put("t", 1)
            put("e", template.entity)
            put("p", buildJsonObject {
                put("acrd", midnight(completedOn))
                put("tir", midnight(completedOn.plusDays(days)))
            })
        }
    }

    /** Open, completed or cancelled (`ss` 0, 3, 2), with the stop date Things writes (`sp`). */
    fun setStatus(task: Task, status: TaskStatus, now: Double): JsonObject = update(
        task,
        mapOf(
            "ss" to JsonPrimitive(statusCode(status)),
            "sp" to if (status == TaskStatus.OPEN) JsonNull else InboxTask.plainNumber(now),
        ),
        now,
    )

    fun statusCode(status: TaskStatus) = when (status) { TaskStatus.OPEN -> 0; TaskStatus.CANCELLED -> 2; TaskStatus.COMPLETED -> 3 }

    /** The deadline (`dd`), at the day's midnight UTC like the other day fields, or none. */
    fun setDeadline(task: Task, deadline: LocalDate?, now: Double): JsonObject? {
        if (deadline == task.deadline) return null
        val value = deadline?.let { JsonPrimitive(it.toEpochDay() * InboxTask.SECONDS_PER_DAY) } ?: JsonNull
        return update(task, mapOf("dd" to value), now)
    }

    /**
     * A to-do filed under [to]: one of `pr`, `agr`, `ar` set, the other two emptied; out of the
     * Inbox it goes to Anytime, into the Inbox it loses its dates. Null when already there.
     */
    fun move(task: Task, to: PendingChange.Destination, now: Double): JsonObject? {
        val already = when {
            to.inbox -> task.start == StartList.INBOX && task.projectId == null && task.areaId == null
            to.headingId != null -> task.headingId == to.headingId
            to.projectId != null -> task.headingId == null && task.projectId == to.projectId
            to.areaId != null -> task.headingId == null && task.projectId == null && task.areaId == to.areaId
            else -> task.projectId == null && task.areaId == null && task.start != StartList.INBOX
        }
        if (already) return null
        fun ids(id: String?) = JsonArray(listOfNotNull(id).map(::JsonPrimitive))
        val changes = linkedMapOf<String, JsonElement>(
            "pr" to ids(to.projectId.takeIf { to.headingId == null }),
            "ar" to ids(to.areaId),
            "agr" to ids(to.headingId),
        )
        if (to.inbox) {
            changes["st"] = JsonPrimitive(0)
            changes["sr"] = JsonNull
            changes["tir"] = JsonNull
            changes["sb"] = JsonPrimitive(0)
        } else if (task.start == StartList.INBOX) {
            changes["st"] = JsonPrimitive(1)
        }
        return update(task, changes, now)
    }

    /** A project into an area, or out of any (`ar`). */
    fun moveProject(project: Task, areaId: String?, now: Double): JsonObject? =
        if (project.areaId == areaId) null else update(project, mapOf("ar" to JsonArray(listOfNotNull(areaId).map(::JsonPrimitive))), now)

    /**
     * The new indexes once [moved] sits where it is in [siblings]: none when it is already between
     * its neighbours, else halfway between them (one past at either end), else every sibling
     * renumbered (position + 1) * 1024, only those that change, as things3-cloud does.
     */
    fun placeIndexes(siblings: List<Task>, moved: Task, index: (Task) -> Int): Map<String, Int> {
        val position = siblings.indexOfFirst { it.id == moved.id }
        require(position >= 0) { "The to-do is not in this list." }
        val previous = siblings.getOrNull(position - 1)?.let(index)
        val next = siblings.getOrNull(position + 1)?.let(index)
        val current = index(moved)
        if ((previous == null || previous < current) && (next == null || current < next)) return emptyMap()
        val placed = when {
            previous == null && next == null -> 0
            previous == null -> next!! - 1
            next == null -> previous + 1
            previous + 1 < next -> Math.floorDiv(previous + next, 2)
            else -> null
        }
        if (placed != null) return mapOf(moved.id to placed)
        return siblings.withIndex()
            .filter { (i, task) -> index(task) != (i + 1) * 1024 }
            .associate { (i, task) -> task.id to (i + 1) * 1024 }
    }

    /** An index (`ix` or `ti`) and, for Today, the section (`sb`). */
    fun reorder(task: Task, index: Pair<String, Int>?, evening: Boolean?, now: Double): JsonObject {
        val changes = linkedMapOf<String, JsonElement>()
        index?.let { (key, value) -> changes[key] = JsonPrimitive(value) }
        evening?.let { changes["sb"] = JsonPrimitive(if (it) 1 else 0) }
        return update(task, changes, now)
    }

    /** Moves a to-do to the Trash, or back (`tr`). */
    fun setTrashed(task: Task, trashed: Boolean, now: Double): JsonObject =
        update(task.entity, mapOf("tr" to JsonPrimitive(trashed)), now)

    /** A new checklist item, shaped like the ones the Things app writes. */
    fun newChecklistItem(taskId: String, title: String, index: Int, now: Double): JsonObject {
        val trimmed = title.trim()
        require(trimmed.isNotEmpty()) { "The title is empty." }
        return buildJsonObject {
            put("t", 0)
            put("e", "ChecklistItem3")
            put("p", buildJsonObject {
                put("tt", trimmed)
                put("ts", JsonArray(listOf(JsonPrimitive(taskId))))
                put("ix", index)
                put("ss", 0)
                put("sp", JsonNull)
                put("cd", now.toLong())
                put("md", JsonNull)
                put("lt", true)
                put("xx", buildJsonObject {
                    put("_t", "oo")
                    put("sn", JsonObject(emptyMap()))
                })
            })
        }
    }

    /** A new area, with the keys things3-cloud writes (no `cd` or `md`). */
    fun newArea(title: String, index: Int): JsonObject {
        val trimmed = title.trim()
        require(trimmed.isNotEmpty()) { "The title is empty." }
        return buildJsonObject {
            put("t", 0)
            put("e", "Area3")
            put("p", buildJsonObject {
                put("tt", trimmed)
                put("tg", JsonArray(emptyList()))
                put("ix", index)
                put("xx", buildJsonObject {
                    put("_t", "oo")
                    put("sn", JsonObject(emptyMap()))
                })
            })
        }
    }

    fun renameArea(area: Area, title: String, now: Double): JsonObject? {
        val trimmed = title.trim()
        require(trimmed.isNotEmpty()) { "The title is empty." }
        return if (trimmed == area.title) null else update(area.entity, mapOf("tt" to JsonPrimitive(trimmed)), now)
    }

    fun checkChecklistItem(item: ChecklistItem, done: Boolean, now: Double): JsonObject = update(
        item.entity,
        mapOf(
            "ss" to JsonPrimitive(if (done) 3 else 0),
            "sp" to if (done) InboxTask.plainNumber(now) else JsonNull,
        ),
        now,
    )

    fun renameChecklistItem(item: ChecklistItem, title: String, now: Double): JsonObject? {
        val trimmed = title.trim()
        require(trimmed.isNotEmpty()) { "The title is empty." }
        return if (trimmed == item.title) null else update(item.entity, mapOf("tt" to JsonPrimitive(trimmed)), now)
    }

    /** Deletes an object outright (`t:2`), as things3-cloud does. */
    fun delete(entity: String?): JsonObject = buildJsonObject {
        put("t", 2)
        put("e", entity)
        put("p", JsonObject(emptyMap()))
    }

    private fun update(task: Task, changes: Map<String, JsonElement>, now: Double) = update(task.entity, changes, now)

    private fun update(entity: String?, changes: Map<String, JsonElement>, now: Double) = buildJsonObject {
        put("t", 1)
        put("e", entity)
        put("p", buildJsonObject {
            changes.forEach { (key, value) -> put(key, value) }
            put("md", InboxTask.plainNumber(now))
        })
    }
}
