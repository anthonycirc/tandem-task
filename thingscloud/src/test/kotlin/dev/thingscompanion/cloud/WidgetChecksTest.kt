package dev.thingscompanion.cloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WidgetChecksTest {

    @Test
    fun `a tap ticks a to-do and a second tap unticks it`() {
        val ticked = WidgetChecks().toggle("a", token = 1)
        assertTrue(ticked.isTicked("a"))
        assertFalse(ticked.toggle("a", token = 2).isTicked("a"))
    }

    @Test
    fun `only the tap that ticked it still holds once its delay is over`() {
        val again = WidgetChecks().toggle("a", 1).toggle("a", 2).toggle("a", 3)
        assertFalse(again.stillTicked("a", 1))
        assertTrue(again.stillTicked("a", 3))
        assertFalse(again.without("a").isTicked("a"))
    }

    @Test
    fun `a tick whose send was cut short does not stay ticked`() {
        val checks = WidgetChecks().toggle("old", 1_000).toggle("new", 60_000)
        assertEquals(setOf("new"), checks.freshAt(nowMillis = 61_000, maxAgeMillis = 30_000).ticks.keys)
    }

    @Test
    fun `survives being stored, and skips what it cannot read`() {
        val checks = WidgetChecks().toggle("a", 1).toggle("b:c", 2)
        assertEquals(checks, WidgetChecks.decode(checks.encode()))
        assertEquals(WidgetChecks(), WidgetChecks.decode(setOf("garbage", "x:y")))
    }
}
