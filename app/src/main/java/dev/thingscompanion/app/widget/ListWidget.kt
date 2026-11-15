package dev.thingscompanion.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
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
                // Read on every redraw: a Glance session outlives update() calls, so reading once
                // before provideContent kept showing the lists of the first draw.
                val snapshot = readWidgetSnapshot(context)
                val prefs = currentState<Preferences>()
                val listKey = prefs[LIST_KEY] ?: DEFAULT_LIST
                val theme = prefs[THEME_KEY]?.let { runCatching { WidgetTheme.valueOf(it) }.getOrNull() } ?: WidgetTheme.SYSTEM
                Content(context, snapshot, listKey, WidgetColors(theme, prefs[OPACITY_KEY] ?: DEFAULT_OPACITY))
            }
        }
    }

    @Composable
    private fun Content(context: Context, snapshot: WidgetSnapshot?, listKey: String, colors: WidgetColors) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(colors.background)
                .padding(12.dp),
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = listLabel(context, snapshot, listKey),
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.text),
                    modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(openApp(context))),
                )
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    text = "+",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, color = colors.accent),
                    modifier = GlanceModifier.padding(horizontal = 8.dp).clickable(actionStartActivity(openApp(context, openAdd = true))),
                )
            }
            val items = snapshot?.items(listKey).orEmpty()
            when {
                snapshot == null -> Message(context.getString(R.string.widget_open_app), colors)
                items.isEmpty() -> Message(context.getString(R.string.nothing_here), colors)
                else -> LazyColumn {
                    items(items, itemId = { it.id.hashCode().toLong() }) { item ->
                        Text(
                            text = "○  ${item.title}",
                            maxLines = 2,
                            style = TextStyle(fontSize = 14.sp, color = colors.text),
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable(actionStartActivity(openApp(context, taskId = item.id))),
                        )
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

        /** Redraws every placed widget from the latest snapshot. */
        suspend fun refreshAll(context: Context) = ListWidget().updateAll(context)

        data class Settings(val listKey: String, val theme: WidgetTheme, val opacity: Int)

        suspend fun settings(context: Context, appWidgetId: Int): Settings {
            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
            return Settings(
                listKey = prefs[LIST_KEY] ?: DEFAULT_LIST,
                theme = prefs[THEME_KEY]?.let { runCatching { WidgetTheme.valueOf(it) }.getOrNull() } ?: WidgetTheme.SYSTEM,
                opacity = prefs[OPACITY_KEY] ?: DEFAULT_OPACITY,
            )
        }

        suspend fun save(context: Context, appWidgetId: Int, settings: Settings) {
            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            updateAppWidgetState(context, glanceId) {
                it[LIST_KEY] = settings.listKey
                it[THEME_KEY] = settings.theme.name
                it[OPACITY_KEY] = settings.opacity.coerceIn(0, 100)
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
    "anytime" to R.string.list_anytime,
    "someday" to R.string.list_someday,
)

/** Localized name of a built-in list, or the project's title. */
fun listLabel(context: Context, snapshot: WidgetSnapshot?, listKey: String): String =
    LIST_LABEL_RES[listKey]?.let(context::getString) ?: snapshot?.label(listKey) ?: context.getString(R.string.app_name)

/** The built-in list keys in display order. */
val BUILT_IN_LISTS: List<String> = LIST_LABEL_RES.keys.toList()
