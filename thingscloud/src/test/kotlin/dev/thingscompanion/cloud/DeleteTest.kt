package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeleteTest {

    private val now = 1_794_750_000.5
    private val today = LocalDate.of(2026, 11, 15)
    private val day15 = 1_794_700_800L

    private val seed = """{
        "home": {"t": 0, "e": "Area3", "p": {"tt": "Home", "ix": 1}},
        "proj": {"t": 0, "e": "Task7", "p": {"tt": "Move", "tp": 1, "st": 1, "ar": ["home"]}},
        "head": {"t": 0, "e": "Task6", "p": {"tt": "Boxes", "tp": 2, "pr": ["proj"], "st": 1}},
        "inProj": {"t": 0, "e": "Task7", "p": {"tt": "Pack", "st": 1, "pr": ["proj"], "sr": $day15, "tir": $day15}},
        "underHead": {"t": 0, "e": "Task7", "p": {"tt": "Tape", "st": 1, "agr": ["head"]}},
        "loose": {"t": 0, "e": "Task7", "p": {"tt": "Water", "st": 1, "ar": ["home"]}},
        "binned": {"t": 0, "e": "Task7", "p": {"tt": "Old", "st": 1, "ar": ["home"], "tr": true}},
        "elsewhere": {"t": 0, "e": "Task7", "p": {"tt": "Call", "st": 1}}
    }"""

    private fun state() = HistoryState().apply { apply(WireParser.parseItem(Json.parseToJsonElement(seed) as JsonObject)) }

    private fun library(vararg changes: Map<String, JsonObject>) =
        ThingsLibrary.from(state().apply { changes.forEach { apply(WireParser.parseItem(JsonObject(it))) } })

    @Test
    fun `a project goes to the Trash like a to-do, and comes back`() {
        val trashed = PendingChange.SetTrashed("op", "proj", true).build(library(), now)!!
        assertEquals(setOf("proj"), trashed.keys)
        assertEquals(JsonPrimitive(true), trashed.getValue("proj")["p"]!!.jsonObject["tr"])

        val lib = library(trashed)
        assertEquals(setOf("proj", "binned"), lib.trash().map { it.id }.toSet())
        assertTrue(lib.canRestore(lib.task("proj")!!))
    }

    @Test
    fun `the to-dos of a trashed project leave every list with it`() {
        val lib = library(PendingChange.SetTrashed("op", "proj", true).build(library(), now)!!)
        assertEquals(listOf("elsewhere"), lib.anytime(today).map { it.id }.filter { it != "loose" })
        assertTrue(lib.today(today).isEmpty())
        assertTrue(lib.projectSections("proj").all { it.tasks.isEmpty() })
        assertFalse(lib.projects().any { it.id == "proj" })
    }

    @Test
    fun `a heading goes to the Trash with its to-dos, and comes back with them`() {
        assertTrue(library().canTrash(library().task("head")!!))
        val trashed = PendingChange.SetTrashed("op", "head", true).build(library(), now)!!
        assertEquals(setOf("head"), trashed.keys)

        val lib = library(trashed)
        assertTrue(lib.projectSections("proj").none { it.heading?.id == "head" })
        assertFalse(lib.anytime(today).any { it.id == "underHead" })
        assertTrue(lib.trash().any { it.id == "head" })

        val restored = library(trashed, PendingChange.SetTrashed("op2", "head", false).build(lib, now)!!)
        assertTrue(restored.anytime(today).any { it.id == "underHead" })
    }

    @Test
    fun `deleting an area trashes its open projects and own to-dos, then deletes it, in one item`() {
        val payload = PendingChange.DeleteArea("op", "home").build(library(), now)!!

        assertEquals(setOf("home", "proj", "loose"), payload.keys)
        assertEquals(2, payload.getValue("home")["t"].toString().toInt())
        assertEquals("\"Area3\"", payload.getValue("home")["e"].toString())
        assertEquals(JsonObject(emptyMap()), payload.getValue("home")["p"])
        for (id in listOf("proj", "loose")) assertEquals(JsonPrimitive(true), payload.getValue(id)["p"]!!.jsonObject["tr"], id)

        val after = library(payload)
        assertTrue(after.areas().isEmpty())
        assertEquals(setOf("proj", "loose", "binned"), after.trash().map { it.id }.toSet())
    }

    @Test
    fun `an area already gone sends nothing`() {
        val gone = library(PendingChange.DeleteArea("op", "home").build(library(), now)!!)
        assertEquals(null, PendingChange.DeleteArea("op2", "home").build(gone, now))
    }

    @Test
    fun `a project whose area no longer exists is listed without an area`() {
        val gone = library(mapOf("home" to TaskEdits.delete("Area3")))
        assertEquals(null, gone.task("proj")!!.areaId)
    }
}
