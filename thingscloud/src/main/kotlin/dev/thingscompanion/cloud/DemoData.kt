package dev.thingscompanion.cloud

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.LocalTime

/** Sample to-dos for the demo mode and screenshots: realistic, invented, never the user's data. */
object DemoData {

    fun state(today: LocalDate, nowSeconds: Double): HistoryState {
        val items = mutableMapOf<String, JsonElement>()

        fun put(id: String, entity: String, props: Map<String, Any?>) {
            items[id] = buildJsonObject {
                put("t", 0)
                put("e", entity)
                put("p", JsonObject(props.mapValues { (_, v) -> json(v) }))
            }
        }

        fun area(id: String, title: String, index: Int) = put(id, "Area3", mapOf("tt" to title, "ix" to index))
        fun tag(id: String, title: String) = put(id, "Tag4", mapOf("tt" to title))

        fun todo(
            id: String,
            title: String,
            schedule: Schedule,
            index: Int,
            notes: String = "",
            reminder: LocalTime? = null,
            project: String? = null,
            area: String? = null,
            tags: List<String> = emptyList(),
            deadline: LocalDate? = null,
            completed: Boolean = false,
            type: Int = 0,
            extra: Map<String, JsonElement> = emptyMap(),
        ) {
            val base = (InboxTask.payload(title, notes, index, nowSeconds, schedule, reminder)["p"] as JsonObject).toMutableMap()
            base["tp"] = JsonPrimitive(type)
            base["pr"] = JsonArray(listOfNotNull(project).map(::JsonPrimitive))
            base["ar"] = JsonArray(listOfNotNull(area).map(::JsonPrimitive))
            base["tg"] = JsonArray(tags.map(::JsonPrimitive))
            deadline?.let { base["dd"] = JsonPrimitive(it.toEpochDay() * 86_400) }
            base.putAll(extra)
            if (completed) {
                base["ss"] = JsonPrimitive(3)
                base["sp"] = JsonPrimitive(nowSeconds - 1800)
            }
            items[id] = buildJsonObject {
                put("t", 0)
                put("e", "Task7")
                put("p", JsonObject(base))
            }
        }

        fun checklist(id: String, owner: String, title: String, index: Int, done: Boolean = false) =
            put(id, "ChecklistItem3", mapOf("tt" to title, "ts" to listOf(owner), "ix" to index, "ss" to if (done) 3 else 0))

        val day = { days: Long -> today.plusDays(days) }

        area("area-work", "Work", 0)
        area("area-home", "Personal", 1)
        tag("tag-deep", "Deep work")
        tag("tag-errand", "Errand")
        tag("tag-call", "Call")
        tag("tag-home", "Home")

        todo("p-site", "Launch the new website", Schedule.Anytime, 0, area = "area-work", type = 1)
        todo("p-kitchen", "Kitchen renovation", Schedule.Anytime, 1, area = "area-home", type = 1)
        todo("p-lisbon", "Weekend in Lisbon", Schedule.Anytime, 2, area = "area-home", type = 1)
        todo("p-novel", "Write a short story", Schedule.Someday, 3, area = "area-home", type = 1)

        // Today
        todo(
            "t-1", "Review the homepage copy", Schedule.Today(today), 0, project = "p-site", tags = listOf("tag-deep"),
            notes = "Tighten the hero headline.\nCheck the pricing table on mobile.",
        )
        todo("t-2", "Call the plumber about the sink", Schedule.Today(today), 1, project = "p-kitchen", tags = listOf("tag-call"), reminder = LocalTime.of(10, 30))
        todo("t-3", "Book the flight to Lisbon", Schedule.Today(today), 2, project = "p-lisbon", deadline = day(3))
        todo("t-4", "Prepare the sprint demo", Schedule.Today(today), 3, area = "area-work", tags = listOf("tag-deep"))
        checklist("c-1", "t-4", "Record the onboarding flow", 0, done = true)
        checklist("c-2", "t-4", "Update the release notes", 1)
        checklist("c-3", "t-4", "Send the invite", 2)
        todo("t-5", "Pick up the dry cleaning", Schedule.Today(today), 4, area = "area-home", tags = listOf("tag-errand"), completed = true)
        todo("t-6", "Read two chapters", Schedule.Evening(today), 5, area = "area-home", reminder = LocalTime.of(21, 0))
        todo("t-7", "Stretch for 15 minutes", Schedule.Evening(today), 6, tags = listOf("tag-home"))

        // A to-do repeating every Monday and Thursday: its template, and today's instance.
        val weekly = kotlinx.serialization.json.Json.parseToJsonElement(
            """{"tp": 0, "fu": 256, "fa": 1, "of": [{"wd": 1}, {"wd": 4}], "ed": 64092211200, "rc": 0}""",
        )
        todo("r-tmpl", "Water the plants", Schedule.Anytime, 30, area = "area-home", extra = mapOf("rr" to weekly))
        todo("t-8", "Water the plants", Schedule.Today(today), 7, area = "area-home", extra = mapOf("rt" to JsonArray(listOf(JsonPrimitive("r-tmpl")))))

        // Inbox
        todo("i-1", "Ideas for the team offsite", Schedule.Inbox, -3, notes = "Cooking class? Boat trip?")
        todo("i-2", "Renew the passport", Schedule.Inbox, -2)
        todo("i-3", "Look into a standing desk", Schedule.Inbox, -1)

        // Upcoming
        todo("u-1", "Dentist appointment", Schedule.OnDate(day(1)), 0, area = "area-home", reminder = LocalTime.of(8, 45))
        todo("u-2", "Pay the tiles deposit", Schedule.OnDate(day(2)), 1, project = "p-kitchen", deadline = day(5))
        todo("u-3", "Ship the website", Schedule.OnDate(day(4)), 2, project = "p-site", tags = listOf("tag-deep"))
        todo("u-4", "Check in online", Schedule.OnDate(day(6)), 3, project = "p-lisbon")

        // Anytime
        todo("a-1", "Choose the cabinet handles", Schedule.Anytime, 10, project = "p-kitchen")
        todo("a-2", "Write the launch announcement", Schedule.Anytime, 11, project = "p-site")
        todo("a-3", "Find a pastel de nata bakery", Schedule.Anytime, 12, project = "p-lisbon")
        todo("a-4", "Order new running shoes", Schedule.Anytime, 13, area = "area-home", tags = listOf("tag-errand"))

        // Someday
        todo("s-1", "Learn to make sourdough", Schedule.Someday, 20)
        todo("s-2", "Plan a cycling trip", Schedule.Someday, 21)
        todo("s-3", "Outline the first chapter", Schedule.Someday, 22, project = "p-novel")

        val state = HistoryState()
        state.apply(WireParser.parseItem(JsonObject(items)))
        return state
    }

    private fun json(value: Any?): JsonElement = when (value) {
        null -> kotlinx.serialization.json.JsonNull
        is JsonElement -> value
        is String -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is List<*> -> JsonArray(value.map(::json))
        else -> error("Unsupported demo value: $value")
    }
}
