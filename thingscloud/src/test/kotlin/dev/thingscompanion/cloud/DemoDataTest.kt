package dev.thingscompanion.cloud

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertTrue

class DemoDataTest {

    private val today = LocalDate.of(2026, 11, 15)

    @Test
    fun `demo data fills every list with believable content`() {
        val library = ThingsLibrary.from(DemoData.state(today, nowSeconds = 1_794_740_000.0))

        assertTrue(library.today(today).size >= 5)
        assertTrue(library.today(today).any { it.evening })
        assertTrue(library.today(today).any { it.reminder != null })
        assertTrue(library.today(today).any { it.notes != null })
        assertTrue(library.inbox().size >= 3)
        assertTrue(library.upcoming(today).size >= 3)
        assertTrue(library.anytime(today).size >= 3)
        assertTrue(library.someday().size >= 2)
        assertTrue(library.projects().size >= 3)
        assertTrue(library.tags().size >= 3)
        assertTrue(library.today(today).any { library.tagTitles(it).isNotEmpty() })
        assertTrue(library.today(today, closedOn = today, zone = java.time.ZoneOffset.UTC).any { !it.isOpen })
        assertTrue(library.checklist(library.today(today).first { library.checklist(it.id).isNotEmpty() }.id).size >= 3)
    }
}
