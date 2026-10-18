# JSON-Viewer

[![API](https://img.shields.io/badge/API-36-brightgreen.svg?style=flat-square)](https://source.android.com/docs/setup/about/build-numbers)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-blueviolet.svg?style=flat-square)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Compose%20%2B%20M3-blue.svg?style=flat-square)](https://developer.android.com/jetpack/compose)

A minimal, read-only JSON viewer for Android 16. Open a `.json` file and read it pretty-printed with syntax highlighting.

## What it is

JSON-Viewer is intentionally small. It does **a few things**:

1. Opens a file through the Storage Access Framework (no storage
   permissions), or via **Open with** from a file manager, downloads or a
   mail attachment — pick *Always* there to make it the default JSON app.
2. Pretty-prints the JSON (`org.json`, 4-space indent) and renders it line
   by line in a `LazyColumn`.
3. Highlights each line with a per-line tokenizer: keys, strings, numbers,
   booleans, null and punctuation each get their own colour.

The app bar shows the file name and size; loading and parse errors surface
as a snackbar.

No editing, no writing JSON back out, no network fetch, no collapsible tree
view, no search, no multi-file tabs, no persistence. If you need any of
those, this is the wrong tool — on purpose.

## Requirements

- **Android 16** (API 36). `minSdk = compileSdk = targetSdk = 36`.
- **JDK 21** to build — the Robolectric / SDK-36 test layer needs it.
- Files up to **10 MB**.

## Tech

- 100% Kotlin, Jetpack Compose, Material 3 (static teal scheme — no dynamic
  colour, so the syntax palette stays stable).
- MVVM with `StateFlow` + `viewModelScope`. **No Hilt** — manual DI via the
  `Application` class (`JsonViewerApplication` + `JsonViewModelFactory`).
- `org.json` for pretty-printing; a custom `JsonTokenizer` (per line) drives
  the highlighting, rendered into a Compose `AnnotatedString`.
- AGP 9 with built-in Kotlin, Gradle 9, version catalog. No persistence, no
  network, no analytics.
- Tests: JUnit 4 + MockK + Turbine (JVM) and Robolectric for the
  ContentResolver path.

Reference points for code style and Gradle setup: **Lobber** and
**jellychrome** (same author).

## Build

```bash
./gradlew assembleDebug         # debug APK
./gradlew testDebugUnitTest     # unit tests (JVM + Robolectric)
./gradlew bundleRelease         # release AAB (signed if keystore.properties is present)
```

Release signing reads `keystore.properties` at the project root; without it
the release bundle is built unsigned (e.g. on CI).

## Use

1. Install the APK and launch it.
2. Tap the **open-file** icon (top right) and pick a `.json`.
3. The file is pretty-printed and syntax-highlighted; the app bar shows its
   name and size. Tap the icon again to open another file.

Or tap a `.json` in any file manager and choose JSON-Viewer.

## Privacy

Everything stays on device. There is no network access, no telemetry, no
crash reporter, no analytics. The app only reads the file you explicitly
pick or open with it.

## License

GPL v3 — see [LICENSE](LICENSE).
