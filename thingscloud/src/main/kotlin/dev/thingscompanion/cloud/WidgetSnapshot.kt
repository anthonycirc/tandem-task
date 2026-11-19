package dev.thingscompanion.cloud

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * A small copy of every list, written after each sync, so a home-screen widget can show
 * any of them without loading the whole history. Keys: `today`, `inbox`, `upcoming`,
 * `anytime`, `someday`, and `project:<id>`.
 */
@Serializable
data class WidgetSnapshot(
    val updatedAtMillis: Long,
    val lists: Map<String, List<Item>>,
    val projects: List<Item>,
) {
    /**
     * [canToggle]: the widget may check it (see [ThingsLibrary.canComplete]); otherwise its circle opens it.
     * [deadlineDay] (epoch day) and [reminderSeconds] (from midnight) are highlighted by the widget.
     */
    @Serializable
    data class Item(
        val id: String,
        val title: String,
        val canToggle: Boolean = true,
        val deadlineDay: Long? = null,
        val reminderSeconds: Int? = null,
    )

    fun items(key: String): List<Item> = lists[key].orEmpty()

    fun label(key: String): String =
        LIST_LABELS[key] ?: projects.firstOrNull { "$PROJECT_PREFIX${it.id}" == key }?.title ?: "Things"

    /** The same lists without [id]: what the widget shows right after it is checked. */
    fun without(id: String): WidgetSnapshot = copy(lists = lists.mapValues { (_, items) -> items.filterNot { it.id == id } })

    fun toJson(): String = Json.encodeToString(serializer(), this)

    companion object {
        const val PROJECT_PREFIX = "project:"

        /** The built-in lists, in the order the app shows them. */
        val LIST_LABELS = linkedMapOf(
            "today" to "Today",
            "inbox" to "Inbox",
            "upcoming" to "Upcoming",
            "anytime" to "Anytime",
            "someday" to "Someday",
        )

        fun from(library: ThingsLibrary, today: LocalDate, updatedAtMillis: Long): WidgetSnapshot {
            fun List<Task>.items() = map {
                Item(it.id, it.title, canToggle = library.canComplete(it), deadlineDay = it.deadline?.toEpochDay(), reminderSeconds = it.reminder?.toSecondOfDay())
            }
            val projects = library.projects()
            // Open to-dos only: a to-do checked from the widget leaves it.
            val lists = mapOf(
                "today" to library.today(today).items(),
                "inbox" to library.inbox().items(),
                "upcoming" to library.upcoming(today).items(),
                "anytime" to library.anytime(today).items(),
                "someday" to library.someday().items(),
            ) + projects.associate { "$PROJECT_PREFIX${it.id}" to library.projectTasks(it.id).items() }
            return WidgetSnapshot(updatedAtMillis, lists, projects.map { Item(it.id, it.title) })
        }

        /** Lenient: a snapshot written by another app version still reads. */
        fun fromJson(json: String): WidgetSnapshot = lenient.decodeFromString(serializer(), json)

        private val lenient = Json { ignoreUnknownKeys = true }
    }
}
