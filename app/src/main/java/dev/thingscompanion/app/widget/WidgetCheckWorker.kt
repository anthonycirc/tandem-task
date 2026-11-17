package dev.thingscompanion.app.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dev.thingscompanion.app.data.ThingsRepository
import dev.thingscompanion.app.data.readWidgetSnapshot
import dev.thingscompanion.app.data.writeWidgetSnapshot
import dev.thingscompanion.app.sync.BackgroundSync
import dev.thingscompanion.cloud.PendingChange
import dev.thingscompanion.cloud.ThingsDay
import dev.thingscompanion.cloud.ThingsIds
import java.util.concurrent.TimeUnit

/**
 * Sends a widget tick once its delay is over, if that same tap still holds it: the widget drops the
 * to-do and the check goes to the app's queue. WorkManager runs it even if the widget's process
 * was stopped in between.
 */
class WidgetCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString(TASK_ID) ?: return Result.success()
        val token = inputData.getLong(TOKEN, 0)
        val context = applicationContext
        val (before, _) = WidgetCheckStore.update(context) { if (it.stillTicked(id, token)) it.without(id) else it }
        if (!before.stillTicked(id, token)) return Result.success()

        val repository = ThingsRepository(context)
        val library = repository.cached()?.library
        // Demo lists, or a to-do gone or no longer checkable since the widget was drawn: it stays
        // listed, unticked, and nothing is sent.
        val task = library?.task(id)?.takeIf(library::canComplete)
        if (task == null) {
            ListWidget.refreshAll(context)
            return Result.success()
        }
        WidgetCheckStore.editSnapshot {
            readWidgetSnapshot(context)?.let { writeWidgetSnapshot(context, it.without(id)) }
        }
        ListWidget.refreshAll(context)
        repository.enqueue(PendingChange.SetDone(ThingsIds.random(), task.id, true, day = ThingsDay.today().toEpochDay()))
        BackgroundSync.sendSoon(context)
        return Result.success()
    }

    companion object {
        private const val TASK_ID = "task_id"
        private const val TOKEN = "token"

        /**
         * Sends the tick of [id] made by the tap [token] after [delayMillis]. Each tap has its own
         * work, never cancelled by a later tap (one could be mid-send): only the work of the tap
         * that still holds the tick sends it.
         */
        fun schedule(context: Context, id: String, token: Long, delayMillis: Long) {
            val request = OneTimeWorkRequestBuilder<WidgetCheckWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(TASK_ID to id, TOKEN to token))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("widget_check_${id}_$token", ExistingWorkPolicy.KEEP, request)
        }
    }
}
