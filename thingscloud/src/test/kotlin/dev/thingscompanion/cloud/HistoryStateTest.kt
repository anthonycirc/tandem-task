package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HistoryStateTest {

    private fun item(json: String): JsonObject = Json.parseToJsonElement(json) as JsonObject

    @Test
    fun `parses wire keys t e p`() {
        val changes = WireParser.parseItem(item("""{"abc": {"t": 1, "e": "Task6", "p": {"tt": "Title", "ss": 0}}}"""))

        assertEquals(1, changes.size)
        val change = changes.single()
        assertEquals("abc", change.uuid)
        assertEquals(Operation.UPDATE, change.operation)
        assertEquals("Task6", change.entity)
        assertEquals("Title", change.props["tt"]!!.jsonPrimitive.content)
    }

    @Test
    fun `create replaces, update merges, delete removes`() {
        val state = HistoryState()
        state.apply(WireParser.parseItem(item("""{"a": {"t": 0, "e": "Task6", "p": {"tt": "First", "ss": 0, "st": 0}}}""")))
        state.apply(WireParser.parseItem(item("""{"a": {"t": 1, "e": "Task6", "p": {"tt": "Renamed"}}}""")))

        val merged = state.objects.getValue("a")
        assertEquals("Renamed", merged.props["tt"]!!.jsonPrimitive.content)
        assertEquals(0, merged.props["st"]!!.jsonPrimitive.int)

        state.apply(WireParser.parseItem(item("""{"a": {"t": 0, "e": "Task6", "p": {"tt": "Recreated"}}}""")))
        assertNull(state.objects.getValue("a").props["st"])

        state.apply(WireParser.parseItem(item("""{"a": {"t": 2, "e": "Task6", "p": {}}}""")))
        assertNull(state.objects["a"])
    }

    @Test
    fun `update on unknown uuid inserts it`() {
        val state = HistoryState()
        state.apply(WireParser.parseItem(item("""{"b": {"t": 1, "e": "Area3", "p": {"tt": "Work"}}}""")))

        assertEquals("Area3", state.objects.getValue("b").entity)
    }

    @Test
    fun `state survives a json round trip`() {
        val state = HistoryState()
        state.apply(WireParser.parseItem(item("""{"a": {"t": 0, "e": "Task6", "p": {"tt": "Keep", "pr": ["p1"]}}}""")))

        val restored = HistoryState.fromJson(state.toJson())

        assertEquals(state.objects, restored.objects)
    }

    private fun notesOf(state: HistoryState, uuid: String) = Notes.toPlainText(state.objects.getValue(uuid).props["nt"])

    @Test
    fun `a note edited in Things applies its patches to the note, at UTF-8 byte offsets`() {
        // As the Things app sent it: `ch` is the CRC32 of the edited text.
        val state = HistoryState()
        state.apply(WireParser.parseItem(item("""{"a": {"t": 0, "e": "Task6", "p": {"tt": "A", "nt": {"_t": "tx", "t": 1, "ch": 0, "v": "Native Task7 note α 🚀\nSecond line."}}}}""")))
        state.apply(WireParser.parseItem(item("""{"a": {"t": 1, "e": "Task6", "p": {"nt": {"_t": "tx", "t": 2, "ps": [{"r": "Update", "p": 26, "l": 5, "ch": 3672733299}]}}}}""")))

        assertEquals("Native Task7 note α 🚀\nUpdated line.", notesOf(state, "a"))
        assertEquals("A", state.objects.getValue("a").props["tt"]!!.jsonPrimitive.content)
    }

    @Test
    fun `successive note edits build on each other, and a full note still replaces it`() {
        val state = HistoryState()
        state.apply(WireParser.parseItem(item("""{"a": {"t": 0, "e": "Task6", "p": {"tt": "A", "nt": {"_t": "tx", "t": 1, "v": "Plan"}}}}""")))
        state.apply(WireParser.parseItem(item("""{"a": {"t": 1, "p": {"nt": {"_t": "tx", "t": 2, "ps": [{"r": "\nLinear : https://linear.app/x", "p": 4, "l": 0}]}}}}""")))
        state.apply(WireParser.parseItem(item("""{"a": {"t": 1, "p": {"nt": {"_t": "tx", "t": 2, "ps": [{"r": "État", "p": 0, "l": 4}, {"r": "\nObsidian : obsidian://open?vault=V", "p": 35, "l": 0}]}}}}""")))

        assertEquals("État\nLinear : https://linear.app/x\nObsidian : obsidian://open?vault=V", notesOf(state, "a"))

        state.apply(WireParser.parseItem(item("""{"a": {"t": 1, "p": {"nt": {"_t": "tx", "t": 1, "v": "Fresh"}}}}""")))
        assertEquals("Fresh", notesOf(state, "a"))
    }

    @Test
    fun `an edited note survives the saved state`() {
        val state = HistoryState()
        state.apply(WireParser.parseItem(item("""{"a": {"t": 0, "e": "Task6", "p": {"nt": {"_t": "tx", "t": 1, "v": "ab"}}}}""")))
        state.apply(WireParser.parseItem(item("""{"a": {"t": 1, "p": {"nt": {"_t": "tx", "t": 2, "ps": [{"r": "c", "p": 2, "l": 0}]}}}}""")))
        val restored = HistoryState.fromJson(state.toJson())
        restored.apply(WireParser.parseItem(item("""{"a": {"t": 1, "p": {"nt": {"_t": "tx", "t": 2, "ps": [{"r": "d", "p": 3, "l": 0}]}}}}""")))

        assertEquals("abcd", notesOf(restored, "a"))
    }
}
