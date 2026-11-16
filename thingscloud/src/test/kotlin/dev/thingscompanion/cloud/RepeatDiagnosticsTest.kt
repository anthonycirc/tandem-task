package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RepeatDiagnosticsTest {

    private fun item(json: String) = Json.parseToJsonElement(json) as JsonObject

    private val day = 86_400L
    private val d0 = 1_794_700_800L

    private val history = listOf(
        item("""{"tmpl": {"t": 0, "e": "Task7", "p": {"tt": "Secret title", "nt": "secret notes", "rr": {"tp": 1, "fu": 16, "fa": 2, "ia": $d0}}}}"""),
        item("""{"i1": {"t": 0, "e": "Task7", "p": {"tt": "Secret title", "rt": ["tmpl"], "sr": $d0, "ss": 0}}}"""),
        item("""{"other": {"t": 0, "e": "Task7", "p": {"tt": "Unrelated"}}}"""),
        // Completing i1 on d0 + 1 day (+ some hours), the template moves on, the next instance is created.
        item(
            """{"i1": {"t": 1, "e": "Task7", "p": {"ss": 3, "sp": ${d0 + day + 3600}.5, "md": ${d0 + day + 3600}.5}},
                "tmpl": {"t": 1, "e": "Task7", "p": {"acrd": ${d0 + day}, "rr": {"tp": 1, "fu": 16, "fa": 2, "ia": ${d0 + 3 * day}}}},
                "i2": {"t": 0, "e": "Task7", "p": {"tt": "Secret title", "rt": ["tmpl"], "sr": ${d0 + 3 * day}, "ss": 0}}}""",
        ),
    )

    @Test
    fun `reports the shape of writes around after-completion repeats, with no content`() {
        val report = RepeatDiagnostics.analyze(history)
        val text = report.toString()

        assertFalse("Secret" in text)
        assertFalse("secret" in text)
        assertFalse("Unrelated" in text)
        assertEquals(1, report["afterCompletionTemplates"].toString().toInt())

        val events = report["events"].toString()
        // The completion item: instance update, template update, new instance create, in one item.
        assertTrue("\"instance\"" in events && "\"template\"" in events && "\"new-instance\"" in events)
        assertTrue("acrd" in events && "rr.ia" in events)
        // Dates are given in days relative to the completion (sp) of the same item.
        assertTrue("\"sr\":\"+2d\"" in events, events)
        assertTrue("\"acrd\":\"+0d\"" in events, events)
    }

    @Test
    fun `items not about after-completion repeats are left out`() {
        val report = RepeatDiagnostics.analyze(history)
        assertFalse("other" in report["events"].toString())
    }

    @Test
    fun `summarizes completion values and when the next instance comes, per rule`() {
        val withLaterInstance = history.dropLast(1) + listOf(
            item(
                """{"i1": {"t": 1, "e": "Task7", "p": {"ss": 3, "sp": ${d0 + day + 3600}.5, "md": ${d0 + day + 3600}.5}},
                    "tmpl": {"t": 1, "e": "Task7", "p": {"acrd": ${d0 + day}, "tir": ${d0 + 3 * day}}}}""",
            ),
            item("""{"i2": {"t": 0, "e": "Task7", "p": {"tt": "Secret", "rt": ["tmpl"], "sr": ${d0 + 3 * day}, "tir": ${d0 + 3 * day}}},
                    "tmpl": {"t": 1, "e": "Task7", "p": {"acrd": ${d0 + day}, "icc": 1}}}"""),
        )
        val report = RepeatDiagnostics.analyze(withLaterInstance)

        val completions = report["completions"].toString()
        assertTrue("\"rule\":\"16x2\"" in completions, completions)
        assertTrue("\"acrd\":\"+0d\"" in completions && "\"tir\":\"+2d\"" in completions, completions)
        assertTrue("\"acrdAtMidnight\":true" in completions, completions)
        val next = report["nextInstances"].toString()
        assertTrue("\"srFromCompletion\":\"+2d\"" in next && "\"tirFromCompletion\":\"+2d\"" in next, next)
        assertFalse("Secret" in report.toString())
    }
}
