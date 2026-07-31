# Lor Focus — Android

A local-only Android app that intervenes on short-form feeds (YouTube Shorts, Instagram
Reels, Facebook Reels, TikTok, Snapchat Spotlight) with a **block**, a **mindful pause**, or a
**daily time limit**, plus a YouTube **Learning mode** allowlist. No accounts, no network, no
analytics — everything lives in a local Room database + DataStore.

This is the native implementation of the design in `../index.html` (the interactive web
prototype) and `Lor Focus premium.dc.html`.

## Stack

Kotlin · Jetpack Compose (Material 3) · Room · DataStore · AccessibilityService · foreground
service · Device Admin. Versions are a mutually-compatible set (see `build.gradle.kts`):
AGP 8.5.2 · Kotlin 1.9.24 · Compose compiler 1.5.14 · Compose BOM 2024.06 · minSdk 26 ·
compileSdk 34.

## Build & run

The Gradle **wrapper jar/scripts are not included** (binary can't be generated here). Easiest:

1. Open the `android/` folder in **Android Studio** (Koala or newer). It supplies Gradle and
   syncs automatically.
2. Or generate the wrapper once from a machine with Gradle installed:
   ```bash
   cd android && gradle wrapper --gradle-version 8.7 && ./gradlew :app:installDebug
   ```

Then, on the device, grant the permissions the onboarding screen deep-links to
(Accessibility is the one that actually drives detection; Display-over-other-apps is needed
for the block/pause screens; Usage access feeds the time-limit/insights numbers).

## What's implemented

- **Full Compose UI** — onboarding (welcome → how-it-works → permissions → pick feeds →
  approach), Today, Rules, Rule detail (mode + budget/pause steppers + native time pickers),
  Learning mode (add/remove channels), Add channel, Focus schedule, Insights, Settings
  (light/dark/system), Strict mode, Goals. Light + dark palette from the design.
- **Honey-badger emblem picker** (Settings → Emblem, or the Welcome screen) — six vector marks
  (`res/drawable/badger_*.xml`); the choice persists and shows on Welcome, Settings and the
  block screen. Emblems render on a fixed light disc so the two-tone mark reads in dark mode.
- **Detection → intervention → overlay pipeline** — `FeedDetectionService` filters by package,
  debounces, bounded-DFS matches bundled signatures, and hands off to a block/pause overlay
  (`OverlayService`, a foreground service). Degrades gracefully when a signature stops matching.
- **Room** (feeds + allowlist) with seeded defaults; **DataStore** for settings; reset-all wipes
  both and re-seeds.
- **Device Admin** optional uninstall protection (Strict mode toggle).

## Known ceilings / TODO (marked in code)

- **Detection signatures are PLACEHOLDERS** (`DetectionRules.kt`). Real view-id/text fragments
  must be captured from each target app's current build (dump the accessibility node tree while
  a feed is on screen) and the `VERSION` bumped. Everything else in the pipeline is real.
- **LIMIT mode** currently behaves like a pause — foreground-time accumulation via
  `PACKAGE_USAGE_STATS` (F3.3/F3.4) isn't wired yet (`FeedDetectionService.handle`).
- **Strict-mode cooldown** (F6.1) is surfaced in copy but the deferred-change queue isn't
  enforced yet.
- **Fonts** fall back to the system serif/sans — drop the Instrument Serif/Sans TTFs into
  `res/font` and point `Theme.kt` at them to match the design exactly.
- **Schedules & Insights** are presentational (static data); the schedule engine and real
  stat aggregation are the next data-layer work.
- Not compiled in this environment — expect the usual first-open-in-Android-Studio tidy-ups.
