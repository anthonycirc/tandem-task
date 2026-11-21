# Changelog

All notable changes to Tandem Task are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow [Semantic Versioning](https://semver.org/).

## [0.5.4] - 2026-11-21

### Added

- **Search the tags.** The tag picker has a search field: type part of a name (accents and case aside) to narrow the list. Tags already picked stay picked while hidden.

## [0.5.3] - 2026-11-21

### Added

- **Logbook.** A new list shows what was completed or cancelled, latest first and grouped by the day it was closed, as Things' Logbook does for the weekly review. It is added after Projects; Settings can move or hide it.
- **Change a deadline in place.** Tapping the deadline in a to-do's details, or in a project's header, opens the calendar to change it, or remove it with No deadline, without opening the form. Undo puts the old one back.
- **Obsidian links in notes.** An `obsidian://` link in a note, bare or as `[label](obsidian://…)`, is now a link: a tap opens the note in Obsidian. Without Obsidian on the phone, a short message says no app opens it.

### Fixed

- **Notes edited in Things showed only their last change.** Things sends an edited note as the changes made to it, and the app took the last change for the whole note: the rest of the text, links included, went missing. Notes are now rebuilt from those changes. After the update, the app downloads your Things history again once, to repair the notes already on the phone.

## [0.5.2] - 2026-11-20

### Added

- **Share to Tandem Task.** From any app, Share → Tandem Task opens a new to-do filled in with what was shared: a page's title with its link in the notes, or the text's first line with the rest in the notes.
- **Share a to-do.** The share button in a to-do's sheet sends its title, notes and checklist as text to another app.

### Changed

- The checklist's fields no longer draw a line under them: in a to-do's sheet, it ran right above the line that sets off the details.

## [0.5.1] - 2026-11-19

### Added

- **Any colour for the widget.** Next to the preset colours, Custom picks the widget's background by its red, green and blue, or typed as hex (`#2F5BB7`), with a live preview. Over a light colour the text turns dark so it stays readable.

### Security

- **Releases are no longer debuggable.** Every APK published so far was a debug build: with the phone unlocked and USB debugging on, a computer could read the app's data (the Things cache, and possibly the history key that opens the account). From 0.5.1 the APK is a release build, signed with the same key, so it installs over the previous version as an update.
- The app's data is left out of transfers to a new phone (Android 12+), as it already was of cloud backups.
- The screenshot demo mode only starts on development builds, so another app can't switch Tandem Task into it.

## [0.5.0] - 2026-11-19

Navigation, your way: tabs at the top, a side menu or a bar at the bottom (0.4.6 to 0.4.9), now shown as a choice of three sketches in Settings.

### Changed

- **Navigation choices with a preview.** In Settings, each Navigation choice shows a small sketch of the screen it gives (tabs, side menu, bottom bar).

## [0.4.9] - 2026-11-19

### Added

- **Counts next to the lists, as in Things**: how many to-dos are in the Inbox and in Today, and how many items are past their deadline (in red, on Deadlines). They show on the tabs, in the side menu and in the bottom bar.

## [0.4.8] - 2026-11-19

### Added

- **A bar at the bottom.** A third Navigation choice puts your lists in a bar at the bottom of the screen, in the order set under Lists. Up to five fit; beyond that, the first four and a More button that opens the others. A list opened from More or the search takes the fourth place while it is open.

## [0.4.7] - 2026-11-19

### Added

- **Areas and projects in the side menu**, as in Things' sidebar: under the lists, the projects with no area, then each area with its projects and their progress circles. An area's arrow folds its projects away, in the menu and in Projects alike; tapping an area or a project opens it.

## [0.4.6] - 2026-11-19

### Added

- **Choose the navigation.** Settings has a Navigation choice: tabs at the top (as before) or a side menu. The side menu opens from the menu button at the top left, as in Things and Todoist, and lists your lists in the order set under Lists. A swipe to the right on a row still means the Trash, so the menu only opens from its button.

## [0.4.5] - 2026-11-19

### Added

- **A Deadlines list, as in Things 3.24.** A new tab lists every open to-do and project with a deadline, overdue ones first under "Overdue", then by deadline day. The widget can show it too.
- **Arrange the tabs.** In Settings, under Lists, move each list up or down and switch off the ones you don't use. A hidden list is still found by its name in the search, and opens as a tab while you use it.

### Changed

- **A to-do opens in a sheet.** Tapping a to-do slides its details up from the bottom over the list, as in Todoist, instead of opening a new page; swipe it down or press Back to close it. Edit, repeat, move and Trash are at the top of the sheet.

## [0.4.4] - 2026-11-19

### Changed

- **Projects and areas in one list.** The Areas tab is gone: in Projects, each area has an arrow that folds its projects away or shows them again, and a count of what it holds. Tapping an area still opens it (rename, delete, its own to-dos); swiping it right still asks before deleting it. A project dropped on a folded area goes first in it.

## [0.4.3] - 2026-11-19

### Added

- **Tags when creating.** The new to-do and new project forms have a Tags field; the tags are written with the creation, as Things writes them.
- **Cancel from the checkbox.** A long press on a to-do's checkbox offers Complete, Cancel or Reopen, as in Things; a cancelled to-do shows a crossed grey box, and a tap reopens it. Undo is offered. On a project's page, a long press on its circle cancels it (or reopens it). Repeats that move on after completion are left to Things.

### Fixed

- Unchecking a cancelled to-do now reopens it (it used to do nothing).

## [0.4.2] - 2026-11-19

### Added

- **Deadlines in Today and Upcoming, as in Things.** A to-do with no date whose deadline is today or past now shows in Today; an undated to-do or project due later shows in Upcoming, under its deadline's day. Rows say how many days are left (or how late it is) instead of the date.
- **The widget highlights deadlines and reminders**: under a to-do's title, its reminder time and its deadline ("2 days left", in red).

## [0.4.1] - 2026-11-19

### Added

- **Change When and place from the details.** In a to-do's details, tap When to reschedule it and its project or area to move it; on a project's page, tap its When or its area. Tags have their own icon, like the other facts.

### Changed

- The selection button now shows checked boxes, so it no longer reads as "mark done".
- **Swiping to the Trash needs half the width.** A row goes to the Trash only when dragged at least halfway across; a short or quick swipe slides back. Rescheduling (swipe left) is unchanged.

## [0.4.0] - 2026-11-17

### Added

- **Select several to-dos or projects.** The check button in the top bar starts a selection: tap rows to pick them, then reschedule, move, check off or trash them all at once (trashing and checking off have one Undo). Items that can't take the action are skipped and counted; in a selection of to-dos and projects, a move applies to the to-dos.
- **Empty projects and headings from the Trash.** A trashed project now goes for good with its headings, to-dos and checklists, and a trashed heading with its to-dos, as Things empties its Trash.

### Fixed

- Deleting an area also takes its projects and to-dos out of the area, as Things does, so nothing keeps pointing at the deleted area.
- **Undo was never offered**: snackbars vanished as soon as they appeared. They now show, with Undo, and a newer one replaces the one showing.
- Tags the app doesn't know (not synced yet) stay on an item when its tags change.
- A widget tap on a check it no longer shows checks again instead of cancelling the late send.

## [0.3.10] - 2026-11-17

### Added

- **Swipe repeating to-dos.** An "after completion" occurrence can now be swiped to the Trash: like a check, its next date moves on, and Undo or Restore puts it back, as in Things. Any occurrence can be swiped to another day on its own; the repeat rule is untouched.

## [0.3.9] - 2026-11-17

### Added

- **Tags.** Tap a to-do's tags (or "Tags" when it has none), or use a project's menu, to pick its tags, written as Things 3 writes a tag change.

## [0.3.8] - 2026-11-17

### Added

- **Swipes in search results**: left reschedules, right moves to the Trash.

### Fixed

- A widget check is no longer lost when Android runs its send late, and two checks sent together no longer undo each other.
- A project's progress no longer counts to-dos under a heading in the Trash.

## [0.3.7] - 2026-11-17

### Added

- **Search.** Pull a list down past its top, or tap the magnifier, to find to-dos, projects and areas by title or notes, ignoring case and accents. Open items come first.
- **Project progress.** A project's circle fills with the share of its to-dos that are done, as in Things.
- **Pick the project or area when adding a to-do.** The new to-do form has a "Project or area" field.
- **The + button**: a tap adds a to-do (in Projects, a project; in Areas, an area), a long press offers every choice, such as a heading in a project.
- **Emoji in titles.** The title field has an emoji button that puts an emoji in front of the title, or takes it off.
- **Delete a heading from its dialog.** Tapping a heading offers Delete next to Rename (a swipe to the right still works).
- **Widget**: a checked to-do stays shown, ticked, for 3 seconds, so a second tap takes a mistake back; the check is then sent in the background, even if Android stopped the app meanwhile. A ⋯ button on the widget opens its settings, which now include a background color.

### Fixed

- **Things for Mac crashing on sync.** A to-do moved into an area from an older version of Things (which Things no longer has) made Things for Mac quit on every sync. Such areas, and old tags, are now hidden, and a to-do can no longer be filed in an area that does not exist.

### Changed

- Tags are listed in Things' own tag order, in the filters and on to-dos.
- In the new to-do form, When follows the project or area: Anytime once filed, Inbox when taken out.

## [0.3.6] - 2026-11-16

### Added

- **Delete for good from the Trash.** A trashed to-do has a Delete button, and the Trash an "Empty Trash" button, both behind a confirmation, written as Things 3 writes it: the to-do and its checklist are deleted, and a deleted repeating to-do's remaining occurrences become ordinary to-dos. Trashed projects and headings are deleted in Things.

## [0.3.5] - 2026-11-16

### Added

- **Yearly repeats.** The repeat button now also sets "every N years": on the to-do's date, on its weekday's rank in its month ("the third Monday of November"), on the last day of its month, or after completion, written as Things 3 writes it.
- **Check off yearly "repeat after completion" to-dos**: every repeating to-do can now be checked from the app.

### Changed

- **Filters**: tags and the project or area are picked from drop-down menus (projects under their area) instead of long rows of chips.
- Yearly rules on a weekday's rank or the last day now name their month.

## [0.3.4] - 2026-11-16

### Added

- **Monthly repeats.** The repeat button now also sets "every N months": on the to-do's day of the month, on its weekday's rank ("the third Monday"), on the last day when the to-do is on it, or after completion, written as Things 3 writes it.
- **Check off monthly "repeat after completion" to-dos**: the next one comes N months after the day it was checked, as in Things.
- Rules on a weekday's rank ("every month on the third Monday") are now shown in full.

## [0.3.3] - 2026-11-16

### Added

- **Fixed-schedule repeats.** The repeat button now also sets "every N days" or "every week on Monday and Thursday" on a fixed schedule, not only after completion, written as Things 3 writes it. A weekly schedule always includes the to-do's day; several days are offered for every week only.

## [0.3.2] - 2026-11-16

### Added

- **Make a to-do repeat.** In a dated to-do, the repeat button sets "every N days" or "every N weeks", after completion, written as Things 3 writes it: the to-do becomes the first occurrence of a new repeating to-do (with its notes, tags, reminder and checklist), and Things creates the next ones. Fixed-day, monthly and yearly repeats, to-dos with a deadline, and changing or stopping a repeat are still done in Things.

## [0.3.1] - 2026-11-16

### Added

- **Check off "repeat after completion" to-dos** that repeat by days or weeks, written as the Things apps write it: the to-do is completed and its repeat moves on, so Things creates the next one on the right day. Monthly and yearly ones stay with Things, and a checked one is unchecked in Things.

## [0.3.0] - 2026-11-16

### Added

- **Check off from the widget.** Tap a to-do's circle in the widget: it leaves the widget at once and the change is sent in the background.
- **Headings.** A project shows its to-dos under their headings, as in Things.
- **Areas.** The Projects list shows every area; an area opens on its projects and its own to-dos.
- **Delete checklist items** from the to-do detail.
- **Projects and areas.** Create projects (with When and deadline), headings and areas; rename areas and headings; complete, cancel or reopen a project (its open to-dos follow, as in Things); move a project to another area.
- **Delete projects and areas.** A project goes to the Trash (with Undo) and can be restored; its to-dos leave the lists with it. Deleting an area first moves its projects and to-dos to the Trash, after a confirmation.
- **Swipe to delete** projects and headings (to the Trash, with Undo; a heading takes its to-dos with it) and areas (after a confirmation).
- **Areas tab** listing every area.
- **Deadlines** on to-dos and projects, in the forms.
- **Move** a to-do to the Inbox, a project, a heading or an area.
- **Drag to reorder.** Long-press a to-do, a heading or a project and drag it: in Today (also into This Evening), the Inbox, Anytime, Someday, a project (also under another heading), an area, and Projects (also into another area). Off in Upcoming and while a filter is on.
- **+ in context.** In a project: new to-do or heading. In an area: new to-do or project. In Projects: new project or area.
- **Markdown in notes.** Bold, italic, strikethrough, code, headings, lists and links are styled in the to-do detail and while you type; links open in the browser.

### Changed

- **Taps never wait for Things Cloud.** A change is applied on the phone at once and sent in the background a few seconds after the last tap (or when you leave the app), together with the others. The history stays in memory, so a tap no longer re-reads it.

### Fixed

- The widget did not redraw after a change.

## [0.2.0] - 2026-11-16

### Added

- **Works offline, never waits.** Adding, editing, checking off or moving a to-do shows at once. Changes are kept on the phone and sent in the background, also later when the network comes back or the app is closed. A "to send" counter shows what is still waiting; Settings can discard changes Things keeps refusing.
- **Swipe actions.** Swipe a to-do to the left to reschedule it (Today, This Evening, Tomorrow, a date, Anytime, Someday, Inbox). Swipe it to the right to move it to the Trash, with Undo.
- **Trash.** A Trash button in the to-do detail and a Trash list to restore to-dos.
- **Checklists.** Check items from the to-do detail, add an item at the bottom, and add, rename or delete items when adding or editing a to-do.
- **Repeat rules shown.** The to-do detail says how a repeating to-do repeats ("Every week on Monday, Thursday", "Every month on the last day, after completion"), and rows mark repeating to-dos with a repeat icon.
- **Version number** in Settings.

### Changed

- **A design closer to Things:** coloured list icons, large list titles, Things-style checkboxes, a This Evening section, Upcoming grouped by day, projects grouped by area, and lighter rows with deadline flags, notes and checklist marks, and tags.
- **The to-do form is full screen**, with Save in the top bar and the keyboard on the title.
- **Background refresh** when you come back to the app.

### Fixed

- The keyboard could hide the Save button of the to-do form.
- Tapping outside the form discarded what you had typed.

## [0.1.0] - 2026-11-15

First public release.

- Today (with This Evening), Inbox, Upcoming, Anytime, Someday and Projects, with notes, checklists, tags and deadlines.
- Add to-dos to the Inbox, Today, This Evening, Anytime, Someday or a date, with an optional reminder.
- Edit and check off to-dos.
- Reminders as Android notifications.
- Home-screen widget with a list of your choice, theme and opacity.
- Filters by tag, project or area, and date.
- Background sync every 30 minutes.
- Light, dark or system theme. English and French.

[0.3.2]: https://github.com/anthonycirc/tandem-task/releases/tag/v0.3.2
[0.3.1]: https://github.com/anthonycirc/tandem-task/releases/tag/v0.3.1
[0.3.0]: https://github.com/anthonycirc/tandem-task/releases/tag/v0.3.0
[0.2.0]: https://github.com/anthonycirc/tandem-task/releases/tag/v0.2.0
[0.1.0]: https://github.com/anthonycirc/tandem-task/releases/tag/v0.1.0
