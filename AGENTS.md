# AGENTS.md

> **Maintenance rule:** Whenever you touch a piece of code, check whether this
> file adequately describes it — within the limits of what AGENTS.md is for
> (structure, conventions, and how things fit together; not exhaustive API
> docs or per-class commentary). If it doesn't, add or update the relevant
> section as part of your change. If something here turns out to be wrong or
> stale, fix it.

## What this project is

DistroHopper is an Android home screen (launcher) replacement that mimics
various Linux desktops: Ubuntu Unity, elementary OS Pantheon, GNOME, Cinnamon,
KDE Plasma, MATE, Pop!_OS COSMIC, and Solus Budgie. It started as a high-school
project around 2011; code quality and style vary considerably across the
codebase — older Java alongside newer Kotlin.

## Build & test

- Single-module Gradle project: the `:app` module is the entire application.
- `compileSdk`/`targetSdk` 36, `minSdk` 31. Mixed Java/Kotlin.
- Build: `./gradlew assembleDebug`
- Unit tests: `./gradlew testDebugUnitTest` — JVM tests under
  `app/src/test/`, written in Kotlin using Robolectric (they exercise real
  activities/views without a device).
- Instrumented tests live under `app/src/androidTest/` (require a
  device/emulator; rarely the right place for new tests — prefer Robolectric).
- Release workflow: pushing a `v*` tag builds signed release artifacts and
  attaches them to a GitHub Release. Tags whose version ends in a letter (for
  example `v3.0.0d`) are marked as GitHub pre-releases and are not promoted to
  latest; plain numeric versions (for example `v3.0.0`) remain full releases.
  Release attachments are named `DistroHopper-<version>.apk`,
  `DistroHopper-<version>.aab`, and `DistroHopper-<version>-paranoia.apk`,
  where `<version>` is the full version tag, including its leading `v`.
- Releases can also be cut from GitHub: run the **CI** workflow manually
  (Actions → CI → Run workflow) and give it a version such as `v3.0.1` or
  `v3.0.1a`. The `release-context` job validates the format, refuses a version
  whose tag already exists but does not carry that version (a tag whose commit
  already sets `baseVersionName` to it is a previous run that pushed but failed
  later, and the release resumes from it — judged by what the tag holds, so a
  resume still works once `master` has moved on past it, and the release builds
  the tagged commit rather than the branch tip; the tag must also be reachable
  from `master` and be the newest release, so a resume cannot reuse a superseded
  build's `appVersionCode`; and no GitHub Release may exist for the tag yet — a
  release that outlived a failed run, or its deleted tag, must be deleted before
  retrying, since the release action overwrites its attachments by default), bumps `baseVersionName` and
  increments
  `appVersionCode` in `app/build.gradle`, commits that to `master`, tags the
  commit (authored as `Robin Jacobs <RobinJ1995@users.noreply.github.com>` — the
  same identity as hand-made commits here, and `git config user.*` sets author,
  committer and tagger alike; the *push* is still authenticated by
  `GITHUB_TOKEN`, so the pusher remains `github-actions[bot]` and the commit is
  unsigned), and the release job builds from that tag in the same run. Unit
  tests are skipped on this path — the code being released is master's already-tested
  tip, and the bump commit touches only the version constants. Pushing a tag by
  hand still runs the tests first, unchanged. Note that if branch protection is
  ever enabled on `master`, `github-actions[bot]` needs a bypass entry or the
  bump commit cannot be pushed.
- Tagged CI releases build the normal signed APK/AAB plus a best-effort
  signed `-paranoia` APK (`./gradlew clean assembleRelease -PparanoiaBuild=true`):
  it appends `-paranoia` to `versionName`, forces `BuildConfig.ACRA_CONFIGURED`
  off even if credentials exist, and omits AGP dependency-info metadata from
  APKs. It deliberately does not remove dependencies/classes; it is a
  privacy-conscious courtesy sideload artifact, not a separately supported
  product line.
