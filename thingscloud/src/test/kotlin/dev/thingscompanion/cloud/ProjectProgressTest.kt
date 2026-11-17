package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/** The share of a project's to-dos that are done, as Things fills its project circle. */
class ProjectProgressTest {

    private fun library(json: String) = ThingsLibrary.from(HistoryState().apply {
        apply(WireParser.parseItem(Json.parseToJsonElement(json) as JsonObject))
    })

    @Test
    fun `completed and cancelled to-dos count as done, trashed ones and headings do not count`() {
        val lib = library("""{
            "p": {"t": 0, "e": "Task7", "p": {"tt": "Move", "tp": 1, "st": 1}},
            "h": {"t": 0, "e": "Task7", "p": {"tt": "Phase", "tp": 2, "st": 1, "pr": ["p"]}},
            "open": {"t": 0, "e": "Task7", "p": {"tt": "A", "st": 1, "pr": ["p"]}},
            "underHeading": {"t": 0, "e": "Task7", "p": {"tt": "B", "st": 1, "agr": ["h"]}},
            "done": {"t": 0, "e": "Task7", "p": {"tt": "C", "st": 1, "pr": ["p"], "ss": 3, "sp": 1}},
            "cancelled": {"t": 0, "e": "Task7", "p": {"tt": "D", "st": 1, "agr": ["h"], "ss": 2, "sp": 1}},
            "trashed": {"t": 0, "e": "Task7", "p": {"tt": "E", "st": 1, "pr": ["p"], "ss": 3, "sp": 1, "tr": true}}
        }""")
        assertEquals(0.5f, lib.projectProgress("p"))
    }

    @Test
    fun `an empty project is not started`() {
        val lib = library("""{"p": {"t": 0, "e": "Task7", "p": {"tt": "Move", "tp": 1, "st": 1}}}""")
        assertEquals(0f, lib.projectProgress("p"))
    }
}
