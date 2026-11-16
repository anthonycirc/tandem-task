package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RepeatRuleTest {

    private fun library(json: String) = ThingsLibrary.from(
        HistoryState().apply { apply(WireParser.parseItem(Json.parseToJsonElement(json) as JsonObject)) },
    )

    private fun rule(rr: String): RepeatRule? {
        val lib = library("""{"t": {"t": 0, "e": "Task7", "p": {"tt": "T", "st": 1, "rr": $rr}}}""")
        return lib.repeatRule(lib.task("t")!!)
    }

    @Test
    fun `units, interval and after-completion are read from the template's rule`() {
        assertEquals(
            RepeatRule(RepeatUnit.DAY, every = 2, afterCompletion = true),
            rule("""{"tp": 1, "fu": 16, "fa": 2, "of": [{"dy": 0}], "ed": 64092211200, "rc": 0}"""),
        )
    }

    @Test
    fun `weekly rules name their days, Sunday being 0`() {
        assertEquals(
            RepeatRule(RepeatUnit.WEEK, every = 1, afterCompletion = false, weekdays = listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY)),
            rule("""{"tp": 0, "fu": 256, "fa": 1, "of": [{"wd": 1}, {"wd": 5}, {"wd": 0}], "ed": 64092211200}"""),
        )
    }

    @Test
    fun `monthly days count from 0, and -1 is the last day`() {
        assertEquals(15, rule("""{"tp": 0, "fu": 8, "fa": 1, "of": [{"dy": 14}]}""")!!.monthDay)
        assertEquals(-1, rule("""{"tp": 0, "fu": 8, "fa": 3, "of": [{"dy": -1}]}""")!!.monthDay)
    }

    @Test
    fun `yearly rules carry a 0-based month and day`() {
        val yearly = rule("""{"tp": 0, "fu": 4, "fa": 1, "of": [{"dy": 4, "mo": 2}]}""")!!
        assertEquals(Month.MARCH, yearly.month)
        assertEquals(5, yearly.monthDay)
    }

    @Test
    fun `an end date is kept unless it is Things' far-future "never"`() {
        assertNull(rule("""{"tp": 0, "fu": 16, "fa": 1, "ed": 64092211200}""")!!.endDate)
        assertEquals(LocalDate.of(2026, 11, 15), rule("""{"tp": 0, "fu": 16, "fa": 1, "ed": 1794700800}""")!!.endDate)
    }

    @Test
    fun `an instance shows its template's rule, other to-dos have none`() {
        val lib = library(
            """{"tmpl": {"t": 0, "e": "Task7", "p": {"tt": "Water", "st": 1, "rr": {"tp": 0, "fu": 256, "fa": 1, "of": [{"wd": 3}]}}},
                "inst": {"t": 0, "e": "Task7", "p": {"tt": "Water", "st": 1, "rt": ["tmpl"]}},
                "plain": {"t": 0, "e": "Task7", "p": {"tt": "Call"}}}""",
        )
        assertEquals(listOf(DayOfWeek.WEDNESDAY), lib.repeatRule(lib.task("inst")!!)!!.weekdays)
        assertNull(lib.repeatRule(lib.task("plain")!!))
    }

    @Test
    fun `an unknown unit gives no rule rather than a wrong one`() {
        assertNull(rule("""{"tp": 0, "fu": 999, "fa": 1}"""))
    }

    @Test
    fun `a daily after-completion rule matches the wire item Things wrote for the user's test to-do`() {
        val start = java.time.LocalDate.of(2026, 11, 16)
        // From the history item that created it on the Mac on 2026-11-16, no occurrence yet (L3).
        val expected = Json.parseToJsonElement(
            """{"ia": 0, "rrv": 4, "tp": 1, "ts": 0, "fu": 16, "sr": 1794787200, "fa": 1, "rc": 0, "of": [], "ed": 64092211200}""",
        )
        assertEquals(expected, RepeatRule(RepeatUnit.DAY, every = 1, afterCompletion = true).toRr(start, anchor = null))
    }

    @Test
    fun `rules written by the app read back the same`() {
        val start = java.time.LocalDate.of(2026, 11, 16)
        val rules = listOf(
            RepeatRule(RepeatUnit.DAY, every = 3, afterCompletion = false),
            RepeatRule(RepeatUnit.WEEK, every = 1, afterCompletion = false, weekdays = listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY)),
            RepeatRule(RepeatUnit.WEEK, every = 2, afterCompletion = true),
            RepeatRule(RepeatUnit.MONTH, every = 1, afterCompletion = false, monthDay = 15),
            RepeatRule(RepeatUnit.MONTH, every = 3, afterCompletion = false, monthDay = -1),
            RepeatRule(RepeatUnit.YEAR, every = 1, afterCompletion = false, monthDay = 5, month = Month.MARCH),
            RepeatRule(RepeatUnit.DAY, every = 1, afterCompletion = false, endDate = LocalDate.of(2027, 1, 31)),
        )
        for (rule in rules) {
            val lib = library("""{"t": {"t": 0, "e": "Task7", "p": {"tt": "T", "st": 1, "rr": ${rule.toRr(start, anchor = start)}}}}""")
            assertEquals(rule, lib.repeatRule(lib.task("t")!!), rule.toString())
        }
    }
}
