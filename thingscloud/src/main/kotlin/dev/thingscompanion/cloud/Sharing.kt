package dev.thingscompanion.cloud

/** Sharing with other apps: text shared to the app becomes a new to-do, a to-do is shared as text. */
object Sharing {

    /** The title and notes a new to-do starts with. */
    data class Draft(val title: String, val notes: String)

    /**
     * A to-do from what another app shares: its [subject] (a page's title) and its [text] (often a
     * link). Without a subject, the text's first line is the title and the rest the notes. Null when
     * both are blank.
     */
    fun draft(subject: String?, text: String?): Draft? {
        val subjectLine = subject?.trim().orEmpty()
        val body = text?.trim().orEmpty()
        if (subjectLine.isNotEmpty()) return Draft(subjectLine, body.takeIf { it != subjectLine }.orEmpty())
        if (body.isEmpty()) return null
        val firstLine = body.substringBefore('\n').trim()
        return Draft(firstLine, body.substringAfter('\n', "").trim())
    }

    /** A to-do as plain text: its title, then its notes and its checklist, each after a blank line. */
    fun text(title: String, notes: String?, checklist: List<ChecklistItem>): String = listOfNotNull(
        title,
        notes?.trim()?.takeIf { it.isNotEmpty() },
        checklist.takeIf { it.isNotEmpty() }?.joinToString("\n") { (if (it.done) "☑ " else "☐ ") + it.title },
    ).joinToString("\n\n")
}
