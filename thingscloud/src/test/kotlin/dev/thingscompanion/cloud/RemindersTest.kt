package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RemindersTest {

    private val paris = ZoneId.of("Europe/Paris")
    private val today = LocalDate.of(2026, 11, 15)
    private val day15 = 1_794_700_800L
    private val day16 = day15 + 86_400

    private fun library(json: String): ThingsLibrary {
        val state = HistoryState()
        state.apply(WireParser.parseItem(Json.parseToJsonElement(json) as JsonObject))
        return ThingsLibrary.from(state)
    }

    private fun task(props: String) = """{"t": 0, "e": "Task7", "p": {$props}}"""

    @Test
    fun `upcoming reminders are open dated to-dos with a time still ahead`() {
        val lib = library(
            """{"later": ${task(""""tt": "Call", "st": 1, "sr": $day15, "ato": 72000""")},
                "tomorrow": ${task(""""tt": "Gym", "st": 2, "sr": $day16, "ato": 32400""")},
                "past": ${task(""""tt": "Old", "st": 1, "sr": $day15, "ato": 3600""")},
                "done": ${task(""""tt": "Done", "st": 1, "sr": $day15, "ato": 72000, "ss": 3""")},
                "trashed": ${task(""""tt": "Gone", "st": 1, "sr": $day15, "ato": 72000, "tr": true""")},
                "undated": ${task(""""tt": "No date", "st": 1, "ato": 72000""")},
                "seen": ${task(""""tt": "Seen", "st": 1, "sr": $day16, "ato": 32400, "lai": ${day16 + 40_000}""")}}"""
        )
        val now = ZonedDateTime.of(today, LocalTime.of(12, 0), paris).toInstant()

        val reminders = Reminders.upcoming(lib, now, paris)

        assertEquals(listOf("later", "tomorrow"), reminders.map { it.id })
        assertEquals(ZonedDateTime.of(today, LocalTime.of(20, 0), paris).toInstant(), reminders[0].at)
        assertEquals("Call", reminders[0].title)
        assertEquals(LocalTime.of(20, 0), lib.task("later")!!.reminder)
    }

    @Test
    fun `a reminder time is written as seconds from midnight on dated to-dos only`() {
        val payload = InboxTask.payload("Call", "", 0, 1.0, Schedule.Today(today), reminder = LocalTime.of(20, 30))

        assertEquals("73800", (payload["p"] as JsonObject)["ato"].toString())
        assertFailsWith<IllegalArgumentException> {
            InboxTask.payload("Call", "", 0, 1.0, Schedule.Inbox, reminder = LocalTime.of(9, 0))
        }
    }

    @Test
    fun `a draft matches only with the same reminder`() {
        val lib = library(
            """{"x": ${InboxTask.payload("Call", "", 0, 1.0, Schedule.Today(today), reminder = LocalTime.of(9, 0))}}"""
        )
        val task = lib.task("x")!!

        assertEquals(true, InboxTask.matches(task, "Call", "", Schedule.Today(today), LocalTime.of(9, 0)))
        assertEquals(false, InboxTask.matches(task, "Call", "", Schedule.Today(today), null))
    }
}
