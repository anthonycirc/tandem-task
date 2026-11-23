package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The coming occurrences of repeating to-dos, shown in Upcoming before Things creates them. */
class RepeatsAheadTest {

    private val today = LocalDate.of(2026, 11, 23)
    private val day = 86_400L

    private fun d(offset: Long) = (today.toEpochDay() + offset) * day

    private fun library(json: String) = ThingsLibrary.from(HistoryState().apply {
        apply(WireParser.parseItem(Json.parseToJsonElement(json) as JsonObject))
    })

    private fun ThingsLibrary.ahead() = repeatsAhead(today).map { it.id to (it.startDate!!.toEpochDay() - today.toEpochDay()).toInt() }

    @Test
    fun `a fixed schedule shows its next day, then each occurrence up to four weeks ahead`() {
        // Every Thursday; today is a Monday, the next one is in 3 days.
        val lib = library("""{
            "tmpl": {"t": 0, "e": "Task7", "p": {"tt": "Bins", "st": 2, "tir": ${d(3)}, "rr": {"tp": 0, "fu": 256, "fa": 1, "of": [{"wd": 4}]}}}
        }""")
        assertEquals(listOf("tmpl" to 3, "tmpl" to 10, "tmpl" to 17, "tmpl" to 24), lib.ahead())
    }

    @Test
    fun `days that already have an occurrence are skipped, and the rule's end stops it`() {
        val lib = library("""{
            "tmpl": {"t": 0, "e": "Task7", "p": {"tt": "Pills", "st": 2, "tir": ${d(1)}, "rr": {"tp": 0, "fu": 16, "fa": 1, "of": [{"dy": 0}], "ed": ${d(5)}}}},
            "made": {"t": 0, "e": "Task7", "p": {"tt": "Pills", "st": 1, "sr": ${d(2)}, "rt": ["tmpl"]}}
        }""")
        assertEquals(listOf("tmpl" to 1, "tmpl" to 3, "tmpl" to 4, "tmpl" to 5), lib.ahead())
    }

    @Test
    fun `the next day is shown even past four weeks`() {
        val lib = library("""{
            "tmpl": {"t": 0, "e": "Task7", "p": {"tt": "Taxes", "st": 2, "tir": ${d(100)}, "rr": {"tp": 0, "fu": 4, "fa": 1, "of": [{"dy": 2, "mo": 2}]}}}
        }""")
        assertEquals(listOf("tmpl" to 100), lib.ahead())
    }

    @Test
    fun `an after completion repeat shows its next day only once no occurrence is open`() {
        val waiting = library("""{
            "tmpl": {"t": 0, "e": "Task7", "p": {"tt": "Haircut", "st": 2, "tir": ${d(2)}, "rr": {"tp": 1, "fu": 16, "fa": 3}}},
            "open": {"t": 0, "e": "Task7", "p": {"tt": "Haircut", "st": 1, "sr": ${d(-1)}, "rt": ["tmpl"]}}
        }""")
        assertEquals(emptyList(), waiting.ahead())
        val done = library("""{
            "tmpl": {"t": 0, "e": "Task7", "p": {"tt": "Haircut", "st": 2, "tir": ${d(2)}, "rr": {"tp": 1, "fu": 16, "fa": 3}}},
            "closed": {"t": 0, "e": "Task7", "p": {"tt": "Haircut", "st": 1, "sr": ${d(-1)}, "rt": ["tmpl"], "ss": 3, "sp": ${d(-1)}}}
        }""")
        assertEquals(listOf("tmpl" to 2), done.ahead())
    }

    @Test
    fun `trashed templates and repeating projects show nothing, and Upcoming itself stays without them`() {
        val lib = library("""{
            "gone": {"t": 0, "e": "Task7", "p": {"tt": "Old", "st": 2, "tr": true, "tir": ${d(1)}, "rr": {"tp": 0, "fu": 16, "fa": 1, "of": [{"dy": 0}]}}},
            "proj": {"t": 0, "e": "Task7", "p": {"tt": "Sprint", "tp": 1, "st": 2, "tir": ${d(1)}, "rr": {"tp": 0, "fu": 256, "fa": 2, "of": [{"wd": 2}]}}},
            "tmpl": {"t": 0, "e": "Task7", "p": {"tt": "Bins", "st": 2, "tir": ${d(3)}, "rr": {"tp": 0, "fu": 256, "fa": 1, "of": [{"wd": 4}]}}}
        }""")
        assertTrue(lib.ahead().all { it.first == "tmpl" })
        assertTrue(lib.upcoming(today).none { it.isRecurrenceTemplate })
    }
}
