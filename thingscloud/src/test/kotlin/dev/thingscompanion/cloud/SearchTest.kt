package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchTest {

    private val seed = """{
        "area": {"t": 0, "e": "Area3", "p": {"tt": "Maison", "ix": 0}},
        "proj": {"t": 0, "e": "Task7", "p": {"tt": "Déménagement", "tp": 1, "st": 1, "ix": 0}},
        "a": {"t": 0, "e": "Task7", "p": {"tt": "Appeler le déménageur", "st": 1, "ix": 2}},
        "b": {"t": 0, "e": "Task7", "p": {"tt": "Cartons", "st": 1, "ix": 1, "nt": {"_t": "tx", "t": 1, "ch": 0, "v": "acheter chez le demenageur"}}},
        "done": {"t": 0, "e": "Task7", "p": {"tt": "Devis déménageur", "st": 1, "ss": 3, "sp": 1794700800}},
        "trash": {"t": 0, "e": "Task7", "p": {"tt": "Déménageur ancien", "st": 1, "tr": true}},
        "head": {"t": 0, "e": "Task7", "p": {"tt": "Déménageur", "tp": 2, "st": 1, "pr": ["proj"]}},
        "legacy": {"t": 0, "e": "Task4", "p": {"tt": "Déménageur legacy", "st": 1}}
    }"""

    private val library = ThingsLibrary.from(HistoryState().apply {
        apply(WireParser.parseItem(Json.parseToJsonElement(seed) as JsonObject))
    })

    @Test
    fun `finds to-dos and projects by title or notes, ignoring case and accents, open ones first`() {
        assertEquals(listOf("proj", "b", "a", "done"), library.search("DEMENAG").tasks.map { it.id })
    }

    @Test
    fun `finds areas by title`() {
        assertEquals(listOf("area"), library.search("mais").areas.map { it.id })
    }

    @Test
    fun `a blank query finds nothing`() {
        val result = library.search("  ")
        assertEquals(emptyList(), result.tasks)
        assertEquals(emptyList(), result.areas)
    }
}
