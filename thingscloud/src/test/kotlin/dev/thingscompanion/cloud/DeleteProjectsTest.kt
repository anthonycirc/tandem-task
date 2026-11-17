package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Emptying the Trash of projects and headings, and deleting an area, as Things 3 writes them
 * (history samples: a trashed project goes with its headings, to-dos and checklists, projects and
 * headings without a tombstone; an area's contents lose their area, {ar: []}).
 */
class DeleteProjectsTest {

    private val now = 1_794_902_503.25

    private val seed = """{
        "area": {"t": 0, "e": "Area3", "p": {"tt": "Home", "ix": 0}},
        "proj": {"t": 0, "e": "Task7", "p": {"tt": "Move", "tp": 1, "st": 1, "tr": true, "ar": ["area"]}},
        "head": {"t": 0, "e": "Task7", "p": {"tt": "Phase", "tp": 2, "st": 1, "pr": ["proj"]}},
        "a": {"t": 0, "e": "Task6", "p": {"tt": "Pack", "st": 1, "pr": ["proj"]}},
        "b": {"t": 0, "e": "Task7", "p": {"tt": "Label", "st": 1, "agr": ["head"], "ss": 3, "sp": 1}},
        "cb": {"t": 0, "e": "ChecklistItem3", "p": {"tt": "Tape", "ts": ["b"], "ss": 0}},
        "lone": {"t": 0, "e": "Task7", "p": {"tt": "Old phase", "tp": 2, "st": 1, "pr": ["keep"], "tr": true}},
        "keep": {"t": 0, "e": "Task7", "p": {"tt": "Keep", "tp": 1, "st": 1}},
        "underLone": {"t": 0, "e": "Task7", "p": {"tt": "Gone too", "st": 1, "agr": ["lone"]}},
        "kept": {"t": 0, "e": "Task7", "p": {"tt": "Stays", "st": 1, "pr": ["keep"]}},
        "inArea": {"t": 0, "e": "Task6", "p": {"tt": "Call", "st": 1, "ar": ["area"]}},
        "areaProj": {"t": 0, "e": "Task7", "p": {"tt": "Garden", "tp": 1, "st": 1, "ar": ["area"]}}
    }"""

    private val library = ThingsLibrary.from(HistoryState().apply {
        apply(WireParser.parseItem(Json.parseToJsonElement(seed) as JsonObject))
    })

    private val deleted = Json.parseToJsonElement("""{"t": 2, "e": "Task7", "p": {}}""")

    private fun stones(payload: Map<String, JsonObject>) = payload.values
        .filter { it["e"] == JsonPrimitive("Tombstone2") }
        .map { it["p"]!!.jsonObject["dloid"].toString().trim('"') }.toSet()

    @Test
    fun `a trashed project goes with its headings, to-dos and checklists`() {
        val payload = PendingChange.DeleteForever("op", listOf("proj")).build(library, now)!!
        listOf("proj", "head", "a", "b").forEach { assertEquals(deleted, payload.getValue(it), it) }
        assertEquals(Json.parseToJsonElement("""{"t": 2, "e": "ChecklistItem3", "p": {}}"""), payload.getValue("cb"))
        // Tombstones for its to-dos and checklist items, none for the project or its heading.
        assertEquals(setOf("a", "b", "cb"), stones(payload))
        assertTrue("kept" !in payload && "keep" !in payload)
    }

    @Test
    fun `a trashed heading goes with its to-dos, its project stays`() {
        val payload = PendingChange.DeleteForever("op", listOf("lone")).build(library, now)!!
        assertEquals(deleted, payload.getValue("lone"))
        assertEquals(deleted, payload.getValue("underLone"))
        assertTrue("keep" !in payload && "kept" !in payload)
        assertEquals(setOf("underLone"), stones(payload))
    }

    @Test
    fun `trashed projects and headings can be deleted for good, open ones cannot`() {
        assertTrue(library.canDeleteForever(library.task("proj")!!))
        assertTrue(library.canDeleteForever(library.task("lone")!!))
        assertEquals(false, library.canDeleteForever(library.task("keep")!!))
    }

    @Test
    fun `deleting an area moves its contents to the Trash and out of the area`() {
        val payload = PendingChange.DeleteArea("op", "area").build(library, now)!!
        for (id in listOf("inArea", "areaProj")) {
            val p = payload.getValue(id)["p"]!!.jsonObject
            assertEquals("true", p["tr"].toString(), id)
            assertEquals("[]", p["ar"].toString(), id)
        }
        assertEquals(Json.parseToJsonElement("""{"t": 2, "e": "Area3", "p": {}}"""), payload.getValue("area"))
    }
}
