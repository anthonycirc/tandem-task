package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Checking a repeating to-do's coming occurrence in Upcoming before it is created, as Things 3.23+ does:
 * shapes from the account's history (2026-09-23, 2026-10-10, 2026-10-25), one item with a new
 * completed copy of the template on that day and the template moved on.
 */
class CompleteAheadTest {

    private val today = LocalDate.of(2026, 11, 23)
    private val day = 86_400L
    private val dec1 = LocalDate.of(2026, 12, 1)
    private val now = (today.toEpochDay() * day + 50_000).toDouble() + 0.25

    private fun s(date: LocalDate) = date.toEpochDay() * day

    // Monthly on the 1st, no deadline lead (ts 0), next occurrence on Dec 1, 3 copies made so far.
    private fun rr(extra: String = "") = """{"of": [{"dy": 0}], "rrv": 4, "ia": ${s(LocalDate.of(2026, 7, 1))}, "ts": 0,
        "sr": ${s(LocalDate.of(2026, 7, 1))}, "rc": 0, "fa": 1, "fu": 8, "tp": 0, "ed": 64092211200$extra}"""

    private fun library(templateExtra: String = "", rr: String = rr(), more: String = "") = ThingsLibrary.from(HistoryState().apply {
        apply(WireParser.parseItem(Json.parseToJsonElement("""{
            "home": {"t": 0, "e": "Area3", "p": {"tt": "Home", "ix": 0}},
            "tag": {"t": 0, "e": "Tag4", "p": {"tt": "Bills", "ix": 0}},
            "tmpl": {"t": 0, "e": "Task7", "p": {"tt": "Pay rent", "st": 2, "ix": -1785, "ti": 2677758, "ar": ["home"], "tg": ["tag"],
                "ato": null, "icc": 3, "icsd": ${s(dec1) - 7 * day}, "tir": ${s(dec1)}, "rr": $rr$templateExtra}}$more
        }""") as JsonObject))
    })

    private fun ahead(lib: ThingsLibrary, on: LocalDate = dec1) = lib.repeatsAhead(today).first { it.startDate == on }

    private fun build(lib: ThingsLibrary = library(), on: LocalDate = dec1) =
        PendingChange.CompleteAhead("op", "tmpl", on.toEpochDay(), "copy").build(lib, now)!!

    private fun props(payload: Map<String, JsonObject>, id: String) = payload.getValue(id)["p"]!!.jsonObject

    @Test
    fun `the coming occurrence becomes a completed copy of its template on its day, and the template moves on`() {
        val payload = build()
        assertEquals(setOf("copy", "tmpl"), payload.keys)

        val copy = payload.getValue("copy")
        assertEquals(JsonPrimitive(0), copy["t"])
        assertEquals(JsonPrimitive("Task7"), copy["e"])
        val p = props(payload, "copy")
        assertEquals(34, p.size)
        assertEquals(JsonPrimitive("Pay rent"), p["tt"])
        assertEquals(JsonPrimitive(3), p["ss"])
        assertEquals(JsonPrimitive(2), p["st"])
        assertEquals(JsonPrimitive(s(dec1)), p["sr"])
        assertEquals(JsonPrimitive(s(dec1)), p["tir"])
        assertEquals(JsonPrimitive(true), p["lt"])
        assertEquals(JsonArray(listOf(JsonPrimitive("tmpl"))), p["rt"])
        assertEquals(JsonNull, p["rr"])
        assertEquals(JsonPrimitive(0), p["icc"])
        assertEquals(JsonNull, p["icsd"])
        assertEquals(JsonPrimitive(-1785), p["ix"])
        assertEquals(JsonPrimitive(2677758), p["ti"])
        assertEquals(JsonArray(listOf(JsonPrimitive("home"))), p["ar"])
        assertEquals(JsonArray(listOf(JsonPrimitive("tag"))), p["tg"])
        assertEquals(JsonNull, p["dd"])
        assertEquals(now, p["sp"].toString().toDouble())
        assertEquals(now, p["cd"].toString().toDouble())

        // The template: one more copy made, the next creation the day after, the next occurrence a month on; no md.
        val template = payload.getValue("tmpl")
        assertEquals(JsonPrimitive(1), template["t"])
        assertEquals(JsonPrimitive("Task7"), template["e"])
        assertEquals<Map<String, Any>>(
            mapOf("icc" to JsonPrimitive(4), "icsd" to JsonPrimitive(s(dec1) + day), "tir" to JsonPrimitive(s(LocalDate.of(2027, 1, 1)))),
            props(payload, "tmpl"),
        )
    }

