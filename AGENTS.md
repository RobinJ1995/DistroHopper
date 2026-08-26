# AGENTS.md

> **Maintenance rule:** Whenever you touch a piece of code, check whether this
> file adequately describes it, and update it as part of your change; fix
> anything here you find wrong or stale. This file is a map: what each part
> owns, how the parts connect, and the rules that cut across classes. The
> reasoning behind a particular line of code belongs in a comment at that line,
> and how the code got here belongs in the commit message — not here.

## What this project is

DistroHopper is an Android home screen (launcher) replacement that mimics
various Linux desktops: Ubuntu Unity, elementary OS Pantheon, GNOME, Cinnamon,
KDE Plasma, MATE, Pop!_OS COSMIC, and Solus Budgie. It started as a high-school
project around 2011; code quality and style vary considerably — older Java
alongside newer Kotlin.

## Build & test

- Single-module Gradle project: `:app` is the entire application.
  `compileSdk`/`targetSdk` 36, `minSdk` 31.
- Build: `./gradlew assembleDebug`.
- Unit tests: `./gradlew testDebugUnitTest` — Kotlin Robolectric tests under
  `app/src/test/` that exercise real activities and views. Prefer them over the
  instrumented tests in `app/src/androidTest/`, which need a device.
- Lint: `./gradlew lintDebug`. CI runs it next to the unit tests and releases
  are gated on both. `MissingTranslation`/`ExtraTranslation` are disabled, as
  translations lag `strings.xml` by design. `app/lint-baseline.xml` holds the
  issues that predate the CI job: fix issues out of it, never regenerate it to
  absorb a new failure.
- **Releases.** Pushing a `v*` tag runs the tests, builds signed artifacts and
  attaches them to a GitHub Release as `DistroHopper-<tag>.apk`, `.aab` and
  `-paranoia.apk`. A version ending in a letter (`v3.0.0d`) is a pre-release.
  A release can also be cut by running the **CI** workflow manually with a
  version: the `release-context` job validates it, bumps `baseVersionName` and
  `appVersionCode` in `app/build.gradle`, commits and tags that on `master` (as
  `Robin Jacobs <RobinJ1995@users.noreply.github.com>`, pushed by
  `github-actions[bot]`), and builds the tag in the same run, skipping tests. A
  run that pushed its tag but failed later resumes from that tag; a leftover
  GitHub Release for the tag must be deleted before retrying. The guard
  conditions are commented in the workflow. If `master` ever gets branch
  protection, `github-actions[bot]` needs a bypass or the bump cannot be pushed.
- The `-paranoia` APK (`-PparanoiaBuild=true`) forces crash reporting off and
  omits dependency metadata. It is a courtesy sideload build, not a separate
  product line, and deliberately removes no code.
- `.github/workflows/update-translations.yml` pulls Transifex translations
  every Wednesday and opens or updates a PR.

## Repository layout

```
app/src/main/java/be/robinj/distrohopper/   — all application code
app/src/main/res/                           — layouts, drawables, strings
                                              (values-* translations via Transifex)
app/src/test/java/be/robinj/distrohopper/   — Robolectric unit tests (Kotlin)
app/src/androidTest/                        — instrumented tests
etc/                                        — design assets, asset generators, screenshots
licenses/                                   — licence texts of everything bundled,
                                              staged into the APK by copyLicenseAssets
```

## Architecture in brief

`HomeActivity` is the launcher. It does lifecycle handling and wiring only; each
concern of the home screen lives in a controller in `home/`, built in
`onCreate`. Shared objects live in `DependencyContainer`. The app model is
`AppRepository`, behind the older `AppManager` facade. The home screen is made
of **desktops** (pages of `widgets/WidgetsPager`, holding widgets, desktop apps
and folders), a **launcher bar** (the dock), a **panel** (top bar), and the
**dash** (full-screen app grid plus search). Apps move between these surfaces
by drag and drop.

## Package structure (`be.robinj.distrohopper`)

### Root package

- **`HomeActivity`** — the launcher's activity. Keep view work in `home/`.
  Theme, launcher-edge and panel-edge changes recreate it; rotation does not
  (`configChanges`), so rotation-sensitive code re-applies in
  `onConfigurationChanged`.
