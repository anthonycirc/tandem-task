package dev.thingscompanion.app.widget

import android.content.Context
import dev.thingscompanion.cloud.WidgetChecks

/** The widget's ticked to-dos, shared by every widget and every tap. */
object WidgetCheckStore {
    private const val FILE = "widget_checks"
    private const val KEY = "ticks"
    // How long the widget shows a tick waiting for its send; the send itself holds for a day.
    private const val SHOWN_FOR_MS = 15 * 60_000L
    private val lock = Any()

    /** Ticks the widget shows: a tick whose send has not run after [SHOWN_FOR_MS] is no longer shown. */
    fun read(context: Context): WidgetChecks = stored(context).freshAt(System.currentTimeMillis(), SHOWN_FOR_MS)

    /** Applies [change] to every pending tick, however old, and returns them before and after it. */
    fun update(context: Context, change: (WidgetChecks) -> WidgetChecks): Pair<WidgetChecks, WidgetChecks> = synchronized(lock) {
        val before = stored(context).prunedAt(System.currentTimeMillis())
        val after = change(before)
        prefs(context).edit().putStringSet(KEY, after.encode()).commit()
        before to after
    }

    /** Edits the widget's lists, one edit at a time: concurrent sends never undo each other. */
    fun <T> editSnapshot(block: () -> T): T = synchronized(lock) { block() }

    private fun stored(context: Context) = WidgetChecks.decode(prefs(context).getStringSet(KEY, emptySet()).orEmpty())

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
