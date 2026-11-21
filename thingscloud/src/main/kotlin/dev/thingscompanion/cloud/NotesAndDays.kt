package dev.thingscompanion.cloud

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Task notes come as a plain string, or a structured object: type 1 is the whole text (`v`), type 2
 * the edits made to the note before it (`ps`, see [edited]).
 */
object Notes {
    fun toPlainText(notes: JsonElement?): String? {
        val text = when (notes) {
            is JsonPrimitive -> if (notes.isString) notes.content else null
            is JsonObject -> when ((notes["t"] as? JsonPrimitive)?.intOrNull) {
                1 -> notes["v"].stringOrNull()
                // Edits without the note they apply to: made on an empty one.
                2 -> (notes["ps"] as? JsonArray)?.let { edited("", it) }
                else -> null
            }
            else -> null
        } ?: return null
        return text.replace(' ', '\n').replace(' ', '\n').trim().ifEmpty { null }
    }

    /**
     * The note [update] leaves on an object whose note was [before]: a type 2 update is turned into
     * the whole edited text (type 1), any other one replaces it as it is.
     */
    fun merge(before: JsonElement?, update: JsonElement): JsonElement {
        val patches = (update as? JsonObject)?.takeIf { (it["t"] as? JsonPrimitive)?.intOrNull == 2 }?.get("ps") as? JsonArray
            ?: return update
        return InboxTask.notesJson(edited(rawText(before), patches))
    }

    /** The text as stored, untrimmed, so later edits land on the offsets Things counted from. */
    private fun rawText(notes: JsonElement?): String = when (notes) {
        is JsonPrimitive -> if (notes.isString) notes.content else ""
        is JsonObject -> when ((notes["t"] as? JsonPrimitive)?.intOrNull) {
            1 -> notes["v"].stringOrNull().orEmpty()
            2 -> (notes["ps"] as? JsonArray)?.let { edited("", it) }.orEmpty()
            else -> ""
        }
        else -> ""
    }

    /**
     * [text] with each patch applied in turn: `l` bytes at byte `p` replaced by `r`. Things counts
     * in bytes of the UTF-8 text (each patch's `ch` is the CRC32 of the text it leaves).
     */
    private fun edited(text: String, patches: JsonArray): String {
        var bytes = text.toByteArray(Charsets.UTF_8)
        for (patch in patches) {
            val p = patch as? JsonObject ?: continue
            val position = ((p["p"] as? JsonPrimitive)?.intOrNull ?: 0).coerceIn(0, bytes.size)
            val length = ((p["l"] as? JsonPrimitive)?.intOrNull ?: 0).coerceIn(0, bytes.size - position)
            val replacement = p["r"].stringOrNull().orEmpty().toByteArray(Charsets.UTF_8)
            bytes = bytes.copyOfRange(0, position) + replacement + bytes.copyOfRange(position + length, bytes.size)
        }
        return String(bytes, Charsets.UTF_8)
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
