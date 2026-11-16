# Changelog

All notable changes to Tandem Task are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow [Semantic Versioning](https://semver.org/).

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

[0.2.0]: https://github.com/anthonycirc/tandem-task/releases/tag/v0.2.0
[0.1.0]: https://github.com/anthonycirc/tandem-task/releases/tag/v0.1.0
