package dev.thingscompanion.cloud

import dev.thingscompanion.cloud.Markdown.Kind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownTest {

    /** Each span as "kind:covered text", markers included, in text order. */
    private fun spans(text: String) = Markdown.spans(text).map { "${it.kind}:${text.substring(it.start, it.end)}" }

    @Test
    fun `bold, italic, strikethrough and code keep their markers, dimmed`() {
        assertEquals(listOf("MARKER:**", "BOLD:big", "MARKER:**"), spans("**big**"))
        assertEquals(listOf("MARKER:*", "ITALIC:soft", "MARKER:*"), spans("*soft*"))
        assertEquals(listOf("MARKER:_", "ITALIC:soft", "MARKER:_"), spans("_soft_"))
        assertEquals(listOf("MARKER:~~", "STRIKE:gone", "MARKER:~~"), spans("~~gone~~"))
        assertEquals(listOf("MARKER:`", "CODE:a*b*", "MARKER:`"), spans("`a*b*`"))
    }

    @Test
    fun `headings and list bullets are styled per line`() {
        assertEquals(listOf("MARKER:## ", "HEADING:Plan"), spans("## Plan"))
        assertEquals(listOf("MARKER:- ", "MARKER:1. "), spans("- milk\n1. eggs"))
    }

    @Test
    fun `links carry their url, and bare urls are links too`() {
        val text = "See [the doc](https://ex.com/a) or https://ex.com/b."
        val links = Markdown.spans(text).filter { it.kind == Kind.LINK }
        assertEquals(listOf("the doc" to "https://ex.com/a", "https://ex.com/b" to "https://ex.com/b"), links.map { text.substring(it.start, it.end) to it.url })
        assertTrue("MARKER:](https://ex.com/a)" in spans(text))
    }

    @Test
    fun `plain text, lone stars and snake_case names are left alone`() {
        assertEquals(emptyList(), spans("2 * 3 = 6, file_name_here and a*b"))
        assertEquals(emptyList(), spans(""))
    }

    @Test
    fun `inline styles work inside headings and lists`() {
        assertEquals(listOf("MARKER:- ", "MARKER:**", "BOLD:now", "MARKER:**"), spans("- **now** please"))
    }
}
