package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/** Deadlines put undated to-dos and projects in Today and Upcoming, as Things does. */
class DeadlineListsTest {

    private val today = LocalDate.of(2026, 11, 15)
    private val day14 = 1_794_614_400L
    private val day15 = 1_794_700_800L
    private val day16 = 1_794_787_200L
    private val day17 = day16 + 86_400

    private fun library(vararg objects: Pair<String, String>): ThingsLibrary {
        val state = HistoryState()
        for ((uuid, body) in objects) {
            val item = Json.parseToJsonElement("""{"$uuid": $body}""") as JsonObject
            state.apply(WireParser.parseItem(item))
        }
        return ThingsLibrary.from(state)
    }

    private fun task(props: String) = """{"t": 0, "e": "Task6", "p": {$props}}"""

    @Test
    fun `today lists undated to-dos due today or overdue, not later ones`() {
        val lib = library(
            "overdue" to task(""""tt": "Overdue", "st": 1, "dd": $day14"""),
            "dueToday" to task(""""tt": "Due today", "st": 2, "dd": $day15"""),
            "inboxDue" to task(""""tt": "Inbox due", "st": 0, "dd": $day15"""),
            "dueLater" to task(""""tt": "Due later", "st": 1, "dd": $day16"""),
            "plain" to task(""""tt": "Plain", "st": 1"""),
            "dated" to task(""""tt": "Dated", "st": 1, "sr": $day15"""),
        )

        assertEquals(setOf("overdue", "dueToday", "inboxDue", "dated"), lib.today(today).map { it.id }.toSet())
    }

    @Test
    fun `a to-do scheduled later stays in Upcoming under its start day, even when already due`() {
        val lib = library(
            "late" to task(""""tt": "Late", "st": 1, "sr": $day17, "dd": $day15"""),
        )

        assertEquals(emptyList(), lib.today(today).map { it.id })
        assertEquals(LocalDate.of(2026, 11, 17), lib.upcoming(today).single().upcomingDay)
    }

    @Test
    fun `upcoming lists undated to-dos and projects due later, by their deadline`() {
        val lib = library(
            "dueLater" to task(""""tt": "Due later", "st": 1, "dd": $day17"""),
            "projDue" to task(""""tt": "Project due", "tp": 1, "st": 2, "dd": $day16"""),
            "scheduled" to task(""""tt": "Scheduled", "st": 1, "sr": $day16, "ix": 9"""),
            "dueToday" to task(""""tt": "Due today", "st": 1, "dd": $day15"""),
        )

        val upcoming = lib.upcoming(today)

        assertEquals(listOf("projDue", "scheduled", "dueLater"), upcoming.map { it.id })
        assertEquals(LocalDate.of(2026, 11, 17), upcoming.last().upcomingDay)
    }

    @Test
    fun `an undated to-do shown in Today for its deadline is never given a Today order or evening`() {
        val lib = library(
            "a" to task(""""tt": "A", "st": 1, "sr": $day15, "ti": 1"""),
            "due" to task(""""tt": "Due", "st": 1, "dd": $day15"""),
            "b" to task(""""tt": "B", "st": 1, "sr": $day15, "ti": 2"""),
        )

        kotlin.test.assertFailsWith<IllegalArgumentException> {
            PendingChange.Reorder("op", "due", listOf("due", "a", "b"), "today:false", todayOrder = true, evening = false).build(lib, 1_794_750_000.0)
        }
        val moved = PendingChange.Reorder("op", "b", listOf("due", "b", "a"), "today:false", todayOrder = true, evening = false).build(lib, 1_794_750_000.0).orEmpty()
        kotlin.test.assertFalse("due" in moved)
    }

    @Test
    fun `the Deadlines list holds every open to-do and project with a deadline, earliest first`() {
        val lib = library(
            "late" to task(""""tt": "Late", "st": 1, "sr": $day16, "dd": $day14"""),
            "proj" to task(""""tt": "Project", "tp": 1, "st": 1, "dd": $day17"""),
            "today" to task(""""tt": "Today", "st": 0, "dd": $day15"""),
            "plain" to task(""""tt": "No deadline", "st": 1, "sr": $day15"""),
            "done" to task(""""tt": "Done", "st": 1, "dd": $day15, "ss": 3, "sp": ${day15 + 3600}"""),
            "head" to task(""""tt": "Heading", "tp": 2, "st": 1, "dd": $day15"""),
            "tmpl" to task(""""tt": "Template", "st": 1, "dd": $day15, "rr": {"tp": 0}"""),
        )

        assertEquals(listOf("late", "today", "proj"), lib.deadlines().map { it.id })
    }
}
