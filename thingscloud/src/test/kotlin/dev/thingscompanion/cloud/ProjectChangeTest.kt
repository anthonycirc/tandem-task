package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ProjectChangeTest {

    private val now = 1_794_750_000.5
    private val day15 = 1_794_700_800L

    private val seed = """{
        "proj": {"t": 0, "e": "Task7", "p": {"tt": "Move", "tp": 1, "st": 1, "ss": 0}},
        "head": {"t": 0, "e": "Task6", "p": {"tt": "Boxes", "tp": 2, "pr": ["proj"], "st": 1, "ss": 0}},
        "oldHead": {"t": 0, "e": "Task6", "p": {"tt": "Done part", "tp": 2, "pr": ["proj"], "st": 1, "ss": 3}},
        "a": {"t": 0, "e": "Task7", "p": {"tt": "Pack", "st": 1, "pr": ["proj"], "ss": 0}},
        "b": {"t": 0, "e": "Task6", "p": {"tt": "Tape", "st": 1, "agr": ["head"], "ss": 0}},
        "done": {"t": 0, "e": "Task7", "p": {"tt": "Plan", "st": 1, "pr": ["proj"], "ss": 3, "sp": 5.0}},
        "other": {"t": 0, "e": "Task7", "p": {"tt": "Call", "st": 1, "ss": 0}},
        "repeating": {"t": 0, "e": "Task7", "p": {"tt": "Weekly", "tp": 1, "st": 1, "rr": {"tp": 0}}}
    }"""

    private fun library() = ThingsLibrary.from(HistoryState().apply { apply(WireParser.parseItem(Json.parseToJsonElement(seed) as JsonObject)) })

    private fun props(payload: Map<String, JsonObject>, id: String) = payload.getValue(id)["p"]!!.jsonObject

    @Test
    fun `completing a project completes its open to-dos and headings in the same item, as Things does`() {
        val payload = PendingChange.SetProjectStatus("op", "proj", TaskStatus.COMPLETED).build(library(), now)!!

        assertEquals(setOf("proj", "head", "a", "b"), payload.keys)
        for (id in payload.keys) {
            assertEquals(JsonPrimitive(3), props(payload, id)["ss"], id)
            assertEquals("1794750000.5", props(payload, id)["sp"].toString(), id)
            assertEquals(1, payload.getValue(id)["t"].toString().toInt())
        }
        assertEquals("\"Task6\"", payload.getValue("b")["e"].toString())
    }

    @Test
    fun `cancelling cascades as cancelled, reopening touches the project only`() {
        val lib = library()
        val cancelled = PendingChange.SetProjectStatus("op", "proj", TaskStatus.CANCELLED).build(lib, now)!!
        assertEquals(JsonPrimitive(2), props(cancelled, "a")["ss"])

        val closed = ThingsLibrary.from(HistoryState(lib.let { HistoryState().apply { apply(WireParser.parseItem(Json.parseToJsonElement(seed) as JsonObject)) } }.objects).apply {
            apply(WireParser.parseItem(JsonObject(cancelled)))
        })
        val reopened = PendingChange.SetProjectStatus("op2", "proj", TaskStatus.OPEN).build(closed, now)!!
        assertEquals(setOf("proj"), reopened.keys)
        assertEquals(JsonPrimitive(0), props(reopened, "proj")["ss"])
        assertEquals("null", props(reopened, "proj")["sp"].toString())
    }

    @Test
    fun `nothing is sent when the project already has that status, and repeating projects are refused`() {
        assertNull(PendingChange.SetProjectStatus("op", "proj", TaskStatus.OPEN).build(library(), now))
        assertFailsWith<IllegalArgumentException> { PendingChange.SetProjectStatus("op", "repeating", TaskStatus.COMPLETED).build(library(), now) }
        assertFailsWith<IllegalArgumentException> { PendingChange.SetProjectStatus("op", "a", TaskStatus.COMPLETED).build(library(), now) }
    }

    @Test
    fun `a deadline is set or cleared on a to-do or a project, at the day's midnight`() {
        val lib = library()
        val set = PendingChange.SetDeadline("op", "proj", LocalDate.ofEpochDay(day15 / 86_400).toEpochDay()).build(lib, now)!!
        assertEquals(JsonPrimitive(day15), props(set, "proj")["dd"])
        assertEquals(setOf("dd", "md"), props(set, "proj").keys)

        assertNull(PendingChange.SetDeadline("op", "a", null).build(lib, now))
        val withDeadline = ThingsLibrary.from(HistoryState().apply {
            apply(WireParser.parseItem(Json.parseToJsonElement(seed) as JsonObject))
            apply(WireParser.parseItem(JsonObject(set)))
        })
        assertEquals(LocalDate.ofEpochDay(day15 / 86_400), withDeadline.task("proj")!!.deadline)
        val cleared = PendingChange.SetDeadline("op", "proj", null).build(withDeadline, now)!!
        assertEquals("null", props(cleared, "proj")["dd"].toString())
    }

    @Test
    fun `a project's When is edited like a to-do's`() {
        val payload = PendingChange.EditTask("op", "proj", "Move", "", PendingChange.When.of(Schedule.Someday)).build(library(), now)!!
        assertEquals(JsonPrimitive(2), props(payload, "proj")["st"])
    }
}
