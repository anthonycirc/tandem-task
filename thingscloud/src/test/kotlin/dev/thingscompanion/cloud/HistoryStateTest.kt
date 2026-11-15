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
}
