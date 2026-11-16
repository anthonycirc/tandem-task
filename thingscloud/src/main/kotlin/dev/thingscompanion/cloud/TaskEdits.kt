package dev.thingscompanion.cloud

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
