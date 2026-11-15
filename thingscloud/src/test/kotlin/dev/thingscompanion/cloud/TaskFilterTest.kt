package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskFilterTest {

    private val today = LocalDate.of(2026, 11, 15)
    private val day15 = 1_794_700_800L
    private val day16 = day15 + 86_400

    private val lib by lazy {
        val state = HistoryState()
        state.apply(WireParser.parseItem(Json.parseToJsonElement(
            """{"work": {"t": 0, "e": "Tag4", "p": {"tt": "work"}},
                "home": {"t": 0, "e": "Tag4", "p": {"tt": "home"}},
                "area": {"t": 0, "e": "Area3", "p": {"tt": "Perso"}},
                "proj": {"t": 0, "e": "Task7", "p": {"tt": "Move", "tp": 1, "st": 1}},
                "a": {"t": 0, "e": "Task7", "p": {"tt": "A", "st": 1, "sr": $day15, "tg": ["work"], "pr": ["proj"], "ix": 1}},
                "b": {"t": 0, "e": "Task7", "p": {"tt": "B", "st": 1, "sr": $day15, "tg": ["home"], "ar": ["area"], "ix": 2}},
                "c": {"t": 0, "e": "Task7", "p": {"tt": "C", "st": 1, "sr": $day15, "dd": $day16, "ix": 3}},
                "d": {"t": 0, "e": "Task7", "p": {"tt": "D", "st": 1, "sr": $day15, "ss": 3, "sp": ${day15 + 3600}, "ix": 0}}}"""
        ) as JsonObject))
        ThingsLibrary.from(state)
    }

    private fun List<Task>.ids() = map { it.id }

    private val todayList get() = lib.today(today, closedOn = today, zone = ZoneOffset.UTC)

    @Test
    fun `closed to-dos go to the bottom, or away when hidden`() {
        assertEquals(listOf("a", "b", "c", "d"), TaskFilter().apply(todayList).ids())
        assertEquals(listOf("a", "b", "c"), TaskFilter(showClosed = false).apply(todayList).ids())
    }

    @Test
    fun `tags keep to-dos carrying any chosen tag`() {
        assertEquals(listOf("a", "b"), TaskFilter(tagIds = setOf("work", "home")).apply(todayList).ids())
        assertEquals(listOf("a"), TaskFilter(tagIds = setOf("work")).apply(todayList).ids())
    }

    @Test
    fun `a project or area keeps the to-dos filed in it`() {
        assertEquals(listOf("a"), TaskFilter(containerId = "proj").apply(todayList).ids())
        assertEquals(listOf("b"), TaskFilter(containerId = "area").apply(todayList).ids())
    }

    @Test
    fun `a day keeps to-dos scheduled or due that day, and filters combine`() {
        assertEquals(listOf("c"), TaskFilter(day = today.plusDays(1)).apply(todayList).ids())
        assertEquals(listOf("a"), TaskFilter(day = today, tagIds = setOf("work"), showClosed = false).apply(todayList).ids())
        assertTrue(TaskFilter(day = today, containerId = "area", tagIds = setOf("work")).apply(todayList).isEmpty())
        assertTrue(TaskFilter().isEmpty)
        assertTrue(!TaskFilter(tagIds = setOf("x")).isEmpty)
    }

    @Test
    fun `the library lists tags by title`() {
        assertEquals(listOf("home", "work"), lib.tags().map { it.title })
    }
}