    @Test
    fun `the copy takes the template's values as stored, untrimmed title and reminder fields included, but not its acrd`() {
        val raw = library(templateExtra = """, "rmd": 1794000000, "rp": 3, "icp": true, "sb": 1, "do": 1, "acrd": ${s(dec1) - 40 * day}""")
        val padded = ThingsLibrary.from(HistoryState().apply {
            apply(WireParser.parseItem(Json.parseToJsonElement("""{
                "tmpl": {"t": 0, "e": "Task7", "p": {"tt": " Pay rent  ", "nt": null, "st": 2, "ix": 4, "icc": 1, "tir": ${s(dec1)}, "rr": ${rr()}}}
            }""") as JsonObject))
        })
        val p = props(build(raw), "copy")
        assertEquals(34, p.size)
        assertEquals(JsonPrimitive(1794000000), p["rmd"])
        assertEquals(JsonPrimitive(3), p["rp"])
        assertEquals(JsonPrimitive(true), p["icp"])
        assertEquals(JsonPrimitive(1), p["sb"])
        assertEquals(JsonPrimitive(1), p["do"])
        // Things never copies acrd onto the occurrences it makes.
        assertEquals(JsonNull, p["acrd"])
        assertEquals(JsonPrimitive(" Pay rent  "), props(build(padded), "copy")["tt"])
    }

    @Test
    fun `the last occurrence before the repeat ends stays with Things`() {
        // Ends on Dec 15: after Dec 1 there is no next occurrence.
        val ending = library(rr = rr().replace("\"ed\": 64092211200", "\"ed\": ${s(LocalDate.of(2026, 12, 15))}"))
        assertFalse(ending.canCompleteAhead(ahead(ending)))
        assertFailsWith<IllegalArgumentException> { build(ending) }
    }

    @Test
    fun `only the next occurrence can be checked ahead, the later ones once it is done`() {
        val lib = library()
        assertTrue(lib.canCompleteAhead(ahead(lib)))
        assertFalse(lib.canCompleteAhead(lib.task("tmpl")!!.copy(startDate = LocalDate.of(2027, 1, 1))))
        assertFailsWith<IllegalArgumentException> { build(lib, LocalDate.of(2027, 1, 1)) }
    }

    @Test
    fun `repeats whose writes are not known stay with Things`() {
        // A deadline lead (ts), a deadline on the template, a checklist, or after completion.
        val lead = library(rr = rr().replace("\"ts\": 0", "\"ts\": -15"))
        val due = library(templateExtra = """, "dd": ${s(dec1) + 10 * day}""")
        val checklist = library(more = """, "item": {"t": 0, "e": "ChecklistItem3", "p": {"tt": "Step", "ts": ["tmpl"], "ix": 0, "ss": 0}}""")
        val after = library(rr = rr().replace("\"tp\": 0", "\"tp\": 1"))
        for (lib in listOf(lead, due, checklist)) {
            assertFalse(lib.canCompleteAhead(lib.repeatsAhead(today).first()))
            assertFailsWith<IllegalArgumentException> { build(lib) }
        }
        assertFailsWith<IllegalArgumentException> { build(after) }
    }

    @Test
    fun `a repeat with a deadline lead shows only its next occurrence ahead`() {
        // Daily, so without the rule every day of the next four weeks would show.
        val lead = library(rr = rr().replace("\"ts\": 0", "\"ts\": -15").replace("\"fu\": 8", "\"fu\": 16"))
        assertEquals(listOf(dec1), lead.repeatsAhead(today).map { it.startDate })
    }

    @Test
    fun `once sent, the copy shows checked on its day and the next occurrence moves on`() {
        val lib = library()
        val state = HistoryState().apply {
            apply(WireParser.parseItem(Json.parseToJsonElement("""{
                "home": {"t": 0, "e": "Area3", "p": {"tt": "Home", "ix": 0}},
                "tag": {"t": 0, "e": "Tag4", "p": {"tt": "Bills", "ix": 0}},
                "tmpl": {"t": 0, "e": "Task7", "p": {"tt": "Pay rent", "st": 2, "ix": -1785, "ti": 2677758, "ar": ["home"], "tg": ["tag"],
                    "icc": 3, "icsd": ${s(dec1) - 7 * day}, "tir": ${s(dec1)}, "rr": ${rr()}}}
            }""") as JsonObject))
            apply(WireParser.parseItem(JsonObject(build(lib))))
        }
        val after = ThingsLibrary.from(state)
        val copy = after.task("copy")!!
        assertEquals(TaskStatus.COMPLETED, copy.status)
        assertEquals(dec1, copy.startDate)
        assertEquals(listOf("tmpl"), copy.recurrenceTemplateIds)
        assertEquals(LocalDate.of(2027, 1, 1), after.repeatsAhead(today).first().startDate)
        // Unchecking it later is the plain reopen of a fixed-schedule occurrence.
        assertTrue(after.canComplete(copy))
    }
}
