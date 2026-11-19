package dev.thingscompanion.app.widget

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import dev.thingscompanion.app.R
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import dev.thingscompanion.cloud.RgbColor

/** Per-widget appearance: follow the system, or force light or dark. */
enum class WidgetTheme(@StringRes val label: Int) {
    SYSTEM(R.string.theme_system),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark),
}

/**
 * A widget's background colour: the theme's own, a preset with white text over it, or [CUSTOM], a colour
 * picked by its red, green and blue (kept apart as an RGB value).
 */
enum class WidgetColor(@StringRes val label: Int, val background: Color?) {
    DEFAULT(R.string.widget_color_default, null),
    BLUE(R.string.widget_color_blue, Color(0xFF2F5BB7)),
    GREEN(R.string.widget_color_green, Color(0xFF3C7D4F)),
    PURPLE(R.string.widget_color_purple, Color(0xFF6B4BA8)),
    RED(R.string.widget_color_red, Color(0xFFB3403A)),
    ORANGE(R.string.widget_color_orange, Color(0xFFC0661F)),
    GREY(R.string.widget_color_grey, Color(0xFF4A4D55)),
    CUSTOM(R.string.widget_color_custom, null),
}

/** The background a choice gives: a preset's colour, the [customRgb] for CUSTOM, or null for the theme's own. */
fun WidgetColor.tint(customRgb: Int): Color? =
    if (this == WidgetColor.CUSTOM) Color(0xFF000000.toInt() or customRgb) else background

/** The colors a widget draws with, for its theme, colour and background opacity (0..100 %). */
class WidgetColors(theme: WidgetTheme, opacityPercent: Int, color: WidgetColor = WidgetColor.DEFAULT, customRgb: Int = DEFAULT_CUSTOM_RGB) {
    private val alpha = opacityPercent.coerceIn(0, 100) / 100f
    private val tint = color.tint(customRgb)

    /** Over a light custom colour, the text is dark so it stays readable; over the others, white. */
    private val onTint = if (color == WidgetColor.CUSTOM && RgbColor.isLight(customRgb)) Color(0xFF1C1B1F) else Color.White

    val background = if (tint != null) ColorProvider(tint.copy(alpha = alpha)) else pick(theme, LIGHT_BACKGROUND.copy(alpha = alpha), DARK_BACKGROUND.copy(alpha = alpha))
    val text = if (tint != null) ColorProvider(onTint) else pick(theme, LIGHT_TEXT, DARK_TEXT)
    val secondary = if (tint != null) ColorProvider(onTint.copy(alpha = 0.75f)) else pick(theme, LIGHT_SECONDARY, DARK_SECONDARY)
    val accent = if (tint != null) ColorProvider(onTint) else pick(theme, LIGHT_ACCENT, DARK_ACCENT)
    /** Deadlines, in Things' red; on a coloured background, the text's colour. */
    val deadline = if (tint != null) ColorProvider(onTint) else pick(theme, LIGHT_DEADLINE, DARK_DEADLINE)

    private fun pick(theme: WidgetTheme, light: Color, dark: Color): androidx.glance.unit.ColorProvider = when (theme) {
        WidgetTheme.SYSTEM -> ColorProvider(day = light, night = dark)
        WidgetTheme.LIGHT -> ColorProvider(light)
        WidgetTheme.DARK -> ColorProvider(dark)
    }

    companion object {
        /** The custom colour offered first: the blue preset's. */
        const val DEFAULT_CUSTOM_RGB = 0x2F5BB7
        private val LIGHT_BACKGROUND = Color(0xFFFFFFFF)
        private val LIGHT_TEXT = Color(0xFF1C1B1F)
        private val LIGHT_SECONDARY = Color(0xFF5F5B66)
        private val LIGHT_ACCENT = Color(0xFF2F5BB7)
        private val DARK_BACKGROUND = Color(0xFF1F2024)
        private val DARK_TEXT = Color(0xFFE6E1E5)
        private val DARK_SECONDARY = Color(0xFFB9B4BE)
        private val DARK_ACCENT = Color(0xFF9DB7FF)
        private val LIGHT_DEADLINE = Color(0xFFE5484D)
        private val DARK_DEADLINE = Color(0xFFFF7A7E)
    }
}