- **`Application`** — the Android `Application`. It owns the
  `DependencyContainer`, configures crash reporting, and sweeps every
  activity's decor for the font preference (see `preferences/`).
  **Crash reports include only the `"prefs"` and `"lenses"` SharedPreferences
  files.** Never store user-specific data (paths, app inventory, layouts) in
  those two; give it a file of its own, which is excluded by default.
- **`DependencyContainer`** — hand-wired container for shared objects
  (`PreferencesRepository`, `DispatcherProvider`, `ThemeManager`, …), looked up
  via `DependencyContainer.of(context)`; tests swap it via
  `DependencyContainer.override`. **Hard rule: no DI frameworks
  (Hilt/Dagger/Koin).** `DispatcherProvider` lets tests inject deterministic
  coroutine dispatchers.
- **`AppRepository` / `AppManager`** — `AppRepository` holds the installed and
  pinned app lists (`StateFlow` snapshots plus live lists; the dash grid is
  backed by the live installed list). `AppManager` is a facade kept for its
  many existing callers; prefer `AppRepository` in new model code. The view
  sync is `home/LauncherBarBinder`, resolved lazily so `AppManager` can be
  built on a background thread.
  - **Pins are per desktop.** `AppRepository` keeps a pinned list per desktop.
    In the default `global` mode (`LAUNCHER_APP_PIN_MODE`) every desktop maps
    onto desktop 0, i.e. one shared list. Unqualified ops (`pin`, `unpin`, …)
    act on `currentDesktop`; desktop-explicit overloads exist for per-desktop
    logic. Stored by `PinnedAppsStorage` (`"<page>/<index>"`, legacy bare
    `"<index>"` = desktop 0); `home/PinnedAppsMigration` rewrites it when the
    mode is toggled.
  - **Dash sort order** (`APP_SORT_ORDER`: alphabetical, recent, most used,
    custom) comes from `preferences/AppSortOrder` via `AppComparators`, with
    alphabetical as the tie-breaker. `custom` is applied by
    `DashLayoutRepository`, not a comparator, and is developer-gated for now.
    Usage data is `AppUsageStats` (its own `"app_usage"` file), recorded by
    `App.launch()`.
- **`App`** — one launchable activity. It wraps a `ResolveInfo` (personal
  profile), a `LauncherActivityInfo` (other profiles), or is an **internal
  shortcut** that exists only in the dash (the Settings entry). Internal
  shortcut intents must not set `FLAG_ACTIVITY_NEW_TASK`: the target shares the
  home task, so it would only bring home to the front.
- **Profiles (work, clone, private)** — `Profiles` wraps the profile helpers.
  In the model a null `UserHandle` is the personal profile; `App.getUser()` is
  set only for other profiles, whose apps load through LauncherApps, launch via
  `LauncherApps.startMainActivity`, carry the profile badge, and count in
  `App.equals` (one package can exist in several profiles).
  **Persistence and cache keys use `App.getProfileScopedKey()`**, which equals
  the legacy package+activity key for personal apps; never key by package
  alone. Not every profile has a badge, and some vendor builds throw instead of
  saying so: `Profiles.profileGlyph` returns null for both, and callers fall
  back to an unbadged icon. `broadcast/WorkProfileAppsCallback` keeps other
  profiles current, since package broadcasts only cover the personal one.
- **`HomeRole`** — HOME-role checks and requests. The system role dialog is
  unreliable on some OEM builds, so callers fall back to the home-settings
  screen when the role still isn't held.
- **`AboutActivity`**, **`LicensesActivity`/`LicenseTextActivity`** — licence
  texts are **assets**, not strings, because the bundled fonts' licences must
  ship with them. `copyLicenseAssets` stages `licenses/` and `LICENSE`.
  **Bundling a new font, vendored file or dependency means adding its licence
  text to `licenses/` and an entry to `LicensesActivity.ENTRIES`.** The GPL
  notice on the About screen is required; don't drop it.
- Utilities: `Image`, `Utils`, `ViewFinder`, `InsetsHelper` (system bar and
  cutout insets), `Permission`, `RequestCode`, `ExceptionHandler`,
  `Observed`/`IObserver` (a small observer pattern).

### Drag and drop (cross-cutting)

