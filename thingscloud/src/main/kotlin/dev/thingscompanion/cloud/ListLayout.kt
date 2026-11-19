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
}
