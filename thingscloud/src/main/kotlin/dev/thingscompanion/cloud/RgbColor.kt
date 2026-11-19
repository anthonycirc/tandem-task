package dev.thingscompanion.cloud

import kotlin.math.pow

/** A colour as 0xRRGGBB, for the widget's own background. */
object RgbColor {
    /** "#2F5BB7", "2f5bb7" or the short "#fff"; null when it is not a colour. */
    fun parse(text: String): Int? {
        val hex = text.trim().removePrefix("#")
        val full = when (hex.length) {
            3 -> hex.map { "$it$it" }.joinToString("")
            6 -> hex
            else -> return null
        }
        return full.toIntOrNull(16)
    }

    fun format(rgb: Int): String = "#%06X".format(rgb and 0xFFFFFF)

    /** Each channel kept within 0..255. */
    fun of(red: Int, green: Int, blue: Int): Int =
        (red.coerceIn(0, 255) shl 16) or (green.coerceIn(0, 255) shl 8) or blue.coerceIn(0, 255)

    fun channels(rgb: Int): Triple<Int, Int, Int> = Triple((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF)

    /** Whether dark text reads better than white on it (WCAG relative luminance). */
    fun isLight(rgb: Int): Boolean {
        val (r, g, b) = channels(rgb).toList().map { c ->
            val s = c / 255.0
            if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
        // White and black give equal contrast at about 0.179.
        return luminance > 0.179
    }
}
