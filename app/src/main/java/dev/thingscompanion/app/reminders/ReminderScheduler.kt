package dev.thingscompanion.app.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.thingscompanion.app.MainActivity
import dev.thingscompanion.app.R
import dev.thingscompanion.app.data.ThingsRepository
import dev.thingscompanion.cloud.Reminders
import dev.thingscompanion.cloud.ThingsLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Turns the Things reminders of the synced library into Android alarms. Things only rings on
 * Apple devices, so the phone schedules its own notification for each upcoming reminder.
 */
object ReminderScheduler {
    private const val PREFS = "reminders"
    private const val KEY_SCHEDULED = "scheduled_ids"
    private const val HORIZON_DAYS = 14L
    private const val MAX_ALARMS = 100
    const val CHANNEL_ID = "reminders"
    const val EXTRA_ID = "reminder_task_id"
    const val EXTRA_TITLE = "reminder_title"

    /** Replaces the scheduled alarms with the reminders of [library] in the next two weeks. */
    fun schedule(context: Context, library: ThingsLibrary) {
        val now = Instant.now()
        val reminders = Reminders.upcoming(library, now, ZoneId.systemDefault())
            .filter { it.at.isBefore(now.plus(HORIZON_DAYS, ChronoUnit.DAYS)) }
            .take(MAX_ALARMS)
        val alarms = context.getSystemService(AlarmManager::class.java)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val keep = reminders.map { it.id }.toSet()
        prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty()
            .filterNot { it in keep }
            .forEach { alarms.cancel(alarmIntent(context, it, "")) }

        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        for (reminder in reminders) {
            val intent = alarmIntent(context, reminder.id, reminder.title)
            val at = reminder.at.toEpochMilli()
            // Without the exact-alarm permission, Android may deliver a few minutes late.
            if (exact) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            }
        }
        prefs.edit().putStringSet(KEY_SCHEDULED, keep).apply()
    }

    fun cancelAll(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty().forEach { alarms.cancel(alarmIntent(context, it, "")) }
        prefs.edit().remove(KEY_SCHEDULED).apply()
    }

    /** One PendingIntent per to-do: the request code and data URI make it unique, so cancel finds it. */
    private fun alarmIntent(context: Context, id: String, title: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction("dev.thingscompanion.REMINDER")
            .setData(android.net.Uri.parse("things-reminder://$id"))
            .putExtra(EXTRA_ID, id)
            .putExtra(EXTRA_TITLE, title)
        return PendingIntent.getBroadcast(
            context, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.reminders_channel), NotificationManager.IMPORTANCE_HIGH)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

/** Shows the notification when a reminder is due; tapping it opens the to-do. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(ReminderScheduler.EXTRA_ID) ?: return
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE).orEmpty()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        ReminderScheduler.ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            id.hashCode(),
            Intent(context, MainActivity::class.java)
                .setAction("dev.thingscompanion.OPEN:$id")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_TASK_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.reminder_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(id.hashCode(), notification)
    }
}

/** Alarms do not survive a reboot or an app update: schedule them again from the cache. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ThingsRepository(context.applicationContext).cached()?.let { ReminderScheduler.schedule(context, it.library) }
            } finally {
                pending.finish()
            }
        }
    }
}
