package dev.thingscompanion.cloud

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class Reminder(val id: String, val title: String, val at: Instant)

/** Reminders to notify on this device: still ahead, and not already handled on another device. */
object Reminders {
    fun upcoming(library: ThingsLibrary, now: Instant, zone: ZoneId): List<Reminder> =
        library.withReminders()
            .mapNotNull { task ->
                val at = ZonedDateTime.of(task.startDate!!, task.reminder!!, zone).toInstant()
                val handled = task.lastAlarmInteraction?.let { it >= at.epochSecond } ?: false
                if (at.isAfter(now) && !handled) Reminder(task.id, task.title, at) else null
            }
            .sortedBy { it.at }
}
