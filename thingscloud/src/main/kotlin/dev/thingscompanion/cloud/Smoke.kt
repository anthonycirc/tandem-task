package dev.thingscompanion.cloud

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.io.File
import java.util.UUID
import kotlin.system.exitProcess

/**
 * Live read-only check against a real account:
 *   THINGS_EMAIL=... THINGS_PASSWORD=... ./gradlew -q :thingscloud:run
 * Prints counts only. Writes each list's uuids and dates to build/smoke/lists.json, plus the replayed state (your data, gitignored)
 * so they can be compared with the Things database on a Mac. Never prints or stores the credentials.
 */
fun main() {
    val email = System.getenv("THINGS_EMAIL")
    val password = System.getenv("THINGS_PASSWORD")
    if (email.isNullOrBlank() || password.isNullOrBlank()) {
        System.err.println("Set THINGS_EMAIL and THINGS_PASSWORD.")
        exitProcess(2)
    }

    val client = ThingsCloudClient(appInstanceId = UUID.randomUUID().toString())
    val historyKey = client.authenticate(email, password)
    val state = HistoryState()
    val nextIndex = client.pull(historyKey, state)
    val library = ThingsLibrary.from(state)
    val today = ThingsDay.today()

    val lists = listOf(
        "today" to library.today(today),
        "inbox" to library.inbox(),
        "upcoming" to library.upcoming(today),
        "anytime" to library.anytime(today),
        "someday" to library.someday(),
        "projects" to library.projects(),
    )
    // Things encodes days as UTC midnight; any remainder means the day encoding is wrong.
    val dayRemainders = state.objects.values.flatMap { obj -> listOf("sr", "dd").mapNotNull { obj.props[it] } }
        .mapNotNull { it.primitiveOrNull()?.longOrNull?.mod(86_400L) }
        .filter { it != 0L }

    val report = buildJsonObject {
        put("today", today.toString())
        put("historyItems", nextIndex)
        put("nonMidnightDays", dayRemainders.size)
        for ((name, tasks) in lists) {
            putJsonArray(name) {
                tasks.forEach { task ->
                    addJsonObject {
                        put("uuid", task.id)
                        put("startDate", task.startDate?.let { JsonPrimitive(it.toString()) } ?: JsonNull)
                        put("deadline", task.deadline?.let { JsonPrimitive(it.toString()) } ?: JsonNull)
                    }
                }
            }
        }
    }
    val out = File("build/smoke/lists.json").apply { parentFile.mkdirs() }
    out.writeText(report.toString())
    File("build/smoke/diagnostics.json").writeText(diagnostics(state).toString())
    // Full replayed state (your data, local and gitignored) so list rules can be checked without pulling again.
    File("build/smoke/state.json").writeText(state.toJson())

    println("History items: $nextIndex, objects: ${state.objects.size}, non-midnight days: ${dayRemainders.size}")
    lists.forEach { (name, tasks) -> println("$name: ${tasks.size}") }
    println("Wrote ${out.parentFile.absolutePath}/{lists,diagnostics,state}.json")
}

/** Shape of the data without any content: entity counts, and value kinds of the list-deciding fields per entity. */
private fun diagnostics(state: HistoryState) = buildJsonObject {
    val byEntity = state.objects.values.groupBy { it.entity ?: "null" }
    for ((entity, objects) in byEntity) {
        put(entity, buildJsonObject {
            put("count", objects.size)
            for (key in listOf("tp", "ss", "st", "sr", "tir", "dd", "tr", "pr", "ar", "agr", "rr", "rt", "sb", "ti")) {
                val kinds = objects.groupingBy { obj -> valueKind(obj.props[key]) }.eachCount()
                if (kinds.keys != setOf("absent")) {
                    put(key, buildJsonObject { kinds.forEach { (kind, n) -> put(kind, n) } })
                }
            }
        })
    }
}

private fun valueKind(value: kotlinx.serialization.json.JsonElement?): String = when (value) {
    null -> "absent"
    is JsonNull -> "null"
    is JsonPrimitive -> when {
        value.isString -> "string"
        value.content == "true" || value.content == "false" -> "bool:${value.content}"
        value.longOrNull != null && value.longOrNull!! in -10..10 -> "int:${value.content}"
        value.longOrNull != null -> "long"
        else -> "number:${value.content.take(4)}…"
    }
    is kotlinx.serialization.json.JsonArray -> if (value.isEmpty()) "array:empty" else "array:${value.size}"
    is kotlinx.serialization.json.JsonObject -> "object"
}
