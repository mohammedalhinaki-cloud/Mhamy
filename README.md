# مهامي (Mahami)

A personal Arabic (RTL) to-do app for Android. Single user, local-only —
no accounts, no sync, no statistics.

## Features

- **اليوم (Today)** — overdue section + today's tasks, priority-colored completion
  circles (red = important, gray = normal), one-tap completion with Snackbar undo.
- **القادمة (Upcoming)** — future tasks grouped by day + a "no date" group.
- **التقويم (Calendar)** — Gregorian monthly calendar; the Hijri (Umm al-Qura)
  day number under every day cell, dots on days with tasks, per-day task list.
- **القوائم (Lists)** — list CRUD with 8 colors, per-list tasks, deleting a list
  never deletes its tasks (they become list-less).
- **المنجزة (Completed)** — finished tasks with restore (undo deletes an
  auto-created repeat occurrence again).
- **Quick add** — (+) bottom sheet: keyboard ready, Enter saves and keeps the
  sheet open; defaults today / normal / no list / no reminder.
- **Task editing** — title, notes, priority, date/time, list, snooze shortcuts,
  subtasks (one level, progress indicator «2/5» on the parent), repeating tasks
  (daily / weekly / monthly — completing spawns the next occurrence), reminders
  (off, at time, 10 min, 1 h, 1 day before — exact alarm with inexact fallback,
  re-synced after every change, restored after reboot, notifications with an
  «إنجاز» action).
- **Home-screen widget** — today's tasks with tap-to-open (Glance).

## Tech stack

- Kotlin 2.0.21 + Jetpack Compose (BOM 2024.12.01) + Material 3 (fixed brand
  colors, follows device light/dark, no Dynamic Color)
- Room 2.6.1 (KSP) + Kotlin Flow + ViewModel + Hilt 2.52
- Jetpack Glance 1.1.0 (home-screen widget)
- AlarmManager + notifications (reminders; POST_NOTIFICATIONS requested lazily,
  exact alarms with inexact fallback)
- minSdk 26 / targetSdk 35 — `java.time` for dates; Hijri via the built-in
  `HijrahChronology` (Umm al-Qura)
- Architecture: MVVM + Repository, packages `data` / `domain` / `ui` / `widget` / `reminder`

## Project structure

```
app/src/main/java/com/mahami/app/
├── MahamiApplication.kt        # @HiltAndroidApp + notification channel
├── MainActivity.kt             # sets content, refreshes the widget on open
├── data/
│   ├── local/                  # MahamiDatabase (v2 + MIGRATION_1_2), Converters
│   │   ├── dao/                # TaskDao, SubTaskDao, TaskListDao
│   │   └── entity/             # Task, SubTask, TaskList (Room schema)
│   └── repository/             # TaskRepository, TaskListRepository
├── domain/model/               # RepeatType
├── di/                         # AppModule (DB + DAOs)
├── ui/
│   ├── MahamiApp.kt            # 4-section bottom bar + Quick Add FAB
│   ├── theme/                  # Color / Type / Theme / ExtendedColors
│   ├── components/             # TaskItem, RtlIcons, pickers, undo helper…
│   ├── today/                  # TodayViewModel + TodayScreen
│   ├── upcoming/               # UpcomingViewModel + UpcomingScreen
│   ├── calendar/               # CalendarViewModel + CalendarScreen (Hijri)
│   ├── lists/                  # Lists, ListDetail, Completed screens/VMs
│   ├── quickadd/               # QuickAddSheet
│   └── taskedit/               # TaskEditSheet (edit, subtasks, repeat, reminder)
├── widget/                     # Glance widget + receiver (+ WidgetEntryPoint)
└── reminder/                   # ReminderScheduler, Reminder/Complete/Boot receivers
```

## UI conventions

- Fully Arabic; RTL is forced via `MahamiTheme` (`LocalLayoutDirection`).
- Directional icons (arrows) must come from `ui/components/RtlIcons`
  (backed by `Icons.AutoMirrored.*`), never from `Icons.Filled.Arrow*` directly.
- Priority circles / overdue accent: `MaterialTheme.extendedColors`
  (see `ui/theme/ExtendedColors.kt`).

## تنزيل APK

يمكن تنزيل ملف APK الجاهز للتثبيت من صفحة [الإصدارات](https://github.com/mohammedalhinaki-cloud/Mhamy/releases/tag/v1.0.0):
[`Mahami-v1.0.apk`](https://github.com/mohammedalhinaki-cloud/Mhamy/releases/download/v1.0.0/Mahami-v1.0.apk).

## Build

Open in **Android Studio Ladybug (2024.2.1) or newer** (AGP 8.6.1, JDK 17),
let Gradle sync (Gradle 8.9 via the committed wrapper), then run `app` on a
device or emulator with API 26+. The Room schema is at version 2 with the
manual migration `MIGRATION_1_2` (`data/local/Migrations.kt`).

## Font

[IBM Plex Sans Arabic](https://github.com/IBM/plex) (Regular / Medium / SemiBold / Bold),
bundled in `app/src/main/res/font/`. Licensed under the SIL Open Font License 1.1 —
see `app/src/main/assets/IBM_PLEX_SANS_ARABIC_OFL.txt`.
