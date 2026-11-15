package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

enum class Operation { CREATE, UPDATE, DELETE, UNKNOWN }

/** One entry of a history item: the change applied to the object [uuid]. */
data class WireChange(
    val uuid: String,
    val operation: Operation,
    val entity: String?,
    val props: JsonObject,
)

/** Parses Things Cloud history items: `{uuid: {"t": op, "e": entity, "p": props}}`. */
object WireParser {
    fun parseItem(item: JsonObject): List<WireChange> = item.mapNotNull { (uuid, value) ->
        val obj = value as? JsonObject ?: return@mapNotNull null
        val operation = when ((obj["t"] as? JsonPrimitive)?.intOrNull) {
            0 -> Operation.CREATE
            1 -> Operation.UPDATE
            2 -> Operation.DELETE
            else -> Operation.UNKNOWN
        }
        val entity = (obj["e"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        WireChange(uuid, operation, entity, obj["p"] as? JsonObject ?: JsonObject(emptyMap()))
    }
}

data class StoredObject(val entity: String?, val props: Map<String, JsonElement>)

/** The current state of every object, rebuilt by replaying the history in order. */
class HistoryState(initial: Map<String, StoredObject> = emptyMap()) {
    private val _objects = initial.toMutableMap()
    val objects: Map<String, StoredObject> get() = _objects

    fun apply(changes: List<WireChange>) {
        for (change in changes) {
            when (change.operation) {
                Operation.CREATE -> _objects[change.uuid] = StoredObject(change.entity, change.props)
                Operation.UPDATE -> {
                    val existing = _objects[change.uuid]
                    _objects[change.uuid] = if (existing == null) {
                        StoredObject(change.entity, change.props)
                    } else {
                        StoredObject(change.entity ?: existing.entity, existing.props + change.props)
                    }
                }
                Operation.DELETE -> _objects.remove(change.uuid)
                Operation.UNKNOWN -> Unit
            }
        }
    }

    fun toJson(): String = Json.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            for ((uuid, obj) in _objects) {
                put(uuid, buildJsonObject {
                    put("e", obj.entity?.let(::JsonPrimitive) ?: JsonNull)
                    put("p", JsonObject(obj.props))
                })
            }
        },
    )

    companion object {
        fun fromJson(json: String): HistoryState {
            val root = Json.parseToJsonElement(json).jsonObject
            return HistoryState(root.mapValues { (_, value) ->
                val obj = value.jsonObject
                val entity = (obj["e"] as? JsonPrimitive)?.takeIf { it.isString }?.content
                StoredObject(entity, obj["p"]?.jsonObject ?: emptyMap())
            })
        }
    }
}

internal fun JsonElement?.stringOrNull(): String? =
    (this as? JsonPrimitive)?.takeIf { it.isString }?.content

internal fun JsonElement?.primitiveOrNull(): JsonPrimitive? =
    (this as? JsonPrimitive)?.takeIf { it !is JsonNull }
