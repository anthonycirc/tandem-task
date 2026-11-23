package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/** A list's to-dos classed by project, each group under its project's (or area's) name. */
class GroupByProjectTest {

    private fun library(json: String) = ThingsLibrary.from(HistoryState().apply {
        apply(WireParser.parseItem(Json.parseToJsonElement(json) as JsonObject))
    })

    private val lib = library("""{
        "work": {"t": 0, "e": "Area3", "p": {"tt": "Work", "ix": 1}},
        "home": {"t": 0, "e": "Area3", "p": {"tt": "Home", "ix": 0}},
        "loose": {"t": 0, "e": "Task7", "p": {"tt": "Loose project", "tp": 1, "st": 1, "ix": 5}},
        "report": {"t": 0, "e": "Task7", "p": {"tt": "Report", "tp": 1, "st": 1, "ar": ["work"], "ix": 2}},
        "garden": {"t": 0, "e": "Task7", "p": {"tt": "Garden", "tp": 1, "st": 1, "ar": ["home"], "ix": 3}},
        "h": {"t": 0, "e": "Task7", "p": {"tt": "Phase", "tp": 2, "st": 1, "pr": ["report"]}},
        "a": {"t": 0, "e": "Task7", "p": {"tt": "A", "st": 1, "pr": ["report"], "ix": 1}},
        "b": {"t": 0, "e": "Task7", "p": {"tt": "B", "st": 1, "ix": 2}},
        "c": {"t": 0, "e": "Task7", "p": {"tt": "C", "st": 1, "agr": ["h"], "ix": 3}},
        "d": {"t": 0, "e": "Task7", "p": {"tt": "D", "st": 1, "pr": ["garden"], "ix": 4}},
        "e": {"t": 0, "e": "Task7", "p": {"tt": "E", "st": 1, "ar": ["home"], "ix": 5}},
        "f": {"t": 0, "e": "Task7", "p": {"tt": "F", "st": 1, "pr": ["loose"], "ix": 6}}
    }""")

    private fun ids(vararg id: String) = id.map { lib.task(it)!! }

    @Test
    fun `to-dos in no project come first, then the sidebar's order, keeping the list's order in each group`() {
        val groups = lib.byProject(ids("a", "b", "c", "d", "e", "f"))
        assertEquals(
            listOf(null to listOf("b"), "loose" to listOf("f"), "home" to listOf("e"), "garden" to listOf("d"), "report" to listOf("a", "c")),
            groups.map { (place, tasks) -> place to tasks.map { it.id } },
        )
    }

    @Test
    fun `places without a to-do in the list make no group`() {
        val groups = lib.byProject(ids("c", "a"))
        assertEquals(listOf("report" to listOf("c", "a")), groups.map { (place, tasks) -> place to tasks.map { it.id } })
    }
}
