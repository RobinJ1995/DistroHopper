# ACRA crash-report analysis — August 2026

Analysis of the Acrarium export of 2026-08-04: **245 unresolved bug groups /
37,172 reports**, spanning versions 2.5.0 (versionCode 94) through 3.0.0f (108).

Two questions: which bugs were actually fixed and where, and whether the 3.x
betas introduced anything genuinely new.

Every release from 2.5.0 to 3.0.0g is in the repository's history, so each claim
below is anchored to a commit and verified against the diff. Crash-site line
numbers were checked against the source of the release each trace came from; they
match exactly throughout.

## Method

**Exposure is wildly uneven.** Only three releases have a real user base:

| release | code | installs | reports | still reporting |
|---|---|---|---|---|
| 2.6.1 | 96 | 887 | 18,309 | until 2026-07-13 |
| 2.6.3 | 99 | 1,094 | 11,426 | until 2026-07-26 |
| 2.7.0 | 102 | 480 | 12,471 | today |

Everything else is noise-scale: 2.5.0 (2 installs), 2.6.0 (21), 2.6.2 (1+1),
2.6.4 (3), 2.6.5 (15), and the whole 3.0.0a–f beta line (20 installs across five
builds, 63 reports).

So the field data resolves exactly three transitions:

- **2.6.1 → 2.6.3** (the 2023-03-27 wave)
- **2.6.3 → 2.7.0** (the 2025-10-14/18 wave: 2.6.4, 2.6.5 and 2.7.0 shipped within
  four days, with 3 and 15 installs on the first two — the field cannot tell them
  apart, though the commits can)
- **2.7.0 → 3.0.0x**, which carries **no statistical weight at all**: 20 installs
  cannot disprove anything.

Old releases kept reporting into 2026 alongside newer ones, so a disappearance is
a *code* difference, not the passage of time.

**Confidence model.** For each bug: estimate affected installs in its
highest-exposure release (`bug.installations × reports_in_release ÷
total_reports`), turn that into a per-install rate, project it onto the install
base of every later release to get an expected count λ. Observing zero where λ is
large is evidence (P(0) = e^−λ). Used only where the estimate rests on ≥3 affected
installs in a ≥400-install release. λ ≥ 8 → *very high*, ≥ 4 → *high*, ≥ 2 →
*moderate*. Statistics are never treated as proof — they only rank what to go and
read the diff for.

**Two confounders that look identical in the data:**

1. **The Android population, not the code.** `minSdk` went 19 → 23 in 2.6.2
   (`8e93a10`) and **23 → 29 (Android 10)** in 2.6.4 (`78264919`). Bugs confined to
   older Android became unreachable without anything being repaired. The second
   bump is large: it accounts for **25 of the 82 bugs** that vanished between 2.6.3
   and 2.7.0.
2. **Acrarium's fuzzy grouping** (min_score 95) splits one defect across several
   bug ids. Some ids at a crash site go quiet while others at the *same site* keep
   reporting — the site is not fixed. Crash sites are aggregated before concluding.

**91% of these reports are not crashes.** 33,844 of 37,172 are `is_silent` —
exceptions the app caught and forwarded via `ExceptionHandler.logAndTrack()`.
Only 3,328 are genuine uncaught crashes. Several "top bugs" are *logging volume*,
not user-visible breakage — and, as §3 and §6 show, the project's recurring
failure mode is converting a crash into unbounded silent reporting and calling it
fixed.

---

## 1. Fixed in 2.6.2 (codes 97/98, 2023-03-27)

**`f547fe3` — donations library removed.** Deleted `org.sufficientlysecure.donations`
outright. Bug 440 (25 reports, `DonationsFragment.onIabSetupFinished` →
`Fragment.requireContext` "not attached to a context") and bug 97 (20 reports).
**Certain** — the code no longer exists.

