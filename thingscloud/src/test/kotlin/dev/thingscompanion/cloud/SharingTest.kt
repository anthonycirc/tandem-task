package dev.thingscompanion.cloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SharingTest {

    @Test
    fun `a shared page gives its subject as the title and its link as the notes`() {
        assertEquals(
            Sharing.Draft("Kotlin 2.1 released", "https://kotlinlang.org/news"),
            Sharing.draft(subject = "Kotlin 2.1 released", text = "https://kotlinlang.org/news"),
        )
    }

    @Test
    fun `shared text without a subject gives its first line as the title and the rest as the notes`() {
        assertEquals(
            Sharing.Draft("Buy milk", "2 litres\nfrom the market"),
            Sharing.draft(subject = null, text = "  Buy milk \n\n2 litres\nfrom the market\n"),
        )
        assertEquals(Sharing.Draft("https://example.com", ""), Sharing.draft(subject = "", text = "https://example.com"))
    }

    @Test
    fun `text that only repeats the subject is not copied into the notes`() {
        assertEquals(Sharing.Draft("Call Ana", ""), Sharing.draft(subject = "Call Ana", text = "Call Ana"))
        assertEquals(Sharing.Draft("Call Ana", ""), Sharing.draft(subject = "Call Ana", text = null))
    }

    @Test
    fun `nothing to share gives no draft`() {
        assertNull(Sharing.draft(subject = null, text = null))
        assertNull(Sharing.draft(subject = " ", text = "\n "))
    }

    @Test
    fun `a to-do is shared as its title, notes and checklist`() {
        val checklist = listOf(
            ChecklistItem("c1", "Milk", done = true, index = 0),
            ChecklistItem("c2", "Bread", done = false, index = 1),
        )
        assertEquals(
            "Groceries\n\nFor Sunday\n\n☑ Milk\n☐ Bread",
            Sharing.text("Groceries", "For Sunday", checklist),
        )
    }

    @Test
    fun `a to-do without notes or checklist is shared as its title alone`() {
        assertEquals("Call Ana", Sharing.text("Call Ana", null, emptyList()))
        assertEquals("Call Ana\n\n☐ Ask about Sunday", Sharing.text("Call Ana", " ", listOf(ChecklistItem("c", "Ask about Sunday", false, 0))))
    }
}
