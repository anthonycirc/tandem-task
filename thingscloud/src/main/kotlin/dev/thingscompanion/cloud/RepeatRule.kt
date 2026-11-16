package dev.thingscompanion.cloud

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

@kotlinx.serialization.Serializable
enum class RepeatUnit { DAY, WEEK, MONTH, YEAR }

/**
 * How a repeating to-do repeats, read from (and written to) its template's `rr`. [monthDay] is 1-based, -1 meaning the month's last day; [month] is for yearly rules.
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
    /**
     * The `rr` for this rule, starting on [start]: the inverse of [parse]. [anchor] is `ia`,
     * which Things sets to the first occurrence's day when it makes a to-do repeat (and to 0 when
     * it creates a repeating to-do with no occurrence yet). `rc` 0, `rrv` 4 and `ts` 0 are the
     * constants of every day and week rule in the user's history.
     */
    fun toRr(start: LocalDate, anchor: LocalDate?): JsonObject = buildJsonObject {
        put("ed", endDate?.let { it.toEpochDay() * 86_400 } ?: NEVER)
        put("fa", every)
        put("fu", when (unit) { RepeatUnit.DAY -> 16; RepeatUnit.WEEK -> 256; RepeatUnit.MONTH -> 8; RepeatUnit.YEAR -> 4 })
        put("ia", anchor?.let { it.toEpochDay() * 86_400 } ?: 0L)
        putJsonArray("of") {
            when (unit) {
                RepeatUnit.DAY -> Unit
                RepeatUnit.WEEK -> weekdays.forEach { day -> addJsonObject { put("wd", day.value % 7) } }
                RepeatUnit.MONTH, RepeatUnit.YEAR -> if (monthDay != null) addJsonObject {
                    put("dy", if (monthDay < 0) -1 else monthDay - 1)
                    month?.let { put("mo", it.value - 1) }
                }
            }
        }
        put("rc", 0)
        put("rrv", 4)
        put("sr", start.toEpochDay() * 86_400)
        put("tp", if (afterCompletion) 1 else 0)
        put("ts", 0)
    }

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
