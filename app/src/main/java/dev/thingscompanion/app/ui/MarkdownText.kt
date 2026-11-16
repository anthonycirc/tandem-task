package dev.thingscompanion.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import androidx.compose.material3.MaterialTheme
import dev.thingscompanion.cloud.Markdown

/** Colours of styled notes: markers dimmed, links in the accent colour. */
data class MarkdownColors(val marker: Color, val link: Color, val codeBackground: Color)

@Composable
fun markdownColors() = MarkdownColors(
    marker = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    link = ListColors.checked,
    codeBackground = MaterialTheme.colorScheme.surfaceVariant,
)

/**
 * [text] with Things' Markdown styled in place, markers kept. With [clickableLinks], links open
 * in the browser (the detail view); in a text field they are only coloured.
 */
fun markdown(text: String, colors: MarkdownColors, clickableLinks: Boolean): AnnotatedString = buildAnnotatedString {
    append(text)
    for (span in Markdown.spans(text)) {
        val style = when (span.kind) {
            Markdown.Kind.MARKER -> SpanStyle(color = colors.marker)
            Markdown.Kind.HEADING -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 1.15.em)
            Markdown.Kind.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
            Markdown.Kind.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
            Markdown.Kind.STRIKE -> SpanStyle(textDecoration = TextDecoration.LineThrough)
            Markdown.Kind.CODE -> SpanStyle(fontFamily = FontFamily.Monospace, background = colors.codeBackground)
            Markdown.Kind.LINK -> SpanStyle(color = colors.link, textDecoration = TextDecoration.Underline)
        }
        val url = span.url
        if (clickableLinks && url != null) {
            addLink(LinkAnnotation.Url(url, TextLinkStyles(style)), span.start, span.end)
        } else {
            addStyle(style, span.start, span.end)
        }
    }
}

/** Styles the notes field as the user types; the text itself is unchanged. */
class MarkdownTransformation(private val colors: MarkdownColors) : VisualTransformation {
    override fun filter(text: AnnotatedString) = TransformedText(markdown(text.text, colors, clickableLinks = false), OffsetMapping.Identity)

    override fun equals(other: Any?) = other is MarkdownTransformation && other.colors == colors

    override fun hashCode() = colors.hashCode()
}
