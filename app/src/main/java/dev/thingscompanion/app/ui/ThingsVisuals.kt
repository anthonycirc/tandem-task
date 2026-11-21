package dev.thingscompanion.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import dev.thingscompanion.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** List colours in the spirit of Things: each list has its own hue. */
object ListColors {
    val inbox = Color(0xFF3D8BF2)
    val today = Color(0xFFF5C02B)
    val upcoming = Color(0xFFF0484E)
    val anytime = Color(0xFF3FA99A)
    val someday = Color(0xFFC9A66B)
    val project = Color(0xFF3D8BF2)
    val trash = Color(0xFF8E8E93)
    val logbook = Color(0xFF4CAF50)
    val evening = Color(0xFF6E8FD8)
    val deadline = Color(0xFFE5484D)
    val checked = Color(0xFF3D8BF2)
    val heading = Color(0xFF3D8BF2)
    val area = Color(0xFF3FA99A)
}

enum class ListKind { INBOX, TODAY, UPCOMING, DEADLINES, ANYTIME, SOMEDAY, PROJECT, AREA, LOGBOOK, TRASH, EVENING }

/** The icon of a list, drawn in its colour; a project's circle fills with its [progress], 0 to 1. */
@Composable
fun ListIcon(kind: ListKind, modifier: Modifier = Modifier, size: Dp = 22.dp, progress: Float = 0f) {
    when (kind) {
        ListKind.TODAY -> Icon(Icons.Filled.Star, null, modifier.size(size), tint = ListColors.today)
        ListKind.DEADLINES -> DeadlineFlag(modifier, size = size)
        ListKind.UPCOMING -> Icon(Icons.Filled.DateRange, null, modifier.size(size), tint = ListColors.upcoming)
        ListKind.TRASH -> Icon(Icons.Filled.Delete, null, modifier.size(size), tint = ListColors.trash)
        ListKind.LOGBOOK -> Icon(Icons.Filled.CheckCircle, null, modifier.size(size), tint = ListColors.logbook)
        else -> Canvas(modifier.size(size)) {
            val w = this.size.width
            val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (kind) {
                ListKind.INBOX -> {
                    // A tray: rounded box with a notch on top.
                    val c = ListColors.inbox
                    drawRoundRect(c, Offset(w * 0.12f, w * 0.18f), Size(w * 0.76f, w * 0.64f), CornerRadius(w * 0.12f), style = stroke)
                    val notch = Path().apply {
                        moveTo(w * 0.12f, w * 0.52f)
                        lineTo(w * 0.36f, w * 0.52f)
                        lineTo(w * 0.42f, w * 0.62f)
                        lineTo(w * 0.58f, w * 0.62f)
                        lineTo(w * 0.64f, w * 0.52f)
                        lineTo(w * 0.88f, w * 0.52f)
                    }
                    drawPath(notch, c, style = stroke)
                }
                ListKind.ANYTIME -> {
                    // Stacked layers.
                    val c = ListColors.anytime
                    drawRoundRect(c, Offset(w * 0.14f, w * 0.44f), Size(w * 0.72f, w * 0.38f), CornerRadius(w * 0.08f))
                    drawRoundRect(c.copy(alpha = 0.55f), Offset(w * 0.22f, w * 0.28f), Size(w * 0.56f, w * 0.12f), CornerRadius(w * 0.05f))
                    drawRoundRect(c.copy(alpha = 0.3f), Offset(w * 0.3f, w * 0.14f), Size(w * 0.4f, w * 0.1f), CornerRadius(w * 0.04f))
                }
                ListKind.SOMEDAY -> {
                    // A box with its lid.
                    val c = ListColors.someday
                    drawRoundRect(c, Offset(w * 0.12f, w * 0.18f), Size(w * 0.76f, w * 0.2f), CornerRadius(w * 0.05f))
                    drawRoundRect(c, Offset(w * 0.18f, w * 0.42f), Size(w * 0.64f, w * 0.42f), CornerRadius(w * 0.06f), style = stroke)
                    drawLine(c, Offset(w * 0.4f, w * 0.56f), Offset(w * 0.6f, w * 0.56f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
                }
                ListKind.PROJECT -> {
                    drawCircle(ListColors.project, radius = w * 0.36f, style = stroke)
                    // The done share as a pie inside the ring, from twelve o'clock, as in Things.
                    if (progress > 0f) {
                        val r = w * 0.24f
                        drawArc(
                            ListColors.project,
                            startAngle = -90f,
                            sweepAngle = 360f * progress.coerceAtMost(1f),
                            useCenter = true,
                            topLeft = Offset(w / 2 - r, w / 2 - r),
                            size = Size(r * 2, r * 2),
                        )
                    }
                }
                ListKind.AREA -> {
                    // A cube, as Things marks areas.
                    val c = ListColors.area
                    val cube = Path().apply {
                        moveTo(w * 0.5f, w * 0.1f)
                        lineTo(w * 0.86f, w * 0.3f)
                        lineTo(w * 0.86f, w * 0.7f)
                        lineTo(w * 0.5f, w * 0.9f)
                        lineTo(w * 0.14f, w * 0.7f)
                        lineTo(w * 0.14f, w * 0.3f)
                        close()
                        moveTo(w * 0.14f, w * 0.3f)
                        lineTo(w * 0.5f, w * 0.5f)
                        lineTo(w * 0.86f, w * 0.3f)
                        moveTo(w * 0.5f, w * 0.5f)
                        lineTo(w * 0.5f, w * 0.9f)
                    }
                    drawPath(cube, c, style = stroke)
                }
                ListKind.EVENING -> {
                    // A crescent moon.
                    val moon = Path().apply {
                        op(
                            Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(w / 2, w / 2), w * 0.38f)) },
                            Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(w * 0.7f, w * 0.34f), w * 0.32f)) },
                            PathOperation.Difference,
                        )
                    }
                    drawPath(moon, ListColors.evening)
                }
                else -> Unit
            }
        }
    }
}

