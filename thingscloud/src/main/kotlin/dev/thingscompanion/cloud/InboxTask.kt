package dev.thingscompanion.cloud

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.math.BigDecimal
import java.math.BigInteger
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import java.util.zip.CRC32

/** Things object ids: base58 (Bitcoin alphabet) of the first 16 bytes of SHA-1(uppercase UUID). */
object ThingsIds {
    const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    fun random(): String {
        val digest = MessageDigest.getInstance("SHA-1")
            .digest(UUID.randomUUID().toString().uppercase().toByteArray(Charsets.UTF_8))
        return encode(digest.copyOf(16))
    }

    fun encode(bytes: ByteArray): String {
        val leadingZeros = bytes.takeWhile { it == 0.toByte() }.size
        var value = BigInteger(1, bytes)
        val digits = StringBuilder()
        val base = BigInteger.valueOf(58)
        while (value.signum() > 0) {
            val (quotient, remainder) = value.divideAndRemainder(base)
            digits.append(ALPHABET[remainder.toInt()])
            value = quotient
        }
        return "1".repeat(leadingZeros) + digits.reverse()
    }
}

/** Where a new to-do goes: the Inbox, Today, This Evening, or a later date (Upcoming, then Today on that day). */
sealed interface Schedule {
    data object Inbox : Schedule
    data class Today(val day: LocalDate) : Schedule
    data class Evening(val day: LocalDate) : Schedule
    data class OnDate(val day: LocalDate) : Schedule
    data object Anytime : Schedule
    data object Someday : Schedule

    /** The list the to-do is filed in: `st` on the wire. */
    val start: StartList
        get() = when (this) {
            Inbox -> StartList.INBOX
            is Today, is Evening, Anytime -> StartList.ANYTIME
            is OnDate, Someday -> StartList.SOMEDAY
        }

    val date: LocalDate?
        get() = when (this) {
            Inbox, Anytime, Someday -> null
            is Today -> day
            is Evening -> day
            is OnDate -> day
        }

    val evening: Boolean get() = this is Evening

    companion object {
        /** Where an existing to-do is now; a past date stays as it is (Things keeps it in Today). */
        fun of(task: Task, today: LocalDate): Schedule {
            val date = task.startDate
            return when {
                task.start == StartList.INBOX -> Inbox
                date == null -> if (task.start == StartList.SOMEDAY) Someday else Anytime
                date.isAfter(today) -> OnDate(date)
                task.evening -> Evening(date)
                else -> Today(date)
            }
        }
    }
}

/**
 * The create payload of a new to-do, shaped exactly like the ones the current
 * Things app writes (entity Task7, every property present).
 */
object InboxTask {
    internal const val SECONDS_PER_DAY = 86_400L

    /** Structured notes as the Things app writes them; `ch` is the CRC32 of the UTF-8 text, 0 when empty. */
    internal fun notesJson(text: String) = buildJsonObject {
        put("_t", "tx")
        put("t", 1)
        put("ch", if (text.isEmpty()) 0 else CRC32().apply { update(text.toByteArray(Charsets.UTF_8)) }.value)
        put("v", text)
    }

    internal fun startCode(start: StartList) = when (start) { StartList.INBOX -> 0; StartList.ANYTIME -> 1; StartList.SOMEDAY -> 2 }

    /** Timestamps as plain decimals (`1794700000.25`), never scientific notation. */
    @OptIn(ExperimentalSerializationApi::class)
    internal fun plainNumber(value: Double) = JsonUnquotedLiteral(BigDecimal.valueOf(value).toPlainString())

    /** Whether a stored to-do already holds this draft's text (compared as [payload] would store it). */
    fun matches(task: Task, title: String, notes: String, schedule: Schedule = Schedule.Inbox, reminder: LocalTime? = null): Boolean =
        task.title == title.trim() && (task.notes ?: "") == notes.trim() &&
            task.startDate == schedule.date && task.evening == schedule.evening && task.reminder == reminder

    fun payload(
        title: String,
        notes: String,
        index: Int,
        now: Double,
        schedule: Schedule = Schedule.Inbox,
        reminder: LocalTime? = null,
    ): JsonObject {
        val trimmedTitle = title.trim()
        require(trimmedTitle.isNotEmpty()) { "The title is empty." }
        require(reminder == null || schedule.date != null) { "A reminder needs a date." }
        val trimmedNotes = notes.trim()
        val empty = JsonArray(emptyList())
        val day = schedule.date?.let { JsonPrimitive(it.toEpochDay() * SECONDS_PER_DAY) } ?: JsonNull
        return buildJsonObject {
            put("t", 0)
            put("e", "Task7")
            put("p", buildJsonObject {
                put("acrd", JsonNull)
                put("agr", empty)
                put("ar", empty)
                put("ato", reminder?.let { JsonPrimitive(it.toSecondOfDay()) } ?: JsonNull)
                put("cd", plainNumber(now))
                put("dd", JsonNull)
                put("dds", JsonNull)
                put("dl", empty)
                put("do", 0)
                put("icc", 0)
                put("icp", false)
                put("icsd", JsonNull)
                put("ix", index)
                put("lai", JsonNull)
                put("lt", false)
                put("md", plainNumber(now))
                put("nt", notesJson(trimmedNotes))
                put("pr", empty)
                put("rmd", JsonNull)
                put("rp", JsonNull)
                put("rr", JsonNull)
                put("rt", empty)
                put("sb", if (schedule.evening) 1 else 0)
                put("sp", JsonNull)
                put("sr", day)
                put("ss", 0)
                put("st", startCode(schedule.start))
                put("tg", empty)
                put("ti", 0)
                put("tir", day)
                put("tp", 0)
                put("tr", false)
                put("tt", trimmedTitle)
                put("xx", buildJsonObject {
                    put("_t", "oo")
                    put("sn", JsonObject(emptyMap()))
                })
            })
        }
    }
}
