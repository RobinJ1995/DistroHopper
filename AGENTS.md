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
  - `App`, `Application`, `AppComparatorAlphabetical` — app model classes. `App` usually wraps a PackageManager `ResolveInfo`, but can also represent DistroHopper-owned internal shortcuts that live only in the dash (currently the settings shortcut) and launch by explicit in-app intent rather than a public launcher component. Internal shortcut intents must not set `FLAG_ACTIVITY_NEW_TASK` (the target shares the home task's affinity, so it would only bring the home task to the front); the settings shortcut launches via `startActivityForResult` so `HomeActivity.onActivityResult` can handle the Customise UI result.
  - `IconPackHelper`, `Image`, `Utils`, `ViewFinder`, `InsetsHelper`,
    `Permission`, `RequestCode`, `ExceptionHandler`, `HomeRole` —
    support/utilities. `HomeRole` wraps the HOME-role (default launcher)
    checks/request intent used by the wizard and the preferences screen.
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
    it with only a Context. The ViewModel also exposes preference flows
    that apply live without recreating the activity (panel opacity,
    launcher/dash icon widths, show-running-apps) — the customise-mode
    seekbars only write the preference and the binder applies it. Theme and
    launcher/panel edge changes still recreate the
    activity (wholesale view-tree surgery). Widgets are always enabled (the
    old opt-out preference is gone).
  - `ThemeApplier` — applies the active theme's resources to the views.
  - `LauncherEdgeController` — repositions/reorients the launcher bar per
    edge, panel edge handling, and widget-area insets; owns the current
    `launcherEdge` and navigation insets.
  - `DashController` — opens/closes the dash (visibility, wallpaper and
    widget blur, panel state); owns `isOpen` and the chameleonic background
    colour. Besides the instant `open()`/`close()` it exposes the
    finger-tracked `swipe*()` variants used by the home-screen gestures.
    The customise-mode "close = relaunch" branch stays in
    HomeActivity because it manipulates the activity's intent.
  - `DashAnimator` — the visual side of dash open/close on DashController's
    behalf: the blur and panel opacity normally ramp gradually, and the dash
    itself animates per the theme's `dash_animation` preset. All transitions
    settle immediately while the device is in battery-saver mode. The
    `DashAnimation` enum: genie-from-BFB for gnome, slide-from-launcher for
    cinnamon, zoom-from-label for elementary, fade for unity/default.
    Swipe gestures bypass the theme preset: the dash slides vertically
    tracking an openness fraction (blur/panel/overlay follow the same
    fraction) and settles open or closed when the finger lifts.
  - `HomeGestureController` — the home screen's swipe gestures on empty
    desktop space: swiping down pulls down the system notification shade
    (`NotificationShade`, a reflection call — there is no public API),
    swiping up pulls in the dash tracking the finger, and swiping sideways
    pans between the widget desktops (`widgets/WidgetsPager`). It is also
    `SwipeToCloseLayout`'s delegate for swiping the open dash closed.
    Touch routing gotcha: the widget pager is clickable (tap = exit widget
    edit mode), so empty-desktop touches are consumed by it and never reach
    `Activity#onTouchEvent` — HomeActivity therefore feeds the pager's
    touches in via an `OnTouchListener` (which returns false until a swipe
    is recognised, keeping taps/long-presses working), with
    `Activity#onTouchEvent` as the fallback for genuinely unclaimed touches.
    Hit-testing uses raw coordinates since the two streams' local spaces
    differ; the panel and launcher are excluded. In battery saver there is
    nothing to track, so the dash opens/closes instantly at the trigger
    distance; swipe-to-close is disabled in customise mode (closing there
    relaunches the activity).
  - `WallpaperColourApplier` — applies the wallpaper's primary colour to
    launcher/dash for chameleonic themes (via the permissionless
    `WallpaperManager.getWallpaperColors` API; the storage permission only
    matters for the local-files lens).
  - `CustomiseModeUi` — the customise-mode seekbars/spinners inside the dash.
  - `LauncherBarBinder` — keeps the launcher bar's pinned/running icons and
    the dash grid in sync with `AppRepository` (via the `AppManager` facade).
    Also owns the drag-to-reorder preview: while a pinned icon is dragged its
    view stays in the bar as an invisible placeholder, shifted under the drag
    so the other icons slide over (via the container's `LayoutTransition`)
    and the empty slot shows where the drop would land; the model is only
    touched on drop. Long-pressing an unpinned app in the dash grid or in
    the InstalledApps lens results starts the same kind of drag
    (pin-by-drop, via `dash.AppLauncherLongClickListener.startAppDrag`):
    the placeholder opens at the end of the bar and the app is pinned at
    whichever slot it is dropped on — long-pressing an already-pinned app
    moves its existing icon instead.
    Gotcha: views must not be mutated (not even visibility) while
    ACTION_DRAG_ENDED is being dispatched — post such work instead, or the
    framework throws a ConcurrentModificationException.
  - `LayoutTransitionConfigurer` — the appear/disappear animations.
- **`desktop/`** — the desktop surface itself (`Wallpaper`, `AppIcon`, drag
  listeners). The activity is transparent over the system wallpaper
  (`windowShowWallpaper`), which lives in a separate system-owned window:
  when the dash opens it is blurred via cross-window blur
  (`Window#setBackgroundBlurRadius`, with a colour-adaptive frosted tint and
  fine-grain fallback in `Wallpaper` when blur is disabled or unsupported,
  e.g. on some OEM builds or in battery saver), while widgets —
  which live in the activity's own window — get a `RenderEffect` blur.
  - **`desktop/launcher/`** — the Unity-style launcher bar (the dock of
    pinned/running app icons) and its click/drag listeners. Note there are
    several distinct `AppLauncher` classes in different packages — they are
    per-context icon views, not the same class.
  - **`desktop/launcher/service/`** — `LauncherService`: a foreground-service
    variant of the launcher bar that floats over other apps.
  - **`desktop/dash/`** — the full-screen dash (app grid + search), with
    `GridAdapter` and `SearchTextWatcher`. `SwipeToCloseLayout` is the dash's
    container view (`llDash`): it recognises a downward swipe — only once
    neither the app grid nor the lens results can scroll up any further —
    and hands it to its delegate (`home/HomeGestureController`) to track a
    swipe-to-close.
  - **`desktop/dash/lens/`** — search "lenses": pluggable search providers
    (`InstalledApps`, `LocalFiles`, `DuckDuckGo`, `GitHub`, …) coordinated
    by `LensManager` with `AsyncSearch` and
    result/collection adapters. A lens can declare `requiredPermissions()`;
    lenses missing any of them are left out of the default-enabled set, and
    enabling one in the preferences re-requests them.
- **`onboarding/`** — the first-run wizard. `OnboardingActivity` is a
  full-screen ViewPager2 pager (theme choice, runtime permission prompts,
  set-as-default-home via `RoleManager.ROLE_HOME`) shown over the wallpaper
  (cross-window-blurred, like the dash). `HomeActivity.onCreate` checks
  `OnboardingGate.shouldShow` before any initialisation and redirects
  (finishing itself) on first run; Finish sets the `SETUP_COMPLETED`
  preference and relaunches `HomeActivity` so the chosen theme applies via
  the usual recreate path. A genuine first wizard run also arms a one-time
  default-pin marker unless the manifest `broadcast/AppUpgradeReceiver`
  recorded that this APK arrived as an update (upgrades are never eligible);
  after installed apps load, `home/AppsLoader` consumes it by pinning the
  first known available browser, email app, and known camera app (missing
  categories are skipped). Developer wizard resets do not
  re-arm this marker. A developer preference can explicitly queue the same
  default-pin pass for the next start. Users with a `theme` preference from
  before the wizard existed are marked completed silently — the wizard writes
  `SETUP_STARTED` on launch so its own theme write (made as soon as a card
  is tapped) doesn't trip that grandfathering when a run is interrupted.
  The wizard's permission page is the app's only storage permission prompt
  (`Permission.storagePermissions()` — READ_EXTERNAL_STORAGE up to Android
  12, the READ_MEDIA_* set from 13); HomeActivity no longer requests
  permissions at startup.
  Tests that launch `HomeActivity` with fresh prefs must seed
  `SETUP_COMPLETED` (done by `ActivityTestSupport.launchHome()`) or they
  will be redirected to the wizard.
- **`preferences/`** — `PreferencesActivity` (settings UI built
  programmatically with `PreferenceScreen`/categories), plus dedicated
  activities for lens ordering (`LensPreferencesActivity`) and theme
  selection (`ThemePreferencesActivity`, card UI shared with the wizard via
  `theme/ThemeCards`). `PreferencesRepository` provides
  typed and observable (`valueFlow`, a Kotlin `Flow`) access to the main
  "prefs" file keyed by the `Preference` enum — prefer it over raw
  `SharedPreferences` in new code.
- **`theme/`** — one class per supported desktop look (`Default`, `Gnome`,
  `Elementary`, `Cinnamon`), implementing the `Theme` interface; `Location`
  describes where UI elements sit per theme and `DashAnimation` names the
  per-theme dash open/close animation preset. `ThemeRegistry` is the single
  list of available themes (also drives the theme picker's order);
  `ThemeManager` (on the `DependencyContainer`) resolves the active theme
  from preferences — use `DependencyContainer.of(context).themeManager.current`
  rather than holding `Theme` references in statics. Switching themes still
  recreates `HomeActivity`.
- **`widgets/`** — home-screen widget hosting (mostly Kotlin): `WidgetHost`
  (AppWidgetHost), `WidgetPersistence`, `WidgetPickerDialog`.
  `WidgetsPager` (`R.id.vgWidgets`) is a horizontal pager of widget
  desktops: each page is a full-size `WidgetsContainer`, there is always
  exactly one empty desktop after the last occupied one (capped at 16;
  swiping right past the end lands on the fresh one), `WidgetLayout.page`
  persists which desktop a widget lives on, and pressing home
  (`HomeActivity.onNewIntent` with the HOME intent) animates back to the
  first desktop. Sideways swipes over a widget are intercepted by the pager
  itself; swipes on empty space arrive via `home/HomeGestureController`.
  The per-page grid maths stay inside `WidgetsContainer` (page insets are
  applied as padding per page, not on the pager), which lays widgets out on
  an invisible 8×8 grid (`WidgetGrid` holds the pure grid maths — snapping,
  span clamping, overlap checks). New widgets land on the desktop currently
  shown; drops and moves stay within it. Long-pressing a widget puts its
  `WidgetContainer` into
  edit mode: edge handles resize by touch (clamped to the provider's
  `min`/`maxResize*` limits and `resizeMode`, with a snap-indicator line
  drawn by `WidgetsContainer`), while dragging the body uses the system
  drag-and-drop framework (`WidgetsContainer_DragListener`) and shares the
  launcher's drag-to-trash mechanism. The free-moving system drag shadow is
  accompanied by a snapped landing indicator drawn on `WidgetsContainer`;
  HomeActivity attaches the listener to the topmost launcher/dash container
  and the listener translates its drag coordinates into widget-grid space.
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
