package dev.thingscompanion.cloud

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

enum class RepeatUnit { DAY, WEEK, MONTH, YEAR }

/**
 * How a repeating to-do repeats, read from its template's `rr` (display only; the app does not
 * write rules). [monthDay] is 1-based, -1 meaning the month's last day; [month] is for yearly rules.
 */
data class RepeatRule(
    val unit: RepeatUnit,
    val every: Int,
    val afterCompletion: Boolean,
    val weekdays: List<DayOfWeek> = emptyList(),
    val monthDay: Int? = null,
    val month: Month? = null,
    val endDate: LocalDate? = null,
) {
    companion object {
        /** `ed` Things writes for "never ends" (year 4001). */
        private const val NEVER = 64_092_211_200L

        /**
         * Parses `rr`: `fu` is the unit as an NSCalendarUnit (16 day, 256 week, 8 month, 4 year),
         * `fa` the interval, `tp` 1 for "after completion", `of` the days: `wd` 0 = Sunday,
         * `dy` 0-based day of month (-1 last day), `mo` 0-based month. Unknown units give null.
         */
        internal fun parse(rr: JsonObject): RepeatRule? {
            fun JsonObject.int(key: String) = this[key].primitiveOrNull()?.intOrNull
            val unit = when (rr.int("fu")) {
                16 -> RepeatUnit.DAY
                256 -> RepeatUnit.WEEK
                8 -> RepeatUnit.MONTH
                4 -> RepeatUnit.YEAR
                else -> return null
            }
            val offsets = (rr["of"] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
            val first = offsets.firstOrNull()
            val dayOfMonth = first?.int("dy")?.let { if (it < 0) -1 else it + 1 }
            val end = rr["ed"].primitiveOrNull()?.longOrNull?.takeIf { it != NEVER }?.let(ThingsDay::toLocalDate)
            return RepeatRule(
                unit = unit,
                every = rr.int("fa")?.takeIf { it > 0 } ?: 1,
                afterCompletion = rr.int("tp") == 1,
                weekdays = if (unit == RepeatUnit.WEEK) {
                    offsets.mapNotNull { it.int("wd") }.filter { it in 0..6 }
                        .map { if (it == 0) DayOfWeek.SUNDAY else DayOfWeek.of(it) }
                } else emptyList(),
                monthDay = if (unit == RepeatUnit.MONTH || unit == RepeatUnit.YEAR) dayOfMonth else null,
                month = if (unit == RepeatUnit.YEAR) first?.int("mo")?.takeIf { it in 0..11 }?.let { Month.of(it + 1) } else null,
                endDate = end,
            )
        }
    }
}