**`8e93a10` — `minSdkVersion` 19 → 23.** Bugs reported only from Android ≤ 5
became unreachable: bug 22 (`Wallpaper.init`, 188 reports), bug 2378
(`Wallpaper.blur`, 102), part of the `Wallpaper.blur` cluster. **Dropped, not
fixed** — though most were independently fixed by 2.6.3 as well.

---

## 2. Fixed in 2.6.3 (code 99, 2023-03-27) — commit `692befe`

`692befe` ("Workarounds and fallbacks for wallpaper-based dominant colour
calculations, better error handling around common errors") is the single most
consequential commit in the dataset.

| crash site | bugs | reports | after 2.6.1 | fix |
|---|---|---|---|---|
| `HomeActivity.java:598` `onStop` | 6 | **12,545** | **0** | `widgetHost.stopListening()` moved into its own `try`/`catch` that swallows |
| `App.java:84` `launch` | 23 | **1,154** | **0** | `startActivity()` wrapped in `try`/`catch` + toast |
| `Image.java:66` `getAverageColour` | 4 | 194 | 0 | `instanceof BitmapDrawable` guard before the cast |
| `Debug.java:37` `assertCondition` | 2 | 146 | 0 | indirect: `Wallpaper.getAverageColour` null-checks `img` before constructing `Image` |
| `Wallpaper.java:123` `blur` | 5 | 132 | 0 (after 2.6.2) | early return when `img`/`blurred` are both null, so the null `mode` is never read |

The three largest, verified against the 2.6.1 tree (`f547fe3`):

- **`HomeActivity.java:598`** is literally `this.widgetHost.stopListening ();`.
  Bug 1232 alone is 11,839 reports from 153 installs — an `AppWidgetServiceImpl`
  NPE thrown by the system server, on modern Android (11–14), that 2.6.1 rendered
  as `exh.show(this)`: an error **dialog on every `onStop`**. λ ≈ 278; observed 0.
  *2.6.3 silences it (`// ¯\\_(ツ)_/¯`) rather than avoiding it. The system-server
  failure presumably still happens; the dialog and the report are what went away.*
- **`App.java:84`** is `this.context.startActivity (intent);` — 23 bug groups,
  overwhelmingly `ActivityNotFoundException` from launching an app that was since
  uninstalled. λ ≈ 395 for bug 841 alone; observed 0.
- **`Image.java:66`** is the unguarded `(BitmapDrawable) this.getDrawable()` cast.
  λ ≈ 67 and 31 for bugs 44 and 50; observed 0.

**Confidence: very high** — code-verified and statistically decisive.

**Not fixed here, despite going quiet:** bug 3206 (`App.getIcon`, 58), bug 9
(`DrawableCache.get`, 100), bug 105 (`ViewGroup.dispatchDragEvent` CME — an AOSP
bug fixed in Android 7), bugs 65, 73, 6165, 6168, 2456. All OOM/framework
failures confined to Android ≤ 7.

---

## 3. The regression 2.6.3 introduced — and how it survived two rewrites

`692befe` added a fallback to `Wallpaper.getAverageColour`:

```java
final Color primaryColour = wpman.getWallpaperColors(WallpaperManager.FLAG_SYSTEM).getPrimaryColor();
```

`getWallpaperColors()` returns null (e.g. shortly after boot). No null check. The
surrounding `catch (Exception)` turns it into a silent report on every launch.

`f52afa5d` (2.6.4) ported the file to Kotlin and translated that line to:

```kotlin
val primaryColour = wpman.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)!!.primaryColor
```

**A mechanical `!!`** — the null dereference preserved exactly, now as a bare
Kotlin NPE. That is bug 576963's stack: `Wallpaper.kt:172`, no message.

| bug | releases | reports | installs | share of that release's installs |
|---|---|---|---|---|
| 574479 | 2.6.3 | **5,357** | 501 | **46%** |
| 576963 | 2.6.5 → 2.7.0 | **5,328** | 173 | 36% |

**10,685 reports — 29% of the entire dataset — from one missing null check, live
from 2023-03 to today.**

Fixed by `1c66c082` (2026-06-12, "Polish the wizard and modernise preferences,
lenses, and theming"), which introduced `?.primaryColor` plus an explicit null
branch, hardened further by `91525782` (2026-06-13, "trust samsung to still not
implement android APIs from 5 years ago"). **Shipped in 3.0.0a / 3.0.0b — never in
a stable release.**

---

## 4. The 2025-10 wave: 2.6.4, 2.6.5, 2.7.0 (codes 100–102)

Eleven commits, 2025-10-14 to 2025-10-18. Attributions below are per commit; the
report data can only resolve the wave as a whole.

### 2.6.4 — `acdb54b6`, `d938beb6`, `35bb4f64`, `f52afa5d`, `78264919`, `6014da92`, `0ff1d69f`

- **`78264919` — `minSdkVersion` 23 → 29 (Android 10).** The largest single effect
  in this wave: **25 bug groups / 521 reports** simply became unreachable, among
  them bug 575782 (212), bug 9 (100), bug 3805 (41), bug 73 (39), bug 616 (26),
  bug 105 (20), and the Android ≤ 7 half of the `applyTheme` /
  `asyncInitWallpaperDone` cluster. **Not fixes.** Marking these solved is
  correct; treating them as evidence the code improved is not.
- **`6014da92` — `AppManager.apps`/`pinned` → `CopyOnWriteArrayList`.** Addresses
  the `ConcurrentModificationException` family around app-list iteration.
- **`f52afa5d` + `35bb4f64` — `Wallpaper.java` → `Wallpaper.kt`.** Mechanical
  port; it carried the §3 defect forward.
- `d938beb6` (targetSdk 36), `acdb54b6` (Gradle/AGP, dropping the pinned
  `buildToolsVersion '28.0.3'`), `0ff1d69f` (Java 8 → 17).

### 2.6.5 — `d881912f`, `26db9dee`

- **`26db9dee` ("Speculative fix for #31") — `Image.toBitmap` size guard:**

  ```java
  if (width <= 0 || height <= 0) { … return null; }
  ```

  **Fixes bug 1561** (816 reports, 35 installs, `Image.java:284`, "width and height
  must be > 0", λ ≈ 15, Android 10–14 so not a population effect).
  **Confidence: certain** — direct code fix, correct site, decisive statistics.
  The same commit adds the matching null check in `DrawableCache.put`.

- **`26db9dee` also flips `Log.enabled` from `false` to `true`** — dev logging is
  on by default from 2.6.5 onward. Worth knowing when reading `Log`/`DevLogs`
  report volume from 2.6.5 on.

- **`d881912f` ("fix crash on launch") — `catch (OutOfMemoryError)` →
  `catch (Exception)` in `Wallpaper.init`.** This ends the `Wallpaper.init` crash
  family (bugs 335, 838, 6239, 575140 — 527 reports) but **does not fix anything**:
  it converts the crash into `logAndTrack()`. The failure is now bug 576809 and
  friends — `Wallpaper.kt:67` (`this.img = wpman.getDrawable()`), 3,060 silent
  reports on 2.7.0. See §6.

- **A "fix" that relocated its bug.** The same commit changed
  `DrawableCache.clear()` to iterate `Set.copyOf(this.keySet())`. But `keySet()`
  itself does:

  ```java
  final Set<String> keys = this.prefs.getStringSet("keys", new HashSet<String>());
  for (final String key : keys) { if (!this.containsKey(key)) this.remove(key); }
  ```

  — iterating the live `SharedPreferences` set while `remove()` mutates it. The
  `ConcurrentModificationException` moved one frame deeper: bug **576818**
  (`DrawableCache.java:140`, `clear`, 2.6.5) became bug **576823**
  (`DrawableCache.java:154`, `keySet`, 237 reports on 2.7.0). Properly fixed only
  by `49e1a660` ("Fix fd leak, key collisions, and SharedPreferences set misuse in
  DrawableCache", #43) — `keys` is now an immutable `Set.copyOf(...)` reassigned on
  mutation, so iteration is always over a snapshot. **Shipped in 3.0.0a.**

### 2.7.0 — `46d4e4f4`

Only one commit: "hacked together icon pack support (finally, after 11 years)".
It is also this wave's regression — see §6.

### Unexplained by code

Bugs **574313** and **574625** (`HomeActivity.applyTheme` /
`asyncInitWallpaperDone`, `Resources$NotFoundException`, 774 reports, λ ≈ 20,
Android 10–14) stop dead at 2.6.3. `HomeActivity.java`'s *only* change across this
wave is the icon-pack block, and the referenced drawables are unchanged (6
`theme_cinnamon_*_search_background` resources in both trees). **No code fix
explains these.** The most plausible mechanism is the build side — 2.6.3 was built
with `buildToolsVersion '28.0.3'` against compileSdk 33, which `acdb54b6` dropped —
but that is a hypothesis, not a finding. Flagged as unresolved rather than claimed
as fixed.

---

## 5. Grouping artefacts — sites that look fixed but are not

| bug | reports | why it is not fixed |
|---|---|---|
| 574192 | 88 | `Image.java:42 adaptiveIconToDrawable` — bugs 574570 and 574344 at the same site still report on 2.7.0, and the current tree still calls `Bitmap.createBitmap(getIntrinsicWidth(), getIntrinsicHeight(), …)` with no size guard |
| 575066 | 103 | `HomeActivity.java:524 onActivityResult` — bugs 575684 and 575725 still report on 2.7.0 |
| 3805, 576484 | 48 | `WidgetHost.java:103 configureWidget` — bugs 5931, 472 and ~34 others still report on 2.7.0 |

`Image.adaptiveIconToDrawable` is the notable one: `26db9dee` added exactly this
guard to its sibling `toBitmap()` in the same file and left
`adaptiveIconToDrawable` alone. Still unfixed today.

---

## 6. The regression 2.7.0 introduced

`46d4e4f4` added icon-pack support, and with it, in `HomeActivity` (and later
`AppsLoader.kt:49`):

```java
try {
    final String iconPack = prefs.getString(Preference.ICON_PACK.getName(), "");
    if (!iconPack.isEmpty()) installedApps.loadIconPack(iconPack);
} catch (Exception ex) { new ExceptionHandler(ex).logAndTrack(); }
```

`IconPackHelper.loadIconPack` calls `pm.getResourcesForApplication(packageName)`,
which throws `NameNotFoundException` once the selected pack is uninstalled. The
preference is never cleared, so **every launch, forever, reports it**.

Bugs 576880, 576844, 576866: **2,978 reports from 94 installs**, 2.7.0 only, still
arriving today. Same shape as §3 and §4's `Wallpaper.init` change — a `catch`
that converts a real failure into unbounded silent reporting. The icon-pack code
reached the 3.x line separately (`4625ef3c`), **so 3.x has this too**.

---

## 7. Still open on 2.7.0 — 13,676 reports (37% of the dataset)

### Fixed in the 3.x line, never released to stable

| bugs | site | reports | fix | in |
|---|---|---|---|---|
| 576963 | `Wallpaper.kt:172` `getAverageColour` NPE | 5,328 | `1c66c082`, `91525782` — `?.primaryColor` + null branch | 3.0.0a/b |
| 576809, 576822, 576802, 576835, 576836 | `Wallpaper.kt:67` `init` — `SecurityException: Op READ_MEDIA_IMAGES ignore` | 3,060 | `1c66c082` — `init()` no longer reads the wallpaper bitmap; colours come from `WallpaperColors`, which needs no permission | 3.0.0a |
| 576823 | `DrawableCache.java:154` `keySet` — `ConcurrentModificationException` | 237 | `49e1a660` (#43) — immutable key set | 3.0.0a |

**8,625 reports — 69% of 2.7.0's volume — are already fixed and sitting unreleased.**

### Not fixed, in 2.7.0 *or* in the current tree

| bugs | site | reports | what it is |
|---|---|---|---|
| 576880, 576844, 576866 | `IconPackHelper.java:84` `loadIconPack` | **2,978** | §6. Fix: clear the stale `ICON_PACK` preference on `NameNotFoundException`; log without tracking |
| 5953 | `DrawableCache.java:92` `put` — `ENOSPC` | 773 | User's device is full. Not a defect; should not be reported |
| 5931, 472, 574968, 575136, +34 | `WidgetHost.java:103` `configureWidget` — `SecurityException` | 660 | Third-party widget config activities refusing an external caller. Environmental |
| 574570, 574344, 577168 | `Image.java:42/46` `adaptiveIconToDrawable` | 133 | **Genuine, unfixed.** No size guard, unlike `toBitmap()` since 2.6.5. Cheapest real fix in the list |
| 104, 574658 (+ 577673 in 3.x) | `DevLogsActivity.java:56` `nudge` | 86 | **Genuine, unfixed, since 2.6.0.** See §8.3 |
| 596 | `AppManager.java:328` `refreshPinnedView` NPE | 76 | |
| 575684, 575725 | `HomeActivity.java:524` `onActivityResult` | 60 | |
| 606, 5740 | `ActivityNotFoundException: ACTION_SET_WALLPAPER` | 72 | Device has no wallpaper picker. Environmental |
| 577540, 577541 | `IconPackHelper.java:132` `parseAppFilter` | 16 | Malformed third-party `appfilter.xml` |

### Not a bug

**577329** — `NoSuchMethodError: No static method isEnabled()Z in android.os.Trace`
from `androidx.startup`. **Android 9**, 1 install, 7 reports. `Trace.isEnabled` is
API 29 and 2.7.0's `minSdk` **is** 29. Forced install below minSdk, same category
as §8.1.

---

## 8. The 3.x betas: what is actually new

Five bug groups touch 3.0.0a–f — 63 reports across 20 installs. 3.0.0e and 3.0.0g
produced none (3.0.0g is two days old).

### 8.1. Not a bug — forced install below minSdk (58 of 63 reports)

**577659** — `NoSuchMethodError: No interface method
addCrossWindowBlurEnabledListener(Ljava/util/function/Consumer;)V in class
Landroid/view/WindowManager;` at `OnboardingActivity.kt:91`.

**Android 11 only. One device: OnePlus8Pro.** 58 reports, 16 "installs" (one
person reinstalling). `addCrossWindowBlurEnabledListener` is **API 31**; the app's
`minSdk` **is 31**; Android 11 is API 30. The call is correct for a minSdk-31 app.

**Your read is right, and this is 92% of all 3.x report volume.** No action.

### 8.2. Genuine, introduced by 3.x, already fixed

Both hit on 3.0.0c and fixed within the hour; neither reached 3.0.0d.

- **577672** — `IllegalStateException: Page(s) contain a ViewGroup with a
  LayoutTransition … interferes with the scrolling animation`, from ViewPager2's
  `ScrollEventAdapter`. Crash 2026-06-15 23:11:52Z → `eba7fa2` at 23:45:38Z sets
  `setAnimateParentHierarchy(false)` on the per-page grid transition in
  `ProfilePagerAdapter`. **Fixed in 3.0.0d.**
- **577671** — `IllegalStateException: The specified child already has a parent` at
  `DesktopFolderHost.placeWidget` (via `removeMember`, dragging a member out of a
  desktop folder). Crash 22:21:27Z → `ca16dfe` ("Drop widgets-from-folders")
  removed `placeWidget` entirely at 23:03Z. **Fixed in 3.0.0d** — the path is gone.

Code-confirmed. Note that with 20 beta installs the field data could not have
shown a regression either way.

### 8.3. Genuine and still open

- **577666** — `IllegalArgumentException: width and height must be > 0` at
  `Profiles.profileGlyph` ← `UnityRibbonIndicator.glyphFor` ←
  `LauncherBarBinder.rebindDashApps`. 3.0.0b, one Android-17 emulator, one report.
  New work/private-profile code. Today's `profileGlyph` still does
  `Bitmap.createBitmap(sizePx, sizePx, …)` unguarded, though its only real caller
  passes `(36 × density)`, always positive — probably unreachable now, but that is
  inference. **Low severity; a `sizePx <= 0` guard would settle it.**

- **577673** — `CalledFromWrongThreadException` at `DevLogsActivity.nudge`, called
  off a `DefaultDispatcher-worker` thread via `Log.appendToDevLog` ←
  `AppsLoader.loadIcons` ← `StartupLoader`. 3.0.0c/3.0.0d, Android 16, SM-S928B.
  **Not new** — the same defect as bugs 104 and 574658 (`nudge` at line 56),
  running since 2.6.0 and still live on 2.7.0: 95 reports across 6 bug groups.
  `nudge()` calls `tvLogs.setText(...)` on whatever thread logged; coroutines
  changed which background thread reaches it, not the bug. **Fix: marshal
  `nudge()` onto the main thread** (`runOnUiThread` / `tvLogs.post`). Only affects
  users with the dev-logs screen open — but `Log.enabled` has defaulted to `true`
  since 2.6.5.

### Summary

**No new genuine bug in 3.x has meaningful field impact.** Three real defects were
introduced by 3.x code (577671, 577672, 577666); two are already fixed, the third
is a one-report emulator edge case. The one high-volume 3.x "bug" is a forced
install below minSdk, as you suspected. The one 3.x defect that matters is
`DevLogsActivity.nudge` — six years old, not a 3.x regression.

---

## 9. Recommended actions, by value

1. **Ship the 3.x `Wallpaper` and `DrawableCache` fixes.** 8,625 reports — 69% of
   2.7.0's volume — are already fixed in 3.0.0a/3.0.0b and have never reached a
   stable release.
2. **Stop re-reporting a missing icon pack** (2,978 reports / 94 installs, §6).
   Clear the stale preference on `NameNotFoundException`; log without tracking.
   Present in 3.x too.
3. **Guard `Image.adaptiveIconToDrawable`** the way `toBitmap()` has been guarded
   since 2.6.5. Genuine, unfixed, ~3 lines.
4. **Marshal `DevLogsActivity.nudge()` onto the main thread.** Genuine, unfixed
   since 2.6.0, and the only real 3.x report.
5. **Stop tracking environmental exceptions** — cache-write `ENOSPC`,
   `configureWidget` `SecurityException`, `ACTION_SET_WALLPAPER`
   `ActivityNotFoundException`: ~1,500 reports that are not defects.
6. **Watch the recurring anti-pattern.** Three times now — `692befe` (2.6.3),
   `d881912f` (2.6.5), `46d4e4f4` (2.7.0) — a crash was wrapped in a `catch` that
   calls `logAndTrack()` on a condition that recurs every launch. Each time the
   crash stopped and the report volume exploded. A `catch` around a *persistent*
   condition needs to either fix the state or report once, not per launch.
7. **Mark solved in Acrarium**: everything in §1, §2 and §4 — roughly 24,000
   reports across ~150 bug groups that cannot recur on a supported build.

## Limitations

- 2.6.4 (3 installs) and 2.6.5 (15) are too small to separate in the field; their
  commits are attributed individually, but the *evidence* covers the 2025-10 wave
  as a whole.
- Bugs 574313/574625 (774 reports) have no code explanation — see §4.
- 3 bugs marked solved in Acrarium are excluded from the export; if any were 3.x
  issues already triaged, they are outside this analysis.
- The export covers 37,172 of the 42,426 reports in the database.
- Some reports carry a bogus device clock (dates like 1970-01-01), so `first_seen`
  can predate the release. Version codes, not dates, are used for ordering.
