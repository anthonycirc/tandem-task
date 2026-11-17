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
import kotlinx.coroutines.delay

/**
 * The widget's circle: ticks a to-do, and shows it ticked for [CHECK_DELAY_MS] so a second tap
 * can take a mistake back. Once the delay is over and the tick still holds, the widget drops the
 * to-do and the change goes to the same queue as the app, sent in the background.
 */
class CheckTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TASK_ID] ?: return
        Log.d(TAG, "tick from widget")
        val token = System.currentTimeMillis()
        val (_, ticked) = WidgetCheckStore.update(context) { it.toggle(id, token) }
        ListWidget.refreshAll(context)
        if (!ticked.isTicked(id)) return

        delay(CHECK_DELAY_MS)
        val (before, _) = WidgetCheckStore.update(context) { if (it.stillTicked(id, token)) it.without(id) else it }
        if (!before.stillTicked(id, token)) return

        val shown = readWidgetSnapshot(context)
        shown?.let { writeWidgetSnapshot(context, it.without(id)) }
        ListWidget.refreshAll(context)

        val repository = ThingsRepository(context)
        val library = repository.cached()?.library
        // Demo lists, or a to-do gone or no longer checkable since the widget was drawn: show it again, send nothing.
        val task = library?.task(id)?.takeIf(library::canComplete)
        if (task == null) {
            shown?.let { writeWidgetSnapshot(context, it) }
            ListWidget.refreshAll(context)
            return
        }
        repository.enqueue(PendingChange.SetDone(ThingsIds.random(), task.id, true, day = ThingsDay.today().toEpochDay()))
        BackgroundSync.sendSoon(context)
    }

    companion object {
        val TASK_ID = ActionParameters.Key<String>("task_id")
        private const val TAG = "CheckTaskAction"
        const val CHECK_DELAY_MS = 3_000L
    }
}