- Translation updates run weekly through `.github/workflows/update-translations.yml`:
  it runs at 00:00 UTC each Wednesday, installs the Transifex CLI, runs `tx
  pull -a` using the `TRANSIFEX_TOKEN` repository secret, and opens or updates
  a pull request with changed Android resource translations.

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
    Pinned apps are **per widget desktop**: `AppRepository` holds a list per
    desktop and a `perDesktop` flag from `preferences/LauncherPinMode`
    (`LAUNCHER_APP_PIN_MODE` = `global`|`desktop`, default `global`; unknown
    → global). In global mode every desktop maps onto desktop 0, so it is
    exactly the old single shared list. The no-desktop ops (`pin`/`unpin`/…)
    act on `currentDesktop` (set as the pager settles); desktop-explicit
    overloads (`pinnedOn`, `pin(app,desktop)`, `removePinnedDesktop`, …) drive
    per-desktop logic and deletion. `PinnedAppsStorage` is the shared on-disk
    format (`"<page>/<index>"` per-desktop, bare `"<index>"` legacy → desktop
    0), the value being an `App.profileScopedKey` (see Profiles below);
    `home/PinnedAppsMigration` rewrites it when the mode is toggled in
    settings (which then relaunches home). Same app may be pinned on several
    desktops; unpin/uninstall and the trash act on the current desktop (or all
    desktops for uninstall).
  - `App`, `Application`, `AppComparatorAlphabetical` — app model classes. `App` usually wraps a PackageManager `ResolveInfo`, but can also wrap a `LauncherActivityInfo` for apps in another profile (see Profiles below), or represent DistroHopper-owned internal shortcuts that live only in the dash (currently the settings shortcut) and launch by explicit in-app intent rather than a public launcher component. Internal shortcut intents must not set `FLAG_ACTIVITY_NEW_TASK` (the target shares the home task's affinity, so it would only bring the home task to the front); the settings shortcut launches via `startActivityForResult` so `HomeActivity.onActivityResult` can handle the Customise UI result.
  - **Dash sort order**: `AppRepository.sort()` orders the installed list by the `preferences/AppSortOrder` chosen in settings (`APP_SORT_ORDER` = `alphabetical`|`recent`|`most_used`|`custom`, default `alphabetical`; unknown → alphabetical). `AppComparators.forOrder` builds the comparator; the two usage-based orders sort by score descending with `AppComparatorAlphabetical` as the secondary key (so a tied group stays alphabetical), and `custom` degrades to alphabetical there (the manual arrangement is applied by `DashLayoutRepository`, not by a comparator). The `custom` order is developer-gated for now (needs more testing before general availability): `PreferencesActivity` only offers it while developer mode is on and resets a stored `custom` back to alphabetical when it isn't. The usage data lives in `AppUsageStats` — per-app launch count and last-launched timestamp keyed by `App.profileScopedKey`, in its own `"app_usage"` SharedPreferences file (kept out of the main "prefs" so a launch doesn't trip its change listeners). `App.launch()` records every real launch there (internal shortcuts excluded). The order is read fresh on each `sort()`, so changing it in settings takes effect when home reloads on leaving the preferences screen.
  - **Dash folders & custom order**: the dash grid renders `DashItem`s (an `AppItem` or a `FolderItem`) built by `DashLayoutRepository` from the installed list plus the persisted folders/manual order (`DashLayoutStorage`, the `"dash_layout"` SharedPreferences file, partitioned per profile). `AppManager` owns it (`getDashLayout`, `loadDashLayout` after `loadPinnedApps`, `dashLayoutChanged` to refresh; `remove` reconciles it). Folders are unnamed, hold ≥2 apps (a folder dropped to 1 dissolves), are capped at 9 (`DashLayoutRepository.MAX_FOLDER_APPS`, a 3x3 grid — see `folder/FolderGrid`), and render via `folder/FolderIconDrawable` (a mini-grid icon). `DashComparators` orders items: a folder ranks by its highest-scoring member, and in `alphabetical` mode folders group ahead of loose apps. Tapping a folder opens `FolderPopup`. Dragging within the dash is `desktop/dash/DashGridDragListener` (pause over another app to fold — the `dash_folder_drop_indicator` ring — drop on a folder to add, reorder under `custom` only, drag a folder member onto the dash to pull it out); a loose app keeps passing its `App` as the drag's local state (so the launcher still pins it), while folders/members carry a `desktop/dash/DashDragPayload`. `TrashDragListener` deletes a dropped folder (members return loose). The dash no longer auto-closes when a drag starts (`LauncherBarBinder.startedDragging` keeps it open and the BFB visible); the cross-surface drag instead closes the dash when the drag hovers the launcher or panel and re-opens it when it hovers a BFB while the dash is closed, so an app can move dash↔launcher↔desktop in one drag. `desktop/launcher/DashCrossSurfaceController` resolves the intent **by dash state**: while the dash is open, hovering the launcher (reported from `LauncherDragListener`) or the panel (`DashEdgeDragListener` on `llPanel`) closes it; while it is closed, hovering a BFB (`DashEdgeDragListener` on `lalBfb` / `tvPanelBfb`) re-opens it. A BFB only ever *opens* (**open precedence**): it sits inside its bar, so a hover registers both an open- and a close-target — counting that as a close would make the BFB a toggle and the dash would flicker across the BFB↔bar boundary, so while open the close fires only when a close-target is hovered and no BFB is, and opening drops the close-target that rode in with the BFB hover (so leaving the BFB doesn't slam it shut). Enter/exit are edge-triggered (no per-LOCATION action) so a hover resolves once without oscillating; the change is posted + debounced. Dropping while the dash is open means dropping **into** the dash: `WidgetsContainer_DragListener.dropIntoDash` removes the app from its source surface (unpin a dock pin / take a desktop app off the grid / leave a desktop folder) so it returns to just the app drawer, instead of letting the drop fall through onto the desktop behind.
  - **Launcher folders**: `LauncherLayoutRepository` (+ `LauncherLayoutStorage`, the `"launcher_layout"` file) layers folders over the **per-desktop** pinned apps, the counterpart to the dash's `DashLayoutRepository`. It stores only folder **membership** — the bar's order is the pinned order itself (`AppRepository.pinnedOn`), with a folder rendered (once) at its first member's position — so reordering the bar reorders the pinned model (`AppRepository.reorderPinned`, fed by `LauncherBarBinder.flattenBarKeys`) and the floating launcher service / persistence stay in step. `AppManager` owns it (`getLauncherLayout`, `loadLauncherLayout` after `loadPinnedApps`, `launcherLayoutChanged`; `remove` reconciles it). The bar renders `LauncherItem`s (app or `LauncherFolderView` mini-grid); `LauncherBarBinder.refreshPinnedView`/`buildMorph` are item-based and the per-desktop morph (`LauncherMorph`, now generic) fades folders in/out (a folder id is unique to one desktop). Drag uses the existing placeholder reorder generalised to views (`draggedPinnedItemOver`), with `AppLauncherDragListener` adding a dwell-to-fold (`foldDraggedOnto`: create/add, toast when full) and folder drags carrying a `LauncherDragPayload`; `TrashDragListener` **deletes a launcher folder and unpins its members** (unlike the dash, where they return loose). Tapping a launcher folder opens the shared `FolderPopup` (with a `LauncherDragPayload.FolderMemberDrag` payload); long-pressing a member there drags it **out** of the folder, behaving exactly like dragging the pin itself — `LauncherDragListener` opens a placeholder via `LauncherBarBinder.startedDraggingLauncherFolderMember` so it reorders/folds normally, and because a folder only *groups* already-pinned apps the app is **not** re-pinned but **ungrouped** (`removeFromFolder`) on a committing drop (cancel leaves it in the folder; `flattenBarKeysExtracting` keeps the dragged occurrence at the drop slot). `FolderPopup` (shared with the dash) is an **in-activity overlay** — added to the activity's `content`, NOT a `PopupWindow` — so the extract drag originates in the same window as its drop targets and carries its local state to them (a cross-window / `DRAG_FLAG_GLOBAL` drag arrives with a *null* local state, which the listeners need); this mirrors `DesktopFolderOverlay`. A launcher folder member can also be dropped on the **desktop** (`WidgetsContainer_DragListener.Drag.LauncherFolderMember`): it is placed there, then ungrouped and unpinned off the bar, so it leaves the launcher — like dropping a dock pin on the desktop.
  - **Desktop folders** (apps only): a folder is a 2×2 `widgets/DesktopFolderView` on the 8×8 grid whose apps are packed 1×1 on the 3×3 `folder/FolderGrid` for storage (`widgets/DesktopFolderLayout` + `DesktopFolderCell`, `withApp` fit-or-null, ≥2 apps, ≤9; persisted in the shared `"desktop_layout"` file via `DesktopLayoutStorage`, alongside the desktop widgets and pinned apps). The folder view itself is unnamed (the `FolderIconDrawable` mini-grid, no label), but the opened overlay shows each member app's label (it inflates the shared `widget_dash_applauncher` icon-over-label cell, like the dash/launcher popups — not a bare icon). `widgets/DesktopFolderHost` mirrors `DesktopAppHost` (create from two desktop apps, two `addApp` overloads — a loose `DesktopAppView` removed off the grid, or an `App` coming straight from another surface — both toast-on-full, deleteFolder removes apps, moveTo, restore/persist, per-page, uninstall reconcile) and is the third desktop owner in `home/Desktops`; it `restore()`s after widgets + desktop apps so its 2×2 avoids occupied cells, and `WidgetsContainer.collectOccupied` includes folder cells (+ `findViewAtCell` for hit-testing). Tapping a folder opens `widgets/DesktopFolderOverlay` — an **in-activity** overlay (not a PopupWindow) so the extract drag originates in the activity window and carries its local state to the drop targets. The overlay lays its apps out with the adaptive `FolderGrid.columns`/`rows` mapping (NOT the stored packed col/row), the **same** layout the `FolderIconDrawable` preview uses, so an opened folder matches its icon (4 apps read as a 2×2, not a row-major 3+1). All three folder popovers share `folder/FolderOverlay` for their chrome: a dim, GPU-blurred (`RenderEffect`, minSdk 31, no wallpaper bitmap) backdrop with the grid opened **centred over the tapped icon** (clamped on-screen) by a scale+fade animation that grows from it. The desktop's loose-XOR-in-folder invariant is also enforced on restore (a folder member that is *also* a loose desktop app is dropped — loose wins — via `DesktopAppHost.viewForKey`) and live (a cross-surface drop that lands an app loose on the desktop calls `DesktopFolderHost.dropFromFolders`). Drag wiring is in `widgets/WidgetsContainer_DragListener` (`Drag.DesktopFolder` reposition; dwell a desktop app over another app/folder folds — app→app creates **at the target's cell** (the stationary app dragged onto), app→folder adds; an **incoming** app — a `Drag.IncomingApp` from the dash/search or off the launcher bar — dwelt over a folder adds straight into it via `addApp(folderId, app)`, the drop ring showing as for any fold, then the launcher origin is cleared like the loose-drop path); `TrashDragListener` deletes a desktop folder (its apps gone). Long-pressing a member in the overlay extracts it (a `DesktopFolderMemberDrag`): dropped on the desktop it lands at the drop cell (`DesktopFolderHost.removeMember` clears/shrinks the folder *before* placing the extracted app, so the still-present 2×2 can't bump it and a dissolved folder's other member — returned near the folder's old cell — can't take the drop spot), dropped on the trash it is deleted (`deleteMember`).
  - **Profiles (work profile support)**: `Profiles` wraps the profile
    helpers (LauncherApps profile listing, labels, persistence serials).
    Throughout the model a null `UserHandle` means the personal profile;
    `App.getUser()` is set only for apps in other profiles (work profile),
    which load via `AppRepository.queryOtherProfileApps()`
    (LauncherApps), launch via `LauncherApps.startMainActivity`, get the
    profile badge on their icon, and participate in `App.equals` (the same
    package can exist in both profiles). Persistence/cache keys use
    `App.getProfileScopedKey()` — identical to the old
    package+activity key for personal apps, with the profile serial
    appended otherwise (so old pinned-app prefs keep matching). The
    InstalledApps lens splits its results into one section per profile
    (`Lens.searchCollections`) while staying one lens.
    **Dash apps are always a `ViewPager2`** (`desktop/dash/ProfilePagerAdapter`,
    one `GridView` page per profile — a single page in the usual single-
    profile case, so it looks/behaves like the plain grid). This is the one
    consistent layout (no grid-vs-pager swap); `LauncherBarBinder.bindDashApps`/
    `rebindDashApps` rebuild it, and a tab indicator appears once there is
    more than one profile. Note the pager's pages are laid out lazily and the
    dash sits in a `GONE` container when closed, so the current page's grid
    only exists while the dash is open; it carries the `gvDashHomeApps` id and,
    because only the current page is attached when the pager is idle,
    `findViewById(R.id.gvDashHomeApps)` resolves to it — that's how `DashAnimator`
    (the genie) and `ThemeApplier` reach the live grid (both null-safe for when
    it isn't laid out yet). The per-page `LayoutTransition` is set in the adapter
    (it can't be in `LayoutTransitionConfigurer`, which runs before any page
    exists) and **must** keep `setAnimateParentHierarchy(false)`: ViewPager2
    rejects a page whose child ViewGroup has a parent-animating LayoutTransition
    and crashes mid-scroll (notably on rotation). Each page's title
    (`tvDashHomeTitle` — the profile name, or "Applications" for a single
    profile) overlays the top of its grid rather than sitting in a row above it:
    the grid reserves a top padding the height of the title (`clipToPadding=false`
    so that padding scrolls away with the apps) and
    `ProfilePagerAdapter.updateTitleOffset` translates the title up in step with
    the grid's scroll, so it scrolls off-screen with the first row instead of
    permanently occupying the top of the dash. The padding is applied in
    `onBindViewHolder` *before* the grid's adapter is set (from a one-off
    `measureTitleHeight`, stable since the title is one line of identical height
    for every profile): AbsListView does not re-anchor already-filled rows when
    paddingTop changes at runtime, so applying it after the fill left the first
    row at `top=0` and the title resting pre-collapsed on swiped-to pages. A grid
    layout-change listener recomputes the at-rest offset after any (re)fill or
    rotation, since the scroll listener never fires for a page whose apps fit
    without scrolling.
    Tests force the lazy layout via `ActivityTestSupport.layoutDashApps`.
    Grid sizing is owned by `desktop/dash/DashGrid` (the dash counterpart to
    `widgets/WidgetGrid`): the user picks a column count across the short screen
    edge (`DASH_GRID_COLUMNS`, adaptive default from `smallestScreenWidthDp`),
    and `DashGrid.dashColumns` derives the count for the current orientation
    from the screen — not from any one grid's width — so every profile page and
    every lens results grid get the same count and it stays stable as the dash
    area changes (theme, rotation, a future launcher auto-hide). Landscape shows
    proportionally more columns, capped at 2× the short-edge count.
    `DashGridSizer` applies it (`setNumColumns`) to each grid; cells stretch to
    fill. Rotation re-applies via `HomeActivity.onConfigurationChanged` (the
    activity isn't recreated on rotation). The customise-mode "N × M" hint reads
    the apps grid's last laid-out viewport (`LauncherBarBinder.dashGridViewport`,
    captured while the grid is visible, since it is GONE while customising) so
    the row count reflects the real theme/orientation, not the full screen.
    Pinned-icon sizing is the launcher counterpart, owned by
    `desktop/launcher/LauncherIconGrid`: the user picks one of five presets
    (`LAUNCHER_ICON_PRESET`, Tiny…Huge ascending so the customise slider grows
    icons to the right, middle = adaptive default), which maps to a whole slot
    count derived from `smallestScreenWidthDp` alone (so the
    count is identical on every theme; the BFB counts as a slot), and the icon
    size is computed at runtime so exactly that many slots tile the launcher's
    usable interior on the screen's shortest edge (theme `launcher_margin` and
    the launcher background's 9-patch insets subtracted; nothing but the preset
    index is stored). `AppLauncher.init()` reads it; when pinned/running apps
    overflow, `ClippingScrollView`/`ClippingHorizontalScrollView` floor the
    scroll viewport to whole slots so no partial icon peeks (they leave the
    measure alone while everything fits, preserving `PinnedAppsBar`'s
    fractional morph measure). `llLauncher` is a `desktop/launcher/LauncherBar`
    so a *floating* dock (a theme whose launcher wraps its contents, e.g. GNOME)
    stays tight around what it shows: LinearLayout sizes itself from an earlier
    measurement of the weighted viewport, so once that viewport settles on a
    shorter whole-slot height the parent would otherwise keep the difference
    reserved as dead space, making the dock look stretched along the edge.
    LauncherBar re-wraps to its measured children; an expanded launcher keeps
    its full length (there the leftover is inherent to the edge).
    The tab indicator is chosen per theme via the `profile_indicator`
    integer (`theme/ProfileIndicatorStyle`, like `dash_animation`):
    `UNITY_RIBBON` (Unity/Default) puts per-profile glyphs in the always-
    visible dash ribbon (`desktop/dash/profile/UnityRibbonIndicator`),
    `GNOME_PANEL` (Gnome) draws a profile pill at the panel's top-left,
    shown only while the dash is open (`GnomeProfilePillIndicator` +
    the custom-drawn `ProfilePillView`); other themes are `NONE` for now.
    Glyph indicators badge the generic `ic_profile` glyph with the system
    profile badge via `getUserBadgedIcon` (correct for work/private/clone
    profiles); the personal profile uses the theme's
    `profile_indicator_personal_glyph` (the house glyph for Unity).
    Indicators implement `desktop/dash/profile/ProfileIndicator` and are
    driven by the pager's page-scroll callback so the highlight/pill animates
    with the swipe; the dash-open signal reaches them via
    `AppManager.setDashOpen` (wired from `HomeStateBinder`'s `dashOpen` flow).
  - `IconPackHelper`, `Image`, `Utils`, `ViewFinder`, `InsetsHelper`,
    `Permission`, `RequestCode`, `ExceptionHandler`, `HomeRole` —
    support/utilities. `HomeRole` wraps the HOME-role (default launcher)
    checks/request intents used by the wizard and the preferences screen. The
    native role dialog (`roleRequestIntent`) is unreliable on some OEM builds
    (notably Samsung One UI), where it returns without ever showing a picker, so
    both callers observe its result and fall back to the system home-settings
    screen (`homeSettingsIntent`, `Settings.ACTION_HOME_SETTINGS`) when the role
    still isn't held afterwards.
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
  - `AboutActivity` — informational screen.
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
    that apply live without recreating the activity (panel opacity, the
    pinned-icon size preset, dash grid columns, show-running-apps) — the
    customise-mode seekbars only write the preference and the binder
    applies it. Theme and launcher/panel edge changes still recreate the
    activity (wholesale view-tree surgery). Widgets are always enabled (the
    old opt-out preference is gone).
  - `ThemeApplier` — applies the active theme's resources to the views.
    Gotcha: the BFB and the startup loading spinner share
    `llBfbSpinnerWrapper`, so a hidden BFB (themed or by user preference)
    must not collapse the wrapper while the spinner is still up —
    `ThemeApplier` keeps it visible while the spinner is, and
    `StartupLoader` collapses it (instead of revealing the BFB) once loading
    finishes.
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
    desktop space. Swipe-up and swipe-down are each configurable
    (`home/GestureAction`, persisted via `GESTURE_SWIPE_UP`/
    `GESTURE_SWIPE_DOWN`, edited in the Gestures preferences section): open
    the dash, open the dash and focus search, open the notification tray, or
    do nothing. Opening the dash tracks the finger in either direction (with
    the theme's own open animation scrubbed by the swipe — see
    `DashAnimator`); swiping sideways pans between the widget desktops
    (`widgets/WidgetsPager`). It is also `SwipeToCloseLayout`'s delegate for
    swiping the open dash closed. The notification-tray action can't be
    finger-tracked (the only API for it is an `AccessibilityService`'s global
    action — see `accessibility/NotificationAccessibilityService`, since the
    `StatusBarManager` route is blocklisted for non-system apps), so its
    commit is decided on release; it is offered only once that service is
    enabled, and a no-lockout rule (`GestureAction.reconcileOther`) keeps at
    least one of the two gestures opening the dash.
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
  - `DesktopMenuOverlay` — the long-press-on-empty-desktop menu (wired via
    `widgets/WidgetsPager_LongClickListener`, which still exits widget edit
    mode first if a widget is being edited): the desktop zooms out and darkens
    while a bottom sheet (`desktop_menu_sheet.xml`) slides up offering
    "Widgets" (the widget picker), "Customise" (`HomeActivity.openCustomiseMode`,
    the same customise-UI relaunch the Preferences screen's entry triggers via
    result code 4) and "Settings" (`HomeActivity.openPreferences`). The three
    actions sit **side by side**, each an icon over its label — no
    descriptions, the labels carry it.
    Unlike the folder popovers this is **not** an in-activity overlay: the
    sheet is a `Dialog` in a window of its own (`DesktopMenuSheetTheme`, a
    bottom-pinned `ModernDialogTheme`, with `desktop_menu_sheet_{in,out}`
    window animations). That is the *only* way to blur just what the sheet
    covers, and the two blur attributes are **not** interchangeable:
    `windowBlurBehindRadius` (`FLAG_BLUR_BEHIND`, what `ModernDialogTheme`
    itself uses) blurs the whole screen behind the window, while
    `windowBackgroundBlurRadius` blurs only *within the window's bounds*,
    clipped by the window background's alpha — the sheet's rounded shape. The
    sheet theme therefore turns the inherited blur-behind **off** and sets the
    within-bounds one; getting this backwards frosts the entire home screen.
    No view can do it either: a `RenderEffect` blurs the view's own content,
    all of it, and never reaches the wallpaper (a separate system window behind
    the translucent activity).
    `FrostedGlass.applyDialogFallback` covers devices with cross-window blur
    switched off, the sheet's surface being the same rounded card as the pop-up
    dialogs'. That window background *is* the surface, so the layout root must
    **not** set one (it would double the translucent fill).
    Two geometry details keep the sheet flush with the bottom edge, and both
    are load-bearing. `fitInsetsTypes = 0`: without it the window's BOTTOM
    gravity resolves against a parent frame still inset by the system bars, so
    the sheet stops a navigation bar short of the edge with a strip of
    *unblurred* wallpaper below it (`FLAG_LAYOUT_NO_LIMITS` does **not** fix
    that — it frees the display bounds, not the parent frame). And the corner
    radius is **uniform**, with the window offset one radius *below* the screen
    (`y = -CORNER_RADIUS_DP`, `refit` padding the same amount back): the blur is
    clipped to the background drawable's outline, and `GradientDrawable` only
    reports a rounded-rect outline for a single radius — per-corner radii make
    it fall back to the bare rectangle, so the blur halos past the curve in the
    top corners. Hiding the bottom corners off-screen buys the top ones a shape
    the blur can actually follow. `refit` also pads the actions clear of the
    navigation bar. Back and
    outside taps are the Dialog's own; the static active-slot pattern
    (`isShowingIn`/`dismissActive`/`clearFor`) remains so Home dismisses it
    through HomeActivity, and `clearFor` must actually close the window or it
    outlives the activity as a leak. All transitions settle immediately in
    battery saver.
    The zoom-out stays in the activity and scales only `vgWidgets` +
    `llLauncherAndDashContainer` (`DesktopMenuOverlay.zoomTargets`), **not**
    the whole activity content: the panel and status bar shrinking away from
    the screen edge reads as a glitch rather than a zoom, and the wallpaper
    views stand in for the system wallpaper behind the window, which does not
    move either. Both targets are given the *same* pivot — the centre of their
    shared `rlContainer` mapped into each one's local coordinates
    (`applyZoomPivots`) — so scaling them separately is geometrically
    identical to scaling that parent and they zoom as one piece; the pivots
    are re-derived when the sheet resizes, since HomeActivity survives
    rotation.
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
    whichever slot it is dropped on. Long-pressing an *already-pinned* app in
    the dash carries a `LauncherDragPayload.PinnedAppDrag` instead of the
    pinned-index clip: the dash is its own surface, so dropping it back on the
    bar still reorders the existing icon (the bar's placeholder bookkeeping
    keys off the dragged pin, not the local state), but dropping it on the
    **desktop** pins a *separate* desktop copy and leaves the launcher pin
    intact (`WidgetsContainer_DragListener` routes the payload to `Drag.DashApp`).
    The dash grid ignores the payload (it is neither an `App` nor a
    `DashDragPayload`), so — like the old index clip — it falls through to the
    desktop rather than being eaten as an in-dash reorder; using a plain `App`
    here regressed because the grid claimed it and grabbed the drop.
    Gotcha: views must not be mutated (not even visibility) while
    ACTION_DRAG_ENDED is being dispatched — post such work instead, or the
    framework throws a ConcurrentModificationException.
    It also renders the **current desktop's** pins and morphs between desktops
    as they are swiped: `WidgetsPager.onPageScroll` drives `onPageScroll` (a
    finger-tracked morph using the pure `home/LauncherMorph` slot maths —
    shared apps slide slot→slot, unique apps fade+scale in/out) and
    `onPageSettled` drives `showDesktop` (rebuild to the settled desktop's plain
    bar + `setCurrentDesktop`). The pinned bar itself is
    `desktop/launcher/PinnedAppsBar` (a `LinearLayout` subclass): in morph mode
    it lays its icons out at fractional slots and measures to a fractional
    length, so the launcher resizes smoothly and the same child views are
    reused (no rebuild → no LayoutTransition "appear" flash; the binder also
    suppresses that transition around any bar rebuild). The morph is skipped in
    global mode (identical bars on every desktop — the bar is never even rebuilt
    on a swipe) and battery saver (swap on settle).
  - `Desktops` — the single authority over the home screen's desktops (which
    span both widgets and per-desktop pins). It derives how many desktops
    exist (`highestOccupiedDesktop` = max of `WidgetHost.highestWidgetDesktop`
    and `AppManager.highestPinnedDesktop`; `WidgetsPager.occupiedDesktopSupplier`
    is pointed at it) and owns the structural ops that must touch both at once
    — `deleteDesktop` removes a desktop's widgets, pins, desktop apps and
    folders and reindexes the rest (future insert/reorder belong here too).
    `removeEmptyDesktops` is the automatic clean-up: it deletes every desktop
    in the occupied range that holds nothing — no widgets, desktop apps or
    folders, and (in per-desktop pin mode only; in global mode the shared bar
    can't keep a desktop alive) no launcher pins — packing the rest down so no
    gaps remain. It runs at the end of every `WidgetsPager.pagesChanged` (wired
    via `WidgetsPager.onPagesChanged`, but only **after** the initial restore so
    a half-restored desktop is never seen as empty), so deleting the last item
    from a desktop drops the desktop itself. The single trailing empty desktop
    is the pager's own doing and sits above `highestOccupiedDesktop`, so it is
    never touched — there is always an empty desktop at the end to add to. A
    re-entrancy guard stops the `pagesChanged` that `deleteDesktop` itself fires
    from recursing back in.
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
    by `LensManager` and run by `home/SearchLoader` (a coroutine runner on the
    activity's lifecycleScope, like `StartupLoader`; it replaced the old
    `AsyncSearch` AsyncTask). `Lens` is a Kotlin abstract class. A lens can
    declare `requiredPermissions()`; lenses missing any of them are left out of
    the default-enabled set, and enabling one in the preferences re-requests
    them.
    Every lens streams its results progressively: `suspend search(query,
    maxResults, emitter)` pushes each `LensSearchResult` through a
    `LensResultEmitter` the moment it is fully ready (icon and all — no
    placeholders), so e.g. `DuckDuckGo` emits each result as its icon finishes
    rather than after the slowest one. Results group into named sections
    (`LensSearchResultCollection`); the emitter's `emit(result)` uses the
    lens-named default section, while `emit(sectionName, result)` groups into
    several — `InstalledApps` emits one section per profile (personal/work)
    while remaining a single lens in the preferences.
    Lenses are still searched strictly one after another (parallel fan-out is
    too expensive), but `Lens.type` (`LensType` `LOCAL`|`IO`|`NETWORK`, an
    abstract property each lens declares) drives scheduling: `LOCAL` lenses
    (`InstalledApps`) run on every keystroke so installed apps appear instantly,
    while `IO` (`LocalFiles`) and `NETWORK` lenses run only after a short
    debounce so bursts of typing don't hit them.
    Click handling lives within each lens: `Lens.onClick` does nothing by
    default and lenses override it to launch an app (`InstalledApps`), open a
    file (`LocalFiles`), a store page (`FDroid`/`GooglePlayStore`), or a web link
    (`DuckDuckGo`/`GitHub`, via the `openInBrowser` helper). A failed search is
    rendered by `CollectionGridAdapter` as a synthetic error tile that shows the
    failure dialog (`Lens.showError`) when tapped.
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
  `theme/ThemeCards`). Developer-only options live in `pref_dev.xml` and are
  gated by the `dev` preference; they include one-shot maintenance actions
  (clear the app label/icon caches, rerun onboarding, queue default pins) and
  debug toggles such as log toasts and unrestricted widget resizing; toggles
  under this section are cleared when developer mode is switched off.
  `PreferencesRepository` provides typed and observable (`valueFlow`, a
  Kotlin `Flow`) access to the main "prefs" file keyed by the `Preference`
  enum — prefer it over raw `SharedPreferences` in new code.
