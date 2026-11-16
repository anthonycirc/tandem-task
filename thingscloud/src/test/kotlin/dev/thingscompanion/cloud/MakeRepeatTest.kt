package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.time.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pinned to the items Things 3 wrote on 2026-11-16 when the user made R1-R4 repeat (repeat-rules.md L5). */
class MakeRepeatTest {

    private val day = 86_400L
    private val d16 = 1_794_787_200L
    private val now = d16 + 50_000 + 0.5

    private val seed = """{
        "tag": {"t": 0, "e": "Tag4", "p": {"tt": "Home"}},
        "proj": {"t": 0, "e": "Task7", "p": {"tt": "Move", "tp": 1, "st": 1}},
        "r": {"t": 0, "e": "Task7", "p": {"tt": "R3", "st": 1, "sr": $d16, "tir": $d16, "ix": -1687, "ti": -967, "ato": 54000, "tg": ["tag"], "pr": ["proj"]}},
        "c1": {"t": 0, "e": "ChecklistItem3", "p": {"tt": "One", "ts": ["r"], "ix": -622, "ss": 0}},
        "c2": {"t": 0, "e": "ChecklistItem3", "p": {"tt": "Two", "ts": ["r"], "ix": 0, "ss": 3}},
        "undated": {"t": 0, "e": "Task7", "p": {"tt": "Someday", "st": 2}},
        "deadline": {"t": 0, "e": "Task7", "p": {"tt": "Due", "st": 1, "sr": $d16, "dd": ${d16 + 10 * day}}}
    }"""

    private fun library() = ThingsLibrary.from(HistoryState().apply { apply(WireParser.parseItem(Json.parseToJsonElement(seed) as JsonObject)) })

    private val weeklyAfter = RepeatRule(RepeatUnit.WEEK, every = 1, afterCompletion = true)

    private fun make(taskId: String = "r", rule: RepeatRule = weeklyAfter) =
        PendingChange.MakeRepeating("op", taskId, "T", rule, listOf("n1", "n2")).build(library(), now)

    private fun p(payload: Map<String, JsonObject>, id: String) = payload.getValue(id)["p"]!!.jsonObject

    @Test
    fun `the to-do only gets a link to its new template, with no md`() {
        val payload = make()!!
        assertEquals(1, payload.getValue("r")["t"].toString().toInt())
        assertEquals<Map<String, Any>>(mapOf("rt" to JsonArray(listOf(JsonPrimitive("T")))), p(payload, "r"))
    }

    @Test
    fun `the template is a copy of the to-do, undated, with the rule starting on its day`() {
        val t = p(make()!!, "T")
        assertEquals("\"Task7\"", make()!!.getValue("T")["e"].toString())
        assertEquals(JsonPrimitive("R3"), t["tt"])
        assertEquals(JsonArray(listOf(JsonPrimitive("tag"))), t["tg"])
        assertEquals(JsonPrimitive(54000), t["ato"])
        assertEquals(JsonArray(listOf(JsonPrimitive("proj"))), t["pr"])
        assertEquals(JsonPrimitive(-1687), t["ix"])
        assertEquals(JsonPrimitive(2), t["st"])
        assertEquals(JsonNull, t["sr"])
        assertEquals(JsonNull, t["tir"])
        assertEquals(JsonNull, t["acrd"])
        assertEquals(JsonPrimitive(d16 + day), t["icsd"])
        assertEquals(JsonPrimitive(1), t["icc"])
        assertEquals(JsonArray(emptyList()), t["rt"])
        assertEquals(
            Json.parseToJsonElement("""{"ia": 0, "of": [], "tp": 1, "ts": 0, "fu": 256, "rrv": 4, "fa": 1, "rc": 0, "sr": $d16, "ed": 64092211200}"""),
            t["rr"],
        )
        assertTrue("xx" in t && "nt" in t && "cd" in t)
    }

    @Test
    fun `the checklist is copied onto the template, open, as Things copies it`() {
        val payload = make()!!
        val copies = listOf("n1", "n2").map { p(payload, it) }
        assertEquals(listOf("One", "Two"), copies.map { it["tt"]!!.toString().trim('"') })
        assertEquals(listOf(-622, 0), copies.map { it["ix"]!!.toString().toInt() })
        copies.forEach { copy ->
            assertEquals(JsonArray(listOf(JsonPrimitive("T"))), copy["ts"])
            assertEquals(JsonPrimitive(0), copy["ss"])
            assertEquals(JsonPrimitive(false), copy["lt"])
            assertEquals(JsonNull, copy["sp"])
        }
    }

