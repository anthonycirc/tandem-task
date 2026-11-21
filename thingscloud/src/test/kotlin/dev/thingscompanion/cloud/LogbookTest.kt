package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/** Things' Logbook: what was completed or cancelled, most recently closed first. */
class LogbookTest {

    private val day15 = 1_794_700_800L
    private val day16 = 1_794_787_200L

    private fun library(vararg objects: Pair<String, String>): ThingsLibrary {
        val state = HistoryState()
        for ((uuid, body) in objects) {
            val item = Json.parseToJsonElement("""{"$uuid": $body}""") as JsonObject
            state.apply(WireParser.parseItem(item))
        }
        return ThingsLibrary.from(state)
    }

    private fun task(props: String) = """{"t": 0, "e": "Task6", "p": {$props}}"""

    @Test
    fun `the logbook lists completed and cancelled to-dos and projects, latest first`() {
        val lib = library(
            "older" to task(""""tt": "Older", "st": 1, "ss": 3, "sp": ${day15 + 3600}"""),
            "cancelled" to task(""""tt": "Cancelled", "st": 1, "ss": 2, "sp": ${day16 + 60}"""),
            "project" to task(""""tt": "Project", "tp": 1, "st": 1, "ss": 3, "sp": ${day16 + 7200}"""),
            "open" to task(""""tt": "Open", "st": 1"""),
        )

        assertEquals(listOf("project", "cancelled", "older"), lib.logbook().map { it.id })
    }

    @Test
    fun `the logbook leaves out the Trash, headings and to-dos of a trashed project`() {
        val lib = library(
            "trashed" to task(""""tt": "Trashed", "st": 1, "ss": 3, "sp": $day16, "tr": true"""),
            "heading" to task(""""tt": "Heading", "tp": 2, "st": 1, "ss": 3, "sp": $day16"""),
            "gone" to task(""""tt": "Gone", "tp": 1, "st": 1, "tr": true"""),
            "inGone" to task(""""tt": "In gone", "st": 1, "ss": 3, "sp": $day16, "pr": ["gone"]"""),
            "kept" to task(""""tt": "Kept", "st": 1, "ss": 3, "sp": $day15"""),
        )

        assertEquals(listOf("kept"), lib.logbook().map { it.id })
    }
}