- **`theme/`** — one class per supported desktop look (`Default`, `Gnome`,
  `Elementary`, `Cinnamon`, `Plasma`, `Mate`, `Cosmic`, `Budgie`), each
  extending the abstract `Theme` (which lists every themeable field and maps
  them to `R.*` ids in the subclass constructor); `Location`
  describes where UI elements sit per theme and `DashAnimation` names the
  per-theme dash open/close animation preset. `ThemeRegistry` is the single
  list of available themes (also drives the theme picker's order);
  `ThemeManager` (on the `DependencyContainer`) resolves the active theme
  from preferences — use `DependencyContainer.of(context).themeManager.current`
  rather than holding `Theme` references in statics. A theme can be marked
  `dev_only` (currently MATE, COSMIC and Budgie, pending more polish): both
  pickers (`ThemePreferencesActivity` and the onboarding wizard) hide it
  unless developer mode is enabled; the registry itself always lists it so an
  already-applied dev-only theme keeps resolving. Switching themes still
  recreates `HomeActivity`. Each theme also ships a self-contained asset
  generator at `etc/generate_theme_<name>_assets.py` (kept separate per theme
  on purpose — do not factor into a shared lib) that renders its per-density
  drawables and 9-patches from values measured off the baseline screenshots in
  `etc/theme baselines/`.
  Two `Theme` capabilities are opt-in (a `0`/`false` sentinel leaves the
  behaviour off, so only the theme that needs it sets them — no churn for the
  rest): `dash_background_edge` is an optional 5-element drawable array indexed
  by `Location.n` that lets a directional dash pick its background per launcher
  edge (Budgie's "ear" must point at the BFB whichever edge the launcher
  sits on; `WallpaperColourApplier` selects it, falling back to the scalar
  `dash_background`); `statusbar_follows_launcher_edge` (a themed `bool`) makes
  a panel-less theme (Budgie) drive the status bar off the launcher edge instead
  of the panel — opaque (`statusbar_background`) when the launcher is at the top,
  transparent (`statusbar_background_when_panel_not_top`) otherwise. Panelled
  themes that can hide their panel use `statusbar_colour_when_panel_hidden` for
  the status bar while the panel edge is None (Unity blends it toward the
  launcher; others keep their previous value). All resolved in
  `Theme.statusbar_background_resolved`. The "menu button" (launcher BFB) position
  dropdown in customise mode is driven by `launcher_bfb_location_supported` — a
  `position_*` array mirroring `launcher_location_supported`: a theme is
  user-toggleable (and shows the dropdown) only when it lists more than one
  position (`Theme.launcherBfbToggleable`), and `launcher_bfb_visible_by_default`
  is its default state. The choice is stored as the named string
  `Preference.LAUNCHER_BFB_LOCATION` (`none`/`start`/`end`, see the `BfbLocation`
  enum), and `Theme.launcherBfbLocationResolved` resolves the live edge (pref →
  theme default), mapping `start`/`end` to the launcher's leading/trailing end
  relative to the theme's native `launcher_bfb_location`; non-toggleable themes
  always use that fixed location. The dropdown only offers the positions a theme
  declares (Pantheon/COSMIC: Hide / "At the start of the launcher"; GNOME: Hide /
  start / "At the end of the launcher"), and switching theme
  (`ThemeCards.applyTheme`) clears the pref so it
  can't carry over. The launcher
  preferences icon was removed from every theme (`launcher_preferences_location`
  is `none`); settings are reached via the panel cog or the dash's customise
  cog, and the dash is always reachable by swiping up — so the old
  `launcher_preferences_location_when_panel_hidden` fallback is now `none` too.
  Removed icons keep their drawables.
- **`widgets/`** — home-screen widget hosting (mostly Kotlin): `WidgetHost`
  (AppWidgetHost), `DesktopLayoutStorage` (the shared `"desktop_layout"` file
  holding widgets, desktop apps and folders), `WidgetPickerDialog`.
  `WidgetsPager` (`R.id.vgWidgets`) is a horizontal pager of widget
  desktops: each page is a full-size `WidgetsContainer`, there is always
  exactly one empty desktop after the last occupied one (capped at 16;
  swiping right past the end lands on the fresh one), `WidgetLayout.page`
  persists which desktop a widget lives on, and pressing home
  (`HomeActivity.onNewIntent` with the HOME intent) animates back to the
  first desktop. "Occupied" counts widgets **and** per-desktop pins: the
  pager's `occupiedDesktopSupplier` (pointed at `home/Desktops`, which
  combines the pager's own `highestWidgetPage` with the pinned desktops) so
  pins keep a desktop alive too, and emits `onPageScroll`
  / `onPageSettled` so the launcher bar can morph/rebuild (see
  `LauncherBarBinder`). Sideways swipes over a widget are intercepted by the pager
  itself; swipes on empty space arrive via `home/HomeGestureController`. A
  dot-row page indicator (drawn in `WidgetsPager.dispatchDraw`, in the
  pager's own un-scrolled space so it stays viewport-fixed) flashes in while
  swiping and fades out after settling.
  The per-page grid maths stay inside `WidgetsContainer` (page insets are
  applied as padding per page, not on the pager), which lays widgets out on
  an invisible 8×8 grid (`WidgetGrid` holds the pure grid maths — snapping,
  span clamping, overlap checks, and the initial span for new/restored
  widgets). Widget sizing reads the provider's hints — `targetCellWidth/Height`
  (API 33), else `minResizeWidth/Height`, else `minWidth/Height` — and clamps
  to `maxResize*`; `clampSpan` uses nearest rounding for both bounds so the
  coarse grid keeps a real resize range instead of collapsing to one span.
  Placement and restore defer until the page has been measured (cell size > 0)
  so a widget can never land at 1×1, and `WidgetHost.restoreWidgets` re-clamps
  saved spans to the current provider limits once measured. New widgets land on
  the desktop currently shown; drops and moves stay within it. Long-pressing a
  widget puts its `WidgetContainer` into
  edit mode: edge handles resize by touch (clamped to the provider's
  `min`/`maxResize*` limits and `resizeMode`, unless the developer-only
  unrestricted widget resizing preference `DEV_WIDGET_RESIZE_ANY` is enabled,
  with a snap-indicator line drawn by `WidgetsContainer`; committed resizes
  inform the provider via `updateAppWidgetSize`), while dragging the body uses
  the system
  drag-and-drop framework (`WidgetsContainer_DragListener`) and shares the
  launcher's drag-to-trash mechanism. The free-moving system drag shadow is
  accompanied by a snapped landing indicator drawn on `WidgetsContainer`;
  HomeActivity attaches the listener to the topmost launcher/dash container
  and the listener translates its drag coordinates into widget-grid space.
- **`widget/bfb/`** — the BFB widgets, the App Widgets DistroHopper *provides*
  (distinct from `widgets/`, which *hosts* third-party widgets). Two exported
  providers share `BfbWidgetProviderBase` and look identical: `BfbWidgetProvider`
  (`@xml/bfb_widget_info`) opens the dash on tap; `BfbSearchWidgetProvider`
  (`@xml/bfb_search_widget_info`) opens the dash *and* focuses search (keyboard
  up). They render the launcher's menu button (BFB) in the active theme so they
  can be placed on DistroHopper's own widget desktops or any third-party home
  screen. `BfbWidgetRenderer` composites a bitmap mirroring
  `desktop/launcher/AppLauncher`'s tile (themed `launcher_applauncher_background`
  shape tinted with the resolved tile colour, gloss overlay, then the BFB image),
  capped to keep the RemoteViews under the Binder limit. The tile colour comes
  from `home/LauncherTileColour` — the shared rule (static themed colour, or the
  chameleonic wallpaper colour) that `WallpaperColourApplier` also uses, so the
  widgets match the running launcher. Providers are stateless/prefs-driven;
  `BfbWidgetProviderBase.requestUpdate(context)` repaints every placed widget of
  either type and is called from `theme/ThemeCards.applyTheme` (theme switch) and
  `WallpaperColourApplier.apply` (chameleonic colour refresh). Tapping starts
  `HomeActivity` with the `openDash=true` extra (plus `focusSearch=true` for the
  search widget) — the existing dash-open contract; `HomeActivity.onResume`
  consumes the extras once so they can't re-open the dash on a later resume, and
  `openDash(forceSearchFocus)` threads the flag into `DashController.open`.
  singleTop means it opens the dash in place when DistroHopper is already
  foreground, or launches it first otherwise. The two tap PendingIntents must use
  distinct request codes: they target the same HomeActivity component and differ
  only in extras, which PendingIntent identity (Intent.filterEquals) ignores.
- Background loading uses Kotlin coroutines: `home/StartupLoader` runs the
  startup sequence (wallpaper init → app list → label/icon caches, strictly
  in that order — both the wallpaper and app paths touch the BFB) in the
  activity's `lifecycleScope` on the `DependencyContainer`'s
  `DispatcherProvider`; `home/AppsLoader` holds the blocking halves. Tests
  swap the IO dispatcher for `Dispatchers.Unconfined` via
  `ActivityTestSupport.installTestDispatchers()` so `drainTasks()` is
  deterministic. `home/SearchLoader` runs dash searches the same way (see the
  lens section above); no `AsyncTask`s remain.
- **`broadcast/`** — `PackageManagerBroadcastReceiver`: reacts to app
  install/uninstall to keep `AppManager` current. Package broadcasts only
  cover the personal profile, so `WorkProfileAppsCallback` (a
  `LauncherApps.Callback` registered alongside the receiver) does the same
  for other profiles.
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
