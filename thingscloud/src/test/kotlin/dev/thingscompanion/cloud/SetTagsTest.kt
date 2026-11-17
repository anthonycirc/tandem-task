package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Tagging a to-do or a project, written as Things 3 writes it: {tg, md} on the item, in its entity. */
class SetTagsTest {

    private val now = 1_794_902_503.25

    private val seed = """{
        "work": {"t": 0, "e": "Tag4", "p": {"tt": "Work", "ix": 2}},
        "home": {"t": 0, "e": "Tag4", "p": {"tt": "Home", "ix": 1}},
        "old": {"t": 0, "e": "Tag3", "p": {"tt": "Old", "ix": 0}},
        "todo": {"t": 0, "e": "Task6", "p": {"tt": "Call", "st": 1, "tg": ["work"]}},
        "proj": {"t": 0, "e": "Task7", "p": {"tt": "Move", "tp": 1, "st": 1}},
        "head": {"t": 0, "e": "Task7", "p": {"tt": "Phase", "tp": 2, "st": 1, "pr": ["proj"]}},
        "trashed": {"t": 0, "e": "Task7", "p": {"tt": "Gone", "st": 1, "tr": true}},
        "tmpl": {"t": 0, "e": "Task7", "p": {"tt": "Water", "st": 2, "rr": {"tp": 1, "fu": 16, "fa": 1}}}
    }"""

    private val library = ThingsLibrary.from(HistoryState().apply {
        apply(WireParser.parseItem(Json.parseToJsonElement(seed) as JsonObject))
    })

    private fun tag(id: String, vararg tags: String) = PendingChange.SetTags("op", id, tags.toList()).build(library, now)

    @Test
    fun `a to-do's tags are written in Things' tag order, with md, in its own entity`() {
        val payload = tag("todo", "work", "home")!!
        assertEquals(
            Json.parseToJsonElement("""{"t": 1, "e": "Task6", "p": {"tg": ["home", "work"], "md": 1794902503.25}}"""),
            payload.getValue("todo"),
        )
        assertEquals(1, payload.size)
    }

    @Test
    fun `a project is tagged the same way, and tags can all be taken off`() {
        assertEquals("""["home"]""", tag("proj", "home")!!.getValue("proj").getValue("p").jsonObject["tg"].toString())
        assertEquals("[]", tag("todo")!!.getValue("todo").getValue("p").jsonObject["tg"].toString())
    }

    @Test
    fun `unchanged tags write nothing, and unknown tags cannot be added`() {
        assertNull(tag("todo", "work"))
        assertNull(tag("todo", "work", "old", "missing"))
    }

    @Test
    fun `tags the app does not know stay on the item when its other tags change`() {
        val lib = ThingsLibrary.from(HistoryState().apply {
            apply(WireParser.parseItem(Json.parseToJsonElement(seed) as JsonObject))
            apply(WireParser.parseItem(Json.parseToJsonElement("""{"todo": {"t": 1, "e": "Task6", "p": {"tg": ["work", "notSynced", "old"]}}}""") as JsonObject))
        })
        val payload = PendingChange.SetTags("op", "todo", listOf("home")).build(lib, now)!!
        assertEquals("""["home","notSynced","old"]""", payload.getValue("todo").getValue("p").jsonObject["tg"].toString())
    }

    @Test
    fun `headings, trashed items and repeating templates are refused`() {
        listOf("head", "trashed", "tmpl").forEach { id ->
            assertFailsWith<IllegalArgumentException>(id) { tag(id, "home") }
        }
        assertFailsWith<NoSuchElementException> { tag("missing", "home") }
    }

    @Test
    fun `canTag matches what can be written`() {
        assertEquals(listOf(true, true, false, false, false), listOf("todo", "proj", "head", "trashed", "tmpl").map { library.canTag(library.task(it)!!) })
    }
}
