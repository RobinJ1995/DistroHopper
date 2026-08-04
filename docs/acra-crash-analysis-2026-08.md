# ACRA crash-report analysis — August 2026

Analysis of the Acrarium export of 2026-08-04: **245 unresolved bug groups /
37,172 reports**, spanning versions 2.5.0 (versionCode 94) through 3.0.0f (108).

Two questions: which bugs were actually fixed and where, and whether the 3.x
betas introduced anything genuinely new.

## Method, and what the data can and cannot support

**Exposure is wildly uneven.** Only three releases have a real user base:

| release | code | installs | reports | still reporting |
|---|---|---|---|---|
| 2.6.1 | 96 | 887 | 18,309 | until 2026-07-13 |
| 2.6.3 | 99 | 1,094 | 11,426 | until 2026-07-26 |
| 2.7.0 | 102 | 480 | 12,471 | today |

Everything else is noise-scale: 2.5.0 (2 installs), 2.6.0 (21), 2.6.2 (1+1),
2.6.4 (3), 2.6.5 (15), and the whole 3.0.0a–f beta line (20 installs across
five builds, ~63 reports).

Consequences:

- A bug vanishing across **2.6.1 → 2.6.3** or **2.6.3 → 2.7.0** is real evidence.
- A bug "not appearing in 3.0.0x" is **no evidence at all** — 20 installs cannot
  disprove anything.
- Old releases kept reporting into 2026 alongside newer ones, so a
  disappearance is a *code* difference, not the passage of time.

**Confidence model.** For each bug: estimate affected installs in its
highest-exposure release (`bug.installations × reports_in_release ÷
total_reports`), turn that into a per-install rate, and project it onto the
install base of every later release to get an expected count λ. Observing zero
where λ is large is evidence of a fix (P(0) = e^−λ). Reported only where the
estimate rests on ≥3 affected installs in a ≥400-install release. λ ≥ 8 → *very
high*, ≥ 4 → *high*, ≥ 2 → *moderate*.

Statistics alone are never treated as proof: every "fixed" claim below is
cross-checked against the actual diff where the source exists.

**Two confounders that statistics alone get wrong:**

1. **The Android population, not the code.** 2.6.2 raised `minSdk` 19 → 23
   (`8e93a10`, "drop support for android <6"). Bugs confined to Android ≤ 7
   stopped because those devices were dropped or aged out, not because anything
   was repaired.
2. **Acrarium's fuzzy grouping** (min_score 95) splits one defect across several
   bug ids. Some ids at a crash site go quiet while others at the *same site*
   keep reporting — the site is not fixed. Crash sites are therefore aggregated
   before drawing conclusions.

**A caveat on the repository.** Git history jumps 2023-03-27 → 2026-06-13 with
the 2026 commits as roots. **2.6.4, 2.6.5 and 2.7.0 have no source in this
repo**, so fixes landing there can only be inferred from the report data plus
the current tree. 2.6.1 (`f547fe3`) and 2.6.3 (`9ae071e`) trees are present, and
their line numbers match the stack traces exactly.

**91% of these reports are not crashes.** 33,844 of 37,172 are `is_silent` —
exceptions the app caught and forwarded via `ExceptionHandler.logAndTrack()`
(`ACRA.handleSilentException`). Only 3,328 are genuine uncaught crashes. Several
"top bugs" are really *logging volume*, not user-visible breakage.

---

## 1. Fixed in 2.6.2 (codes 97/98, 2023-03-27)

### 1a. Donations library removed — definitive

`f547fe3` ("Removed donations section") deleted `org.sufficientlysecure.donations`.

| bug | reports | trace |
|---|---|---|
| 440 | 25 | `DonationsFragment.onIabSetupFinished` → `Fragment.requireContext` "not attached to a context" |
| 97 | 20 | same library |

**Confidence: certain.** The code no longer exists.

### 1b. minSdk 19 → 23 — dropped, not fixed

