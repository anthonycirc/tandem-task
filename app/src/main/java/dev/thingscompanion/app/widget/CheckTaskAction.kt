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
        // A tap unticks what the widget shows ticked; a tick it no longer shows (its send is late)
        // is ticked again by this tap, so the tap does what the user sees.
        val (_, ticked) = WidgetCheckStore.update(context) { stored ->
            if (WidgetCheckStore.shown(stored, token).isTicked(id)) stored.without(id) else stored.ticked(id, token)
        }
        ListWidget.refreshAll(context)
        if (ticked.isTicked(id)) WidgetCheckWorker.schedule(context, id, token, CHECK_DELAY_MS)
    }

    companion object {
        val TASK_ID = ActionParameters.Key<String>("task_id")
        private const val TAG = "CheckTaskAction"
        const val CHECK_DELAY_MS = 3_000L
    }
}
