# مهامي (Mahami)

A personal Arabic (RTL) to-do app. Local-only, no accounts, no sync.

## Tech stack

- Kotlin 2.0.21 + Jetpack Compose (BOM 2024.12.01) + Material 3
- Room 2.6.1 (KSP) + Kotlin Flow + ViewModel + Hilt 2.52
- DataStore (settings) — added when the first setting appears
- Jetpack Glance (widget) — added in Stage 6
- minSdk 26 / targetSdk 35 — java.time for dates (Hijri via `HijrahChronology` in Stage 5)
- Architecture: MVVM + Repository (`data` / `domain` / `ui` / `widget` / `reminder`)

## Build

Open in **Android Studio Ladybug (2024.2.1) or newer** (AGP 8.6.1, requires JDK 17),
let Gradle sync (Gradle 8.9 via the committed wrapper), then run `app` on a device
or emulator with API 26+.

## Font

[IBM Plex Sans Arabic](https://github.com/IBM/plex) (Regular / Medium / SemiBold / Bold),
bundled in `app/src/main/res/font/`. Licensed under the SIL Open Font License 1.1 —
see `app/src/main/assets/IBM_PLEX_SANS_ARABIC_OFL.txt`.
