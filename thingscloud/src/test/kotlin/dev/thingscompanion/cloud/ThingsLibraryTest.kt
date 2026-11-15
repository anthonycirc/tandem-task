package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ThingsLibraryTest {

    private val today = LocalDate.of(2026, 11, 15)
    private val day14 = 1_794_614_400L
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

    private fun List<Task>.ids() = map { it.id }

    @Test
    fun `inbox keeps open unfiled inbox todos only`() {
        val lib = library(
            "area" to """{"t": 0, "e": "Area3", "p": {"tt": "Work"}}""",
            "in" to task(""""tt": "Loose", "st": 0, "ix": 2"""),
            "in2" to task(""""tt": "First", "st": 0, "ix": 1"""),
            "filed" to task(""""tt": "Filed", "st": 0, "ar": ["area"]"""),
            "done" to task(""""tt": "Done", "st": 0, "ss": 3"""),
            "trashed" to task(""""tt": "Trashed", "st": 0, "tr": true"""),
            "blank" to task(""""tt": "  ", "st": 0"""),
        )

        assertEquals(listOf("in2", "in"), lib.inbox().ids())
    }

    @Test
    fun `today keeps dated anytime and someday todos up to today`() {
        val lib = library(
            "past" to task(""""tt": "Past", "st": 1, "sr": $day14"""),
            "now" to task(""""tt": "Now", "st": 2, "sr": $day15"""),
            "later" to task(""""tt": "Later", "st": 1, "sr": $day16"""),
            "inbox" to task(""""tt": "Inbox dated", "st": 0, "sr": $day15"""),
            "proj" to task(""""tt": "Project", "tp": 1, "st": 1, "sr": $day15"""),
            "head" to task(""""tt": "Heading", "tp": 2, "st": 1, "sr": $day15"""),
            "evening" to task(""""tt": "Evening", "st": 1, "sr": $day15, "sb": 1"""),
        )

        val ids = lib.today(today).ids().toSet()

        assertEquals(setOf("past", "now", "evening"), ids)
        assertEquals("evening", lib.today(today).last().id)
    }

    @Test
    fun `newer task entities like Task7 are listed, legacy Task4 are not`() {
        val lib = library(
            "new" to """{"t": 0, "e": "Task7", "p": {"tt": "New", "st": 1, "sr": $day15}}""",
            "legacy" to """{"t": 0, "e": "Task4", "p": {"tt": "Old", "st": 1, "sr": $day15}}""",
        )

        assertEquals(listOf("new"), lib.today(today).ids())
    }

    @Test
    fun `upcoming keeps items scheduled after today in date order`() {
        val lib = library(
            "soon" to task(""""tt": "Soon", "st": 2, "sr": $day16, "ix": 5"""),
            "proj" to task(""""tt": "Project later", "tp": 1, "st": 1, "sr": $day16, "ix": 1"""),
            "now" to task(""""tt": "Now", "st": 1, "sr": $day15"""),
            "template" to task(""""tt": "Repeat", "st": 1, "sr": $day16, "rr": {"tp": 0}"""),
        )

        assertEquals(listOf("proj", "soon"), lib.upcoming(today).ids())
    }

    @Test
    fun `anytime hides tasks of someday or future projects`() {
        val lib = library(
            "active" to task(""""tt": "Active", "tp": 1, "st": 1"""),
            "deferred" to task(""""tt": "Deferred", "tp": 1, "st": 2"""),
            "a" to task(""""tt": "In active", "st": 1, "pr": ["active"], "ix": 1"""),
            "b" to task(""""tt": "In deferred", "st": 1, "pr": ["deferred"]"""),
            "loose" to task(""""tt": "Loose", "st": 1, "ix": 2"""),
            "future" to task(""""tt": "Future", "st": 1, "sr": $day16"""),
        )

        assertEquals(listOf("a", "loose"), lib.anytime(today).ids())
    }

    @Test
    fun `someday keeps undated someday projects and unparented todos`() {
        val lib = library(
            "proj" to task(""""tt": "Dream", "tp": 1, "st": 2, "ix": 1"""),
            "child" to task(""""tt": "Child", "st": 2, "pr": ["proj"]"""),
            "loose" to task(""""tt": "Maybe", "st": 2, "ix": 2"""),
            "dated" to task(""""tt": "Dated", "st": 2, "sr": $day16"""),
        )

        assertEquals(listOf("proj", "loose"), lib.someday().ids())
    }

    @Test
    fun `tasks under a heading belong to the heading's project and area`() {
        val lib = library(
            "area" to """{"t": 0, "e": "Area3", "p": {"tt": "Home", "ix": 0}}""",
            "proj" to task(""""tt": "Move", "tp": 1, "st": 1, "ar": ["area"]"""),
            "head" to task(""""tt": "Boxes", "tp": 2, "pr": ["proj"]"""),
            "t" to task(""""tt": "Buy tape", "st": 1, "agr": ["head"]"""),
        )

        val task = lib.task("t")!!
        assertEquals("proj", task.projectId)
        assertEquals("area", task.areaId)
        assertEquals("head", task.headingId)
        assertEquals(listOf("t"), lib.projectTasks("proj").ids())
        assertEquals("Home", lib.areas().single().title)
    }

    @Test
    fun `projects lists open untrashed projects with their area`() {
        val lib = library(
            "p1" to task(""""tt": "B project", "tp": 1, "st": 1, "ix": 2"""),
            "p2" to task(""""tt": "A project", "tp": 1, "st": 1, "ix": 1"""),
            "p3" to task(""""tt": "Closed", "tp": 1, "st": 1, "ss": 3"""),
            "p4" to task(""""tt": "Gone", "tp": 1, "st": 1, "tr": true"""),
        )

        assertEquals(listOf("p2", "p1"), lib.projects().ids())
    }

    @Test
    fun `task exposes notes, dates, tags and checklist`() {
        val lib = library(
            "tag" to """{"t": 0, "e": "Tag4", "p": {"tt": "urgent"}}""",
            "t" to task(""""tt": "Pay", "st": 1, "sr": $day15, "dd": $day16, "tg": ["tag"], "nt": {"_t": "tx", "t": 1, "v": "IBAN"}"""),
            "c2" to """{"t": 0, "e": "ChecklistItem3", "p": {"tt": "Second", "ts": ["t"], "ix": 2}}""",
            "c1" to """{"t": 0, "e": "ChecklistItem3", "p": {"tt": "First", "ts": ["t"], "ix": 1, "ss": 3}}""",
        )

        val task = lib.task("t")!!
        assertEquals("IBAN", task.notes)
        assertEquals(today, task.startDate)
        assertEquals(today.plusDays(1), task.deadline)
        assertEquals(listOf("urgent"), lib.tagTitles(task))
        assertEquals(listOf("First" to true, "Second" to false), lib.checklist("t").map { it.title to it.done })
        assertNull(lib.task("missing"))
    }

    @Test
    fun `to-dos closed today stay in their list when asked, earlier ones do not`() {
        val utc = java.time.ZoneOffset.UTC
        val noonToday = day15 + 12 * 3600
        val lib = library(
            "open" to task(""""tt": "Open", "st": 1, "sr": $day15, "ix": 1"""),
            "doneToday" to task(""""tt": "Done today", "st": 1, "sr": $day15, "ss": 3, "sp": $noonToday, "ix": 2"""),
            "cancelledToday" to task(""""tt": "Cancelled", "st": 0, "ss": 2, "sp": $noonToday"""),
            "doneYesterday" to task(""""tt": "Done before", "st": 1, "sr": $day14, "ss": 3, "sp": ${day14 + 3600}"""),
        )

        assertEquals(listOf("open"), lib.today(today).ids())
        assertEquals(listOf("open", "doneToday"), lib.today(today, closedOn = today, zone = utc).ids())
        assertEquals(listOf("cancelledToday"), lib.inbox(closedOn = today, zone = utc).ids())
        assertEquals(java.time.Instant.ofEpochSecond(noonToday), lib.task("doneToday")!!.stopDate)
    }
}
