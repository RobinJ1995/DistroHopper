# AGENTS.md

> **Maintenance rule:** Whenever you touch a piece of code, check whether this
> file adequately describes it — within the limits of what AGENTS.md is for
> (structure, conventions, and how things fit together; not exhaustive API
> docs or per-class commentary). If it doesn't, add or update the relevant
> section as part of your change. If something here turns out to be wrong or
> stale, fix it.

## What this project is

DistroHopper is an Android home screen (launcher) replacement that mimics
various Linux desktops: Ubuntu Unity, elementary OS Pantheon, GNOME, and
Cinnamon. It started as a high-school project around 2011; code quality and style
vary considerably across the codebase — older Java alongside newer Kotlin.

## Build & test

- Single-module Gradle project: the `:app` module is the entire application.
- `compileSdk`/`targetSdk` 36, `minSdk` 29. Mixed Java/Kotlin.
- Build: `./gradlew assembleDebug`
- Unit tests: `./gradlew testDebugUnitTest` — JVM tests under
  `app/src/test/`, written in Kotlin using Robolectric (they exercise real
  activities/views without a device).
- Instrumented tests live under `app/src/androidTest/` (require a
  device/emulator; rarely the right place for new tests — prefer Robolectric).

## Repository layout

```
app/src/main/java/be/robinj/distrohopper/   — all application code
app/src/main/res/                           — layouts, drawables, strings
                                              (many values-* translations via Transifex)
app/src/test/java/be/robinj/distrohopper/   — Robolectric unit tests (Kotlin)
app/src/androidTest/                        — instrumented tests
etc/                                        — design assets (SVG/XCF sources, screenshots)
```

## Package structure (`be.robinj.distrohopper`)

- **Root package** — core glue:
  - `HomeActivity` — the launcher's main activity; hosts the desktop UI and
    wires most components together.
  - `AppManager` — central registry of installed/pinned/running apps; the
    model behind both the launcher and the dash.
  - `App`, `Application`, `AppComparatorAlphabetical` — app model classes.
  - `IconPackHelper`, `Image`, `Utils`, `ViewFinder`, `InsetsHelper`,
    `Permission`, `RequestCode`, `ExceptionHandler` — support/utilities.
    `InsetsHelper` handles system bar / display cutout insets (e.g. keeping
    UI clear of the 3-button navigation bar).
  - `Observed`/`IObserver` — small homegrown observer pattern.
  - `AboutActivity`, `ContributeActivity` — informational screens.
- **`desktop/`** — the desktop surface itself (`Wallpaper`, `AppIcon`, drag
  listeners).
  - **`desktop/launcher/`** — the Unity-style launcher bar (the dock of
    pinned/running app icons) and its click/drag listeners. Note there are
    several distinct `AppLauncher` classes in different packages — they are
    per-context icon views, not the same class.
  - **`desktop/launcher/service/`** — `LauncherService`: a foreground-service
    variant of the launcher bar that floats over other apps.
  - **`desktop/dash/`** — the full-screen dash (app grid + search), with
    `GridAdapter` and `SearchTextWatcher`.
  - **`desktop/dash/lens/`** — search "lenses": pluggable search providers
    (`InstalledApps`, `LocalFiles`, `DuckDuckGo`, `GitHub`, Stack Exchange
    sites, `Reddit`, …) coordinated by `LensManager` with `AsyncSearch` and
    result/collection adapters.
- **`preferences/`** — `PreferencesActivity` (settings UI built
  programmatically with `PreferenceScreen`/categories), plus dedicated
  activities for lens ordering (`LensPreferencesActivity`) and theme
  selection (`ThemePreferencesActivity`).
- **`theme/`** — one class per supported desktop look (`Default`, `Gnome`,
  `Elementary`, `Cinnamon`), implementing the `Theme` interface; `Location`
  describes where UI elements sit per theme.
- **`widgets/`** — home-screen widget hosting: `WidgetHost`, `WidgetGrid`,
  `WidgetContainer`, `WidgetPersistence`, `WidgetPickerDialog` (mostly
  Kotlin).
- **`async/`** — `AsyncTask`-style background loaders for apps, icons,
  labels, and wallpaper.
- **`broadcast/`** — `PackageManagerBroadcastReceiver`: reacts to app
  install/uninstall to keep `AppManager` current.
- **`cache/`** — `AppIconCache`.
- **`dev/`** — in-app debug logging (`Log`, `LogToaster`,
  `DevLogsActivity`).
- **`thirdparty/`** — vendored third-party views/helpers; avoid editing
  unless necessary.

## Conventions & gotchas

- New code is generally written in Kotlin; much of the existing code is
  older Java. Match the style of the file you're editing rather than
  refactoring wholesale.
- Listener classes are typically separate top-level classes named
  `<View><Event>Listener` (e.g. `AppLauncherLongClickListener`) rather than
  anonymous/inner classes — follow that pattern where it's already in use.
- User-facing strings belong in `res/values/strings.xml`; translations are
  managed externally on Transifex, so never hand-edit the `values-*`
  locale files.
- Do not commit `local.properties` or `secret.properties`.