Every surface uses the platform drag-and-drop framework. The drag's local
state says what is being dragged: an `App` (a loose dash app, pinnable by
drop), a `desktop/dash/DashDragPayload` (dash folders and members), a
`desktop/launcher/LauncherDragPayload` (launcher folders and members, and a
dash icon whose app is already pinned — so a desktop drop pins a separate copy
instead of unpinning it), or the view itself (desktop apps and widgets). A drag
starting on the launcher bar carries the pinned index as its clip.

- **Local state only survives within one window.** Pop-ups that start drags
  (all three folder pop-overs) are therefore **in-activity overlays** added to
  the activity's content, never a `PopupWindow`; a cross-window drag arrives
  with null local state.
- **Never mutate views, not even visibility, while `ACTION_DRAG_ENDED` is
  dispatching.** Post the work instead, or the framework throws a
  `ConcurrentModificationException`.
- Reorder previews keep the dragged item as an invisible placeholder and move
  the others around it; the model changes only on drop. Fold (make or add to a
  folder) versus reorder is decided spatially — over a cell's centre versus its
  edge — or, where noted, by dwelling.
- Dragging an app **icon** out of the dash shows the **app-info target**
  (`desktop/launcher/AppInfoDragListener`) in the trash's place, since there is
  nothing to delete from the dash; dash folders and members keep the trash.
- **Cross-surface drags.** The dash stays open when a drag starts.
  `desktop/launcher/DashCrossSurfaceController` closes it when the drag hovers
  the launcher or panel and reopens it when the drag hovers a BFB, so one drag
  can go dash → launcher → desktop. A BFB only ever opens (it sits inside its
  bar, so treating it as a toggle would flicker). Dropping while the dash is
  open means dropping into the dash: `WidgetsContainer_DragListener.dropIntoDash`
  removes the app from its source surface.
- Starting a dash drag hides the soft keyboard, which could otherwise cover the
  drop targets. Near the grid's top or bottom edge `desktop/dash/DashEdgeScroller`
  auto-scrolls, and the listener re-resolves the target as rows move under a
  still finger.

### Folders (dash, launcher, desktop)

All folders are unnamed, hold 2 to 9 apps (`folder/FolderGrid`, a 3×3 grid;
dropping to one app dissolves a folder), draw as `folder/FolderIconDrawable`
(a mini grid), and open in an in-activity pop-over sharing
`folder/FolderOverlay`'s chrome (blurred dim backdrop, grid opened over the
tapped icon). Opened folders lay apps out with `FolderGrid.columns`/`rows`,
the same layout as the icon. Each surface has its own model:

- **Dash** — `DashLayoutRepository` builds `DashItem`s (app or folder) from the
  installed list plus stored folders and custom order (`DashLayoutStorage`,
  `"dash_layout"`, partitioned per profile); `DashComparators` orders them.
  Dragging is `desktop/dash/DashGridDragListener`: live reorder preview (custom
  order only) and spatial fold; members come out by dwell-to-fold or dropping
  on the dash. Opens `desktop/dash/FolderPopup`. Trash: members return loose.
- **Launcher** — `LauncherLayoutRepository` (`"launcher_layout"`) stores only
  folder **membership** over the per-desktop pins; the bar's order is the
  pinned order, with a folder drawn at its first member's slot, so reordering
  the bar reorders the pins. Fold by dwell (`AppLauncherDragListener`). Opens
  the shared `FolderPopup`; dragging a member out ungroups it (it stays
  pinned). Trash: the folder's apps are unpinned.
- **Desktop** — a 2×2 `widgets/DesktopFolderView` on the desktop grid;
  `widgets/DesktopFolderHost` owns them (mirroring `DesktopAppHost`) and stores
  them in `"desktop_layout"`. An app is either loose on a desktop or in one of
  its folders, never both (enforced on restore and on drop). Opens
  `widgets/DesktopFolderOverlay`. Trash: the apps go too.

`AppManager` owns the dash and launcher layouts (`loadDashLayout` and
`loadLauncherLayout` run after `loadPinnedApps`; `remove` reconciles both).

### `home/` — the home screen's controllers

Each owns one concern and is constructed in `HomeActivity.onCreate` with the
`ViewFinder` plus what it needs from the container.

