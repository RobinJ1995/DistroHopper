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
- `compileSdk`/`targetSdk` 36, `minSdk` 31. Mixed Java/Kotlin.
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
    wires the components together. The heavy view work lives in the `home/`
    controllers (below); keep HomeActivity to lifecycle handling and wiring.
  - `AppManager` — facade over the app model and its view binding, keeping
    the API its many callers (listeners, lenses, the broadcast receiver)
    already use. The actual model is `AppRepository` (installed/pinned
    lists with `StateFlow` snapshots plus live lists — the dash grid is
    backed by the live installed list); the launcher-bar/dash view sync is
    `home/LauncherBarBinder` (resolved lazily so AppManager can be
    constructed on a background thread). Prefer `AppRepository` directly
    in new model-level code.
  - `App`, `Application`, `AppComparatorAlphabetical` — app model classes.
  - `IconPackHelper`, `Image`, `Utils`, `ViewFinder`, `InsetsHelper`,
    `Permission`, `RequestCode`, `ExceptionHandler` — support/utilities.
    `InsetsHelper` handles system bar / display cutout insets (e.g. keeping
    UI clear of the 3-button navigation bar).
  - `Observed`/`IObserver` — small homegrown observer pattern.
  - `DependencyContainer` — hand-wired container for shared objects
    (`PreferencesRepository`, `DispatcherProvider`, …), owned by `Application`
    and looked up via `DependencyContainer.of(context)`. **Hard rule: no DI
    frameworks (Hilt/Dagger/Koin) in this project** — all wiring is explicit.
    Tests can substitute the whole container via
    `DependencyContainer.override`.
  - `DispatcherProvider` — indirection over coroutine dispatchers so tests
    can inject deterministic ones.
  - `AboutActivity`, `ContributeActivity` — informational screens.
- **`home/`** — Kotlin controller/applier classes extracted from
  HomeActivity, each owning one concern of the home screen and constructed
  in `onCreate` with the `ViewFinder` plus what they need from the
  `DependencyContainer`:
  - `HomeViewModel` — AndroidX ViewModel holding the screen's state as
    `StateFlow`s (e.g. `dashOpen`); survives `recreate()` (but not the
    finish-and-restart relaunches used for edge changes). It never touches
    views; `HomeStateBinder` collects its flows into the controllers.
    HomeActivity's event handlers also call the controllers directly (they
    are idempotent) so UI reactions stay synchronous; the flows are the
    state of record. Customise mode lives as a `MutableStateFlow` on the
    `DependencyContainer` (not the ViewModel) because `App.launch()` checks
    it with only a Context.
  - `ThemeApplier` — applies the active theme's resources to the views.
  - `LauncherEdgeController` — repositions/reorients the launcher bar per
    edge, panel edge handling, and widget-area insets; owns the current
    `launcherEdge` and navigation insets.
  - `DashController` — opens/closes the dash (visibility, wallpaper and
    widget blur, panel state); owns `isOpen` and the chameleonic background
    colour. The customise-mode "close = relaunch" branch stays in
    HomeActivity because it manipulates the activity's intent.
  - `WallpaperColourApplier` — applies the wallpaper's average colour to
    launcher/dash for chameleonic themes.
  - `CustomiseModeUi` — the customise-mode seekbars/spinners inside the dash.
  - `LauncherBarBinder` — keeps the launcher bar's pinned/running icons and
    the dash grid in sync with `AppRepository` (via the `AppManager` facade).
  - `LayoutTransitionConfigurer` — the appear/disappear animations.
- **`desktop/`** — the desktop surface itself (`Wallpaper`, `AppIcon`, drag
  listeners). The activity is transparent over the system wallpaper
  (`windowShowWallpaper`), which lives in a separate system-owned window:
  when the dash opens it is blurred via cross-window blur
  (`Window#setBackgroundBlurRadius`, with a darken fallback in `Wallpaper`
  when blur is disabled at runtime, e.g. battery saver), while widgets —
  which live in the activity's own window — get a `RenderEffect` blur.
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
  selection (`ThemePreferencesActivity`). `PreferencesRepository` provides
  typed and observable (`valueFlow`, a Kotlin `Flow`) access to the main
  "prefs" file keyed by the `Preference` enum — prefer it over raw
  `SharedPreferences` in new code.
- **`theme/`** — one class per supported desktop look (`Default`, `Gnome`,
  `Elementary`, `Cinnamon`), implementing the `Theme` interface; `Location`
  describes where UI elements sit per theme. `ThemeRegistry` is the single
  list of available themes (also drives the theme picker's order);
  `ThemeManager` (on the `DependencyContainer`) resolves the active theme
  from preferences — use `DependencyContainer.of(context).themeManager.current`
  rather than holding `Theme` references in statics. Switching themes still
  recreates `HomeActivity`.
- **`widgets/`** — home-screen widget hosting (mostly Kotlin): `WidgetHost`
  (AppWidgetHost), `WidgetPersistence`, `WidgetPickerDialog`.
  `WidgetsContainer` lays widgets out on an invisible 8×8 grid
  (`WidgetGrid` holds the pure grid maths — snapping, span clamping,
  overlap checks). Long-pressing a widget puts its `WidgetContainer` into
  edit mode: edge handles resize by touch (clamped to the provider's
  `min`/`maxResize*` limits and `resizeMode`, with a snap-indicator line
  drawn by `WidgetsContainer`), while dragging the body uses the system
  drag-and-drop framework (`WidgetsContainer_DragListener`) and shares the
  launcher's drag-to-trash mechanism — `TrashDragListener` in
  `desktop/launcher/` recognises widget drags via the drag's local state.
- Background loading uses Kotlin coroutines: `home/StartupLoader` runs the
  startup sequence (wallpaper init → app list → label/icon caches, strictly
  in that order — both the wallpaper and app paths touch the BFB) in the
  activity's `lifecycleScope` on the `DependencyContainer`'s
  `DispatcherProvider`; `home/AppsLoader` holds the blocking halves. Tests
  swap the IO dispatcher for `Dispatchers.Unconfined` via
  `ActivityTestSupport.installTestDispatchers()` so `drainTasks()` is
  deterministic. (`desktop/dash/lens/AsyncSearch` is the one remaining
  `AsyncTask`.)
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
