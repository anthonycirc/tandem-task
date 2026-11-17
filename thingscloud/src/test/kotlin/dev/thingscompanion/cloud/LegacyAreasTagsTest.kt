package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Areas and tags of older Things versions (Area2, Tag3, dashed ids) are gone from the Mac: the app hides them too. */
class LegacyAreasTagsTest {

    private val seed = """{
        "A-OLD-1": {"t": 0, "e": "Area2", "p": {"tt": "Old area", "ix": 0}},
        "area": {"t": 0, "e": "Area3", "p": {"tt": "Work", "ix": 1}},
        "T-OLD-1": {"t": 0, "e": "Tag3", "p": {"tt": "old tag", "ix": 0}},
        "tagB": {"t": 0, "e": "Tag4", "p": {"tt": "Bravo", "ix": 2}},
        "tagA": {"t": 0, "e": "Tag4", "p": {"tt": "alpha", "ix": 5}},
        "tagC": {"t": 0, "e": "Tag4", "p": {"tt": "Charlie", "ix": 1}},
        "inOld": {"t": 0, "e": "Task6", "p": {"tt": "Filed in an old area", "st": 1, "ar": ["A-OLD-1"]}},
        "tagged": {"t": 0, "e": "Task6", "p": {"tt": "Tagged", "st": 1, "ar": ["area"], "tg": ["tagA", "T-OLD-1", "tagC", "tagB"]}}
    }"""

    private val library = ThingsLibrary.from(HistoryState().apply {
        apply(WireParser.parseItem(Json.parseToJsonElement(seed) as JsonObject))
    })

    @Test
    fun `legacy areas are not listed and nothing is filed in them`() {
        assertEquals(listOf("area"), library.areas().map { it.id })
        assertNull(library.area("A-OLD-1"))
        assertNull(library.task("inOld")!!.areaId)
        assertEquals("area", library.task("tagged")!!.areaId)
    }

    @Test
    fun `legacy tags are neither listed nor shown on a to-do`() {
        assertEquals(setOf("tagA", "tagB", "tagC"), library.tags().map { it.id }.toSet())
        assertEquals(3, library.tagTitles(library.task("tagged")!!).size)
    }

    @Test
    fun `tags follow Things' own tag order, in the list and on a to-do`() {
        assertEquals(listOf("Charlie", "Bravo", "alpha"), library.tags().map { it.title })
        assertEquals(listOf("Charlie", "Bravo", "alpha"), library.tagTitles(library.task("tagged")!!))
    }

    /**
     * 2026-11-17: a to-do moved into a legacy area (gone from Things) made Things 3 for Mac crash
     * on every sync until the to-do was deleted. Nothing may be filed in such an area.
     */
    @Test
    fun `nothing is filed in a legacy area, the change is refused instead`() {
        val now = 1_794_902_503.0
        val toArea = PendingChange.Destination(areaId = "A-OLD-1")
        assertFailsWith<NoSuchElementException> { PendingChange.MoveTask("op", "tagged", toArea).build(library, now) }
        assertFailsWith<NoSuchElementException> {
            PendingChange.AddTask("op", "new", "Idea", "", PendingChange.When.of(Schedule.Anytime), areaId = "A-OLD-1").build(library, now)
        }
        assertFailsWith<NoSuchElementException> {
            PendingChange.AddTask("op", "new", "Idea", "", PendingChange.When.of(Schedule.Anytime), areaId = "missing").build(library, now)
        }
    }

    @Test
    fun `filing in a current area still works`() {
        val now = 1_794_902_503.0
        val payload = PendingChange.AddTask("op", "new", "Idea", "", PendingChange.When.of(Schedule.Anytime), areaId = "area").build(library, now)!!
        assertEquals("[\"area\"]", payload.getValue("new").getValue("p").jsonObject["ar"].toString())
    }
}
