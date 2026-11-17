# Changelog

All notable changes to Tandem Task are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- **Search.** Pull a list down past its top, or tap the magnifier, to find to-dos, projects and areas by title or notes, ignoring case and accents. Open items come first.
- **Project progress.** A project's circle fills with the share of its to-dos that are done, as in Things.
- **Pick the project or area when adding a to-do.** The new to-do form has a "Project or area" field.
- **The + button**: a tap adds a to-do (in Projects, a project; in Areas, an area), a long press offers every choice, such as a heading in a project.
- **Emoji in titles.** The title field has an emoji button that puts an emoji in front of the title, or takes it off.
- **Delete a heading from its dialog.** Tapping a heading offers Delete next to Rename (a swipe to the right still works).
- **Widget**: a checked to-do stays shown, ticked, for 3 seconds, so a second tap takes a mistake back. A ⋯ button on the widget opens its settings, which now include a background color.

### Fixed

- **Things for Mac crashing on sync.** A to-do moved into an area from an older version of Things (which Things no longer has) made Things for Mac quit on every sync. Such areas, and old tags, are now hidden, and a to-do can no longer be filed in an area that does not exist.

### Changed

- Tags are listed in Things' own tag order, in the filters and on to-dos.

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
