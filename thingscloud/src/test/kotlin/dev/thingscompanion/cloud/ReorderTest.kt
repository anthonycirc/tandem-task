package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReorderTest {

    private val now = 1_794_750_000.5

    private fun library(json: String) = ThingsLibrary.from(HistoryState().apply { apply(WireParser.parseItem(Json.parseToJsonElement(json) as JsonObject)) })

    private fun todo(ix: Int, extra: String = "") = """{"t": 0, "e": "Task7", "p": {"tt": "x", "st": 0, "ix": $ix$extra}}"""

    private val inbox = library("""{"a": ${todo(10)}, "b": ${todo(20)}, "c": ${todo(30)}, "d": ${todo(31)}}""")

    private fun written(payload: Map<String, JsonObject>?) =
        payload.orEmpty().mapValues { (_, obj) -> obj["p"]!!.jsonObject.filterKeys { it != "md" } }

    @Test
    fun `a to-do dropped between two others takes the index halfway`() {
        val payload = PendingChange.Reorder("op", "c", listOf("a", "c", "b", "d"), "inbox").build(inbox, now)
        assertEquals(mapOf("c" to mapOf("ix" to JsonPrimitive(15))), written(payload))
    }

    @Test
    fun `at the top or the bottom it goes one past the neighbour`() {
        assertEquals(mapOf("c" to mapOf("ix" to JsonPrimitive(9))), written(PendingChange.Reorder("op", "c", listOf("c", "a", "b", "d"), "inbox").build(inbox, now)))
        assertEquals(mapOf("a" to mapOf("ix" to JsonPrimitive(32))), written(PendingChange.Reorder("op", "a", listOf("b", "c", "d", "a"), "inbox").build(inbox, now)))
    }

    @Test
    fun `without room between the neighbours, the list is renumbered in the same commit`() {
        val payload = written(PendingChange.Reorder("op", "a", listOf("b", "c", "a", "d"), "inbox").build(inbox, now))
        assertEquals(
            mapOf("b" to 1024, "c" to 2048, "a" to 3072, "d" to 4096).mapValues { mapOf("ix" to JsonPrimitive(it.value)) },
            payload,
        )
    }

    @Test
    fun `two drags in the same list both stay queued, and both show`() {
        val first = PendingChange.Reorder("op1", "d", listOf("d", "a", "b", "c"), "inbox")
        val second = PendingChange.Reorder("op2", "b", listOf("d", "a", "c", "b"), "inbox")
        val queue = PendingChanges.enqueue(PendingChanges.enqueue(emptyList(), first), second)

        assertEquals(listOf(first, second), queue)
        val state = HistoryState().apply { apply(WireParser.parseItem(Json.parseToJsonElement("""{"a": ${todo(10)}, "b": ${todo(20)}, "c": ${todo(30)}, "d": ${todo(31)}}""") as JsonObject)) }
        assertEquals(listOf("d", "a", "c", "b"), PendingChanges.overlay(state, queue, now).inbox().map { it.id })
    }

    @Test
    fun `a second drag of the same to-do replaces the first`() {
        val first = PendingChange.Reorder("op1", "d", listOf("d", "a", "b", "c"), "inbox")
        val again = PendingChange.Reorder("op2", "d", listOf("a", "d", "b", "c"), "inbox")
        assertEquals(listOf(again), PendingChanges.enqueue(listOf(first), again))
    }

    @Test
    fun `nothing is sent when the to-do is already in place`() {
        assertNull(PendingChange.Reorder("op", "b", listOf("a", "b", "c", "d"), "inbox").build(inbox, now))
    }

    @Test
    fun `in Today the order is ti within the section dropped in, and crossing into This Evening sets sb`() {
        val today = library(
            // Rows of Today are dated (sr): undated ones there are only shown for their deadline.
            """{"x": ${todo(1, ""","st": 1, "sr": 1794700800, "ti": 5""")}, "y": ${todo(2, ""","st": 1, "sr": 1794700800, "ti": 9""")}, "z": ${todo(3, ""","st": 1, "sr": 1794700800, "ti": 1, "sb": 1""")}}""",
        )
        // x (ti 5) already follows z (ti 1): only its section changes.
        val toEvening = written(PendingChange.Reorder("op", "x", listOf("z", "x"), "today", todayOrder = true, evening = true).build(today, now))
        assertEquals(mapOf("x" to mapOf("sb" to JsonPrimitive(1))), toEvening)
        // Before z, it needs a ti below z's.
        val first = written(PendingChange.Reorder("op", "x", listOf("x", "z"), "today", todayOrder = true, evening = true).build(today, now))
        assertEquals(mapOf("x" to mapOf("ti" to JsonPrimitive(0), "sb" to JsonPrimitive(1))), first)
    }

    @Test
    fun `the library breaks index ties by id, so a refresh keeps the order`() {
        val tied = library("""{"b": ${todo(5)}, "a": ${todo(5)}}""")
        assertEquals(listOf("a", "b"), tied.inbox().map { it.id })
    }
}
