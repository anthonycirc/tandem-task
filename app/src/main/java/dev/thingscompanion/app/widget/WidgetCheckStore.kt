package dev.thingscompanion.app.widget

import android.content.Context
import dev.thingscompanion.cloud.WidgetChecks

/** The widget's ticked to-dos, shared by every widget and every tap. */
object WidgetCheckStore {
    private const val FILE = "widget_checks"
    private const val KEY = "ticks"
    private const val MAX_AGE_MS = 30_000L
    private val lock = Any()

    /** Ticks still waiting for their delay; one left by a send cut short expires. */
    fun read(context: Context): WidgetChecks =
        WidgetChecks.decode(prefs(context).getStringSet(KEY, emptySet()).orEmpty()).freshAt(System.currentTimeMillis(), MAX_AGE_MS)

    /** Applies [change] and returns the ticks before and after it. */
    fun update(context: Context, change: (WidgetChecks) -> WidgetChecks): Pair<WidgetChecks, WidgetChecks> = synchronized(lock) {
        val before = read(context)
        val after = change(before)
        prefs(context).edit().putStringSet(KEY, after.encode()).commit()
        before to after
    }

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