- `HomeViewModel` — screen state as `StateFlow`s, collected into the
  controllers by `HomeStateBinder`; survives `recreate()`. Its preference flows
  apply some settings live (panel opacity, launcher icon preset, dash columns,
  running apps). Customise mode is a `MutableStateFlow` on the container, not
  the ViewModel, because `App.launch()` checks it with only a Context.
- `StartupLoader` / `AppsLoader` — startup on the activity's `lifecycleScope`:
  wallpaper, then apps, then the label and icon caches, strictly in that order
  (both early steps touch the BFB). `AppsLoader` also applies the first-run
  default pins (`DefaultPinnedApps`). `SearchLoader` runs dash searches the
  same way. Tests make these deterministic with
  `ActivityTestSupport.installTestDispatchers()`.
- `IconMemoryTrimmer` — releases the icons the apps hold, on
  `HomeActivity.onTrimMemory`. `TRIM_MEMORY_UI_HIDDEN` spares the pinned apps,
  `TRIM_MEMORY_BACKGROUND` and deeper drop those too; the legacy
  `TRIM_MEMORY_RUNNING_*` levels are not delivered since Android 14.
  `App.getIcon` restores a dropped icon from `AppManager.iconCache`.
- `ThemeApplier` — applies the theme's resources. The BFB and the startup
  spinner share one wrapper, so a hidden BFB must not collapse it while the
  spinner is up.
- `LauncherEdgeController` — launcher and panel placement per edge, and the
  widget area's insets.
- `DashController` / `DashAnimator` — open and close the dash (instantly or
  finger-tracked) and its visuals: wallpaper and widget blur, panel opacity, and
  the theme's `DashAnimation` preset. Everything settles instantly in battery
  saver. Closing the dash in customise mode relaunches the activity instead.
- `HomeGestureController` — swipe gestures on empty desktop: configurable
  swipe-up/down (`GestureAction`), sideways desktop paging, and swipe-to-close
  for the dash (`desktop/dash/SwipeToCloseLayout`). The notification-tray action
  goes through `accessibility/NotificationAccessibilityService` and is offered
  only once that service is enabled; at least one gesture always opens the
  dash. The widget pager consumes desktop touches, so HomeActivity feeds them
  in through an `OnTouchListener`.
- `WallpaperColourApplier` / `LauncherTileColour` — chameleonic themes' colour,
  from `WallpaperManager.getWallpaperColors` (the wallpaper bitmap itself is
  unreadable on Android 13+).
- `CustomiseModeUi` — the customise-mode controls inside the dash (sliders,
  segmented rows, Done). Done is the only way out; it relaunches home.
- `DesktopMenuOverlay` — the long-press-on-desktop menu (Widgets, Customise,
  Settings): the desktop zooms out while a bottom sheet slides up. Unlike the
  folder pop-overs it is a `Dialog` in its own window, the only way to blur just
  the area it covers; the blur attributes and sheet geometry are subtle and
  commented in `DesktopMenuSheetTheme` (styles.xml) and the class.
- `LauncherBarBinder` — keeps the launcher bar and dash grid in sync with the
  model, owns the bar's drag-reorder preview (including pin-by-drop from the
  dash and search results), and morphs the bar between desktops as they are
  swiped (`LauncherMorph` slot maths over `desktop/launcher/PinnedAppsBar`;
  skipped in global pin mode and battery saver).
- `Desktops` — the single authority on how many desktops exist and on
  operations spanning widgets, pins, desktop apps and folders at once
  (`deleteDesktop`, and `removeEmptyDesktops`, which runs after every page
  change once the initial restore is done). There is always one empty desktop
  at the end.
- `LayoutTransitionConfigurer` — appear/disappear animations.

### `desktop/` — the surfaces

The activity is transparent over the system wallpaper, which is a separate
window: opening the dash blurs it with cross-window blur, with a frosted
fallback (`FrostedGlass`, `Wallpaper`) where that is off or unsupported.
Widgets, in the activity's own window, get a `RenderEffect` blur instead.
Several distinct `AppLauncher` classes exist in different packages; they are
per-surface icon views, not one class.

