package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotesAndDaysTest {

    @Test
    fun `plain notes are trimmed and line separators normalized`() {
        assertEquals("one\ntwo", Notes.toPlainText(JsonPrimitive("  one two ")))
    }

    @Test
    fun `structured notes of type 1 read v`() {
        val notes = Json.parseToJsonElement("""{"_t": "tx", "t": 1, "ch": 0, "v": "hello world"}""")
        assertEquals("hello\nworld", Notes.toPlainText(notes))
    }

    @Test
    fun `structured notes of type 2 join paragraphs`() {
        val notes = Json.parseToJsonElement("""{"_t": "tx", "t": 2, "ps": [{"r": "first"}, {"r": "second"}]}""")
        assertEquals("first\nsecond", Notes.toPlainText(notes))
    }

    @Test
    fun `empty or unknown notes are null`() {
        assertNull(Notes.toPlainText(null))
        assertNull(Notes.toPlainText(JsonPrimitive("   ")))
        assertNull(Notes.toPlainText(Json.parseToJsonElement("""{"t": 9}""")))
        assertNull(Notes.toPlainText(Json.parseToJsonElement("[1, 2]")))
    }

    @Test
    fun `a day value is the utc midnight of that calendar date`() {
        // 2026-11-16T00:00:00Z
        assertEquals(LocalDate.of(2026, 11, 16), ThingsDay.toLocalDate(1_794_787_200))
    }

    @Test
    fun `today follows the local calendar, not the utc one, near midnight`() {
        val paris = ZoneId.of("Europe/Paris")
        // 00:30 in Paris on 2026-11-16 is still 2026-11-15 in UTC.
        val instant = Instant.parse("2026-11-15T23:30:00Z")

        assertEquals(LocalDate.of(2026, 11, 16), ThingsDay.today(instant, paris))
    }
}