    @Test
    fun `undated to-dos, to-dos with a deadline, and months stay with Things`() {
        assertFailsWith<IllegalArgumentException> { make("undated") }
        assertFailsWith<IllegalArgumentException> { make("deadline") }
        assertFailsWith<IllegalArgumentException> { make(rule = RepeatRule(RepeatUnit.MONTH, every = 1, afterCompletion = true, monthDay = 1)) }
        assertFailsWith<IllegalArgumentException> { make(rule = RepeatRule(RepeatUnit.MONTH, every = 1, afterCompletion = false, monthDay = 16)) }
    }

    // Pinned to R6 and R7 (repeat-rules.md L10): made to repeat on Monday 2026-11-16, on a fixed schedule.

    @Test
    fun `every day on a fixed schedule, as Things wrote it for R6`() {
        val t = p(make(rule = RepeatRule(RepeatUnit.DAY, every = 1, afterCompletion = false))!!, "T")
        assertEquals(JsonPrimitive(d16 + day), t["tir"])
        assertEquals(JsonPrimitive(d16 + day), t["icsd"])
        assertEquals(JsonPrimitive(1), t["icc"])
        assertEquals(JsonNull, t["sr"])
        assertEquals(
            Json.parseToJsonElement("""{"ed": 64092211200, "fa": 1, "fu": 16, "ia": $d16, "of": [{"dy": 0}], "rc": 0, "rrv": 4, "sr": $d16, "tp": 0, "ts": 0}"""),
            t["rr"],
        )
    }

    @Test
    fun `every week on Monday and Thursday, as Things wrote it for R7`() {
        val rule = RepeatRule(RepeatUnit.WEEK, every = 1, afterCompletion = false, weekdays = listOf(DayOfWeek.THURSDAY, DayOfWeek.MONDAY))
        val t = p(make(rule = rule)!!, "T")
        assertEquals(JsonPrimitive(d16 + 3 * day), t["tir"])
        assertEquals(JsonPrimitive(d16 + 3 * day), t["icsd"])
        assertEquals(
            Json.parseToJsonElement("""{"ed": 64092211200, "fa": 1, "fu": 256, "ia": $d16, "of": [{"wd": 1}, {"wd": 4}], "rc": 0, "rrv": 4, "sr": $d16, "tp": 0, "ts": 0}"""),
            t["rr"],
        )
    }

    @Test
    fun `the next occurrence of a fixed schedule follows the interval`() {
        fun next(rule: RepeatRule) = p(make(rule = rule)!!, "T")["tir"]
        assertEquals(JsonPrimitive(d16 + 3 * day), next(RepeatRule(RepeatUnit.DAY, every = 3, afterCompletion = false)))
        assertEquals(JsonPrimitive(d16 + 14 * day), next(RepeatRule(RepeatUnit.WEEK, every = 2, afterCompletion = false, weekdays = listOf(DayOfWeek.MONDAY))))
        assertEquals(JsonPrimitive(d16 + 7 * day), next(RepeatRule(RepeatUnit.WEEK, every = 1, afterCompletion = false, weekdays = listOf(DayOfWeek.MONDAY))))
        assertEquals(JsonPrimitive(d16 + 6 * day), next(RepeatRule(RepeatUnit.WEEK, every = 1, afterCompletion = false, weekdays = listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY))))
    }

    @Test
    fun `weekly fixed schedules the app cannot place are refused`() {
        fun weekly(every: Int, vararg days: DayOfWeek) = RepeatRule(RepeatUnit.WEEK, every, afterCompletion = false, weekdays = days.toList())
        assertFailsWith<IllegalArgumentException> { make(rule = weekly(1)) }
        assertFailsWith<IllegalArgumentException> { make(rule = weekly(1, DayOfWeek.TUESDAY)) }
        assertFailsWith<IllegalArgumentException> { make(rule = weekly(2, DayOfWeek.MONDAY, DayOfWeek.THURSDAY)) }
        assertFailsWith<IllegalArgumentException> { make(rule = RepeatRule(RepeatUnit.WEEK, 1, afterCompletion = true, weekdays = listOf(DayOfWeek.MONDAY))) }
    }

    @Test
    fun `sent again once it applied, nothing more is written`() {
        val lib = ThingsLibrary.from(HistoryState().apply {
            apply(WireParser.parseItem(Json.parseToJsonElement(seed) as JsonObject))
            apply(WireParser.parseItem(JsonObject(make()!!)))
        })
        assertNull(PendingChange.MakeRepeating("op", "r", "T", weeklyAfter, listOf("n1", "n2")).build(lib, now))
        assertTrue(lib.task("r")!!.recurrenceTemplateIds == listOf("T"))
        assertTrue(lib.task("T")!!.isRecurrenceTemplate)
    }
}
