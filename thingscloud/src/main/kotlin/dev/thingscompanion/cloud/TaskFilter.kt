package dev.thingscompanion.cloud

import java.time.LocalDate

/**
 * Display filter applied on top of a list. Closed to-dos (shown by the list for today) go to the
 * bottom, or are removed when [showClosed] is false. Criteria combine: tags match when the to-do
 * carries any of [tagIds]; [containerId] is a project or an area; [day] keeps to-dos scheduled
 * or due that day. [groupByProject], like [showClosed], is a preference: the lists class their to-dos by project.
 */
data class TaskFilter(
    val showClosed: Boolean = true,
    val tagIds: Set<String> = emptySet(),
    val containerId: String? = null,
    val day: LocalDate? = null,
    val groupByProject: Boolean = false,
) {
    /** No narrowing criterion is set (the closed-to-dos option is a preference, not a filter). */
    val isEmpty: Boolean get() = tagIds.isEmpty() && containerId == null && day == null

    fun apply(tasks: List<Task>): List<Task> {
        val kept = tasks.filter { task ->
            (showClosed || task.isOpen) &&
                (tagIds.isEmpty() || task.tagIds.any { it in tagIds }) &&
                (containerId == null || task.projectId == containerId || task.areaId == containerId) &&
                (day == null || task.startDate == day || task.deadline == day)
        }
        val (open, closed) = kept.partition { it.isOpen }
        return open + closed
    }
}
