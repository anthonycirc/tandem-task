package dev.thingscompanion.cloud

/** An emoji in front of a to-do's title: Things keeps it in the title itself. */
object TitleEmoji {
    /**
     * An emoji at the start of a title: a flag (two regional indicators), a pictograph (U+1F000
     * and up) or a symbol in emoji presentation (followed by U+FE0F), with skin tones and
     * zero-width joins. A plain symbol such as an arrow or a star stays part of the title.
     */
    private val LEADING = Regex(
        """^(?:[\x{1F1E6}-\x{1F1FF}]{2}|[\x{1F000}-\x{1FAFF}][\x{1F3FB}-\x{1F3FF}]?\uFE0F?|\p{So}\uFE0F)""" +
            """(?:\u200D(?:[\x{1F000}-\x{1FAFF}]|\p{So})\uFE0F?[\x{1F3FB}-\x{1F3FF}]?)*""",
    )

    fun leading(title: String): String? =
        CHOICES.filter { title.startsWith(it) }.maxByOrNull { it.length } ?: LEADING.find(title)?.value

    /** [title] led by [emoji] instead of the one it had; null takes it off. */
    fun apply(title: String, emoji: String?): String {
        val rest = leading(title)?.let { title.removePrefix(it).trimStart() } ?: title
        return if (emoji == null) rest else "$emoji $rest".trimEnd()
    }

    /** The choices offered in the to-do form. */
    val CHOICES = listOf(
        "⭐", "🔥", "✅", "❗", "💡", "📌", "📞", "✉️", "💬", "📅", "⏰", "🎯",
        "💻", "🐛", "🚀", "📝", "📚", "🎓", "💼", "💰", "🛒", "🏠", "🧹", "🍽️",
        "🏃", "💊", "❤️", "🎉", "🎁", "✈️", "🚗", "🔧", "🌱", "🐶", "🎵", "📷",
    )
}