/**
 * The to-do checkbox Things draws: a small rounded square, filled with a check once done, or
 * crossed in grey once [cancelled]. A disabled box (repeating to-dos the app can't check) is faded.
 * With [onLongClick], a long press offers more (cancel, as in Things).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThingsCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 22.dp,
    contentDescription: String? = null,
    cancelled: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val outline = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.7f else 0.3f)
    val state = stringResource(if (checked) R.string.state_checked else if (cancelled) R.string.state_cancelled else R.string.state_unchecked)
    val toggle = if (onLongClick != null) {
        Modifier
            .semantics { stateDescription = state }
            .combinedClickable(enabled = enabled, role = Role.Checkbox, onClick = { onCheckedChange(!checked) }, onLongClick = onLongClick)
    } else {
        Modifier.toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
    }
    Box(
        modifier
            .size(44.dp)
            .then(toggle)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            val w = this.size.width
            val radius = CornerRadius(w * 0.24f)
            if (cancelled) {
                val grey = outline.copy(alpha = 0.6f)
                drawRoundRect(grey, cornerRadius = radius, style = Stroke(width = w * 0.08f))
                val cross = Stroke(width = w * 0.1f, cap = StrokeCap.Round)
                drawLine(grey, Offset(w * 0.3f, w * 0.3f), Offset(w * 0.7f, w * 0.7f), strokeWidth = cross.width, cap = StrokeCap.Round)
                drawLine(grey, Offset(w * 0.7f, w * 0.3f), Offset(w * 0.3f, w * 0.7f), strokeWidth = cross.width, cap = StrokeCap.Round)
            } else if (checked) {
                drawRoundRect(ListColors.checked.copy(alpha = if (enabled) 1f else 0.5f), cornerRadius = radius)
                val check = Path().apply {
                    moveTo(w * 0.26f, w * 0.52f)
                    lineTo(w * 0.43f, w * 0.68f)
                    lineTo(w * 0.75f, w * 0.34f)
                }
                drawPath(check, Color.White, style = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            } else {
                drawRoundRect(outline, cornerRadius = radius, style = Stroke(width = w * 0.08f))
            }
        }
    }
}

/** A small red flag for deadlines. */
@Composable
fun DeadlineFlag(modifier: Modifier = Modifier, size: Dp = 12.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        drawLine(ListColors.deadline, Offset(w * 0.2f, w * 0.08f), Offset(w * 0.2f, w * 0.95f), strokeWidth = w * 0.14f, cap = StrokeCap.Round)
        val flag = Path().apply {
            moveTo(w * 0.24f, w * 0.1f)
            lineTo(w * 0.88f, w * 0.3f)
            lineTo(w * 0.24f, w * 0.52f)
            close()
        }
        drawPath(flag, ListColors.deadline)
    }
}

/** The repeat mark of a repeating to-do: a circular arrow, as Things draws it. */
@Composable
fun RepeatGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 12.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val stroke = w * 0.13f
        // Most of a circle, open at the top right, with the arrow head at its end.
        drawArc(
            color,
            startAngle = -60f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(w * 0.14f, w * 0.14f),
            size = Size(w * 0.72f, w * 0.72f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        val head = Path().apply {
            moveTo(w * 0.62f, w * 0.02f)
            lineTo(w * 0.98f, w * 0.18f)
            lineTo(w * 0.66f, w * 0.42f)
            close()
        }
        drawPath(head, color)
    }
}

/** A tag, as a small grey pill. */
@Composable
fun TagPill(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            .padding(horizontal = 7.dp, vertical = 1.dp),
    )
}

/** A tag, as Things draws it: a label with a hole, pointing left. */
@Composable
fun TagGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val tag = Path().apply {
            moveTo(w * 0.08f, w * 0.5f)
            lineTo(w * 0.38f, w * 0.18f)
            lineTo(w * 0.92f, w * 0.18f)
            lineTo(w * 0.92f, w * 0.82f)
            lineTo(w * 0.38f, w * 0.82f)
            close()
        }
        drawPath(tag, color, style = Stroke(width = w * 0.1f, join = StrokeJoin.Round))
        drawCircle(color, radius = w * 0.07f, center = Offset(w * 0.4f, w * 0.5f))
    }
}

/** "Select several": two checked boxes with their lines, so it doesn't read as "mark done". */
@Composable
fun SelectGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        listOf(0.1f, 0.56f).forEach { top ->
            drawRoundRect(color, topLeft = Offset(w * 0.06f, w * top), size = Size(w * 0.34f, w * 0.34f), cornerRadius = CornerRadius(w * 0.08f), style = stroke)
            val check = Path().apply {
                moveTo(w * 0.13f, w * (top + 0.17f))
                lineTo(w * 0.21f, w * (top + 0.25f))
                lineTo(w * 0.34f, w * (top + 0.09f))
            }
            drawPath(check, color, style = stroke)
            drawLine(color, Offset(w * 0.52f, w * (top + 0.17f)), Offset(w * 0.94f, w * (top + 0.17f)), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        }
    }
}
