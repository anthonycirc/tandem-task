package dev.thingscompanion.cloud

import kotlinx.serialization.json.JsonObject
import java.time.LocalDate
import java.time.LocalTime

/** Where the replayed history is kept between syncs (files on Android, memory in tests). */
interface HistoryCache {
    /** The cached state and the start index to resume from, or null to pull from scratch. */
    fun load(): Pair<HistoryState, Long>?

    fun save(state: HistoryState, nextStartIndex: Long, snapshot: WidgetSnapshot)
}

/** The commit returned but the new to-do did not show up in the history; retrying with the same id is safe. */
class TaskNotConfirmedException : Exception("The to-do was sent but is not confirmed yet.")

/** A previous attempt for this draft was saved with different text; the draft's edits were not sent. */
class EarlierAttemptLandedException(val savedTitle: String) :
    Exception("An earlier attempt was already added with another text.")

/** Sync and Inbox-add use cases, independent of Android. */
class ThingsSync(
    private val client: ThingsCloudClient,
    private val cache: HistoryCache,
    private val now: () -> Double = { System.currentTimeMillis() / 1000.0 },
    private val today: () -> LocalDate = { ThingsDay.today() },
    /** Changes not sent yet: the widget snapshot already shows them. */
    private val pending: () -> List<PendingChange> = { emptyList() },
) {
    class Synced(val library: ThingsLibrary, val headIndex: Long, val state: HistoryState)

    /** [done]: changes Things now shows (by [PendingChange.opId]); [failed]: changes that can't apply, with why. */
    class Flushed(val synced: Synced, val done: List<String>, val failed: List<Pair<PendingChange, Exception>>) {
        val library get() = synced.library
    }

    /**
     * Pulls new history items on top of the cache and saves the result.
     * [beforeSave] runs once the pull is done and may throw to keep the cache untouched (e.g. after sign-out).
     */
    fun sync(historyKey: String, beforeSave: () -> Unit = {}): Synced {
        val (state, startIndex) = cache.load() ?: (HistoryState() to 0L)
        val pulled = client.pullWithHead(historyKey, state, startIndex)
        beforeSave()
        val library = ThingsLibrary.from(state)
        val shown = pending().let { if (it.isEmpty()) library else PendingChanges.overlay(state, it, now()) }
        cache.save(state, pulled.nextStartIndex, WidgetSnapshot.from(shown, today(), (now() * 1000).toLong()))
        return Synced(library, pulled.headIndex, state)
    }

    /**
     * Adds a to-do on top of the Inbox. [id] is chosen once per draft and reused on retry: if an
     * earlier attempt already landed, nothing is sent again, and success is reported only when it
     * holds this text. Succeeds only once a fresh pull shows the new to-do.
     */
    fun addInboxTask(
        historyKey: String,
        id: String,
        title: String,
        notes: String,
        beforeSave: () -> Unit = {},
        schedule: Schedule = Schedule.Inbox,
        reminder: LocalTime? = null,
    ): ThingsLibrary {
        // Pull first so the commit's ancestor index is the server's current head.
        val before = sync(historyKey, beforeSave)
        before.library.task(id)?.let { landed ->
            if (InboxTask.matches(landed, title, notes, schedule, reminder)) return before.library
            throw EarlierAttemptLandedException(landed.title)
        }

        client.createInboxTask(
            historyKey = historyKey,
            ancestorIndex = before.headIndex,
            id = id,
            title = title,
            notes = notes,
            index = before.library.nextTopIndex(schedule.start),
            now = now(),
            schedule = schedule,
            reminder = reminder,
        )

        val after = sync(historyKey, beforeSave)
        if (after.library.task(id) == null) throw TaskNotConfirmedException()
        return after.library
    }

    /** Checks or unchecks a to-do; refused for repeating to-dos the app can't complete safely. */
    fun completeTask(historyKey: String, id: String, done: Boolean, beforeSave: () -> Unit = {}): ThingsLibrary =
        update(historyKey, id, beforeSave) { library, task ->
            require(library.canComplete(task)) { "This to-do can't be checked from the app." }
            // This path writes the to-do alone: "after completion" repeats go through PendingChange.SetDone.
            require(library.afterCompletionRule(task) == null) { "This to-do can't be checked from the app." }
            if ((task.status == TaskStatus.COMPLETED) == done) null else TaskEdits.complete(task, done, now())
        }

    /** Edits title, notes, When and reminder; sends only what changed. */
    fun editTask(
        historyKey: String,
        id: String,
        title: String,
        notes: String,
        schedule: Schedule,
        reminder: LocalTime?,
        beforeSave: () -> Unit = {},
    ): ThingsLibrary = update(historyKey, id, beforeSave) { _, task ->
        TaskEdits.edit(task, title, notes, schedule, reminder, now())
    }

    /**
     * Pull, build the update from the current to-do, commit against the server head, then pull
     * again and require the same update to be a no-op (the change is visible).
     */
    private fun update(
        historyKey: String,
        id: String,
        beforeSave: () -> Unit,
        build: (ThingsLibrary, Task) -> JsonObject?,
    ): ThingsLibrary {
        val before = sync(historyKey, beforeSave)
        val task = before.library.task(id) ?: throw NoSuchElementException("The to-do no longer exists.")
        val change = build(before.library, task) ?: return before.library
        client.commitInboxTask(historyKey, before.headIndex, id, change)

        val after = sync(historyKey, beforeSave)
        val updated = after.library.task(id) ?: throw TaskNotConfirmedException()
        if (runCatching { build(after.library, updated) }.getOrNull() != null) throw TaskNotConfirmedException()
        return after.library
    }

    /**
     * Sends queued changes in order. Each is rebuilt against a fresh pull and committed against
     * the server head; nothing is sent for a change Things already shows. A change counts as done
     * once the pull right after its own commit shows it. Network and server errors stop the flush (the remaining
     * changes stay queued); a change that can no longer apply is reported in [Flushed.failed].
     */
    fun flush(historyKey: String, changes: List<PendingChange>, beforeSave: () -> Unit = {}): Flushed {
        val done = mutableListOf<String>()
        val failed = mutableListOf<Pair<PendingChange, Exception>>()
        var current = sync(historyKey, beforeSave)
        for (change in changes) {
            val payload = try {
                change.build(current.library, now())
            } catch (e: IllegalArgumentException) {
                failed += change to e
                continue
            } catch (e: NoSuchElementException) {
                failed += change to e
                continue
            }
            if (payload == null) {
                done += change.opId
                continue
            }
            client.commit(historyKey, current.headIndex, payload)
            // Also the pull before the next commit, so each one is made against the server head.
            current = sync(historyKey, beforeSave)
            // Confirmed here, before a later change can undo it: otherwise it would look unsent and go out again.
            if (runCatching { change.build(current.library, now()) }.getOrNull() == null) done += change.opId
        }
        return Flushed(current, done, failed)
    }

    /** Whether the to-do [id] exists after a fresh pull. */
    fun landed(historyKey: String, id: String, beforeSave: () -> Unit = {}): Boolean =
        sync(historyKey, beforeSave).library.task(id) != null
}
