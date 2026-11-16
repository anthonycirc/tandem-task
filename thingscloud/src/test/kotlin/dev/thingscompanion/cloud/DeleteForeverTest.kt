package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pinned to the item Things 3 wrote when the user emptied the Trash (delete-forever.md L2). */
class DeleteForeverTest {

    private val now = 1_794_854_313.25

    private val seed = """{
        "old": {"t": 0, "e": "Task6", "p": {"tt": "Old", "st": 1, "tr": true}},
        "c1": {"t": 0, "e": "ChecklistItem3", "p": {"tt": "One", "ts": ["old"], "ss": 0}},
        "c2": {"t": 0, "e": "ChecklistItem3", "p": {"tt": "Two", "ts": ["old"], "ss": 3}},
        "tmpl": {"t": 0, "e": "Task7", "p": {"tt": "Water", "st": 2, "tr": true, "rr": {"tp": 0, "fu": 16, "fa": 1}}},
        "tc": {"t": 0, "e": "ChecklistItem3", "p": {"tt": "Can", "ts": ["tmpl"], "ss": 0}},
        "live": {"t": 0, "e": "Task6", "p": {"tt": "Water", "st": 1, "sr": 1794787200, "rt": ["tmpl"]}},
        "gone": {"t": 0, "e": "Task7", "p": {"tt": "Water", "st": 1, "sr": 1794700800, "rt": ["tmpl"], "tr": true}},
        "open": {"t": 0, "e": "Task7", "p": {"tt": "Keep", "st": 1}},
        "proj": {"t": 0, "e": "Task7", "p": {"tt": "Move", "tp": 1, "st": 1, "tr": true}},
        "head": {"t": 0, "e": "Task7", "p": {"tt": "Phase", "tp": 2, "st": 1, "tr": true}}
    }"""

    private fun library(vararg more: Map<String, JsonObject>) = ThingsLibrary.from(HistoryState().apply {
        apply(WireParser.parseItem(Json.parseToJsonElement(seed) as JsonObject))
        more.forEach { apply(WireParser.parseItem(JsonObject(it))) }
    })

    private fun delete(vararg ids: String, lib: ThingsLibrary = library()) =
        PendingChange.DeleteForever("op", ids.toList()).build(lib, now)

    private fun tombstones(payload: Map<String, JsonObject>) = payload.values
        .filter { it["e"] == JsonPrimitive("Tombstone2") }
        .map { it["p"]!!.jsonObject }

    @Test
    fun `a to-do and its checklist are deleted in one item, as Things writes it`() {
        val payload = delete("old")!!
        val deleted = Json.parseToJsonElement("""{"t": 2, "e": "Task7", "p": {}}""")
        assertEquals(deleted, payload.getValue("old"))
        for (item in listOf("c1", "c2")) {
            assertEquals(Json.parseToJsonElement("""{"t": 2, "e": "ChecklistItem3", "p": {}}"""), payload.getValue(item))
        }
    }

    @Test
    fun `each deleted to-do and checklist item gets a tombstone`() {
        val payload = delete("old")!!
        val stones = tombstones(payload)
        assertEquals(setOf("old", "c1", "c2"), stones.map { it["dloid"]!!.toString().trim('"') }.toSet())
        stones.forEach { assertEquals("1794854313.25", it["dld"].toString()) }
        payload.filterValues { it["e"] == JsonPrimitive("Tombstone2") }.forEach { (id, item) ->
            assertEquals(JsonPrimitive(0), item["t"])
            assertTrue(id.length in 21..22 && id.all { it in ThingsIds.ALPHABET }, id)
        }
        assertEquals(6, payload.size)
    }

    @Test
    fun `a deleted template unlinks its live instances, without md, and leaves no tombstone`() {
        val payload = delete("tmpl", "gone")!!
        assertEquals(Json.parseToJsonElement("""{"t": 2, "e": "Task7", "p": {}}"""), payload.getValue("tmpl"))
        assertEquals(Json.parseToJsonElement("""{"t": 2, "e": "ChecklistItem3", "p": {}}"""), payload.getValue("tc"))
        assertEquals(Json.parseToJsonElement("""{"t": 1, "e": "Task7", "p": {"rt": []}}"""), payload.getValue("live"))
        // Deleted with it: no update for the trashed instance.
        assertEquals(Json.parseToJsonElement("""{"t": 2, "e": "Task7", "p": {}}"""), payload.getValue("gone"))
        assertEquals(setOf("gone"), tombstones(payload).map { it["dloid"]!!.toString().trim('"') }.toSet())
    }

    @Test
    fun `only trashed to-dos are deleted, projects, headings and open to-dos are skipped`() {
        val payload = delete("old", "open", "proj", "head", "missing")!!
        assertFalse("open" in payload || "proj" in payload || "head" in payload)
        assertNull(delete("open", "proj", "head", "missing"))
        val lib = library()
        assertTrue(lib.canDeleteForever(lib.task("old")!!))
        assertTrue(lib.canDeleteForever(lib.task("tmpl")!!))
        listOf("open", "proj", "head", "live").forEach { assertFalse(lib.canDeleteForever(lib.task(it)!!), it) }
    }

    @Test
    fun `sent again once applied, nothing more is written`() {
        val applied = library(delete("old", "tmpl")!!)
        assertNull(applied.task("old"))
        assertTrue(applied.checklist("old").isEmpty())
        assertEquals(emptyList(), applied.task("live")!!.recurrenceTemplateIds)
        assertNull(delete("old", "tmpl", lib = applied))
    }

    @Test
    fun `the tombstone ids do not change when the change is built again`() {
        assertEquals(delete("old")!!.keys, delete("old")!!.keys)
        val other = PendingChange.DeleteForever("op2", listOf("old")).build(library(), now)!!
        assertEquals(3, (delete("old")!!.keys - other.keys).size)
    }

    @Test
    fun `the Trash's deletable to-dos are the trashed to-dos and templates`() {
        val lib = library()
        assertEquals(setOf("old", "tmpl", "gone"), lib.trash().filter(lib::canDeleteForever).map { it.id }.toSet())
    }
}