- **`desktop/dash/`** — the dash. Its app grid is always a `ViewPager2`
  (`ProfilePagerAdapter`) with one `GridView` page per profile, a single page
  in the usual case. Pages are laid out lazily and the dash is `GONE` when
  closed, so the live grid (id `gvDashHomeApps`) exists only while it is open;
  tests force it with `ActivityTestSupport.layoutDashApps`. Each page's title
  scrolls away with its first row. `DashGrid` owns sizing: the user picks
  columns across the short edge (`DASH_GRID_COLUMNS`), and landscape shows
  proportionally more, capped at twice that, the same count for every grid
  including lens results (`DashGridSizer` applies it). A per-theme
  `ProfileIndicator` (`profile/`, chosen by the `profile_indicator` theme
  integer) shows which profile page is current.
- **`desktop/dash/lens/`** — search providers ("lenses") coordinated by
  `LensManager`. Each lens:
  - declares a **`key`, frozen once shipped** — it is what enabled lenses are
    persisted under, independent of class and display name; changing it drops
    the lens for every user (that is how a lens is retired, see
    `LocalFiles_v2`). `LensKeyTest` pins the set.
  - streams results through a `LensResultEmitter` as each is ready, grouped in
    named sections (`InstalledApps` emits one per profile).
  - declares a `LensType`: `LOCAL` lenses run on every keystroke, `IO` and
    `NETWORK` lenses after a debounce; lenses run one after another.
  - handles its own clicks (`Lens.onClick`) and may offer a settings screen.
  `LocalFiles` searches only folders granted through the system folder picker
  (`localfiles/SearchFolderStore`, in the lens's own prefs file) and needs no
  storage permission; Android refuses to grant `Download` and storage roots.
- **`desktop/launcher/`** — the launcher bar and its listeners.
  `LauncherIconGrid` owns sizing: the user picks one of five presets
  (`LAUNCHER_ICON_PRESET`), which maps to a slot count across the short edge,
  and the icon size makes that many slots fill the launcher's interior, the same
  on every theme. A launcher on a side edge runs along the long edge at that
  size and so shows more; `visibleCountForPreset` is the count users actually
  see. The clipping scroll views floor overflow to whole slots, and
  `LauncherBar` keeps a floating dock tight around its contents.
- **`desktop/launcher/service/`** — `LauncherService`, a developer-mode-only
  started service that floats a copy of the launcher bar over other apps.

### Other packages

- **`widgets/`** — hosting third-party widgets, and the desktops.
  `WidgetsPager` is the horizontal pager of desktops (each a
  `WidgetsContainer`; capped at 16, always one empty desktop at the end; Home
  returns to the first). Desktops lay items out on a `WidgetGrid` whose size is
  **snapshot state, not a formula**: five presets are computed and persisted on
  first launch (`DESKTOP_GRID_PRESETS`) along with the grid in use
  (`DESKTOP_GRID_SIZE`), because the layout (`DesktopLayoutStorage`,
  `"desktop_layout"`: widgets, desktop apps, folders) stores absolute cells.
  The grid is transposed in landscape. Changing the preset relaunches home.
  Widget sizing follows the provider's hints; placement waits until the page is
  measured. Long-pressing a widget enters edit mode (resize handles; drag to
  move or trash).
- **`widget/bfb/`** — the widgets DistroHopper *provides*: two BFB widgets that
  open the dash (one also focuses search), drawn like the active theme's BFB.
  `BfbWidgetProviderBase.requestUpdate` repaints them on theme and wallpaper
  colour changes. They start `HomeActivity` with `openDash`/`focusSearch`
  extras, consumed once in `onResume`.
- **`onboarding/`** — the first-run wizard (theme choice, set as default home).
  `HomeActivity.onCreate` redirects there via `OnboardingGate` until
  `SETUP_COMPLETED` is set; tests get that from
  `ActivityTestSupport.launchHome()`. A genuine first run, never an upgrade
  (`broadcast/AppUpgradeReceiver`), arms one-time default pins. The app
  declares no dangerous permissions, so there is no permission page.
- **`preferences/`** — `PreferencesActivity` plus screens for lenses, themes and
  icons. `PreferencesRepository` gives typed, observable access to `"prefs"`
  keyed by the `Preference` enum; prefer it over raw `SharedPreferences`.
  Developer options (`pref_dev.xml`) are gated by the `dev` preference and
  cleared when it is switched off. **Fonts:** `FontInflaterFactory` applies the
  font preference to every inflated `TextView`, keeping only the typeface
  *style*, so set weight with `android:textStyle`, never `fontFamily`. It also
  corrects size and spacing for fonts that draw larger (OpenDyslexic),
  relative to each view's own values and idempotently. Text built in code must
  call `FontPreference.applyTo(View)` itself.
- **`theme/`** — one `Theme` subclass per desktop look, mapping every themeable
  field to resources. `ThemeRegistry` lists them; resolve the active one with
  `DependencyContainer.of(context).themeManager.current`, never a static.
  `dev_only` themes are hidden from the pickers outside developer mode. Opt-in
  capabilities use a `0`/`false` sentinel so only themes that need them set
  them (e.g. `dash_background_edge`, `statusbar_follows_launcher_edge`). The
  BFB position (`LAUNCHER_BFB_LOCATION`) is user-choosable only on themes that
  declare several positions and is reset on theme change. Each theme's
  drawables come from its own `etc/generate_theme_<name>_assets.py`; keep those
  generators separate, do not share code between them.
- **`icons/`** — how app icons are drawn. `IconRenderer` composites adaptive
  icons (masked to the chosen `IconShape`, or tinted from the monochrome layer)
  and leaves legacy and icon-pack icons untouched. `IconConfig` holds the
  settings and its `signature()`: any change to it purges the disk icon cache
  on the next load. The render size is `IconRenderSize`, the largest an icon is
  drawn on any surface **in either orientation** (dash, launcher, desktop,
  folder pop-overs), capped at the 108dp adaptive canvas. One rendered icon is
  kept per app, so this size is the app's memory footprint; rendering below
  what is drawn visibly blurs icons. It depends only on the screen edges,
  density and grid preferences, so neither rotation nor a theme switch moves
  it. `IconPackHelper` applies icon packs; a pack that fails to resolve falls
  back to system icons and keeps the preference. DistroHopper's own monochrome
  launcher icon is generated by `etc/generate_launcher_monochrome.py`; do not
  hand-edit it.
- **`cache/`** — persistent caches behind the `ICache` map interface: app labels
  (`AppLabelCache`) and icons (`AppIconCache`, PNGs in the cache dir via
  `DrawableCache`; `HomeActivity` wraps it in an `ExpiringCache` so icons are
  refreshed weekly).
- **`broadcast/`** — `PackageManagerBroadcastReceiver` keeps the app model in
  step with installs and removals.
- **`dev/`** — in-app debug log. `Log` mirrors `android.util.Log` and, in
  developer mode, keeps a capped buffer shown by `DevLogsActivity`.
  `LogEntry.format()`'s `"[W] Tag: message"` output is a compatibility contract.
- **`thirdparty/`** — vendored code; avoid editing.

## Conventions & gotchas

- New code is Kotlin; match the style of the file you're editing rather than
  refactoring wholesale.
- Keep comments, commit messages and PR descriptions concise: say what the
  code cannot, then stop. Comment only where it reduces cognitive load (a
  non-obvious reason, a constraint, a trap). PR titles and descriptions follow
  `.claude/rules/pull-requests.md`.
- Listener classes are top-level classes named `<View><Event>Listener`.
- User-facing strings go in `res/values/strings.xml`. Never hand-edit the
  `values-*` locale files; Transifex manages them.
- **Locales.** `res/values/strings.xml` is **British English** (Transifex source
  `en_GB`); American English is the `values-en-rUS` translation. Because the
  default resources have no locale qualifier, en-IE/en-GB/en-AU devices would
  otherwise fall back to `en-rUS`, so `values-b+en+001` carries a generated copy
  of the default strings (the weekly workflow writes it; don't edit or
  translate it). `res/xml/locales_config.xml` sets `defaultLocale="en-GB"` and
  is the per-app language list: add a locale there only once it has real
  translations. Language splits are disabled so every language ships in the
  base APK.
- Do not commit `local.properties` or `secret.properties`.

## Note for Claude

If you are Claude: Your "present plan" and "ask question" tools are often
broken. Anything that is not explicit plan approval or a bundle of answers
means you STOP, and do NOT proceed with implementation on your own terms.
