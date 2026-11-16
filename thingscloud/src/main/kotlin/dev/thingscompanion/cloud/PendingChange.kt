package dev.thingscompanion.cloud

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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
    ) : PendingChange {
        val reminder: LocalTime? get() = reminderSecond?.let { LocalTime.ofSecondOfDay(it.toLong()) }

        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            if (library.task(taskId) != null) return null
            val schedule = schedule.toSchedule()
            return mapOf(taskId to InboxTask.payload(title, notes, library.nextTopIndex(schedule.start), now, schedule, reminder))
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
    data class SetDone(override val opId: String, val taskId: String, val done: Boolean) : PendingChange {
        override val replaces get() = "done:$taskId"

        override fun build(library: ThingsLibrary, now: Double): Map<String, JsonObject>? {
            val task = library.task(taskId) ?: throw NoSuchElementException("The to-do no longer exists.")
            if ((task.status == TaskStatus.COMPLETED) == done) return null
            require(library.canComplete(task)) { "This to-do can't be checked from the app." }
            return mapOf(taskId to TaskEdits.complete(task, done, now))
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
    fun overlay(state: HistoryState, changes: List<PendingChange>, now: Double): ThingsLibrary {
        if (changes.isEmpty()) return ThingsLibrary.from(state)
        val copy = HistoryState(state.objects)
        var library = ThingsLibrary.from(copy)
        for (change in changes) {
            val payload = try {
                change.build(library, now)
            } catch (e: IllegalArgumentException) {
                null
            } catch (e: NoSuchElementException) {
                null
            } ?: continue
            copy.apply(WireParser.parseItem(JsonObject(payload)))
            library = ThingsLibrary.from(copy)
        }
        return library
    }
}