`8e93a10` raised `minSdkVersion` to 23. Bugs reported exclusively from Android
≤ 5 (bug 22, `Wallpaper.init`, 188 reports; bug 2378, `Wallpaper.blur`, 102
reports; parts of the `Wallpaper.blur` cluster) became unreachable.

**Confidence: high that they cannot recur; but this is a support-window change,
not a repair.** Several were also independently fixed by 2.6.3 (§2).

---

## 2. Fixed in 2.6.3 (code 99, 2023-03-27) — commit `692befe`

`692befe` ("Workarounds and fallbacks for wallpaper-based dominant colour
calculations, better error handling around common errors") is the single most
consequential commit in the dataset. Every claim here is verified against the
2.6.1 source: the crash-site line numbers match the pre-fix code exactly.

| crash site | bugs | reports | reports after 2.6.1 | fix |
|---|---|---|---|---|
| `HomeActivity.java:598` `onStop` | 6 | **12,545** | **0** | `widgetHost.stopListening()` moved into its own `try`/`catch` that swallows |
| `App.java:84` `launch` | 23 | **1,154** | **0** | `startActivity()` wrapped in `try`/`catch` + toast |
| `Image.java:66` `getAverageColour` | 4 | 194 | 0 | `instanceof BitmapDrawable` guard before the cast |
| `Debug.java:37` `assertCondition` | 2 | 146 | 0 | indirect: `Wallpaper.getAverageColour` null-checks `img` before constructing `Image` |
| `Wallpaper.java:123` `blur` | 5 | 132 | 0 (after 2.6.2) | early return when `img`/`blurred` are both null, so the null `mode` is never read |

Verification of the three largest:

- **`HomeActivity.java:598`** in 2.6.1 is literally `this.widgetHost.stopListening ();`.
  Bug 1232 alone is 11,839 reports from 153 installs — an `AppWidgetServiceImpl`
  NPE thrown by the system server, on modern Android (11–14), that 2.6.1 caught
  and rendered as `exh.show(this)` — an error **dialog on every `onStop`**.
  λ ≈ 278 expected in later releases; observed 0.
  *Nuance: 2.6.3 silences it (`// ¯\\_(ツ)_/¯`) rather than avoiding it. The
  system-server failure presumably still happens; the user-facing dialog and the
  report are what went away. As a defect that is genuinely fixed — as a root
  cause it is suppressed.*
- **`App.java:84`** is `this.context.startActivity (intent);`. 23 bug groups,
  overwhelmingly `ActivityNotFoundException` (launching an app that has since
  been uninstalled). λ ≈ 395 for bug 841 alone; observed 0.
- **`Image.java:66`** is the unguarded `(BitmapDrawable) this.getDrawable()`
  cast. λ ≈ 67 and 31 for bugs 44 and 50; observed 0.

**Confidence: very high (code-verified + statistically decisive).**

### Not actually fixed here — population effects

These went quiet across the same boundary but are OOM/framework failures
confined to Android ≤ 7, i.e. the device population, not the code: bug 3206
(`App.getIcon`, 58), bug 9 (`DrawableCache.get`, 100), bug 105 (`ViewGroup.dispatchDragEvent`
CME — an AOSP bug fixed in Android 7), bug 65 (`DevLogsActivity.nudge`), bug 73,
bug 6165, bug 6168, bug 2456. **Do not count these as fixes.**

---

## 3. A regression introduced by that same fix — and it is still shipping

`692befe` added a fallback path to `Wallpaper.getAverageColour`:

```java
final Color primaryColour = wpman.getWallpaperColors(WallpaperManager.FLAG_SYSTEM).getPrimaryColor();
```

`getWallpaperColors()` returns null (e.g. shortly after boot). There is no null
check. It is caught by the surrounding `catch (Exception)` and reported silently.

| bug | version | reports | installs | share of that release's installs |
|---|---|---|---|---|
| 574479 | 2.6.3 only | **5,357** | 501 | **46%** |
| 576963 | 2.6.5 → 2.7.0 | **5,328** | 173 | 36% |

Bug 576963's stack is `Wallpaper.kt:172 getAverageColour` ←
`HomeActivity.asyncInitWallpaperDone` — the identical call path after the Kotlin
port. **The same missing null check survived the Java → Kotlin rewrite.**
(2.7.0's source is not in the repo, so this identification is from the call path
and symptom, not the diff — but there is no plausible alternative.)

It is **still live on 2.7.0 today** and is the single largest ongoing source of
reports. The current 3.x tree does fix it, in `desktop/Wallpaper.kt`:

```kotlin
val primaryColour = WallpaperManager.getInstance(this.context)
    .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor
if (primaryColour != null) { ... }
```

**Confidence: very high.** Between them these two bug groups are 10,685 reports —
29% of the entire dataset — from one missing null check.

---

## 4. Fixed between 2.6.3 and 2.7.0 (source not in the repo)

Statistical evidence only, except where the current tree confirms the fix.

| crash site | bugs | reports | λ | confidence | note |
|---|---|---|---|---|---|
| `HomeActivity.java:1153` `asyncInitWallpaperDone` — `Resources$NotFoundException` | 6 | 435 | 21 | **very high** | |
| `HomeActivity.java:780` `applyTheme` — `Resources$NotFoundException` | 7 | 435 | 20 | **very high** | Cinnamon theme drawables |
| `Wallpaper.java:117/61` `init` — `DeadSystemException` / `getDrawable()` | 10 | 754 | 20 | **very high** | 3.x `init()` no longer reads the wallpaper bitmap at all |
| `Image.java:284` `toBitmap` — "width and height must be > 0" | 2 | 819 | 15 | **very high** | current tree has the explicit `width <= 0 \|\| height <= 0` guard |
| `WidgetHost.java:123` `selectWidget` | 2 | 29 | 3 | moderate | |
| `DrawableCache.java:75` `get` | 2 | 101 | 21 | *rejected* | Android ≤ 7 OOM — population |

The `Image.toBitmap` guard is present in today's `Image.java:303`, so that one is
confirmed by code even though the releasing commit is missing.

**Beware of grouping artefacts.** Bug 574192 (`Image.java:42
adaptiveIconToDrawable`, 88 reports, λ ≈ 7) reads as fixed, but bugs 574570 and
574344 at the *same site* are still reporting on 2.7.0, and the current
`adaptiveIconToDrawable` still calls `Bitmap.createBitmap(getIntrinsicWidth(),
getIntrinsicHeight(), …)` with no size guard. **Not fixed** — see §6.

---

## 5. Still open on 2.7.0 — 13,676 reports (37% of the dataset)

Status against the current 3.x tree:

### Already fixed in 3.x code, unreleased

| bugs | site | reports | fix in current tree |
|---|---|---|---|
| 576963 | `Wallpaper.kt:172` `getAverageColour` NPE | 5,328 | `?.primaryColor` + null check |
| 576809, 576822, 576802, 576835, 576836 | `Wallpaper.kt:67` `init` — `SecurityException: Op READ_MEDIA_IMAGES ignore` | 3,060 | `init()` no longer reads the wallpaper bitmap; colours come from `WallpaperColors`, which needs no permission |
| 576823 | `DrawableCache.java:154` `keySet` — `ConcurrentModificationException` | 237 | `keys` is now an immutable `Set.copyOf(...)` reassigned on mutation, and all accessors are `synchronized`, so `clear()` iterates a snapshot |

### Not fixed — live in 2.7.0 *and* in the current 3.x tree

| bugs | site | reports | what it is |
|---|---|---|---|
| 576880, 576844, 576866 | `IconPackHelper.java:84` `loadIconPack` | **2,978** | `getResourcesForApplication()` throws `NameNotFoundException` when the selected icon pack has been uninstalled. Both call sites (`AppsLoader.kt:49`, `HomeActivity.java:989`) catch it and `logAndTrack()` — so it is re-reported on **every launch**, forever, from 94 installs. Pure self-inflicted report noise; the fix is to clear the stale preference (and not report). |
| 5953 | `DrawableCache.java:92` `put` — `ENOSPC` | 773 | User's device is full. Not a defect; should not be reported. |
| 5931, 472, 574968, 575136, +34 more | `WidgetHost.java:103` `configureWidget` — `SecurityException` | 660 | Third-party widget config activities that refuse an external caller. Environmental, but worth swallowing quietly. |
| 574570, 574344, 577168 | `Image.java:42/46` `adaptiveIconToDrawable` — "width and height must be > 0" | 133 | **Genuine, unfixed.** `Bitmap.createBitmap(adaptive.getIntrinsicWidth(), adaptive.getIntrinsicHeight(), …)` has no size guard, unlike its sibling `toBitmap()` which does. Cheapest real fix in the whole list. |
| 104, 574658 (+ 577673 in 3.x) | `DevLogsActivity.java:56` `nudge` — `CalledFromWrongThreadException` | 86 | **Genuine, unfixed, present since 2.6.0.** See §6.3. |
| 596 | `AppManager.java:328` `refreshPinnedView` NPE | 76 | |
| 575684, 575725 | `HomeActivity.java:524` `onActivityResult` — `IndexOutOfBoundsException` | 60 | |
| 606, 5740 | `ActivityNotFoundException: ACTION_SET_WALLPAPER` | 72 | Device has no wallpaper picker. Environmental. |
| 577540, 577541 | `IconPackHelper.java:132` `parseAppFilter` — `XmlPullParserException` | 16 | Malformed third-party `appfilter.xml`. |

### Not a bug

**577329** — `NoSuchMethodError: No static method isEnabled()Z in
android.os.Trace`, thrown from `androidx.startup` during app init. **Android 9**
only, 1 install, 7 reports. `Trace.isEnabled` is API 29. Same category as §6.1:
the APK was forced onto an OS below its `minSdk`.

---

## 6. The 3.x betas: what is actually new

Only **five** bug groups touch 3.0.0a–f at all — 63 reports across 20 installs.
3.0.0e and 3.0.0g produced no reports at all (3.0.0g is two days old).

### 6.1. Not a bug — forced install below minSdk (58 of the 63 reports)

**577659** — `NoSuchMethodError: No interface method
addCrossWindowBlurEnabledListener(Ljava/util/function/Consumer;)V in class
Landroid/view/WindowManager;` at `OnboardingActivity.kt:91`.

- **Android 11 only. One device: OnePlus8Pro.** 58 reports, 16 "installs" (one
  person reinstalling).
- `addCrossWindowBlurEnabledListener` is **API 31**. The app's `minSdk` **is 31**.
  Android 11 is API 30.
- The call is correct for a minSdk-31 app. The APK was sideloaded onto an OS the
  project dropped.

**Your read is right, and this is 92% of all 3.x report volume.** No action.

### 6.2. Genuine, introduced by 3.x code, already fixed in the tree

Both were hit on the maintainer's own emulator on 3.0.0c and fixed within the
hour — they never reached 3.0.0d.

- **577672** — `IllegalStateException: Page(s) contain a ViewGroup with a
  LayoutTransition … interferes with the scrolling animation`, from ViewPager2's
  `ScrollEventAdapter` mid-scroll. Crash 2026-06-15 23:11:52Z; fixed by
  `eba7fa2` at 23:45:38Z, which sets `setAnimateParentHierarchy(false)` on the
  per-page grid transition in `ProfilePagerAdapter`. **Fixed in 3.0.0d.**
- **577671** — `IllegalStateException: The specified child already has a parent`
  at `DesktopFolderHost.placeWidget` (via `removeMember`, dragging a member out
  of a desktop folder). Crash 2026-06-15 22:21:27Z; `ca16dfe` ("Drop
  widgets-from-folders; desktop folders hold apps only") removed `placeWidget`
  entirely at 23:03Z. **Fixed in 3.0.0d** — the crashing path no longer exists.

Confidence that both are fixed: **high by code**, but note that with 20 total
beta installs the field data could not have shown a regression either way.

### 6.3. Genuine and still open

- **577666** — `IllegalArgumentException: width and height must be > 0` at
  `Profiles.profileGlyph` ← `UnityRibbonIndicator.glyphFor` ←
  `LauncherBarBinder.rebindDashApps`. 3.0.0b, one Android-17 emulator, one
  report. New code (work/private-profile support). The pre-fix `Profiles.kt` is
  not in `master`'s history so the exact failing line cannot be diffed. Today's
  `profileGlyph` still does `Bitmap.createBitmap(sizePx, sizePx, …)` unguarded,
  though its only real caller passes `(36 × density)`, which is always positive —
  so it is probably no longer reachable, but that is inference, not proof.
  **Low severity; worth a `sizePx <= 0` guard.**

- **577673** — `CalledFromWrongThreadException` at `DevLogsActivity.nudge`
  (`DevLogsActivity.java:58`), called off a `DefaultDispatcher-worker` thread via
  `Log.appendToDevLog` ← `AppsLoader.loadIcons` ← `StartupLoader`. 3.0.0c and
  3.0.0d, Android 16, Samsung SM-S928B.
  **This is not new.** It is the same defect as bugs 104 and 574658 (`nudge` at
  line 56), running since 2.6.0 and still live on 2.7.0 — 95 reports across 6 bug
  groups. `nudge()` calls `tvLogs.setText(...)` on whatever thread logged. The
  Kotlin/coroutines migration changed which background thread reaches it, not the
  bug. **Fix: marshal `nudge()` onto the main thread** (`runOnUiThread` /
  `tvLogs.post`). Only affects users with the dev-logs screen open.

### Summary

**No new genuine bug in 3.x has meaningful field impact.** Three real defects
were introduced by 3.x code (577671, 577672, 577666); two are already fixed in
the tree, the third is a one-report emulator edge case. The one high-volume 3.x
"bug" is a forced install below minSdk, as you suspected. The one 3.x defect that
matters is `DevLogsActivity.nudge` — and it is six years old, not a 3.x
regression.

---

## 7. Recommended actions, by value

1. **Ship the 3.x `Wallpaper` fixes.** ~8,400 of 2.7.0's reports (68% of its
   volume) are the null `WallpaperColors` and the `READ_MEDIA_IMAGES`
   `SecurityException`, both already fixed in the tree.
2. **Stop re-reporting a missing icon pack** (`IconPackHelper.loadIconPack`,
   2,978 reports / 94 installs). Clear the stale `ICON_PACK` preference on
   `NameNotFoundException` and log without tracking.
3. **Guard `Image.adaptiveIconToDrawable`** against zero-size drawables, the way
   `toBitmap()` already is. Genuine, unfixed, ~3 lines.
4. **Marshal `DevLogsActivity.nudge()` onto the main thread.** Genuine, unfixed
   since 2.6.0, and it is the only real 3.x report.
5. **Stop tracking environmental exceptions** — `ENOSPC` on cache writes, widget
   `configureWidget` `SecurityException`, `ACTION_SET_WALLPAPER`
   `ActivityNotFoundException`. Together ~1,500 reports that are not defects.
6. **Consider marking solved in Acrarium** everything in §1, §2 and §4 — roughly
   23,500 reports across ~150 bug groups that cannot recur on a supported build.

## Limitations

- 2.6.4 / 2.6.5 / 2.7.0 source is absent from this repository; §4 and the
  2.7.0-era identifications rest on report data plus the current tree.
- 3 bugs marked solved in Acrarium are excluded from the export; if any were 3.x
  issues already triaged, they are outside this analysis.
- The export covers 37,172 of the 42,426 reports in the database.
- Some reports carry a bogus device clock (dates like 1970-01-01), so `first_seen`
  can predate the release. Version codes, not dates, are used for ordering
  throughout.
