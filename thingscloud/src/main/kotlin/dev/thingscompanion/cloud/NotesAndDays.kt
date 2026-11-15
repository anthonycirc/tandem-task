package dev.thingscompanion.cloud

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Task notes come as a plain string, or a structured object of type 1 (`v`) or 2 (`ps[].r`). */
object Notes {
    fun toPlainText(notes: JsonElement?): String? {
        val text = when (notes) {
            is JsonPrimitive -> if (notes.isString) notes.content else null
            is JsonObject -> when ((notes["t"] as? JsonPrimitive)?.intOrNull) {
                1 -> notes["v"].stringOrNull()
                2 -> (notes["ps"] as? JsonArray)
                    ?.mapNotNull { (it as? JsonObject)?.get("r").stringOrNull() }
                    ?.joinToString("\n")
                else -> null
            }
            else -> null
        } ?: return null
        return text.replace(' ', '\n').replace(' ', '\n').trim().ifEmpty { null }
    }
}

/**
 * Things stores a calendar day as the epoch seconds of that date at 00:00 UTC.
 * "Today" is the user's local calendar date encoded the same way.
 */
object ThingsDay {
    private const val SECONDS_PER_DAY = 86_400L

    fun toLocalDate(seconds: Long): LocalDate = LocalDate.ofEpochDay(Math.floorDiv(seconds, SECONDS_PER_DAY))

    fun today(now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        now.atZone(zone).toLocalDate()
}
