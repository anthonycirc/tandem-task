package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class WidgetSnapshotTest {

    private val today = LocalDate.of(2026, 11, 15)
    private val day15 = 1_794_700_800L

    private fun library(json: String): ThingsLibrary {
        val state = HistoryState()
        state.apply(WireParser.parseItem(Json.parseToJsonElement(json) as JsonObject))
        return ThingsLibrary.from(state)
    }

    @Test
    fun `snapshot holds every list and every project's to-dos`() {
        val lib = library(
            """{"t1": {"t": 0, "e": "Task7", "p": {"tt": "Today task", "st": 1, "sr": $day15}},
                "i1": {"t": 0, "e": "Task7", "p": {"tt": "Inbox task", "st": 0}},
                "p1": {"t": 0, "e": "Task7", "p": {"tt": "Move", "tp": 1, "st": 1}},
                "c1": {"t": 0, "e": "Task7", "p": {"tt": "Pack", "st": 1, "pr": ["p1"]}}}"""
        )

        val snapshot = WidgetSnapshot.from(lib, today, updatedAtMillis = 42)

        assertEquals(listOf("t1" to "Today task"), snapshot.items("today").map { it.id to it.title })
        assertEquals(listOf("i1"), snapshot.items("inbox").map { it.id })
        assertEquals(listOf("c1"), snapshot.items("project:p1").map { it.id })
        assertEquals(listOf("p1" to "Move"), snapshot.projects.map { it.id to it.title })
        assertEquals("Move", snapshot.label("project:p1"))
        assertEquals("Today", snapshot.label("today"))
        assertEquals(emptyList(), snapshot.items("project:unknown"))
    }

    @Test
    fun `snapshot survives a json round trip`() {
        val lib = library("""{"i1": {"t": 0, "e": "Task7", "p": {"tt": "Inbox \"quoted\"", "st": 0}}}""")
        val snapshot = WidgetSnapshot.from(lib, today, updatedAtMillis = 42)

        val restored = WidgetSnapshot.fromJson(snapshot.toJson())

        assertEquals(snapshot, restored)
        assertEquals(42, restored.updatedAtMillis)
    }
}
