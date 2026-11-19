package dev.thingscompanion.cloud

/** The list tabs as the user arranged them: their order, and which ones are hidden. Keys are list names. */
object ListLayout {
    /** [saved] first (known keys, once each), then the lists it does not name yet, in their default order. */
    fun order(all: List<String>, saved: List<String>): List<String> {
        val known = saved.filter { it in all }.distinct()
        return known + all.filter { it !in known }
    }

    /** The tabs to show: [order] without the [hidden] ones, but never none (the first list stays). */
    fun shown(all: List<String>, saved: List<String>, hidden: Set<String>): List<String> {
        val ordered = order(all, saved)
        return ordered.filter { it !in hidden }.ifEmpty { ordered.take(1) }
    }

    /** [key] moved [by] places (-1 up, 1 down), swapping with its neighbour; unchanged at the ends. */
    fun move(order: List<String>, key: String, by: Int): List<String> {
        val from = order.indexOf(key)
        val to = from + by
        if (from < 0 || to !in order.indices) return order
        return order.toMutableList().apply { add(to, removeAt(from)) }
    }

    /** What a bottom bar shows: [bar] as buttons, [more] behind a More button (empty when all fit). */
    data class Bar(val bar: List<String>, val more: List<String>)

    /**
     * Up to [BAR_SLOTS] lists fit; beyond that, the first four and a More button for the rest. A [current]
     * list that would sit behind More takes the fourth place, so the open list always shows in the bar.
     */
    fun bottomBar(shown: List<String>, current: String? = null): Bar {
        if (shown.size <= BAR_SLOTS) return Bar(shown, emptyList())
        val first = shown.take(BAR_SLOTS - 1)
        if (current == null || current in first || current !in shown) return Bar(first, shown.drop(BAR_SLOTS - 1))
        val bar = first.dropLast(1) + current
        return Bar(bar, shown.filter { it !in bar })
    }

    /** Buttons a bottom bar holds, as Material's navigation bar suggests. */
    const val BAR_SLOTS = 5
}
