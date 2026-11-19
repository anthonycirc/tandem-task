package dev.thingscompanion.cloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A widget's own colour, picked as red, green and blue or typed as hex. */
class RgbColorTest {

    @Test
    fun `hex is read with or without the hash, in any case`() {
        assertEquals(0x2F5BB7, RgbColor.parse("#2F5BB7"))
        assertEquals(0x2F5BB7, RgbColor.parse("2f5bb7"))
        assertEquals(0xFFFFFF, RgbColor.parse(" #fff "))
    }

    @Test
    fun `anything that is not a colour is refused`() {
        assertNull(RgbColor.parse(""))
        assertNull(RgbColor.parse("#12345"))
        assertNull(RgbColor.parse("#GG0000"))
        assertNull(RgbColor.parse("#1234567"))
    }

    @Test
    fun `a colour is written as hash and six upper-case digits`() {
        assertEquals("#2F5BB7", RgbColor.format(0x2F5BB7))
        assertEquals("#000000", RgbColor.format(0))
    }

    @Test
    fun `red, green and blue make the colour and come back out of it`() {
        assertEquals(0x2F5BB7, RgbColor.of(0x2F, 0x5B, 0xB7))
        assertEquals(Triple(0x2F, 0x5B, 0xB7), RgbColor.channels(0x2F5BB7))
        assertEquals(0xFF00FF, RgbColor.of(300, -4, 255))
    }

    @Test
    fun `light colours take dark text, dark colours white text`() {
        assertTrue(RgbColor.isLight(0xFFFFFF))
        assertTrue(RgbColor.isLight(0xF5D547))
        assertFalse(RgbColor.isLight(0x2F5BB7))
        assertFalse(RgbColor.isLight(0x000000))
    }
}
