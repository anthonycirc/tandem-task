package dev.thingscompanion.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.thingscompanion.app.data.ThingsRepository
import dev.thingscompanion.app.widget.ListWidget
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Pulls new history items in the background so the widget stays current without opening the app. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repository = ThingsRepository(applicationContext)
        if (!repository.isSignedIn) return Result.success()
        return try {
            repository.refresh()
            runCatching { ListWidget.refreshAll(applicationContext) }
            Result.success()
        } catch (e: IOException) {
            Result.retry()
        } catch (e: Exception) {
            // Auth or server errors: wait for the next period rather than hammering the server.
            Result.failure()
        }
    }
}

object BackgroundSync {
    private const val WORK_NAME = "things_background_sync"
    private const val INTERVAL_MINUTES = 30L

    /** Idempotent: keeps an already scheduled sync. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
