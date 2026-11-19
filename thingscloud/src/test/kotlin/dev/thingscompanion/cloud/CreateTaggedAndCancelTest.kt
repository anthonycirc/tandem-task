package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Tags set when creating a to-do or project, written in the create item as Things does (`tg`), and
 * a to-do cancelled or reopened from its checkbox, written as Things writes it: {ss, sp, md}.
 */
class CreateTaggedAndCancelTest {

    private val now = 1_794_902_503.25

    private val seed = """{
        "work": {"t": 0, "e": "Tag4", "p": {"tt": "Work", "ix": 2}},
        "home": {"t": 0, "e": "Tag4", "p": {"tt": "Home", "ix": 1}},
        "open": {"t": 0, "e": "Task7", "p": {"tt": "Call", "st": 1}},
        "cancelled": {"t": 0, "e": "Task7", "p": {"tt": "Skip", "st": 1, "ss": 2, "sp": 1794900000.5}},
        "done": {"t": 0, "e": "Task7", "p": {"tt": "Done", "st": 1, "ss": 3, "sp": 1794900000.5}},
        "tmpl": {"t": 0, "e": "Task7", "p": {"tt": "Water", "st": 2, "rr": {"tp": 1, "fu": 16, "fa": 1}}},
        "occ": {"t": 0, "e": "Task7", "p": {"tt": "Water", "st": 1, "rt": ["tmpl"]}}
    }"""

    private val library = ThingsLibrary.from(HistoryState().apply {
        apply(WireParser.parseItem(Json.parseToJsonElement(seed) as JsonObject))
    })

    private fun props(payload: Map<String, JsonObject>, id: String) = payload.getValue(id)["p"]!!.jsonObject

    private fun ids(vararg id: String) = JsonArray(id.map(::JsonPrimitive))

    @Test
    fun `a new to-do carries its tags in Things' tag order, unknown ones dropped`() {
        val payload = PendingChange.AddTask("op", "t1", "Pay", "", PendingChange.When("anytime"), tagIds = listOf("work", "nope", "home")).build(library, now)!!
        assertEquals(ids("home", "work"), props(payload, "t1")["tg"])
    }

    @Test
    fun `a new project carries its tags, and none means an empty list as before`() {
        val tagged = PendingChange.AddProject("op", "p1", "Garden", "", PendingChange.When("anytime"), tagIds = listOf("home")).build(library, now)!!
        assertEquals(ids("home"), props(tagged, "p1")["tg"])
        val plain = PendingChange.AddProject("op", "p2", "Shed", "", PendingChange.When("anytime")).build(library, now)!!
        assertEquals(ids(), props(plain, "p2")["tg"])
    }

    @Test
    fun `cancelling an open to-do writes ss 2 with its stop date`() {
        val payload = PendingChange.SetCancelled("op", "open", true).build(library, now)!!
        assertEquals(
            Json.parseToJsonElement("""{"t": 1, "e": "Task7", "p": {"ss": 2, "sp": 1794902503.25, "md": 1794902503.25}}"""),
            payload.getValue("open"),
        )
        assertEquals(1, payload.size)
    }

    @Test
    fun `reopening a cancelled to-do clears its status and stop date`() {
        val payload = PendingChange.SetCancelled("op", "cancelled", false).build(library, now)!!
        val p = props(payload, "cancelled")
        assertEquals(JsonPrimitive(0), p["ss"])
        assertEquals(JsonNull, p["sp"])
    }

    @Test
    fun `nothing is sent when the to-do already shows it`() {
        assertNull(PendingChange.SetCancelled("op", "cancelled", true).build(library, now))
        assertNull(PendingChange.SetCancelled("op", "open", false).build(library, now))
        // Reopening only undoes a cancel: a completed to-do is left as it is.
        assertNull(PendingChange.SetCancelled("op", "done", false).build(library, now))
    }

    @Test
    fun `an after completion occurrence is refused, its template would have to move on`() {
        assertFailsWith<IllegalArgumentException> { PendingChange.SetCancelled("op", "occ", true).build(library, now) }
    }

    @Test
    fun `a cancel replaces a queued check of the same to-do, and the other way round`() {
        val check = PendingChange.SetDone("a", "open", true)
        val cancel = PendingChange.SetCancelled("b", "open", true)
        assertEquals(listOf<PendingChange>(cancel), PendingChanges.enqueue(listOf(check), cancel))
        assertEquals(listOf<PendingChange>(check), PendingChanges.enqueue(listOf(cancel), check))
    }

    @Test
    fun `unchecking a cancelled to-do reopens it`() {
        val payload = PendingChange.SetDone("op", "cancelled", false).build(library, now)!!
        assertEquals(JsonPrimitive(0), props(payload, "cancelled")["ss"])
    }

    @Test
    fun `the queued cancel and tagged creates survive the stored queue`() {
        val queue = listOf(
            PendingChange.SetCancelled("a", "open", true),
            PendingChange.AddTask("b", "t1", "Pay", "", PendingChange.When("anytime"), tagIds = listOf("home")),
            PendingChange.AddProject("c", "p1", "Garden", "", PendingChange.When("anytime"), tagIds = listOf("work")),
        )
        assertEquals(queue, PendingChanges.fromJson(PendingChanges.toJson(queue)))
    }
}
