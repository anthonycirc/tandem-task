package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RepeatCompleteTest {

    private val day = 86_400L
    private val d15 = 1_794_700_800L
    // Noon UTC on day 15: the same calendar day in any zone from UTC-11 to UTC+11.
    private val now = d15 + 43_200 + 0.5

    private fun library(rr: String, instanceExtra: String = "") = ThingsLibrary.from(
        HistoryState().apply {
            apply(WireParser.parseItem(Json.parseToJsonElement(
                """{"tmpl": {"t": 0, "e": "Task7", "p": {"tt": "Water", "st": 1, "rr": $rr, "acrd": ${d15 - 3 * day}, "tir": $d15}},
                    "inst": {"t": 0, "e": "Task6", "p": {"tt": "Water", "st": 1, "sr": $d15, "tir": $d15, "rt": ["tmpl"]$instanceExtra}}}""",
            ) as JsonObject))
        },
    )

    private fun complete(rr: String) = PendingChange.SetDone("op", "inst", true).build(library(rr), now)!!

    private fun props(payload: Map<String, JsonObject>, id: String) = payload.getValue(id)["p"]!!.jsonObject

    @Test
    fun `checking an after-completion instance writes it and its template in one item, as Things does`() {
        val payload = complete("""{"tp": 1, "fu": 16, "fa": 1}""")

        assertEquals(setOf("inst", "tmpl"), payload.keys)
        assertEquals(setOf("ss", "sp", "md"), props(payload, "inst").keys)
        assertEquals(JsonPrimitive(3), props(payload, "inst")["ss"])
        assertEquals("\"Task6\"", payload.getValue("inst")["e"].toString())
        // The template: day of completion at midnight, next start one interval later; no md.
        assertEquals<Map<String, Any>>(mapOf("acrd" to JsonPrimitive(d15), "tir" to JsonPrimitive(d15 + day)), props(payload, "tmpl"))
        assertEquals("\"Task7\"", payload.getValue("tmpl")["e"].toString())
        assertEquals(1, payload.getValue("tmpl")["t"].toString().toInt())
    }

    @Test
    fun `the next start is the interval in days or weeks after the completion day`() {
        assertEquals(JsonPrimitive(d15 + 2 * day), props(complete("""{"tp": 1, "fu": 16, "fa": 2}"""), "tmpl")["tir"])
        assertEquals(JsonPrimitive(d15 + 7 * day), props(complete("""{"tp": 1, "fu": 256, "fa": 1}"""), "tmpl")["tir"])
        assertEquals(JsonPrimitive(d15 + 14 * day), props(complete("""{"tp": 1, "fu": 256, "fa": 2}"""), "tmpl")["tir"])
    }

    @Test
    fun `monthly after-completion repeats move on by months, as Things wrote it for R13`() {
        assertEquals(JsonPrimitive(d15 + 30 * day), props(complete("""{"tp": 1, "fu": 8, "fa": 1}"""), "tmpl")["tir"])
        assertEquals(JsonPrimitive(d15 + 61 * day), props(complete("""{"tp": 1, "fu": 8, "fa": 2}"""), "tmpl")["tir"])
        assertEquals(JsonPrimitive(d15), props(complete("""{"tp": 1, "fu": 8, "fa": 1}"""), "tmpl")["acrd"])
    }

    @Test
    fun `yearly after-completion repeats move on by years, as Things wrote it for Y3`() {
        assertEquals(JsonPrimitive(d15 + 365 * day), props(complete("""{"tp": 1, "fu": 4, "fa": 1}"""), "tmpl")["tir"])
    }

    @Test
    fun `after-completion repeats in units the app does not know stay with Things`() {
        val lib = library("""{"tp": 1, "fu": 64, "fa": 1}""")
        assertFalse(lib.canComplete(lib.task("inst")!!))
        assertFailsWith<IllegalArgumentException> { PendingChange.SetDone("op", "inst", true).build(lib, now) }
        val daily = library("""{"tp": 1, "fu": 16, "fa": 1}""")
        assertTrue(daily.canComplete(daily.task("inst")!!))
    }

    @Test
    fun `a checked after-completion instance is not unchecked from the app`() {
        val done = library("""{"tp": 1, "fu": 16, "fa": 1}""", instanceExtra = """, "ss": 3, "sp": $now""")
        assertFalse(done.canComplete(done.task("inst")!!))
        assertFailsWith<IllegalArgumentException> { PendingChange.SetDone("op", "inst", false).build(done, now) }
    }

    @Test
    fun `an after-completion instance can be checked and trashed from the app`() {
        val lib = library("""{"tp": 1, "fu": 16, "fa": 1}""")
        assertTrue(lib.canComplete(lib.task("inst")!!))
        // Trashing moves its template on, as Things does (S6, 2026-11-17; RepeatSwipeTest).
        assertTrue(lib.canTrash(lib.task("inst")!!))
    }

    @Test
    fun `the completion day is the day of the tap, even when sent after midnight`() {
        val tappedOn = d15 / day
        val sentNextDay = d15 + day + 3_600 + 0.5
        val payload = PendingChange.SetDone("op", "inst", true, day = tappedOn).build(library("""{"tp": 1, "fu": 16, "fa": 1}"""), sentNextDay)!!
        assertEquals(JsonPrimitive(d15), props(payload, "tmpl")["acrd"])
        assertEquals(JsonPrimitive(d15 + day), props(payload, "tmpl")["tir"])
    }
}
