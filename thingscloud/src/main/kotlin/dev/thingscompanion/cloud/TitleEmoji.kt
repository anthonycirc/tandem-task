package dev.thingscompanion.cloud

/** An emoji in front of a to-do's title: Things keeps it in the title itself. */
object TitleEmoji {
    /** Emoji, with their variation selectors, skin tones and zero-width joins, at the start of a title. */
    private val LEADING = Regex("""^(?:\p{So}|\p{Cs}{2})(?:[️‍\x{1F3FB}-\x{1F3FF}]|\p{So}|\p{Cs}{2})*""")

    fun leading(title: String): String? = LEADING.find(title)?.value?.takeIf { it.isNotEmpty() }

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
