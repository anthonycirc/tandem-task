package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.longOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class ThingsCloudException(val status: Int, message: String) : Exception(message)

data class PullResult(
    /** Start index to resume from on the next pull. */
    val nextStartIndex: Long,
    /** The server's current head, used as `ancestor-index` when committing. */
    val headIndex: Long,
)

/**
 * Client for the unofficial Things Cloud API.
 * Protocol ported from things3-cloud (MIT), see THIRD_PARTY_NOTICES.md.
 * Writes go through [commit]: one history item holding changes to one or more objects.
 */
class ThingsCloudClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val appInstanceId: String,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
) {
    /** Exchanges the account credentials for the history key. The password is not kept. */
    fun authenticate(email: String, password: String): String {
        val body = send("$baseUrl/account/${percentEncode(email)}", "account", "Password ${percentEncode(password)}")
        return body["history-key"].stringOrNull()
            ?: throw ThingsCloudException(200, "missing history-key in auth response")
    }

    /**
     * Replays every history item from [startIndex] into [state].
     * Returns the start index to resume from on the next pull.
     */
    fun pull(historyKey: String, state: HistoryState, startIndex: Long = 0): Long =
        pullWithHead(historyKey, state, startIndex).nextStartIndex

    fun pullWithHead(historyKey: String, state: HistoryState, startIndex: Long = 0): PullResult {
        var index = startIndex
        var head: Long? = null
        while (true) {
            val page = send("$baseUrl/history/${percentEncode(historyKey)}/items?start-index=$index", "history items")
            val items = page["items"] as? JsonArray ?: JsonArray(emptyList())
            for (item in items) {
                (item as? JsonObject)?.let { state.apply(WireParser.parseItem(it)) }
            }
            index += items.size
            head = page["current-item-index"].primitiveOrNull()?.longOrNull ?: head
            val end = page["end-total-content-size"].primitiveOrNull()?.longOrNull ?: 0
            val latest = page["latest-total-content-size"].primitiveOrNull()?.longOrNull ?: 0
            if (end >= latest || items.isEmpty()) return PullResult(index, head ?: index)
        }
    }

    /** Creates an Inbox to-do; see [InboxTask.payload] for the shape and [commitInboxTask] for retries. */
    fun createInboxTask(
        historyKey: String,
        ancestorIndex: Long,
        id: String,
        title: String,
        notes: String,
        index: Int,
        now: Double,
        schedule: Schedule = Schedule.Inbox,
        reminder: LocalTime? = null,
    ): Long = commitInboxTask(historyKey, ancestorIndex, id, InboxTask.payload(title, notes, index, now, schedule, reminder))

    /**
     * Commits the creation of one Inbox to-do built by [InboxTask.payload].
     * Committing the same [id] again replaces the same object, so a retry cannot duplicate it.
     * Returns the new server head index.
     */
    fun commitInboxTask(historyKey: String, ancestorIndex: Long, id: String, payload: JsonObject): Long =
        commit(historyKey, ancestorIndex, mapOf(id to payload))

    /** Commits changes to several objects in one history item; returns the new server head index. */
    fun commit(historyKey: String, ancestorIndex: Long, changes: Map<String, JsonObject>): Long {
        val body = JsonObject(changes)
        val result = send(
            "$baseUrl/history/${percentEncode(historyKey)}/commit?ancestor-index=$ancestorIndex&_cnt=1",
            "commit",
            body = body,
        )
        return result["server-head-index"].primitiveOrNull()?.longOrNull ?: ancestorIndex
    }

    /** [label] names the request in errors; the URL is left out because it holds the email or history key. */
    private fun send(url: String, label: String, authorization: String? = null, body: JsonObject? = null): JsonObject {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Accept-Charset", "UTF-8")
            .header("User-Agent", USER_AGENT)
            .header("things-client-info", CLIENT_INFO)
            .header("App-Id", APP_ID)
            .header("Schema", SCHEMA)
            .header("App-Instance-Id", appInstanceId)
            .apply { if (authorization != null) header("Authorization", authorization) }
            .apply {
                if (body != null) {
                    header("Content-Encoding", "UTF-8")
                    header("Push-Priority", WRITE_PUSH_PRIORITY)
                    post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                }
            }
            .build()
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw ThingsCloudException(response.code, "HTTP ${response.code} for $label")
            }
            if (text.isBlank()) return JsonObject(emptyMap())
            return Json.parseToJsonElement(text) as? JsonObject
                ?: throw ThingsCloudException(response.code, "unexpected JSON for $label")
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://cloud.culturedcode.com/version/1"
        private const val USER_AGENT = "ThingsMac/32209501"
        private const val CLIENT_INFO =
            "eyJkbSI6Ik1hYzE0LDIiLCJsciI6IlVTIiwibmYiOnRydWUsIm5rIjp0cnVlLCJubiI6IlRoaW5nc01hYyIsIm52IjoiMzIyMDk1MDEiLCJvbiI6Im1hY09TIiwib3YiOiIyNi4zLjAiLCJwbCI6ImVuLVVTIiwidWwiOiJlbi1MYXRuLVVTIn0="
        private const val APP_ID = "com.culturedcode.ThingsMac"
        private const val SCHEMA = "301"
        private const val WRITE_PUSH_PRIORITY = "10"
        private val JSON_MEDIA_TYPE = "application/json; charset=UTF-8".toMediaType()

        /** RFC 3986 percent-encoding: keeps unreserved characters, encodes the rest as UTF-8 bytes. */
        internal fun percentEncode(value: String): String = buildString {
            for (byte in value.toByteArray(Charsets.UTF_8)) {
                val c = byte.toInt() and 0xFF
                if (c in 'a'.code..'z'.code || c in 'A'.code..'Z'.code || c in '0'.code..'9'.code || c.toChar() in "-_.~") {
                    append(c.toChar())
                } else {
                    append('%').append("%02X".format(c))
                }
            }
        }
    }
}
