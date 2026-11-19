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
    fun `checked to-dos are not in the snapshot`() {
        val lib = library(
            """{"open": {"t": 0, "e": "Task7", "p": {"tt": "Open", "st": 1, "sr": $day15}},
                "done": {"t": 0, "e": "Task7", "p": {"tt": "Done", "st": 1, "sr": $day15, "ss": 3, "sp": ${day15 + 43_200}}}}"""
        )

        assertEquals(listOf("open"), WidgetSnapshot.from(lib, today, updatedAtMillis = 42).items("today").map { it.id })
    }

    @Test
    fun `a to-do checked from the widget leaves every list at once`() {
        val lib = library(
            """{"p1": {"t": 0, "e": "Task7", "p": {"tt": "Move", "tp": 1, "st": 1}},
                "a": {"t": 0, "e": "Task7", "p": {"tt": "A", "st": 1, "sr": $day15, "pr": ["p1"]}},
                "b": {"t": 0, "e": "Task7", "p": {"tt": "B", "st": 1, "sr": $day15}}}"""
        )

        val shown = WidgetSnapshot.from(lib, today, updatedAtMillis = 42).without("a")

        assertEquals(listOf("b"), shown.items("today").map { it.id })
        assertEquals(emptyList(), shown.items("project:p1"))
        assertEquals(listOf("p1"), shown.projects.map { it.id })
    }

    @Test
    fun `to-dos the app cannot check are marked so the widget opens them instead`() {
        val lib = library(
            """{"tmpl": {"t": 0, "e": "Task7", "p": {"tt": "Water", "st": 1, "rr": {"tp": 1, "fu": 64, "fa": 1}}},
                "inst": {"t": 0, "e": "Task7", "p": {"tt": "Water", "st": 1, "sr": $day15, "rt": ["tmpl"]}},
                "plain": {"t": 0, "e": "Task7", "p": {"tt": "Call", "st": 1, "sr": $day15}}}"""
        )

        val items = WidgetSnapshot.from(lib, today, updatedAtMillis = 42).items("today").associate { it.id to it.canToggle }

        assertEquals(mapOf("inst" to false, "plain" to true), items)
    }

    @Test
    fun `a snapshot written before check-off support or after it, still reads, as checkable`() {
        val old = """{"updatedAtMillis": 1, "lists": {"today": [{"id": "a", "title": "A", "done": false}]}, "projects": []}"""

        val item = WidgetSnapshot.fromJson(old).items("today").single()

        assertEquals(true, item.canToggle)
    }

    @Test
    fun `snapshot survives a json round trip`() {
        val lib = library("""{"i1": {"t": 0, "e": "Task7", "p": {"tt": "Inbox \"quoted\"", "st": 0}}}""")
        val snapshot = WidgetSnapshot.from(lib, today, updatedAtMillis = 42)

        val restored = WidgetSnapshot.fromJson(snapshot.toJson())

        assertEquals(snapshot, restored)
        assertEquals(42, restored.updatedAtMillis)
    }

    @Test
    fun `items carry their deadline and reminder for the widget to highlight`() {
        val lib = library(
            """{"due": {"t": 0, "e": "Task7", "p": {"tt": "Due", "st": 1, "sr": $day15, "dd": ${day15 + 86_400}, "ato": 34200}},
                "plain": {"t": 0, "e": "Task7", "p": {"tt": "Plain", "st": 1, "sr": $day15}}}"""
        )

        val items = WidgetSnapshot.from(lib, today, updatedAtMillis = 42).items("today").associateBy { it.id }

        assertEquals(LocalDate.of(2026, 11, 16).toEpochDay(), items.getValue("due").deadlineDay)
        assertEquals(34_200, items.getValue("due").reminderSeconds)
        assertEquals(null, items.getValue("plain").deadlineDay)
        assertEquals(null, items.getValue("plain").reminderSeconds)
    }

    @Test
    fun `a snapshot written before deadlines were kept still reads`() {
        val old = """{"updatedAtMillis": 1, "lists": {"today": [{"id": "a", "title": "A"}]}, "projects": []}"""

        assertEquals(null, WidgetSnapshot.fromJson(old).items("today").single().deadlineDay)
    }
}
