<p align="center">
  <img src="assets/logo.svg" width="112" alt="Lor Focus logo">
</p>

<h1 align="center">Lor Focus</h1>

<p align="center">
  <b>Free, open-source app blocker for Android that helps you stop doom-scrolling.</b><br>
  Block YouTube Shorts, Reels, TikTok and any distracting app — on your terms, all on-device.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/platform-Android%206.0%2B-3DDC84?logo=android&logoColor=white" alt="Android 6.0+">
  <img src="https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/license-MIT-4A6B57" alt="MIT License">
  <img src="https://img.shields.io/badge/100%25%20free%20%26%20open%20source-4A6B57" alt="Free and open source">
  <img src="https://img.shields.io/badge/ads-none-B0765E" alt="No ads">
  <img src="https://img.shields.io/badge/tracking-none-B0765E" alt="No tracking">
</p>

---

**Lor Focus** is a calm, local-only digital-wellbeing app. It kills the bottomless feeds —
**YouTube Shorts**, Instagram **Reels**, **TikTok**, Facebook, X, Snapchat, Reddit — while
leaving the useful parts of your apps alone. No accounts. No cloud. No ads. No trackers.
Everything stays on your phone.

## ✨ Features

- 🎯 **Block YouTube Shorts** — a short calm pause, then straight back to full videos.
- 📵 **Block any installed app** — pick from your real app list, each with a mode:
  - **Always** — blocked all the time (strict).
  - **During Focus** — blocked only while a timed session runs.
  - **Daily limit** — use it a few minutes a day, then it locks for the rest of the day.
- ⏳ **Timed Focus sessions** — start a 15–90 min sprint; only what you allow gets through.
- 🌐 **Browser-proof** — the same feeds/sites are blocked in Chrome & other browsers, so you
  can't sneak them in through the web.
- 📊 **Real insights** — reclaimed time, scrolls stopped and streaks, counted on-device.
- 🔒 **Strict mode** — an optional cooldown so you can't disable it on impulse.
- 🦡 **Calm, editorial design** — six honey-badger emblems, light & dark themes.
- 🕊️ **100% private** — works fully offline; nothing ever leaves the device.

## 📦 Install

Grab the latest **[`Lor-Focus.apk`](Lor-Focus.apk)**, open it on your phone, allow install from
unknown sources, then grant **Accessibility** + **Display over other apps** (and **Usage access**
for daily limits). It's self-signed for sideloading — not a Play Store release.

> If Google Play Protect warns, tap **More details → Install anyway**.

## 🛠️ Build from source

Open the **`android/`** folder in Android Studio and run, or from a machine with the Android SDK:

```bash
cd android && ./gradlew :app:assembleRelease
```

See **[`android/README.md`](android/README.md)** for details. A full interactive **web prototype**
of the UI lives in **[`index.html`](index.html)** — just open it in any browser.

## 🧩 How it works

Lor Focus uses Android's `AccessibilityService` to notice when a short-form feed or a blocked app
comes on screen, then shows a calm overlay and steps you back out. State lives in a local **Room**
database + **DataStore**; a foreground service keeps detection reliable. Built with **Kotlin** and
**Jetpack Compose**.

## 🔐 Privacy

No accounts, no analytics, no network calls for core features. The app collects nothing and sends
nothing. Read the in-app privacy note, or the source — it's all here.

## 🤝 Contributing

Issues and pull requests are welcome. Detection signatures for specific apps/devices can drift;
the in-app **Settings → Diagnostics** screen surfaces the live view-ids to make tuning easy.

## 📄 License

[MIT](LICENSE) © mecrimino — free to use, modify and share.

---

<sub>Keywords: Android app blocker · block YouTube Shorts · block Reels · block TikTok · focus app ·
digital wellbeing · screen time · anti doomscrolling · productivity · self-control · open source ·
Kotlin · Jetpack Compose · offline · privacy-friendly · no ads.</sub>
