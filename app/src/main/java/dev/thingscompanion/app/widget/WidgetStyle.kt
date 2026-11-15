package dev.thingscompanion.app.widget

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import dev.thingscompanion.app.R
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider

/** Per-widget appearance: follow the system, or force light or dark. */
enum class WidgetTheme(@StringRes val label: Int) {
    SYSTEM(R.string.theme_system),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark),
}

/** The colors a widget draws with, for its theme and background opacity (0..100 %). */
class WidgetColors(theme: WidgetTheme, opacityPercent: Int) {
    private val alpha = opacityPercent.coerceIn(0, 100) / 100f

    val background = pick(theme, LIGHT_BACKGROUND.copy(alpha = alpha), DARK_BACKGROUND.copy(alpha = alpha))
    val text = pick(theme, LIGHT_TEXT, DARK_TEXT)
    val secondary = pick(theme, LIGHT_SECONDARY, DARK_SECONDARY)
    val accent = pick(theme, LIGHT_ACCENT, DARK_ACCENT)

    private fun pick(theme: WidgetTheme, light: Color, dark: Color): androidx.glance.unit.ColorProvider = when (theme) {
        WidgetTheme.SYSTEM -> ColorProvider(day = light, night = dark)
        WidgetTheme.LIGHT -> ColorProvider(light)
        WidgetTheme.DARK -> ColorProvider(dark)
    }

    private companion object {
        val LIGHT_BACKGROUND = Color(0xFFFFFFFF)
        val LIGHT_TEXT = Color(0xFF1C1B1F)
        val LIGHT_SECONDARY = Color(0xFF5F5B66)
        val LIGHT_ACCENT = Color(0xFF2F5BB7)
        val DARK_BACKGROUND = Color(0xFF1F2024)
        val DARK_TEXT = Color(0xFFE6E1E5)
        val DARK_SECONDARY = Color(0xFFB9B4BE)
        val DARK_ACCENT = Color(0xFF9DB7FF)
    }
}
