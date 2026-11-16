package dev.thingscompanion.cloud

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * How the Things apps write "repeat after completion" to-dos, read from raw history items:
 * which objects each write touches, with which operation and properties, and dates as day
 * offsets from the completion in the same item. Titles, notes and other values are never
 * reported, so the result can be shared.
 */
object RepeatDiagnostics {
    private val DATE_KEYS = setOf("sr", "tir", "dd", "sp", "acrd", "cd", "md", "lai", "ia", "ed")
    private val ENUM_KEYS = setOf("ss", "st", "sb", "tp", "tr", "fu", "fa", "rrv", "rc", "ts")

    fun analyze(items: List<JsonObject>, lastEvents: Int = 15): JsonObject {
        // Templates that repeat after completion at any point of the history.
        val templates = mutableSetOf<String>()
        for (item in items) for ((id, change) in item) {
            val rr = ((change as? JsonObject)?.get("p") as? JsonObject)?.get("rr") as? JsonObject
            if (rr?.get("tp").primitiveOrNull()?.intOrNull == 1) templates += id
        }

        val instances = mutableSetOf<String>()
        val events = mutableListOf<JsonObject>()
        val patterns = linkedMapOf<String, Int>()
        // Current rule of each template, and the day of its last completion still waiting for a next instance.
        val rules = mutableMapOf<String, JsonObject>()
        val waiting = mutableMapOf<String, Long>()
        val completions = linkedMapOf<String, Int>()
        val nextInstances = linkedMapOf<String, Int>()
        fun count(map: MutableMap<String, Int>, key: JsonObject) {
            map[key.toString()] = (map[key.toString()] ?: 0) + 1
        }
        fun days(value: JsonElement?, from: Long): String? {
            val seconds = value.primitiveOrNull()?.doubleOrNull?.toLong() ?: return null
            val delta = Math.floorDiv(seconds, InboxTask.SECONDS_PER_DAY) - from
            return if (delta >= 0) "+${delta}d" else "${delta}d"
        }
        fun ruleName(id: String) = rules[id]?.let {
            "${it["fu"].primitiveOrNull()?.intOrNull}x${it["fa"].primitiveOrNull()?.intOrNull}"
        } ?: "unknown"
        fun afterCompletion(id: String) = rules[id]?.get("tp").primitiveOrNull()?.intOrNull == 1
        items.forEachIndexed { index, item ->
            val changes = item.mapNotNull { (id, value) -> (value as? JsonObject)?.let { id to it } }
            fun props(change: JsonObject) = change["p"] as? JsonObject ?: JsonObject(emptyMap())
            fun refersToTemplate(change: JsonObject) =
                (props(change)["rt"] as? JsonArray)?.any { it.stringOrNull() in templates } == true

            val touched = changes.filter { (id, change) -> id in templates || id in instances || refersToTemplate(change) }
            if (touched.isEmpty()) return@forEachIndexed

            // Completion time of this item, when an instance is checked in it.
            val completionDay = changes.firstNotNullOfOrNull { (id, change) ->
                if (id in instances || refersToTemplate(change)) props(change)["sp"].primitiveOrNull()?.doubleOrNull else null
            }?.let { Math.floorDiv(it.toLong(), InboxTask.SECONDS_PER_DAY) }

            // Follow each template's rule as it changes, before reading this item's completion.
            changes.filter { (id, _) -> id in templates }.forEach { (id, change) ->
                (props(change)["rr"] as? JsonObject)?.let { rr -> rules[id] = JsonObject((rules[id] ?: JsonObject(emptyMap())) + rr) }
            }
            // The template's values written with the completion, and the first instance created after it.
            if (completionDay != null) {
                changes.filter { (id, change) -> id in templates && afterCompletion(id) && "acrd" in props(change) }.forEach { (id, change) ->
                    val p = props(change)
                    val acrd = p["acrd"].primitiveOrNull()?.doubleOrNull
                    count(completions, buildJsonObject {
                        put("rule", ruleName(id))
                        put("acrd", days(p["acrd"], completionDay) ?: "null")
                        put("tir", days(p["tir"], completionDay) ?: "absent")
                        put("acrdAtMidnight", acrd != null && acrd % InboxTask.SECONDS_PER_DAY == 0.0)
                    })
                    waiting[id] = completionDay
                }
            }
            changes.filter { (_, change) -> change["t"].primitiveOrNull()?.intOrNull == 0 && refersToTemplate(change) }.forEach { (_, change) ->
                val template = (props(change)["rt"] as? JsonArray)?.firstNotNullOfOrNull { it.stringOrNull()?.takeIf(waiting::containsKey) }
                    ?: return@forEach
                val from = waiting.remove(template)!!
                count(nextInstances, buildJsonObject {
                    put("rule", ruleName(template))
                    put("srFromCompletion", days(props(change)["sr"], from) ?: "null")
                    put("tirFromCompletion", days(props(change)["tir"], from) ?: "null")
                    put("sameItemAsCompletion", completionDay != null)
                })
            }

            val described = changes.map { (id, change) ->
                val op = change["t"].primitiveOrNull()?.intOrNull
                val role = when {
                    id in templates -> "template"
                    op == 0 && refersToTemplate(change) -> "new-instance"
                    id in instances || refersToTemplate(change) -> "instance"
                    else -> "other"
                }
                describe(role, change, completionDay)
            }
            changes.forEach { (id, change) -> if (refersToTemplate(change)) instances += id }

            val signature = described.joinToString(" | ") { "${it["role"].stringOrNull()} ${it["op"]} ${it["keys"]}" }
            patterns[signature] = (patterns[signature] ?: 0) + 1
            events += buildJsonObject {
                put("item", index)
                put("changes", JsonArray(described))
            }
        }

        return buildJsonObject {
            put("afterCompletionTemplates", templates.size)
            put("eventsSeen", events.size)
            putJsonArray("patterns") {
                patterns.entries.sortedByDescending { it.value }.forEach { (signature, count) ->
                    addJsonObject {
                        put("count", count)
                        put("shape", signature)
                    }
                }
            }
            put("completions", summary(completions))
            put("nextInstances", summary(nextInstances))
            put("events", JsonArray(events.takeLast(lastEvents)))
        }
    }

