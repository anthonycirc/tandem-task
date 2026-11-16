package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreateTest {

    private val now = 1_794_750_000.5

    private val seed = """{
        "home": {"t": 0, "e": "Area3", "p": {"tt": "Home", "ix": 4, "tg": []}},
        "work": {"t": 0, "e": "Area3", "p": {"tt": "Work", "ix": 9, "tg": []}},
        "proj": {"t": 0, "e": "Task7", "p": {"tt": "Move", "tp": 1, "st": 1, "ar": ["home"], "ix": 3}},
        "head": {"t": 0, "e": "Task6", "p": {"tt": "Boxes", "tp": 2, "pr": ["proj"], "st": 1, "ix": 7}},
        "a": {"t": 0, "e": "Task7", "p": {"tt": "Pack", "st": 1, "pr": ["proj"], "ix": 12}}
    }"""

    private fun library() = ThingsLibrary.from(HistoryState().apply { apply(WireParser.parseItem(Json.parseToJsonElement(seed) as JsonObject)) })

    private fun p(payload: Map<String, JsonObject>, id: String) = payload.getValue(id)["p"]!!.jsonObject

    private fun ids(vararg id: String) = JsonArray(id.map(::JsonPrimitive))

    @Test
    fun `a new project is a full Task7 of type 1 in its area, at the bottom`() {
        val payload = PendingChange.AddProject("op", "np", "Garden", "", PendingChange.When("anytime"), areaId = "home").build(library(), now)!!
        val props = p(payload, "np")
        assertEquals("\"Task7\"", payload.getValue("np")["e"].toString())
        assertEquals(JsonPrimitive(1), props["tp"])
        assertEquals(ids("home"), props["ar"])
        assertEquals(ids(), props["pr"])
        assertEquals(JsonPrimitive(1), props["st"])
        assertEquals(JsonPrimitive(4), props["ix"])
        assertTrue("xx" in props && "nt" in props && "cd" in props)
        assertNull(PendingChange.AddProject("op", "proj", "Again", "", PendingChange.When("anytime")).build(library(), now))
    }

    @Test
    fun `a new heading is a Task7 of type 2 in its project, after the project's rows`() {
        val payload = PendingChange.AddHeading("op", "nh", "proj", "Unpack").build(library(), now)!!
        val props = p(payload, "nh")
        assertEquals(JsonPrimitive(2), props["tp"])
        assertEquals(ids("proj"), props["pr"])
        assertEquals(ids(), props["ar"])
        assertEquals(JsonPrimitive(1), props["st"])
        assertEquals(JsonPrimitive(13), props["ix"])
        assertFailsWith<IllegalArgumentException> { PendingChange.AddHeading("op", "nh", "a", "Nope").build(library(), now) }
    }

    @Test
    fun `a new area has the keys things3-cloud writes, at the bottom`() {
        val payload = PendingChange.AddArea("op", "na", "Travel").build(library(), now)!!
        assertEquals("\"Area3\"", payload.getValue("na")["e"].toString())
        assertEquals(setOf("tt", "tg", "ix", "xx"), p(payload, "na").keys)
        assertEquals(JsonPrimitive(10), p(payload, "na")["ix"])
        assertEquals(ids(), p(payload, "na")["tg"])
    }

    @Test
    fun `renaming an area writes its title and md, nothing when unchanged`() {
        val payload = PendingChange.RenameArea("op", "home", "House").build(library(), now)!!
        assertEquals(setOf("tt", "md"), p(payload, "home").keys)
        assertEquals(1, payload.getValue("home")["t"].toString().toInt())
        assertEquals("\"Area3\"", payload.getValue("home")["e"].toString())
        assertNull(PendingChange.RenameArea("op", "home", "Home").build(library(), now))
    }

    @Test
    fun `a to-do added in a project, under a heading or in an area is filed there, never in the Inbox`() {
        val lib = library()
        val inProject = p(PendingChange.AddTask("op", "t1", "Paint", "", PendingChange.When("inbox"), projectId = "proj").build(lib, now)!!, "t1")
        assertEquals(ids("proj"), inProject["pr"])
        assertEquals(JsonPrimitive(1), inProject["st"])

        val underHeading = p(PendingChange.AddTask("op", "t2", "Tape", "", PendingChange.When("anytime"), headingId = "head").build(lib, now)!!, "t2")
        assertEquals(ids("head"), underHeading["agr"])
        assertEquals(ids(), underHeading["pr"])

        val inArea = p(PendingChange.AddTask("op", "t3", "Water", "", PendingChange.When("someday"), areaId = "work").build(lib, now)!!, "t3")
        assertEquals(ids("work"), inArea["ar"])
        assertEquals(JsonPrimitive(2), inArea["st"])
    }
}
