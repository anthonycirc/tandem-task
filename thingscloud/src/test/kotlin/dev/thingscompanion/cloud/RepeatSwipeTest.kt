package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Trashing and rescheduling repeating to-dos, as Things 3 writes them (history samples, 2026-11-17). */
class RepeatSwipeTest {

    private val now = 1_794_902_503.25
    private val day17 = 1_794_873_600L

    private val seed = """{
        "after": {"t": 0, "e": "Task7", "p": {"tt": "Water", "st": 2, "rr": {"tp": 1, "fu": 16, "fa": 2, "of": [], "ia": 0, "rrv": 4, "rc": 0, "ts": 0, "ed": 64092211200}, "acrd": null, "tir": null}},
        "afterInst": {"t": 0, "e": "Task7", "p": {"tt": "Water", "st": 1, "sr": $day17, "tir": $day17, "rt": ["after"]}},
        "fixed": {"t": 0, "e": "Task7", "p": {"tt": "Bins", "st": 2, "rr": {"tp": 0, "fu": 16, "fa": 1, "of": [{"dy": 0}], "ia": $day17, "rrv": 4, "rc": 0, "ts": 0, "ed": 64092211200}}},
        "fixedInst": {"t": 0, "e": "Task6", "p": {"tt": "Bins", "st": 1, "sr": $day17, "tir": $day17, "rt": ["fixed"]}}
    }"""

    private fun library(vararg more: Map<String, JsonObject>) = ThingsLibrary.from(HistoryState().apply {
        apply(WireParser.parseItem(Json.parseToJsonElement(seed) as JsonObject))
        more.forEach { apply(WireParser.parseItem(JsonObject(it))) }
    })

    private val day = LocalDate.of(2026, 11, 17).toEpochDay()

    @Test
    fun `trashing an after-completion occurrence moves its template on, as checking it does`() {
        val payload = PendingChange.SetTrashed("op", "afterInst", true, day = day).build(library(), now)!!
        assertEquals(Json.parseToJsonElement("""{"t": 1, "e": "Task7", "p": {"tr": true, "md": 1794902503.25}}"""), payload.getValue("afterInst"))
        assertEquals(Json.parseToJsonElement("""{"t": 1, "e": "Task7", "p": {"acrd": $day17, "tir": ${day17 + 2 * 86_400}}}"""), payload.getValue("after"))
    }

    @Test
    fun `restoring it clears the template's dates, as Things does`() {
        val trashed = library(PendingChange.SetTrashed("op", "afterInst", true, day = day).build(library(), now)!!)
        val payload = PendingChange.SetTrashed("op2", "afterInst", false, day = day).build(trashed, now)!!
        assertEquals(Json.parseToJsonElement("""{"t": 1, "e": "Task7", "p": {"acrd": null, "tir": null}}"""), payload.getValue("after"))
        assertEquals(2, payload.size)
    }

    @Test
    fun `a fixed-schedule occurrence is trashed alone, as before`() {
        val payload = PendingChange.SetTrashed("op", "fixedInst", true, day = day).build(library(), now)!!
        assertEquals(setOf("fixedInst"), payload.keys)
    }

    @Test
    fun `occurrences can be swiped to the Trash or rescheduled, templates cannot`() {
        val lib = library()
        assertTrue(lib.canTrash(lib.task("afterInst")!!))
        assertTrue(lib.canTrash(lib.task("fixedInst")!!))
        assertTrue(lib.canReschedule(lib.task("afterInst")!!))
        assertTrue(lib.canReschedule(lib.task("fixedInst")!!))
        assertFalse(lib.canReschedule(lib.task("after")!!))
        // A repeating to-do's deadline follows its template: not set on one occurrence.
        assertFalse(lib.canSetDeadline(lib.task("fixedInst")!!))
    }

    @Test
    fun `rescheduling an occurrence writes only its own dates, its template untouched`() {
        val payload = PendingChange.EditTask("op", "fixedInst", "Bins", "", PendingChange.When.of(Schedule.OnDate(LocalDate.of(2026, 11, 20))))
            .build(library(), now)!!
        assertEquals(setOf("fixedInst"), payload.keys)
        val p = payload.getValue("fixedInst")["p"] as JsonObject
        // Things writes {sr, st, tir, md} (st when the list changes); never the rule or reminder.
        assertEquals(setOf("sr", "st", "tir", "md"), p.keys)
    }
}
