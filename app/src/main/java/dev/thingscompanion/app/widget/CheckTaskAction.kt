package dev.thingscompanion.app.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import kotlinx.coroutines.delay

/**
 * The widget's circle: ticks a to-do, and shows it ticked for [CHECK_DELAY_MS] so a second tap
 * can take a mistake back. [WidgetCheckWorker] then sends the tick if it still holds.
 */
class CheckTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TASK_ID] ?: return
        Log.d(TAG, "tick from widget")
        val token = System.currentTimeMillis()
        val (_, ticked) = WidgetCheckStore.update(context) { it.toggle(id, token) }
        ListWidget.refreshAll(context)
        if (ticked.isTicked(id)) WidgetCheckWorker.schedule(context, id, token, CHECK_DELAY_MS)
    }

    companion object {
        val TASK_ID = ActionParameters.Key<String>("task_id")
        private const val TAG = "CheckTaskAction"
        const val CHECK_DELAY_MS = 3_000L
    }
}
