# Lor Focus

A local-only Android app that helps you stop doom-scrolling short-form video. It blocks
**YouTube Shorts**, and lets you pick which of your installed apps to block — **Always**,
**During a timed Focus session**, or on a **daily time limit** — including their sites in a
browser. No accounts, no cloud, no analytics: everything stays on the device.

## Contents

- **`index.html`** — an interactive web prototype of the whole UI (open in any browser).
- **`android/`** — the native app (Kotlin + Jetpack Compose, AccessibilityService, Room,
  foreground service, Device Admin). See [`android/README.md`](android/README.md) to build.
- **`Lor-Focus.apk`** — a signed debug/sideload build you can install directly.

## Install the APK

Download `Lor-Focus.apk`, open it on your phone, allow install from unknown sources, then grant
**Accessibility** + **Display over other apps** (and **Usage access** for daily limits). It's
self-signed for sideloading — not a Play Store release.

## Build from source

Open the `android/` folder in Android Studio and run, or from a machine with the Android SDK:

```bash
cd android && ./gradlew :app:assembleRelease
```

## Status

The UI, data layer (Room + DataStore), stats, timed Focus sessions and app/website blocking are
implemented. On-device detection signatures (YouTube Shorts, browser URL bars) may need tuning per
device — the in-app **Settings → Diagnostics** screen surfaces the live view-ids to help.
