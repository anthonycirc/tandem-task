package dev.thingscompanion.app.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dev.thingscompanion.app.data.ThingsRepository
import dev.thingscompanion.app.data.readWidgetSnapshot
import dev.thingscompanion.app.data.writeWidgetSnapshot
import dev.thingscompanion.app.sync.BackgroundSync
import dev.thingscompanion.cloud.PendingChange
import dev.thingscompanion.cloud.ThingsDay
import dev.thingscompanion.cloud.ThingsIds

/**
 * The widget's circle: checks a to-do. The widget drops it at once, before the slower work of
 * queuing the change (the same queue as the app), which is then sent in the background.
 */
class CheckTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TASK_ID] ?: return
        Log.d(TAG, "check from widget")
        val shown = readWidgetSnapshot(context) ?: return
        writeWidgetSnapshot(context, shown.without(id))
        ListWidget.refreshAll(context)

        val repository = ThingsRepository(context)
        val library = repository.cached()?.library
        // Demo lists, or a to-do gone or no longer checkable since the widget was drawn: show it again, send nothing.
        val task = library?.task(id)?.takeIf(library::canComplete)
        if (task == null) {
            writeWidgetSnapshot(context, shown)
            ListWidget.refreshAll(context)
            return
        }
        repository.enqueue(PendingChange.SetDone(ThingsIds.random(), task.id, true, day = ThingsDay.today().toEpochDay()))
        BackgroundSync.sendSoon(context)
    }

    companion object {
        val TASK_ID = ActionParameters.Key<String>("task_id")
        private const val TAG = "CheckTaskAction"
    }
}
