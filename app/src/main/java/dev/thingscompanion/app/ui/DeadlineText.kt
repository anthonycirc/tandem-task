package dev.thingscompanion.app.ui

import android.content.Context
import dev.thingscompanion.app.R
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** A deadline as Things words it next to a to-do: "today", "3 days left" or "2 days ago". */
fun deadlineText(context: Context, deadline: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, deadline).toInt()
    val res = context.resources
    return when {
        days == 0 -> context.getString(R.string.deadline_today)
        days > 0 -> res.getQuantityString(R.plurals.deadline_days_left, days, days)
        else -> res.getQuantityString(R.plurals.deadline_days_ago, -days, -days)
    }
}
