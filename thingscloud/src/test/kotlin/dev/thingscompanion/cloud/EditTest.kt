package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditTest {

    private val today = LocalDate.of(2026, 11, 15)
    private val day15 = 1_794_700_800L
    private val now = 1_794_750_000.5

    private fun library(json: String): ThingsLibrary {
        val state = HistoryState()
        state.apply(WireParser.parseItem(Json.parseToJsonElement(json) as JsonObject))
        return ThingsLibrary.from(state)
    }

    private fun json(s: String) = Json.parseToJsonElement(s)

    private val lib by lazy {
        library(
            """{"plain": {"t": 0, "e": "Task7", "p": {"tt": "Call", "st": 1, "sr": $day15, "tir": $day15, "ato": 32400,
                          "nt": {"_t": "tx", "t": 1, "ch": 0, "v": ""}}},
                "inbox": {"t": 0, "e": "Task6", "p": {"tt": "Loose", "st": 0}},
                "fixedT": {"t": 0, "e": "Task7", "p": {"tt": "Water", "st": 1, "rr": {"tp": 0}}},
                "fixedI": {"t": 0, "e": "Task7", "p": {"tt": "Water", "st": 1, "sr": $day15, "rt": ["fixedT"]}},
                "afterT": {"t": 0, "e": "Task7", "p": {"tt": "Haircut", "st": 1, "rr": {"tp": 1}}},
                "afterI": {"t": 0, "e": "Task7", "p": {"tt": "Haircut", "st": 1, "sr": $day15, "rt": ["afterT"]}}}"""
        )
    }

    @Test
    fun `completing writes status, stop date and modification date on the to-do's own entity`() {
        assertEquals(
            json("""{"t": 1, "e": "Task6", "p": {"ss": 3, "sp": 1794750000.5, "md": 1794750000.5}}"""),
            TaskEdits.complete(lib.task("inbox")!!, done = true, now = now),
        )
        assertEquals(
            json("""{"t": 1, "e": "Task7", "p": {"ss": 0, "sp": null, "md": 1794750000.5}}"""),
            TaskEdits.complete(lib.task("plain")!!, done = false, now = now),
        )
    }

    @Test
    fun `an edit sends only what changed`() {
        val task = lib.task("plain")!!

        assertEquals(
            json("""{"t": 1, "e": "Task7", "p": {"tt": "Call mum", "md": 1794750000.5}}"""),
            TaskEdits.edit(task, "Call mum", "", Schedule.Today(today), LocalTime.of(9, 0), now) as JsonElement?,
        )
        assertNull(TaskEdits.edit(task, "Call", "", Schedule.Today(today), LocalTime.of(9, 0), now))
    }

    @Test
    fun `moving to the inbox clears the date, evening and reminder`() {
        val patch = TaskEdits.edit(lib.task("plain")!!, "Call", "", Schedule.Inbox, null, now)!!["p"] as JsonObject

        assertEquals(json("""{"st": 0, "sr": null, "tir": null, "ato": null, "md": 1794750000.5}"""), patch)
    }

    @Test
    fun `notes and evening edits are encoded like a create`() {
        val patch = TaskEdits.edit(lib.task("inbox")!!, "Loose", "Bring ID", Schedule.Evening(today), LocalTime.of(19, 0), now)!!["p"] as JsonObject

        assertEquals(json("1"), patch["st"])
        assertEquals(json("$day15"), patch["sr"])
        assertEquals(json("1"), patch["sb"])
        assertEquals(json("68400"), patch["ato"])
        assertEquals(json("""{"_t": "tx", "t": 1, "ch": ${java.util.zip.CRC32().apply { update("Bring ID".toByteArray()) }.value}, "v": "Bring ID"}"""), patch["nt"])
    }

    @Test
    fun `existing anytime and someday to-dos keep their list`() {
        val undated = library("""{"a": {"t": 0, "e": "Task7", "p": {"tt": "A", "st": 1}}, "s": {"t": 0, "e": "Task7", "p": {"tt": "S", "st": 2}}}""")

        assertEquals(Schedule.Anytime, Schedule.of(undated.task("a")!!, today))
        assertEquals(Schedule.Someday, Schedule.of(undated.task("s")!!, today))
        assertEquals(Schedule.Today(today), Schedule.of(lib.task("plain")!!, today))
        assertEquals(Schedule.Inbox, Schedule.of(lib.task("inbox")!!, today))
    }

    @Test
    fun `repeating to-dos follow the safe rules`() {
        assertTrue(lib.canComplete(lib.task("plain")!!))
        assertTrue(lib.canComplete(lib.task("fixedI")!!))
        assertFalse(lib.canComplete(lib.task("afterI")!!))
        assertFalse(lib.canComplete(lib.task("fixedT")!!))
        assertTrue(lib.canReschedule(lib.task("plain")!!))
        // An occurrence can be rescheduled (S7, 2026-11-17), but not into the Inbox.
        assertTrue(lib.canReschedule(lib.task("fixedI")!!))
        assertFalse(lib.canReschedule(lib.task("fixedT")!!))
        assertFailsWith<IllegalArgumentException> {
            TaskEdits.edit(lib.task("fixedI")!!, "Water", "", Schedule.Inbox, null, now)
        }
    }

    // --- use case ---

    private lateinit var server: MockWebServer

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterTest
    fun tearDown() = server.shutdown()

    private fun page(items: String = "", head: Long) = MockResponse().setBody(
        """{"items": [$items], "current-item-index": $head, "end-total-content-size": 1, "latest-total-content-size": 1}"""
    )

    private fun sync(): ThingsSync {
        val cache = object : HistoryCache {
            var saved: Pair<HistoryState, Long>? = null
            override fun load() = saved?.let { HistoryState.fromJson(it.first.toJson()) to it.second }
            override fun save(state: HistoryState, nextStartIndex: Long, snapshot: WidgetSnapshot) {
                saved = HistoryState.fromJson(state.toJson()) to nextStartIndex
            }
        }
        val client = ThingsCloudClient(baseUrl = server.url("/version/1").toString(), appInstanceId = "i")
        return ThingsSync(client, cache, now = { now }, today = { today })
    }

    @Test
    fun `complete pulls, commits against the head, and confirms the new status`() {
        server.enqueue(page("""{"x": {"t": 0, "e": "Task7", "p": {"tt": "Call", "st": 0}}}""", head = 50))
        server.enqueue(MockResponse().setBody("""{"server-head-index": 51}"""))
        server.enqueue(page("""{"x": {"t": 1, "e": "Task7", "p": {"ss": 3, "sp": 1}}}""", head = 51))

        val library = sync().completeTask("hk", "x", done = true)

        assertEquals(TaskStatus.COMPLETED, library.task("x")!!.status)
        server.takeRequest()
        assertEquals("/version/1/history/hk/commit?ancestor-index=50&_cnt=1", server.takeRequest().path)
    }

    @Test
    fun `an update that does not show up is reported as not confirmed`() {
        server.enqueue(page("""{"x": {"t": 0, "e": "Task7", "p": {"tt": "Call", "st": 0}}}""", head = 50))
        server.enqueue(MockResponse().setBody("""{"server-head-index": 51}"""))
        server.enqueue(page(head = 51))

        assertFailsWith<TaskNotConfirmedException> { sync().editTask("hk", "x", "Call mum", "", Schedule.Inbox, null) }
    }

    @Test
    fun `nothing is sent for an edit without changes or a refused completion`() {
        server.enqueue(page("""{"x": {"t": 0, "e": "Task7", "p": {"tt": "Call", "st": 0}}}""", head = 50))
        sync().editTask("hk", "x", "Call", "", Schedule.Inbox, null)
        assertEquals(1, server.requestCount)

        // A daily "after completion" repeat: only the queued SetDone also moves its template on.
        server.enqueue(page("""{"t": {"t": 0, "e": "Task7", "p": {"tt": "H", "st": 1, "rr": {"tp": 1, "fu": 16, "fa": 1}}},
                             "i": {"t": 0, "e": "Task7", "p": {"tt": "H", "st": 1, "sr": $day15, "rt": ["t"]}}}""", head = 52))
        assertFailsWith<IllegalArgumentException> { sync().completeTask("hk", "i", done = true) }
        assertEquals(2, server.requestCount)
    }
}
