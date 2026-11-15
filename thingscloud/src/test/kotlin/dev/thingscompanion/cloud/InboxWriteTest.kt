package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.time.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class InboxWriteTest {

    private lateinit var server: MockWebServer
    private lateinit var client: ThingsCloudClient

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = ThingsCloudClient(baseUrl = server.url("/version/1").toString(), appInstanceId = "install-1")
    }

    @AfterTest
    fun tearDown() = server.shutdown()

    @Test
    fun `ids are 22 character base58 and unique`() {
        val ids = List(200) { ThingsIds.random() }

        assertTrue(ids.all { it.length in 21..22 && it.all { c -> c in ThingsIds.ALPHABET } }, ids.toString())
        assertEquals(ids.size, ids.toSet().size)
        // Standard Bitcoin-alphabet vectors; leading zero bytes become '1'.
        assertEquals("StV1DL6CwTryKyV", ThingsIds.encode("hello world".toByteArray()))
        assertEquals("1".repeat(16), ThingsIds.encode(ByteArray(16)))
    }

    @Test
    fun `inbox task payload matches what the Things app writes`() {
        val payload = InboxTask.payload(title = "Buy milk", notes = "2 litres", index = -4, now = 1_794_700_000.25)

        val expected = Json.parseToJsonElement(
            """{"t": 0, "e": "Task7", "p": {
                "acrd": null, "agr": [], "ar": [], "ato": null, "cd": 1794700000.25, "dd": null, "dds": null,
                "dl": [], "do": 0, "icc": 0, "icp": false, "icsd": null, "ix": -4, "lai": null, "lt": false,
                "md": 1794700000.25, "nt": {"_t": "tx", "t": 1, "ch": ${java.util.zip.CRC32().apply { update("2 litres".toByteArray()) }.value}, "v": "2 litres"},
                "pr": [], "rmd": null, "rp": null, "rr": null, "rt": [], "sb": 0, "sp": null, "sr": null,
                "ss": 0, "st": 0, "tg": [], "ti": 0, "tir": null, "tp": 0, "tr": false, "tt": "Buy milk",
                "xx": {"_t": "oo", "sn": {}}}}"""
        )
        assertEquals(expected, payload)
    }

    @Test
    fun `empty notes use a zero checksum`() {
        val payload = InboxTask.payload(title = "Call", notes = "", index = 0, now = 1.0) as JsonObject
        val notes = (payload["p"] as JsonObject)["nt"]

        assertEquals(Json.parseToJsonElement("""{"_t": "tx", "t": 1, "ch": 0, "v": ""}"""), notes)
    }

    @Test
    fun `blank titles are refused`() {
        assertFailsWith<IllegalArgumentException> { InboxTask.payload(title = "  ", notes = "", index = 0, now = 1.0) }
    }

    @Test
    fun `the new task lands on top of the inbox`() {
        val state = HistoryState()
        state.apply(WireParser.parseItem(Json.parseToJsonElement(
            """{"a": {"t": 0, "e": "Task7", "p": {"tt": "A", "st": 0, "ix": -3}},
                "b": {"t": 0, "e": "Task6", "p": {"tt": "B", "st": 0, "ix": 5}},
                "c": {"t": 0, "e": "Task7", "p": {"tt": "Done", "st": 0, "ix": -50, "ss": 3}}}"""
        ) as JsonObject))

        assertEquals(-4, ThingsLibrary.from(state).nextInboxTopIndex())
        assertEquals(0, ThingsLibrary.from(HistoryState()).nextInboxTopIndex())
    }

    @Test
    fun `commit posts the changes against the ancestor index`() {
        server.enqueue(MockResponse().setBody("""{"server-head-index": 101}"""))
        val payload = InboxTask.payload(title = "Buy milk", notes = "", index = 0, now = 1.0)

        val head = client.commitInboxTask("hk", ancestorIndex = 100, id = "NEWID", payload = payload)

        assertEquals(101, head)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/version/1/history/hk/commit?ancestor-index=100&_cnt=1", request.path)
        assertEquals("10", request.getHeader("Push-Priority"))
        assertEquals("install-1", request.getHeader("App-Instance-Id"))
        assertEquals(Json.parseToJsonElement("""{"NEWID": $payload}"""), Json.parseToJsonElement(request.body.readUtf8()))
    }

    @Test
    fun `commit errors never expose the history key`() {
        server.enqueue(MockResponse().setResponseCode(409).setBody("conflict"))
        val payload = InboxTask.payload(title = "x", notes = "", index = 0, now = 1.0)

        val error = assertFailsWith<ThingsCloudException> {
            client.commitInboxTask("secret-key-42", ancestorIndex = 1, id = "ID", payload = payload)
        }

        assertEquals(409, error.status)
        assertFalse(error.stackTraceToString().contains("secret-key-42"))
    }

    @Test
    fun `pull reports the server head index`() {
        server.enqueue(
            MockResponse().setBody(
                """{"items": [{"a": {"t": 0, "e": "Task7", "p": {"tt": "A"}}}],
                   "current-item-index": 42, "end-total-content-size": 1, "latest-total-content-size": 1}"""
            )
        )

        val result = client.pullWithHead("hk", HistoryState(), startIndex = 7)

        assertEquals(8, result.nextStartIndex)
        assertEquals(42, result.headIndex)
        assertNotEquals(result.nextStartIndex, result.headIndex)
    }

    @Test
    fun `without a head index from the server, the head follows the items pulled`() {
        server.enqueue(
            MockResponse().setBody(
                """{"items": [{"a": {"t": 0, "e": "Task7", "p": {"tt": "A"}}}],
                   "end-total-content-size": 1, "latest-total-content-size": 1}"""
            )
        )

        val result = client.pullWithHead("hk", HistoryState(), startIndex = 7)

        assertEquals(8, result.headIndex)
    }

    @Test
    fun `a stored to-do matches a draft only when title and notes are the same`() {
        val state = HistoryState()
        state.apply(WireParser.parseItem(Json.parseToJsonElement(
            """{"id1": ${InboxTask.payload(title = "Buy milk", notes = "2 litres", index = 0, now = 1.0)},
                "id2": ${InboxTask.payload(title = "Call", notes = "", index = 0, now = 1.0)}}"""
        ) as JsonObject))
        val library = ThingsLibrary.from(state)

        assertTrue(InboxTask.matches(library.task("id1")!!, title = " Buy milk ", notes = "2 litres\n"))
        assertFalse(InboxTask.matches(library.task("id1")!!, title = "Buy oat milk", notes = "2 litres"))
        assertFalse(InboxTask.matches(library.task("id1")!!, title = "Buy milk", notes = "1 litre"))
        assertTrue(InboxTask.matches(library.task("id2")!!, title = "Call", notes = "  "))
    }

    private fun props(schedule: Schedule) =
        (InboxTask.payload(title = "T", notes = "", index = 0, now = 1.0, schedule = schedule)["p"] as JsonObject)

    @Test
    fun `today, this evening and a later date are encoded like the Things app`() {
        val today = LocalDate.of(2026, 11, 15)
        val day15 = 1_794_700_800L
        val day20 = day15 + 5 * 86_400

        props(Schedule.Today(today)).let {
            assertEquals("1", it["st"].toString()); assertEquals("$day15", it["sr"].toString())
            assertEquals("$day15", it["tir"].toString()); assertEquals("0", it["sb"].toString())
        }
        props(Schedule.Evening(today)).let {
            assertEquals("1", it["st"].toString()); assertEquals("$day15", it["sr"].toString()); assertEquals("1", it["sb"].toString())
        }
        props(Schedule.OnDate(today.plusDays(5))).let {
            assertEquals("2", it["st"].toString()); assertEquals("$day20", it["sr"].toString())
            assertEquals("$day20", it["tir"].toString()); assertEquals("0", it["sb"].toString())
        }
        props(Schedule.Inbox).let {
            assertEquals("0", it["st"].toString()); assertEquals("null", it["sr"].toString()); assertEquals("null", it["tir"].toString())
        }
    }

    @Test
    fun `scheduled to-dos show up in the matching lists`() {
        val today = LocalDate.of(2026, 11, 15)
        val state = HistoryState()
        state.apply(WireParser.parseItem(Json.parseToJsonElement(
            """{"t": ${InboxTask.payload("Now", "", 0, 1.0, Schedule.Today(today))},
                "e": ${InboxTask.payload("Tonight", "", 0, 1.0, Schedule.Evening(today))},
                "d": ${InboxTask.payload("Later", "", 0, 1.0, Schedule.OnDate(today.plusDays(3)))}}"""
        ) as JsonObject))
        val library = ThingsLibrary.from(state)

        assertEquals(listOf("t", "e"), library.today(today).map { it.id })
        assertEquals(listOf("d"), library.upcoming(today).map { it.id })
        assertTrue(library.task("e")!!.evening)
        assertTrue(library.inbox().isEmpty())
    }

    @Test
    fun `a draft matches only with the same schedule`() {
        val today = LocalDate.of(2026, 11, 15)
        val state = HistoryState()
        state.apply(WireParser.parseItem(Json.parseToJsonElement(
            """{"x": ${InboxTask.payload("Call", "", 0, 1.0, Schedule.Today(today))}}"""
        ) as JsonObject))
        val task = ThingsLibrary.from(state).task("x")!!

        assertTrue(InboxTask.matches(task, "Call", "", Schedule.Today(today)))
        assertFalse(InboxTask.matches(task, "Call", "", Schedule.Evening(today)))
        assertFalse(InboxTask.matches(task, "Call", "", Schedule.Inbox))
    }
}
