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

/** A widget's background colour: the theme's own, or a colour with white text over it. */
enum class WidgetColor(@StringRes val label: Int, val background: Color?) {
    DEFAULT(R.string.widget_color_default, null),
    BLUE(R.string.widget_color_blue, Color(0xFF2F5BB7)),
    GREEN(R.string.widget_color_green, Color(0xFF3C7D4F)),
    PURPLE(R.string.widget_color_purple, Color(0xFF6B4BA8)),
    RED(R.string.widget_color_red, Color(0xFFB3403A)),
    ORANGE(R.string.widget_color_orange, Color(0xFFC0661F)),
    GREY(R.string.widget_color_grey, Color(0xFF4A4D55)),
}

/** The colors a widget draws with, for its theme, colour and background opacity (0..100 %). */
class WidgetColors(theme: WidgetTheme, opacityPercent: Int, color: WidgetColor = WidgetColor.DEFAULT) {
    private val alpha = opacityPercent.coerceIn(0, 100) / 100f
    private val tint = color.background

    val background = if (tint != null) ColorProvider(tint.copy(alpha = alpha)) else pick(theme, LIGHT_BACKGROUND.copy(alpha = alpha), DARK_BACKGROUND.copy(alpha = alpha))
    val text = if (tint != null) ColorProvider(Color.White) else pick(theme, LIGHT_TEXT, DARK_TEXT)
    val secondary = if (tint != null) ColorProvider(Color.White.copy(alpha = 0.75f)) else pick(theme, LIGHT_SECONDARY, DARK_SECONDARY)
    val accent = if (tint != null) ColorProvider(Color.White) else pick(theme, LIGHT_ACCENT, DARK_ACCENT)
    /** Deadlines, in Things' red; on a coloured background, white like the text. */
    val deadline = if (tint != null) ColorProvider(Color.White) else pick(theme, LIGHT_DEADLINE, DARK_DEADLINE)

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
        val LIGHT_DEADLINE = Color(0xFFE5484D)
        val DARK_DEADLINE = Color(0xFFFF7A7E)
    }
}
