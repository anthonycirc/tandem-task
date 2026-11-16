package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PendingChangeTest {

    private val today = LocalDate.of(2026, 11, 15)
    private val day15 = 1_794_700_800L
    private val now = 1_794_750_000.5

    private val seed = """{
        "plain": {"t": 0, "e": "Task7", "p": {"tt": "Call", "st": 1, "sr": $day15, "tir": $day15, "ix": 3}},
        "binned": {"t": 0, "e": "Task6", "p": {"tt": "Old", "st": 1, "tr": true, "md": 5.0}},
        "template": {"t": 0, "e": "Task7", "p": {"tt": "Water", "st": 1, "rr": {"tp": 0}}},
        "c1": {"t": 0, "e": "ChecklistItem3", "p": {"tt": "Eggs", "ts": ["plain"], "ix": 4, "ss": 0}},
        "c2": {"t": 0, "e": "ChecklistItem3", "p": {"tt": "Milk", "ts": ["plain"], "ix": 9, "ss": 3}}
    }"""

    private fun state(json: String = seed) = HistoryState().apply {
        apply(WireParser.parseItem(Json.parseToJsonElement(json) as JsonObject))
    }

    private fun library(json: String = seed) = ThingsLibrary.from(state(json))

    private fun json(s: String) = Json.parseToJsonElement(s)

    private fun obj(s: String) = json(s) as JsonObject

    @Test
    fun `trash and restore send only tr and md on the to-do's own entity`() {
        assertEquals(
            mapOf("plain" to obj("""{"t": 1, "e": "Task7", "p": {"tr": true, "md": 1794750000.5}}""")),
            PendingChange.SetTrashed("op", "plain", true).build(library(), now),
        )
        assertEquals(
            mapOf("binned" to obj("""{"t": 1, "e": "Task6", "p": {"tr": false, "md": 1794750000.5}}""")),
            PendingChange.SetTrashed("op", "binned", false).build(library(), now),
        )
    }

    @Test
    fun `trash is a no-op when already in that state and refused for repeating templates`() {
        assertNull(PendingChange.SetTrashed("op", "binned", true).build(library(), now))
        assertNull(PendingChange.SetTrashed("op", "plain", false).build(library(), now))
        assertFailsWith<IllegalArgumentException> { PendingChange.SetTrashed("op", "template", true).build(library(), now) }
        assertFailsWith<NoSuchElementException> { PendingChange.SetTrashed("op", "gone", true).build(library(), now) }
    }

    @Test
    fun `a new checklist item goes last, shaped like the Things app writes it`() {
        assertEquals(
            mapOf(
                "c3" to obj(
                    """{"t": 0, "e": "ChecklistItem3", "p": {"tt": "Bread", "ts": ["plain"], "ix": 10, "ss": 0, "sp": null,
                       "cd": 1794750000, "md": null, "lt": true, "xx": {"_t": "oo", "sn": {}}}}""",
                ),
            ),
            PendingChange.AddChecklistItem("op", "c3", "plain", "  Bread ").build(library(), now),
        )
        // Already there: nothing to send again.
        assertNull(PendingChange.AddChecklistItem("op", "c1", "plain", "Eggs").build(library(), now))
        assertFailsWith<IllegalArgumentException> { PendingChange.AddChecklistItem("op", "c4", "template", "x").build(library(), now) }
        assertFailsWith<IllegalArgumentException> { PendingChange.AddChecklistItem("op", "c4", "plain", "  ").build(library(), now) }
    }

    @Test
    fun `checklist items are checked, renamed and deleted`() {
        assertEquals(
            mapOf("c1" to obj("""{"t": 1, "e": "ChecklistItem3", "p": {"ss": 3, "sp": 1794750000.5, "md": 1794750000.5}}""")),
            PendingChange.SetChecklistItemDone("op", "c1", true).build(library(), now),
        )
        assertEquals(
            mapOf("c2" to obj("""{"t": 1, "e": "ChecklistItem3", "p": {"ss": 0, "sp": null, "md": 1794750000.5}}""")),
            PendingChange.SetChecklistItemDone("op", "c2", false).build(library(), now),
        )
        assertNull(PendingChange.SetChecklistItemDone("op", "c2", true).build(library(), now))
        assertEquals(
            mapOf("c1" to obj("""{"t": 1, "e": "ChecklistItem3", "p": {"tt": "Free-range eggs", "md": 1794750000.5}}""")),
            PendingChange.RenameChecklistItem("op", "c1", "Free-range eggs").build(library(), now),
        )
        assertNull(PendingChange.RenameChecklistItem("op", "c1", "Eggs").build(library(), now))
        assertEquals(
            mapOf("c1" to obj("""{"t": 2, "e": "ChecklistItem3", "p": {}}""")),
            PendingChange.DeleteChecklistItem("op", "c1").build(library(), now),
        )
        // Deleted already: done.
        assertNull(PendingChange.DeleteChecklistItem("op", "zz").build(library(), now))
        assertFailsWith<NoSuchElementException> { PendingChange.SetChecklistItemDone("op", "zz", true).build(library(), now) }
    }

    @Test
    fun `add, edit and check-off reuse the existing payloads and become no-ops once applied`() {
        val add = PendingChange.AddTask("op", "new", "Buy milk", "", PendingChange.When.of(Schedule.Today(today)), reminderSecond = 9 * 3600)
        val payload = add.build(library(), now)!!
        assertEquals(setOf("new"), payload.keys)
        assertEquals(json("32400"), (payload.getValue("new")["p"] as JsonObject)["ato"])
        assertNull(add.build(PendingChanges.overlay(state(), listOf(add), now), now))

        val edit = PendingChange.EditTask("op", "plain", "Call mum", "", PendingChange.When.of(Schedule.Today(today)), null)
        assertNotNull(edit.build(library(), now))
        assertNull(edit.build(PendingChanges.overlay(state(), listOf(edit), now), now))

        val done = PendingChange.SetDone("op", "plain", true)
        assertNotNull(done.build(library(), now))
        assertNull(done.build(PendingChanges.overlay(state(), listOf(done), now), now))
    }

    @Test
    fun `applying a new change alone gives the same lists as replaying the whole queue`() {
        val first = PendingChange.SetDone("op1", "plain", true)
        val second = PendingChange.SetChecklistItemDone("op2", "c1", true)
        val base = state()

        val stepwise = PendingChanges.applyOne(PendingChanges.overlayState(base, listOf(first), now), second, now)
        val replayed = PendingChanges.overlayState(base, listOf(first, second), now)

        assertEquals(replayed.state.objects, stepwise.state.objects)
        assertEquals(false, stepwise.library.task("plain")!!.isOpen)
        assertEquals(true, stepwise.library.checklistItem("c1")!!.done)
        // The base it started from is untouched.
        assertEquals(true, ThingsLibrary.from(base).task("plain")!!.isOpen)
    }

    @Test
    fun `a change may be applied alone only when the queue just grew by it`() {
        val restore = PendingChange.SetTrashed("op1", "binned", false)
        val check = PendingChange.SetDone("op2", "binned", true)
        val trash = PendingChange.SetTrashed("op3", "binned", true)

        assertTrue(PendingChanges.canApplyAlone(listOf(restore), listOf(restore, check), check))
        // Trash replaces the queued restore: the check was judged against a restored to-do, so replay.
        val queue = PendingChanges.enqueue(listOf(restore, check), trash)
        assertEquals(listOf(check, trash), queue)
        assertFalse(PendingChanges.canApplyAlone(listOf(restore, check), queue, trash))
        assertFalse(PendingChanges.canApplyAlone(listOf(restore), listOf(restore), null))
    }

    /** Taps as the app makes them: the queue rule, then one change alone when allowed, else a replay. */
    private fun tapByTap(changes: List<PendingChange>): Pair<List<PendingChange>, PendingChanges.Shown> {
        var queue = emptyList<PendingChange>()
        var shown = PendingChanges.overlayState(state(), queue, now)
        for (change in changes) {
            val next = PendingChanges.enqueue(queue, change)
            shown = if (PendingChanges.canApplyAlone(queue, next, change)) PendingChanges.applyOne(shown, change, now)
            else PendingChanges.overlayState(state(), next, now)
            queue = next
        }
        return queue to shown
    }

    private fun visible(library: ThingsLibrary, id: String) =
        library.task(id)?.let { Triple(it.status, it.trashed, library.checklist(id).map { item -> item.done }) }

    @Test
    fun `tap by tap, the lists match a replay of the queue, undo and redo included`() {
        val sequences = listOf(
            listOf(PendingChange.SetTrashed("1", "binned", false), PendingChange.SetDone("2", "binned", true), PendingChange.SetTrashed("3", "binned", true)),
            listOf(
                PendingChange.SetTrashed("1", "binned", false), PendingChange.SetDone("2", "binned", true),
                PendingChange.SetTrashed("3", "binned", true), PendingChange.SetTrashed("4", "binned", false),
            ),
            listOf(
                PendingChange.SetDone("1", "plain", true), PendingChange.SetTrashed("2", "plain", true),
                PendingChange.SetDone("3", "plain", false), PendingChange.SetTrashed("4", "plain", false),
            ),
            listOf(PendingChange.SetTrashed("1", "plain", true), PendingChange.SetChecklistItemDone("2", "c1", true), PendingChange.SetTrashed("3", "plain", false)),
        )
        for (changes in sequences) {
            val (queue, stepwise) = tapByTap(changes)
            val replayed = PendingChanges.overlayState(state(), queue, now)
            for (id in listOf("plain", "binned")) {
                assertEquals(visible(replayed.library, id), visible(stepwise.library, id), "$id after ${changes.map { it.opId }}")
            }
            assertEquals(replayed.library.today(today).map { it.id }, stepwise.library.today(today).map { it.id })
        }
    }

    @Test
    fun `the overlay shows pending changes at once and skips impossible ones`() {
        val changes = listOf(
            PendingChange.AddTask("o1", "new", "Buy milk", "", PendingChange.When.of(Schedule.Inbox), null),
            PendingChange.SetChecklistItemDone("o4", "c1", true),
            PendingChange.SetTrashed("o2", "plain", true),
            PendingChange.SetTrashed("o3", "template", true),
            // The checklist of a to-do in the Trash can't be changed.
            PendingChange.SetChecklistItemDone("o5", "c2", false),
        )
        val view = PendingChanges.overlay(state(), changes, now)

        assertEquals(listOf("new"), view.inbox().map { it.id })
        assertTrue(view.today(today).none { it.id == "plain" })
        assertEquals(listOf("plain", "binned"), view.trash().map { it.id })
        assertTrue(view.checklist("plain").first { it.id == "c1" }.done)
        assertTrue(view.checklist("plain").first { it.id == "c2" }.done)
        // The cached state itself is untouched.
        assertEquals(listOf("plain"), library().today(today).map { it.id })
    }

    @Test
    fun `the trash lists trashed to-dos and projects, most recently changed first`() {
        val lib = library(
            """{"a": {"t": 0, "e": "Task7", "p": {"tt": "A", "tr": true, "md": 10.0}},
                "b": {"t": 0, "e": "Task7", "p": {"tt": "B", "tr": true, "md": 20.0, "ss": 3}},
                "p": {"t": 0, "e": "Task7", "p": {"tt": "P", "tp": 1, "tr": true, "md": 30.0}},
                "old": {"t": 0, "e": "Task4", "p": {"tt": "Legacy", "tr": true}},
                "live": {"t": 0, "e": "Task7", "p": {"tt": "Live"}}}""",
        )
        // Projects can be trashed from the app, so they are listed to be restored.
        assertEquals(listOf("p", "b", "a"), lib.trash().map { it.id })
        assertTrue(lib.canRestore(lib.task("a")!!))
        assertTrue(lib.canTrash(lib.task("live")!!))
        assertTrue(!lib.canTrash(lib.task("a")!!))
    }

    @Test
    fun `pending changes survive a JSON round trip`() {
        val changes = listOf(
            PendingChange.AddTask("o1", "new", "Buy", "n", PendingChange.When.of(Schedule.OnDate(today)), 3600),
            PendingChange.EditTask("o2", "plain", "Call", "", PendingChange.When.of(Schedule.Evening(today)), null),
            PendingChange.SetDone("o3", "plain", true),
            PendingChange.SetTrashed("o4", "plain", false),
            PendingChange.AddChecklistItem("o5", "c3", "plain", "Bread"),
            PendingChange.RenameChecklistItem("o6", "c1", "Eggs"),
            PendingChange.SetChecklistItemDone("o7", "c1", true),
            PendingChange.DeleteChecklistItem("o8", "c1"),
        )
        assertEquals(changes, PendingChanges.fromJson(PendingChanges.toJson(changes)))
        assertEquals(Schedule.OnDate(today), (changes[0] as PendingChange.AddTask).schedule.toSchedule())
        assertEquals(LocalTime.of(1, 0), (changes[0] as PendingChange.AddTask).reminder)
    }

    // --- Flush against a fake server ---

    private class MemoryCache(var saved: Pair<HistoryState, Long>? = null) : HistoryCache {
        override fun load() = saved?.let { HistoryState.fromJson(it.first.toJson()) to it.second }
        override fun save(state: HistoryState, nextStartIndex: Long, snapshot: WidgetSnapshot) {
            saved = HistoryState.fromJson(state.toJson()) to nextStartIndex
        }
    }

    private lateinit var server: MockWebServer
    private lateinit var sync: ThingsSync

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.start()
        val client = ThingsCloudClient(baseUrl = server.url("/version/1").toString(), appInstanceId = "i")
        sync = ThingsSync(client, MemoryCache(state() to 1L), now = { now }, today = { today })
    }

    @AfterTest
    fun tearDown() = server.shutdown()

    private fun page(items: String = "", head: Long) = MockResponse().setBody(
        """{"items": [$items], "current-item-index": $head, "end-total-content-size": 1, "latest-total-content-size": 1}""",
    )

    @Test
    fun `flush pulls, commits each change against the head, then confirms`() {
        val trash = PendingChange.SetTrashed("o1", "plain", true)
        server.enqueue(page(head = 40))
        server.enqueue(MockResponse().setBody("""{"server-head-index": 41}"""))
        server.enqueue(page("""{"plain": {"t": 1, "e": "Task7", "p": {"tr": true}}}""", head = 41))

        val result = sync.flush("hk", listOf(trash))

        assertEquals(listOf("o1"), result.done)
        assertTrue(result.failed.isEmpty())
        assertTrue(result.library.task("plain")!!.trashed)
        server.takeRequest()
        val commit = server.takeRequest()
        assertEquals("/version/1/history/hk/commit?ancestor-index=40&_cnt=1", commit.path)
        assertEquals(json("""{"plain": {"t": 1, "e": "Task7", "p": {"tr": true, "md": 1794750000.5}}}"""), json(commit.body.readUtf8()))
    }

    @Test
    fun `flush sends nothing for a change already applied, and drops impossible ones`() {
        server.enqueue(page(head = 40))
        server.enqueue(page(head = 40))

        val result = sync.flush(
            "hk",
            listOf(PendingChange.SetTrashed("o1", "binned", true), PendingChange.SetTrashed("o2", "template", true)),
        )

        assertEquals(listOf("o1"), result.done)
        assertEquals(listOf("o2"), result.failed.map { it.first.opId })
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `a change the confirmation pull does not show yet stays pending`() {
        server.enqueue(page(head = 40))
        server.enqueue(MockResponse().setBody("""{"server-head-index": 41}"""))
        server.enqueue(page(head = 41))

        val result = sync.flush("hk", listOf(PendingChange.SetTrashed("o1", "plain", true)))

        assertTrue(result.done.isEmpty())
        assertTrue(result.failed.isEmpty())
    }

    @Test
    fun `a network failure stops the flush and keeps the change`() {
        server.enqueue(page(head = 40))
        server.enqueue(MockResponse().setResponseCode(503))

        assertFailsWith<ThingsCloudException> { sync.flush("hk", listOf(PendingChange.SetTrashed("o1", "plain", true))) }
    }

    @Test
    fun `trash then undo in one flush, each change is confirmed by the pull after its own commit`() {
        server.enqueue(page(head = 40))
        server.enqueue(MockResponse().setBody("""{"server-head-index": 41}"""))
        server.enqueue(page("""{"plain": {"t": 1, "e": "Task7", "p": {"tr": true}}}""", head = 41))
        server.enqueue(MockResponse().setBody("""{"server-head-index": 42}"""))
        server.enqueue(page("""{"plain": {"t": 1, "e": "Task7", "p": {"tr": false}}}""", head = 42))

        val result = sync.flush("hk", listOf(PendingChange.SetTrashed("o1", "plain", true), PendingChange.SetTrashed("o2", "plain", false)))

        assertEquals(listOf("o1", "o2"), result.done)
        assertTrue(!result.library.task("plain")!!.trashed)
    }

    @Test
    fun `a newer change to the same thing replaces the queued one`() {
        val queue = listOf(
            PendingChange.SetTrashed("o1", "plain", true),
            PendingChange.SetDone("o2", "plain", true),
            PendingChange.EditTask("o3", "plain", "A", "", PendingChange.When.of(Schedule.Anytime), null),
            PendingChange.RenameChecklistItem("o4", "c1", "X"),
        )
        assertEquals(
            listOf("o2", "o4", "o5", "o6"),
            PendingChanges.enqueue(
                PendingChanges.enqueue(queue, PendingChange.SetTrashed("o5", "plain", false)),
                PendingChange.EditTask("o6", "plain", "B", "", PendingChange.When.of(Schedule.Anytime), null),
            ).map { it.opId },
        )
        // Different targets or kinds are kept.
        assertEquals(5, PendingChanges.enqueue(queue, PendingChange.SetTrashed("o5", "other", false)).size)
    }
}
