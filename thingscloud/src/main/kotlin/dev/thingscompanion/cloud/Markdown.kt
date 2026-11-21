package dev.thingscompanion.cloud

/**
 * The Markdown Things understands in notes, as styled ranges over the unchanged text: the
 * markers stay where they are (shown dimmed), so the same spans style a note being read and a
 * note being typed.
 */
object Markdown {
    enum class Kind { MARKER, HEADING, BOLD, ITALIC, STRIKE, CODE, LINK }

    /** [start] inclusive, [end] exclusive; [url] for links. */
    data class Span(val start: Int, val end: Int, val kind: Kind, val url: String? = null)

    private val HEADING = Regex("""^(#{1,6} +)(.+)$""")
    private val BULLET = Regex("""^\s*([-*+]|\d+\.) +""")
    private val CODE = Regex("""`([^`\n]+)`""")
    // Web links, and Obsidian's, which open a note in the vault; other app schemes stay plain text.
    private const val SCHEME = """(?:https?|obsidian)://"""
    private val LINK = Regex("""\[([^\]\n]+)]\(($SCHEME[^)\s]+)\)""")
    private val BARE_URL = Regex("""$SCHEME[^\s<>()\[\]]*[^\s<>()\[\].,;:!?'"]""")

    /** Delimited styles; `_` only around whole words, so snake_case names stay plain. */
    private val PAIRS = listOf(
        Regex("""\*\*(?=\S)(.+?)(?<=\S)\*\*""") to Kind.BOLD,
        Regex("""(?<![\w_])__(?=\S)(.+?)(?<=\S)__(?![\w_])""") to Kind.BOLD,
        Regex("""~~(?=\S)(.+?)(?<=\S)~~""") to Kind.STRIKE,
        Regex("""(?<![*\w])\*(?=[^\s*])(.+?)(?<=[^\s*])\*(?![*\w])""") to Kind.ITALIC,
        Regex("""(?<![\w_])_(?=[^\s_])(.+?)(?<=[^\s_])_(?![\w_])""") to Kind.ITALIC,
    )

    fun spans(text: String): List<Span> {
        val spans = mutableListOf<Span>()
        var lineStart = 0
        for (line in text.split('\n')) {
            spans += lineSpans(line, lineStart)
            lineStart += line.length + 1
        }
        return spans.sortedWith(compareBy({ it.start }, { it.end }))
    }

    private fun lineSpans(line: String, offset: Int): List<Span> {
        val spans = mutableListOf<Span>()
        fun add(start: Int, end: Int, kind: Kind, url: String? = null) {
            if (end > start) spans += Span(offset + start, offset + end, kind, url)
        }
        // Ranges already taken by code or links: no other style inside them.
        val taken = mutableListOf<IntRange>()
        fun free(range: IntRange) = taken.none { it.first <= range.last && range.first <= it.last }

        HEADING.find(line)?.let {
            add(0, it.groups[1]!!.range.last + 1, Kind.MARKER)
            add(it.groups[2]!!.range.first, line.length, Kind.HEADING)
        } ?: BULLET.find(line)?.let { add(it.range.first, it.range.last + 1, Kind.MARKER) }

        for (match in CODE.findAll(line)) {
            add(match.range.first, match.range.first + 1, Kind.MARKER)
            add(match.range.first + 1, match.range.last, Kind.CODE)
            add(match.range.last, match.range.last + 1, Kind.MARKER)
            taken += match.range
        }
        for (match in LINK.findAll(line).filter { free(it.range) }) {
            val label = match.groups[1]!!.range
            add(match.range.first, label.first, Kind.MARKER)
            add(label.first, label.last + 1, Kind.LINK, match.groups[2]!!.value)
            add(label.last + 1, match.range.last + 1, Kind.MARKER)
            taken += match.range
        }
        for (match in BARE_URL.findAll(line).filter { free(it.range) }) {
            add(match.range.first, match.range.last + 1, Kind.LINK, match.value)
            taken += match.range
        }
        for ((regex, kind) in PAIRS) {
            for (match in regex.findAll(line).filter { free(it.range) }) {
                val inner = match.groups[1]!!.range
                add(match.range.first, inner.first, Kind.MARKER)
                add(inner.first, inner.last + 1, kind)
                add(inner.last + 1, match.range.last + 1, Kind.MARKER)
                // Markers are consumed; the inside may still hold other styles (***both***, **_x_**).
                taken += match.range.first until inner.first
                taken += inner.last + 1..match.range.last
            }
        }
        return spans
    }
}
