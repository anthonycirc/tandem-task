package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MoveTest {

    private val now = 1_794_750_000.5
    private val day15 = 1_794_700_800L

    private val seed = """{
        "home": {"t": 0, "e": "Area3", "p": {"tt": "Home", "ix": 4}},
        "proj": {"t": 0, "e": "Task7", "p": {"tt": "Move", "tp": 1, "st": 1, "ar": ["home"]}},
        "head": {"t": 0, "e": "Task6", "p": {"tt": "Boxes", "tp": 2, "pr": ["proj"], "st": 1}},
        "inbox": {"t": 0, "e": "Task7", "p": {"tt": "Idea", "st": 0}},
        "dated": {"t": 0, "e": "Task7", "p": {"tt": "Call", "st": 1, "sr": $day15, "tir": $day15, "pr": ["proj"]}},
        "template": {"t": 0, "e": "Task7", "p": {"tt": "Water", "st": 1, "rr": {"tp": 0}}}
    }"""

    private fun library() = ThingsLibrary.from(HistoryState().apply { apply(WireParser.parseItem(Json.parseToJsonElement(seed) as JsonObject)) })

    private fun props(change: PendingChange, id: String) = change.build(library(), now)!!.getValue(id)["p"]!!.jsonObject

    private fun ids(vararg id: String) = JsonArray(id.map(::JsonPrimitive))

    @Test
    fun `moving into a project sets pr, clears ar and agr, and leaves the Inbox for Anytime`() {
        val p = props(PendingChange.MoveTask("op", "inbox", PendingChange.Destination(projectId = "proj")), "inbox")
        assertEquals(ids("proj"), p["pr"])
        assertEquals(ids(), p["ar"])
        assertEquals(ids(), p["agr"])
        assertEquals(JsonPrimitive(1), p["st"])
    }

    @Test
    fun `moving under a heading files by the heading only, keeping the dates`() {
        val p = props(PendingChange.MoveTask("op", "dated", PendingChange.Destination(headingId = "head")), "dated")
        assertEquals(ids("head"), p["agr"])
        assertEquals(ids(), p["pr"])
        assertEquals(setOf("pr", "ar", "agr", "md"), p.keys)
    }

    @Test
    fun `moving to an area, or to the Inbox which also clears the dates`() {
        assertEquals(ids("home"), props(PendingChange.MoveTask("op", "dated", PendingChange.Destination(areaId = "home")), "dated")["ar"])
        val toInbox = props(PendingChange.MoveTask("op", "dated", PendingChange.Destination(inbox = true)), "dated")
        assertEquals(JsonPrimitive(0), toInbox["st"])
        assertEquals(JsonNull, toInbox["sr"])
        assertEquals(JsonNull, toInbox["tir"])
        assertEquals(JsonPrimitive(0), toInbox["sb"])
    }

    @Test
    fun `nothing is sent when already there, and templates and projects are not moved as to-dos`() {
        assertNull(PendingChange.MoveTask("op", "dated", PendingChange.Destination(projectId = "proj")).build(library(), now))
        assertFailsWith<IllegalArgumentException> { PendingChange.MoveTask("op", "template", PendingChange.Destination(areaId = "home")).build(library(), now) }
        assertFailsWith<IllegalArgumentException> { PendingChange.MoveTask("op", "proj", PendingChange.Destination(areaId = "home")).build(library(), now) }
    }

    @Test
    fun `a project moves between areas by its ar only`() {
        val out = props(PendingChange.MoveProject("op", "proj", null), "proj")
        assertEquals(setOf("ar", "md"), out.keys)
        assertEquals(ids(), out["ar"])
        assertNull(PendingChange.MoveProject("op", "proj", "home").build(library(), now))
    }
}
