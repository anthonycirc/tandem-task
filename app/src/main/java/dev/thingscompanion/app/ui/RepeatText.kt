package dev.thingscompanion.app.ui

import android.content.Context
import dev.thingscompanion.app.R
import dev.thingscompanion.cloud.RepeatRule
import dev.thingscompanion.cloud.RepeatUnit
import java.time.MonthDay
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** "Every week on Monday, Friday", "Tous les 3 mois le dernier jour, après achèvement"… */
fun describeRepeat(context: Context, rule: RepeatRule): String {
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    val n = rule.every
    var text = when (rule.unit) {
        RepeatUnit.DAY -> if (n == 1) context.getString(R.string.repeat_daily) else context.getString(R.string.repeat_days, n)
        RepeatUnit.WEEK -> if (n == 1) context.getString(R.string.repeat_weekly) else context.getString(R.string.repeat_weeks, n)
        RepeatUnit.MONTH -> if (n == 1) context.getString(R.string.repeat_monthly) else context.getString(R.string.repeat_months, n)
        RepeatUnit.YEAR -> if (n == 1) context.getString(R.string.repeat_yearly) else context.getString(R.string.repeat_years, n)
    }
    val ordinal = rule.weekdayOrdinal
    if (ordinal != null && rule.weekdays.size == 1) {
        text = context.getString(R.string.repeat_on_nth, text, ordinalName(context, ordinal), rule.weekdays.single().getDisplayName(TextStyle.FULL, locale))
    } else if (rule.weekdays.isNotEmpty()) {
        val days = rule.weekdays.joinToString(", ") { it.getDisplayName(TextStyle.FULL, locale) }
        text = context.getString(R.string.repeat_on, text, days)
    } else if (rule.month != null && rule.monthDay != null && rule.monthDay!! > 0) {
        val date = runCatching {
            MonthDay.of(rule.month, rule.monthDay!!).format(DateTimeFormatter.ofPattern(context.getString(R.string.repeat_month_day_pattern), locale))
        }.getOrNull()
        if (date != null) text = context.getString(R.string.repeat_on, text, date)
    } else if (rule.monthDay == -1) {
        text = context.getString(R.string.repeat_on_last_day, text)
    } else if (rule.monthDay != null) {
        text = context.getString(R.string.repeat_on_day, text, rule.monthDay)
    }
    if (rule.afterCompletion) text = context.getString(R.string.repeat_after_completion, text)
    rule.endDate?.let { text = context.getString(R.string.repeat_until, text, it.format(dateFormat)) }
    return text
}

/** "first" .. "fourth", or "last" for -1, as a monthly rule names the weekday's rank. */
fun ordinalName(context: Context, ordinal: Int): String = when (ordinal) {
    1 -> context.getString(R.string.repeat_ordinal_1)
    2 -> context.getString(R.string.repeat_ordinal_2)
    3 -> context.getString(R.string.repeat_ordinal_3)
    4 -> context.getString(R.string.repeat_ordinal_4)
    5 -> context.getString(R.string.repeat_ordinal_5)
    else -> context.getString(R.string.repeat_ordinal_last)
}
