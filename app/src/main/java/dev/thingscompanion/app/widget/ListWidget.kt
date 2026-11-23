package dev.thingscompanion.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import dev.thingscompanion.app.ui.deadlineText
import dev.thingscompanion.cloud.ThingsDay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import dev.thingscompanion.cloud.WidgetChecks
import dev.thingscompanion.app.MainActivity
import dev.thingscompanion.app.R
import dev.thingscompanion.app.data.readWidgetSnapshot
import dev.thingscompanion.cloud.WidgetSnapshot

/** Home-screen widget showing one Things list, chosen per widget, with a + that opens the Inbox form. */
class ListWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                // A running Glance session does not recompose on update() alone: refreshAll bumps
                // SNAPSHOT_VERSION in the widget's state, which recomposes and re-reads the lists.
                val prefs = currentState<Preferences>()
                val version = prefs[SNAPSHOT_VERSION] ?: 0L
                val snapshot = remember(version) { readWidgetSnapshot(context) }
                val ticks = remember(version) { WidgetCheckStore.read(context) }
                val listKey = prefs[LIST_KEY] ?: DEFAULT_LIST
                val theme = prefs[THEME_KEY]?.let { runCatching { WidgetTheme.valueOf(it) }.getOrNull() } ?: WidgetTheme.SYSTEM
                val color = prefs[COLOR_KEY]?.let { runCatching { WidgetColor.valueOf(it) }.getOrNull() } ?: WidgetColor.DEFAULT
                val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
                val customRgb = prefs[CUSTOM_RGB_KEY] ?: WidgetColors.DEFAULT_CUSTOM_RGB
                Content(context, snapshot, ticks, listKey, appWidgetId, WidgetColors(theme, prefs[OPACITY_KEY] ?: DEFAULT_OPACITY, color, customRgb))
            }
        }
    }

    @Composable
    private fun Content(context: Context, snapshot: WidgetSnapshot?, ticks: WidgetChecks, listKey: String, appWidgetId: Int, colors: WidgetColors) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(colors.background)
                .padding(12.dp),
        ) {
            val items = snapshot?.items(listKey).orEmpty()
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = listLabel(context, snapshot, listKey),
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.text),
                    maxLines = 1,
                    modifier = GlanceModifier.clickable(actionStartActivity(openApp(context))),
                )
                // How many open to-dos the list holds; ticked ones still waiting to be sent are left out.
                val count = items.count { !ticks.isTicked(it.id) }
                Text(
                    text = if (count > 0) "  $count" else "",
                    style = TextStyle(fontSize = 14.sp, color = colors.secondary),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(openApp(context))),
                )
                // The list, theme and colour, changed from the widget itself.
                Text(
                    text = "⋯",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.secondary),
                    modifier = GlanceModifier
                        .padding(horizontal = 8.dp)
                        .semantics { contentDescription = context.getString(R.string.widget_settings) }
                        .clickable(actionStartActivity(configure(context, appWidgetId))),
                )
                Text(
                    text = "+",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, color = colors.accent),
                    modifier = GlanceModifier.padding(horizontal = 8.dp).clickable(actionStartActivity(openApp(context, openAdd = true))),
                )
            }
            when {
                snapshot == null -> Message(context.getString(R.string.widget_open_app), colors)
                items.isEmpty() -> Message(context.getString(R.string.nothing_here), colors)
                else -> LazyColumn {
                    items(items, itemId = { it.id.hashCode().toLong() }) { item -> TaskRow(context, item, ticks.isTicked(item.id), colors) }
                }
            }
        }
    }

    /** The circle ticks the to-do (or opens it when the app cannot check it); the title opens it. */
    @Composable
    private fun TaskRow(context: Context, item: WidgetSnapshot.Item, ticked: Boolean, colors: WidgetColors) {
        val open = actionStartActivity(openApp(context, taskId = item.id))
        val check = if (item.canToggle) actionRunCallback<CheckTaskAction>(actionParametersOf(CheckTaskAction.TASK_ID to item.id)) else open
        val description = context.getString(R.string.widget_check, item.title)
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (ticked) "✓" else "○",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (ticked) colors.accent else colors.secondary),
                modifier = GlanceModifier
                    .padding(start = 2.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)
                    .semantics { contentDescription = description }
                    .clickable(check),
            )
            Column(modifier = GlanceModifier.defaultWeight().padding(vertical = 6.dp).clickable(open)) {
                Text(
                    text = item.title,
                    maxLines = 2,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = if (ticked) colors.secondary else colors.text,
                        textDecoration = if (ticked) TextDecoration.LineThrough else null,
                    ),
                )
                // The reminder time and the deadline stand out under the title.
                val reminder = item.reminderSeconds?.let { "🔔 " + LocalTime.ofSecondOfDay(it.toLong()).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) }
                val deadline = item.deadlineDay?.let { "⚑ " + deadlineText(context, LocalDate.ofEpochDay(it), ThingsDay.today()) }
                if (!ticked && (reminder != null || deadline != null)) {
                    Row {
                        reminder?.let {
                            Text(it, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.accent), modifier = GlanceModifier.padding(end = 8.dp))
                        }
                        deadline?.let { Text(it, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.deadline)) }
                    }
                }
            }
        }
    }

    @Composable
    private fun Message(text: String, colors: WidgetColors) {
        Text(
            text = text,
            style = TextStyle(fontSize = 14.sp, color = colors.secondary),
            modifier = GlanceModifier.padding(top = 8.dp),
        )
    }

    companion object {
        val LIST_KEY = stringPreferencesKey("list_key")
        val THEME_KEY = stringPreferencesKey("theme")
        val OPACITY_KEY = intPreferencesKey("opacity")
        val COLOR_KEY = stringPreferencesKey("color")
        val CUSTOM_RGB_KEY = intPreferencesKey("custom_rgb")
        private val SNAPSHOT_VERSION = longPreferencesKey("snapshot_version")
        const val DEFAULT_LIST = "today"
        const val DEFAULT_OPACITY = 100

        private fun openApp(context: Context, openAdd: Boolean = false, taskId: String? = null): Intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                // A distinct action per target keeps Android from merging the pending intents.
                action = "dev.thingscompanion.OPEN:${if (openAdd) "add" else taskId.orEmpty()}"
                putExtra(MainActivity.EXTRA_OPEN_ADD, openAdd)
                taskId?.let { putExtra(MainActivity.EXTRA_TASK_ID, it) }
            }

        private fun configure(context: Context, appWidgetId: Int): Intent =
            Intent(context, WidgetConfigActivity::class.java).apply {
                // Its own task (see the manifest): the app's open screens stay as they were.
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                action = "dev.thingscompanion.CONFIGURE:$appWidgetId"
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }

        /** Redraws every placed widget from the latest snapshot. */
        suspend fun refreshAll(context: Context) {
            val version = System.nanoTime()
            GlanceAppWidgetManager(context).getGlanceIds(ListWidget::class.java).forEach { glanceId ->
                updateAppWidgetState(context, glanceId) { it[SNAPSHOT_VERSION] = version }
            }
            ListWidget().updateAll(context)
        }

        data class Settings(
            val listKey: String,
            val theme: WidgetTheme,
            val opacity: Int,
            val color: WidgetColor = WidgetColor.DEFAULT,
            /** The colour picked for [WidgetColor.CUSTOM], as 0xRRGGBB. */
            val customRgb: Int = WidgetColors.DEFAULT_CUSTOM_RGB,
        )

        suspend fun settings(context: Context, appWidgetId: Int): Settings {
            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
            return Settings(
                listKey = prefs[LIST_KEY] ?: DEFAULT_LIST,
                theme = prefs[THEME_KEY]?.let { runCatching { WidgetTheme.valueOf(it) }.getOrNull() } ?: WidgetTheme.SYSTEM,
                opacity = prefs[OPACITY_KEY] ?: DEFAULT_OPACITY,
                color = prefs[COLOR_KEY]?.let { runCatching { WidgetColor.valueOf(it) }.getOrNull() } ?: WidgetColor.DEFAULT,
                customRgb = prefs[CUSTOM_RGB_KEY] ?: WidgetColors.DEFAULT_CUSTOM_RGB,
            )
        }

        suspend fun save(context: Context, appWidgetId: Int, settings: Settings) {
            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            updateAppWidgetState(context, glanceId) {
                it[LIST_KEY] = settings.listKey
                it[THEME_KEY] = settings.theme.name
                it[OPACITY_KEY] = settings.opacity.coerceIn(0, 100)
                it[COLOR_KEY] = settings.color.name
                it[CUSTOM_RGB_KEY] = settings.customRgb and 0xFFFFFF
            }
            ListWidget().update(context, glanceId)
        }
    }
}

class ListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ListWidget()
}

private val LIST_LABEL_RES = mapOf(
    "today" to R.string.list_today,
    "inbox" to R.string.list_inbox,
    "upcoming" to R.string.list_upcoming,
    "deadlines" to R.string.list_deadlines,
    "anytime" to R.string.list_anytime,
    "someday" to R.string.list_someday,
)

/** Localized name of a built-in list, or the project's title. */
fun listLabel(context: Context, snapshot: WidgetSnapshot?, listKey: String): String =
    LIST_LABEL_RES[listKey]?.let(context::getString) ?: snapshot?.label(listKey) ?: context.getString(R.string.app_name)

/** The built-in list keys in display order. */
val BUILT_IN_LISTS: List<String> = LIST_LABEL_RES.keys.toList()
