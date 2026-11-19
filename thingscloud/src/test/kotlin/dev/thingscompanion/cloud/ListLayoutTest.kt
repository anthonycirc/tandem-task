package dev.thingscompanion.cloud

import kotlin.test.Test
import kotlin.test.assertEquals

/** The user's order and choice of list tabs. */
class ListLayoutTest {

    private val all = listOf("TODAY", "INBOX", "UPCOMING", "DEADLINES", "PROJECTS", "TRASH")

    @Test
    fun `with nothing saved the lists keep their default order`() {
        assertEquals(all, ListLayout.order(all, saved = emptyList()))
        assertEquals(all, ListLayout.shown(all, saved = emptyList(), hidden = emptySet()))
    }

    @Test
    fun `the saved order comes first, and lists it does not know yet follow in their default order`() {
        val saved = listOf("PROJECTS", "TODAY", "GONE", "TODAY")
        assertEquals(listOf("PROJECTS", "TODAY", "INBOX", "UPCOMING", "DEADLINES", "TRASH"), ListLayout.order(all, saved))
    }

    @Test
    fun `hidden lists are left out of the tabs`() {
        assertEquals(listOf("TODAY", "UPCOMING", "PROJECTS"), ListLayout.shown(all, emptyList(), hidden = setOf("INBOX", "DEADLINES", "TRASH")))
    }

    @Test
    fun `at least one list always stays shown`() {
        assertEquals(listOf("PROJECTS"), ListLayout.shown(all, listOf("PROJECTS"), hidden = all.toSet()))
    }

    @Test
    fun `moving a list swaps it with its neighbour, and stops at the ends`() {
        assertEquals(listOf("INBOX", "TODAY", "UPCOMING", "DEADLINES", "PROJECTS", "TRASH"), ListLayout.move(all, "INBOX", -1))
        assertEquals(all, ListLayout.move(all, "TODAY", -1))
        assertEquals(all, ListLayout.move(all, "TRASH", 1))
    }

    @Test
    fun `the bottom bar holds up to five lists, beyond that four and a More menu with the rest`() {
        assertEquals(ListLayout.Bar(listOf("TODAY", "INBOX", "UPCOMING"), emptyList()), ListLayout.bottomBar(listOf("TODAY", "INBOX", "UPCOMING")))
        assertEquals(ListLayout.Bar(all.take(5), emptyList()), ListLayout.bottomBar(all.take(5)))
        assertEquals(ListLayout.Bar(all.take(4), listOf("PROJECTS", "TRASH")), ListLayout.bottomBar(all))
    }

    @Test
    fun `a list opened from More or the search shows in the bar in place of the fourth one`() {
        assertEquals(ListLayout.Bar(listOf("TODAY", "INBOX", "UPCOMING", "TRASH"), listOf("DEADLINES", "PROJECTS")), ListLayout.bottomBar(all, current = "TRASH"))
        assertEquals(ListLayout.bottomBar(all), ListLayout.bottomBar(all, current = "INBOX"))
    }
}
