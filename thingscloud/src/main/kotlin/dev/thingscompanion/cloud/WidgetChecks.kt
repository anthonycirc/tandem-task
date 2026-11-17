package dev.thingscompanion.cloud

/**
 * To-dos ticked on the widget and still showing, each with the tap that ticked it. The check is
 * sent only when that tap's delay is over and the to-do is still ticked by it, so a quick second
 * tap takes a mistaken tick back.
 */
data class WidgetChecks(val ticks: Map<String, Long> = emptyMap()) {

    fun isTicked(id: String): Boolean = id in ticks

    fun toggle(id: String, token: Long): WidgetChecks =
        if (id in ticks) WidgetChecks(ticks - id) else WidgetChecks(ticks + (id to token))

    fun stillTicked(id: String, token: Long): Boolean = ticks[id] == token

    fun without(id: String): WidgetChecks = WidgetChecks(ticks - id)

    /** Drops ticks older than [maxAgeMillis] (tokens are tap times): their send was cut short. */
    fun freshAt(nowMillis: Long, maxAgeMillis: Long): WidgetChecks =
        WidgetChecks(ticks.filterValues { nowMillis - it <= maxAgeMillis })

    /**
     * Drops ticks a day old: their send never ran. Younger ones still hold for their send, however
     * late WorkManager runs it, even once the widget stopped showing them ([freshAt]).
     */
    fun prunedAt(nowMillis: Long): WidgetChecks = freshAt(nowMillis, PRUNE_AFTER_MILLIS)

    fun encode(): Set<String> = ticks.map { (id, token) -> "$token:$id" }.toSet()

    companion object {
        private const val PRUNE_AFTER_MILLIS = 24 * 60 * 60_000L

        fun decode(stored: Set<String>): WidgetChecks = WidgetChecks(
            stored.mapNotNull { entry ->
                val token = entry.substringBefore(':', "").toLongOrNull() ?: return@mapNotNull null
                entry.substringAfter(':') to token
            }.toMap(),
        )
    }
}