    private fun summary(counts: Map<String, Int>) = JsonArray(
        counts.entries.sortedByDescending { it.value }.map { (shape, n) ->
            buildJsonObject {
                put("count", n)
                put("values", kotlinx.serialization.json.Json.parseToJsonElement(shape))
            }
        },
    )

    /** One object's change: role, entity, operation, property names and safe values only. */
    private fun describe(role: String, change: JsonObject, completionDay: Long?): JsonObject {
        val props = change["p"] as? JsonObject ?: JsonObject(emptyMap())
        val keys = mutableListOf<String>()
        val values = linkedMapOf<String, JsonElement>()
        for ((key, value) in props) {
            if (key == "rr" && value is JsonObject) {
                for ((sub, subValue) in value) {
                    keys += "rr.$sub"
                    safeValue(sub, subValue, completionDay)?.let { values["rr.$sub"] = it }
                }
                if (value.isEmpty()) keys += "rr"
            } else {
                keys += key
                safeValue(key, value, completionDay)?.let { values[key] = it }
            }
        }
        return buildJsonObject {
            put("role", role)
            put("entity", change["e"].stringOrNull())
            put("op", change["t"].primitiveOrNull()?.intOrNull)
            put("keys", JsonArray(keys.sorted().map(::JsonPrimitive)))
            putJsonObject("values") { values.forEach { (k, v) -> put(k, v) } }
        }
    }

    private fun safeValue(key: String, value: JsonElement, completionDay: Long?): JsonElement? = when {
        value is JsonNull -> JsonPrimitive("null")
        key == "rt" -> JsonPrimitive("refs:${(value as? JsonArray)?.size ?: 0}")
        key in ENUM_KEYS -> (value as? JsonPrimitive)?.takeIf { !it.isString }
        key in DATE_KEYS -> {
            val seconds = value.primitiveOrNull()?.doubleOrNull?.toLong()
            when {
                seconds == null -> JsonPrimitive("set")
                completionDay == null -> JsonPrimitive("date")
                else -> {
                    val delta = Math.floorDiv(seconds, InboxTask.SECONDS_PER_DAY) - completionDay
                    JsonPrimitive(if (delta >= 0) "+${delta}d" else "${delta}d")
                }
            }
        }
        key == "of" -> JsonPrimitive("offsets:${(value as? JsonArray)?.size ?: 0}")
        else -> null
    }
}
