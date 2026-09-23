# Changelog

User-facing changes in each DistroHopper release, newest first. Test builds (versions ending in a letter, such as 3.0.0c) are folded into the release that followed them. Releases before 1.0 were published as "Ubuntu Launcher". The history begins at 0.4.0b, the last version developed on Launchpad; nothing earlier is recorded here.

## [Unreleased]
- Fixed Settings → Icons crashing on Android 12 and 12L when tinted icons are on.

## [3.0.3] - 2026-09-17
- Fixed the launcher crashing on Android 14 when the device has a work, clone or private profile.

## [3.0.2] - 2026-09-07
- Fixed a crash when dragging a widget that is too big for the desktop grid, which could happen after choosing a smaller grid size.
- Fixed a startup crash on some vendor ROMs (e.g. Funtouch OS) when a second profile has no system badge.
- If the chosen icon pack can't be loaded (e.g. while it is being updated), all icons now fall back to system icons instead of a mix of both. The pack comes back by itself once it loads again.
- Fixed the loading spinner on the menu button showing blank at startup (a 3.0.1 regression).

## [3.0.1] - 2026-09-01
- Fixed heavy battery and CPU drain when startup was interrupted: the loading spinner could keep animating in the background forever and hold old copies of the home screen in memory.
- DistroHopper now appears in Android's per-app language settings, and every language ships in the Play Store download.
- English devices set to a non-US variant (UK, Ireland, Australia, India, …) now get British English instead of American English.

## [3.0.0] - 2026-08-09

Includes the 3.0.0a–3.0.0h test builds.

### Added
- **KDE Plasma theme**: Breeze Dark, with Plasma 6's translucent panel, a Kickoff-style dash with the search field in its header, and a launcher that can sit on any edge.
- **MATE, COSMIC and Budgie themes** as early-access previews, available with developer mode turned on.
- **Multiple desktops.** Swipe sideways to switch desktops. A row of dots shows where you are while you swipe, and Home returns to the first desktop. There's always one empty desktop at the end (up to 16); desktops left empty are removed automatically.
- **Widgets are now a proper feature** (they were a broken, developer-only experiment before):
  - Widgets snap to a grid on each desktop and stay where you put them across restarts.
  - A new widget picker groups widgets by app and shows previews.
  - New widgets start at the size their app recommends.
  - Long-press a widget to edit it: resize it with handles that respect the widget's limits, drag it to move it, or drop it on the trash to remove it.
- **Apps on the desktop.** Drag apps from the dash or the launcher onto a desktop, next to widgets.
- **Folders** on the dash, the launcher and the desktop:
  - Drop one app onto another to make a folder. A folder holds 2–9 apps, is drawn as a mini grid of their icons, and opens over a blurred backdrop.
  - Drag an app out of an open folder to take it out. A folder left with one app dissolves.
  - Dropping a folder on the trash does something different on each surface:
    - Dash: its apps go back to the dash.
    - Launcher: its apps are unpinned.
    - Desktop: the folder and its apps are removed.
- **One-gesture drags across the dash, launcher and desktop:**
  - The dash stays open when you start a drag from it.
  - Hovering over the launcher or panel closes the dash; hovering over a menu button opens it again.
  - Dropping an app on the open dash removes it from wherever it came from.
  - While you drag an app out of the dash, an **App info** target takes the trash's place.
- **Gestures** (Settings → Gestures):
  - Choose what swiping up and swiping down on the home screen do: open the dash, open the dash and start a search, or open the notification tray.
  - The notification tray option uses an optional accessibility service, which reads no screen content.
  - The dash follows your finger as it opens, and swiping down on the dash closes it.
- **Work profile, Private space and clone profile support.**
  - Apps from other profiles appear in the dash with their profile badge and launch in their own profile. You can pin them.
  - The dash gets one swipeable page per profile.
  - Search results are grouped by profile.
- **Dash sort order** (Settings → Functionality): alphabetical, most recently used or most used. Usage stats stay on the device.
- **Per-desktop pinned apps** (opt-in): each desktop gets its own launcher apps, and the launcher morphs between them as you swipe.
- **Font picker** (Settings → Appearance → Font): system default, OpenDyslexic, Ubuntu or Oxygen, each previewed in its own font. The choice applies to all of DistroHopper's text.
- **Icons screen** (Settings → Appearance → Icons):
  - Icon shape: circle, squircle, rounded square or square, with live previews.
  - Tinted icons (Android 13+), coloured from the wallpaper, the system accent, the theme, or a preset colour.
  - The icon pack setting now lives here too.
- **Menu button widgets** for any home screen, including other launchers. One opens DistroHopper's dash; the other opens it with search focused. Both are drawn in the current theme's style.
- **Desktop grid size** setting in customise mode, with five sizes worked out for your device. Rotation and Android's display size setting no longer scatter desktop items.
- A **"DistroHopper Settings" entry** in the dash app grid.
- **"Start search when Dash opened"** option. Pressing Enter in dash search now opens the first result.
- **Google Play Store and F-Droid search sources.** DuckDuckGo results now show each site's favicon.
- **First-run setup wizard**: pick a desktop theme and set DistroHopper as your home screen.
  - A browser, an email app and the camera are pinned on a fresh install, but not on upgrade.
  - A **"Set as home screen"** entry in settings covers the same step later. It falls back to the system's home app settings on devices where Android's own prompt doesn't work (e.g. some Samsung phones).
- **Per-theme dash animations**, for example GNOME's icons zooming out of the menu button, or Cinnamon's dash sliding in from the launcher's edge. Animations are skipped in battery saver.
- **Crash-report switch** in Advanced settings.
- **Privacy policy and open source licences** in a new Legal section of the About screen, and "Get involved" links to GitHub and Transifex.
- **App icon:** DistroHopper's own icon is now adaptive, with a monochrome layer for themed icons on Android 13+.
- **American English translation.** The default English is British English.
- **Crash-reporting-free APK:** each GitHub release also has a `-paranoia` APK with crash reporting permanently off, for sideloading.

### Changed
- **Requires Android 12 or newer** (was Android 10).
- **Real blur behind the dash.**
  - Opening the dash genuinely blurs the wallpaper and the widgets.
  - On devices without window blur (e.g. many Samsung phones) it falls back to a frosted, wallpaper-tinted layer.
  - Chameleonic themes get their colour from Android's wallpaper colours, which works on Android 13+ and **needs no storage permission**.
- **Themes:**
  - **GNOME** is updated to the GNOME 45+ look:
    - a floating rounded dock, which can also sit at the bottom;
    - a dot-grid apps button, which can go at either end of the dock or be hidden;
    - a pill-shaped search field;
    - a panel that blends into the dash like the overview.
  - GNOME is now listed first in the theme lists.
  - **Cinnamon** is redrawn with Mint-Y dark colours.
  - **Pantheon** is redrawn with a translucent floating dock. Its dock menu button is hidden by default, since the panel's Applications button already opens the dash.
  - "Ubuntu Unity" is renamed **Unity**, and "elementary OS" is renamed **Pantheon**.
  - The settings button on the launcher is gone from every theme. Use the dash's Settings entry or the panel cog.
- **Pinning and reordering:**
  - Long-pressing a dash app starts a drag instead of instantly pinning it. Drop it on the launcher to pin it at that spot.
  - Reordering launcher icons shows a live preview as the other icons slide aside.
  - The "pinned/unpinned" toasts are gone.
- **Long-pressing empty desktop space** now zooms the desktop out and opens a sheet with Widgets, Customise and Settings, instead of going straight to the widget picker.
- **Customise mode is redesigned**, with grouped cards (Launcher, Dash, Desktop, Panel) and button rows instead of dropdowns.
  - Pinned icon size is now five presets (Tiny to Huge) sized so a whole number of icons fits the launcher.
  - Dash icon size is now a column count (e.g. 4 × 6), with more columns in landscape.
  - Your previous icon sizes are not carried over.
- **Local files search is rebuilt.**
  - It searches only folders you pick with Android's folder picker, and needs no storage permission.
  - It finds documents such as PDFs as well as media.
  - It is off by default and has to be turned on again after upgrading.
- **Dash search is faster.** App results appear as you type while web results are still loading. Network searches time out after 10 seconds instead of hanging.
- The dash's "Applications" title scrolls away with the grid.
- Pressing Home closes the dash. Back closes it with its animation instead of snapping shut.
- **Redesigned screens:**
  - Settings now use switches and the redesigned theme picker.
  - The Search sources screen shows cards you can drag to reorder.
  - Dialogs have a frosted, rounded style.
- **Crash reports collect much less:** a fixed list of diagnostic fields and only the launcher's own settings. They never include your app list, layouts, usage history or folders.
- Developer options only appear in developer mode, and turning it off resets them. The debug log viewer is redesigned.
- Translations: Dutch is complete, Turkish and Portuguese are extended, and outdated strings were removed from the other languages.

### Fixed
- On Android 15 and newer, settings screens no longer draw under the status bar, and the launcher and dash no longer sit under the 3-button navigation bar.
- Adaptive icons (most modern apps) were drawn small and blurry.
- Apps occasionally showed another app's icon.
- Entering customise mode, or changing the launcher or panel edge, sometimes dropped you back on the normal home screen.
- Tapping "Wallpaper" in settings crashed when no wallpaper picker was installed.
- Search results were drawn about 50% larger than dash icons until the icon size had been changed once.
- Dragging a launcher icon made the trash icon flicker.
- The whole home screen leaked in memory every time it was recreated (for example on a theme change).
- Errors during background loading were silently swallowed instead of shown.
- The debug log kept growing without limit even with developer mode off.

### Removed
- The Reddit, Ask Ubuntu, Stack Overflow, Super User and Server Fault search sources.
- The "Open Dash by default" and "Widgets" options (widgets are always available now).
- The Contribute screen (its links moved to About) and the About screen's "Honourable mentions".
- The storage, media and wake-lock permissions.

## [2.7.0] - 2025-10-18
- Added icon pack support. A new "Icon pack" setting under Appearance lists installed icon packs (ADW, Nova, Apex, GO Launcher and similar formats) plus "None". Choosing one clears the icon cache so the new icons apply.

## [2.6.5] - 2025-10-14
- Fixed a crash on launch when the wallpaper could not be read or blurred. The wallpaper is now simply left unblurred.
- Fixed a crash while caching app icons when an app's icon has no size ("width and height must be > 0", #31).

## [2.6.4] - 2025-10-14
- Android 10 or newer is now required (previously Android 6.0).
- Fixed crashes (ConcurrentModificationException) when the app list or pinned apps changed while being read, e.g. during app installs or removals.

## [2.6.3] - 2023-03-27
- On Android 13 and newer, the Unity theme's wallpaper-based colour now comes from Android's own wallpaper colours (Android 8.1+), because the app can no longer read the wallpaper image there. If that fails, it falls back to Ubuntu orange instead of using the bundled default wallpaper.
- Fixed a crash when working out the colour of an image that is not a plain bitmap.
- An app that fails to launch now shows a "Failed to launch …" message instead of crashing DistroHopper.
- Fixed a possible crash when leaving the home screen while widgets are enabled.

## [2.6.2] - 2023-03-27
- Android 6.0 or newer is now required (previously Android 4.4).
- Removed the donations section (and the in-app billing permission) from the Contribute screen.
- Updated Dutch translation.

## [2.6.1] - 2020-06-19
- Fixed Dash search results appearing without icons (tiny tiles) after 2.6.0.

## [2.6.0] - 2020-06-12
- New "Dash icon size" slider in Customise mode, which sets the size of app icons and search results in the Dash (#17).
- The Panel now uses the height defined by the current theme. It used to use the elementary OS panel height on every theme.

## [2.5.0] - 2020-06-05
- Removed Google Analytics. Crash reports are now sent through ACRA to the developer's own server, and a "Sending crash report..." message appears when this happens.
- Android 4.4 or newer is now required (previously Android 4.1).

## [2.4.0] - 2019-06-02
- Cinnamon theme: the launcher can now be placed on any screen edge (top, bottom, left or right), with matching backgrounds.
- Sharper Cinnamon search bar graphics.
- Faster lens searches: each lens now stops once it has as many results as it will show.
- Removed theme screenshots from the theme picker.
- Updated Russian translation.

## [2.3.0] - 2019-06-01
- New Cinnamon theme, with a bottom panel-style launcher, the menu button on the left and the settings button on the right.
- Fixed the app-loading progress spinner on the Dash button, which never showed any progress.

## [2.2.0] - 2019-03-31
- Cached app icons now expire after a week, so updated app icons are picked up.
- Faster, more reliable saving of the app icon cache.
- Removed the outdated American English translation, which some devices showed instead of the up-to-date default English strings.
- Updated translations.

## [2.1.0] - 2018-12-03
- Removed the Google+ lens, as Google+ was shutting down.
- The Dash now closes when you start dragging a pinned app in the launcher.
- Fixed a crash when opening a Local Files lens search result.
- Fixed the trash icon being replaced by the settings icon when an app was dragged onto it.
- The search progress wheel only appears after a short delay, so the search box no longer jumps while you type.
- The "Customise" settings entry can now be translated.
- Updated Russian translation.

## [2.0.1] - 2018-10-13
- Fixed a crash when long-pressing a Local Files lens search result.
- GNOME theme: the Dash button is hidden while dragging apps.
- Fixed the settings icon appearing in the launcher of the GNOME and elementary OS themes after dragging apps.

## [2.0.0] - 2018-10-10
- New Customise mode (Settings → Appearance → Customise) for changing the look directly on the home screen. It shows controls inside the Dash for pinned icon size, launcher location and panel location. App launching is disabled while customising.
- The launcher's screen edge is choosable again, per theme. Ubuntu Unity: left, right or bottom. elementary OS: any edge. GNOME: left or right. The launcher location setting is reset on upgrade.
- The Panel can be hidden again on the Ubuntu Unity theme, via Panel location "Hide".
- The pinned icon size slider moved from Settings into Customise mode.
- The theme "Ubuntu" is now named "Ubuntu Unity", and elementary OS moved below GNOME in the theme list.
- Much faster startup: app labels and icons are cached and loaded in the background.
- On Android 6.0+, the app now asks for storage permission at startup, which it needs to read the wallpaper. Without it, colour calculations use a bundled wallpaper.
- Removed the "Take more samples" and "Use HSV" colour calculation settings.
- Fixed a crash when pinning apps that use adaptive icons (Android 8.0).
- Fixed dominant-colour calculation for adaptive icons, and for the wallpaper on OnePlus phones.
- GNOME theme: fixed the launcher not appearing on the right edge when the Dash is closed.
- Fixed DistroHopper listing itself in its own app drawer.
- Fixed a possible freeze (deadlock) and errors from background tasks not being cancelled properly.
- Settings now use the Material theme on Android 5.0+. Fixed text colours in the lens settings and in Customise mode on the elementary OS theme.
- Experimental widget support can be enabled from developer options. Developer mode also gains a "Log toasts" option.
- Android 4.1 or newer is now required (previously Android 4.0).
- Added Serbian translation, and updated many others.

## [1.0.1] - 2017-03-31
- Fixed a crash that some users hit after changing the theme.
- Fixed features that the 1.0 release build broke by shrinking the app's code.
- Removed the experimental "Launcher overlay service" and "Widgets" options from Developer options.

## [1.0] - 2017-03-29
### Added
- **Ubuntu Launcher is now DistroHopper.** The app has a new name, a new logo that isn't tied to Ubuntu, and a new package name (`be.robinj.distrohopper`). Because the package name changed, it installs as a separate app, and pinned apps and settings from Ubuntu Launcher are not carried over.
- **Themes.** A new Themes screen (Settings → Appearance → Theme) shows each theme with screenshots and an Apply button. There are three themes:
  - **Ubuntu**: Unity, the existing look.
  - **elementary OS**: Pantheon. The dock sits at the bottom, centred. The top panel is transparent, with an "Applications" button on the left and a settings cog on the right. The Dash is light with dark text.
  - **GNOME**: GNOME Shell. The dock is on the left, vertically centred, with an app-grid button at the bottom. The top panel has an "Applications" button, and the wallpaper darkens while the Dash is open.
- **Dash search uses "lenses" (search sources).** Results are grouped by source, and each group appears as soon as its source finishes. A progress wheel next to the search field shows how far the search has got, and each source shows up to 10 results. The search hint now just says "Search".
  - Available sources: Installed apps, Local files, DuckDuckGo, Ask Ubuntu, Stack Overflow, Server Fault, Super User, GitHub (repositories), Reddit and Google+.
  - Installed apps and Local files are on by default. Tapping a file result opens it in a suitable app. The app now asks for storage access so it can search local files.
  - A new "Search sources" screen (Settings → Functionality) turns each source on or off and sets the order of the results by drag and drop.
- **App list updates automatically.** Installed apps show up in the Dash straight away. Uninstalled apps are removed from the Dash and unpinned from the launcher, with no need to restart.
- **Donations** on the Contribute screen now go through Google Play in-app purchases (€1–€20) instead of PayPal.

### Changed
- **Android 4.0 (Ice Cream Sandwich) or newer is now required.** Android 2.2 was the previous minimum.
- The Dash now fades in and out when it opens and closes.
- Dash app icons are larger.
- The home screen, About, Contribute, Themes and Search sources screens now use a Material/AppCompat style. Touching items in the Dash shows the Material touch animation.
- Settings are reorganised into Appearance, Functionality, Advanced and Developer options:
  - "Launcher width" is renamed "Pinned icon size".
  - Several options were removed because the theme now controls them:
    - launcher screen edge (left or right)
    - launcher icon background opacity
    - panel show/hide and panel opacity
    - Unity dynamic or custom background colour and background opacity
    - wallpaper blur method
    - "Advanced search"
- Running apps are no longer shown in the launcher, and the "Show running apps" option is removed. Android 5.0 and newer no longer lets apps see this reliably.
- The "Launcher overlay service" (swipe from the screen edge to show the launcher inside other apps) moved into Developer options.
- New "Developer mode" switch under Advanced. It unlocks Developer options, including a live debug log viewer.
- Pinning an app that's already pinned now shows an "is already pinned" message.
- The About screen is redesigned. It credits the translation team as a whole instead of an outdated list of translators.
- Easter egg: tap the logo on the About screen three times.
- The Contribute screen links to the renamed GitHub and Transifex projects.
- New translations: Arabic, Czech, French, Hungarian, Indonesian, Swedish, Ukrainian and Simplified Chinese. Existing translations were updated.

### Fixed
- Fixed crashes when returning to the home screen from Settings. The home screen now reloads properly to apply changes.
- Fixed the launcher getting stuck while dragging if the screen turned off mid-drag.

## [0.5.12] - 2014-11-02
- Added a "Show running apps" option (Launcher settings, on by default). Turning it off hides the running-app arrows and the extra icons for running apps that aren't pinned, on both the home screen Launcher and the floating Launcher.
- Added an "Open Dash by default" option (Dash settings, off by default) that opens the Dash as soon as apps have loaded.
- Added a "How can I contribute?" screen, opened from the Contribute button in Preferences and About. It links to translating on Transifex, reporting bugs on GitHub (or by e-mail) and donating through PayPal.
- The error dialog title is now "(╯°□°）╯︵ ┻━┻" instead of "Error".

## [0.5.11] - 2014-11-01
- New "Screen edge" option in Launcher settings: the Launcher can now sit on the right edge of the screen instead of the left. The Dash and the floating Launcher follow the chosen edge; on the right edge the floating Launcher opens with a leftward swipe.
- Updated the Dutch, Portuguese and US English translations.

## [0.5.10] - 2014-10-27
- New "Wallpaper blur method" option: Don't blur, Darken, or Downscale & upscale (the default). Darken is used instead when downscaling fails or a live wallpaper is set.
- Fixed the Launcher width, Launcher icon opacity, Panel opacity and Unity background opacity sliders not showing their correct default values in Preferences.
- Added a US English translation and updated the Portuguese translation.

## [0.5.9.1] - 2014-10-15
- Fixed Preferences crashing on large-screen devices (tablets). Preferences now always use the single-column layout.

## [0.5.9] - 2014-10-15
- Rebuilt the Preferences screen as a standard Android settings screen with sections (General, Unity, Launcher, Dash, Panel, Advanced), plus About in the menu. Sliders and the colour picker now open as settings entries.
- The About screen now credits translators.
- The Dash app grid is now a regular scrolling grid, no longer stretched to full height inside a scroll view.
- New and updated translations: German, Spanish, Italian, Portuguese and Turkish added; Dutch, Japanese, Polish and Russian updated. Translations now come from Transifex.

## [0.5.8] - 2014-09-05
- Dash search now also matches text anywhere in an app's name, not only at the start (e.g. "Store" finds "Play Store"). Prefix matches are listed first (GitHub issue #3).
- New "Advanced Dash search" option (on by default) to turn off matching anywhere in the name, for slow devices.
- Added Japanese and Polish translations.

## [0.5.7] - 2014-09-03
- You can now drag Launcher icons to reorder pinned apps (Android 3.0+). While you drag, a trash icon replaces the Preferences icon at the bottom of the Launcher; drop an app on it to unpin it. On older Android versions long-press still unpins.
- The Launcher now shows running apps. Pinned apps that are running get an indicator, and running apps that aren't pinned appear below the pinned ones. The floating Launcher shows them too.

## [0.5.6] - 2014-09-02
- On Android 4.4+ the status bar is now translucent. The area behind it is black, and takes the Dash colour while the Dash is open.
- The Preferences and About screens now show the wallpaper behind them and have a title bar.
- The floating Launcher (in other apps) now opens only on a swipe inwards from the screen edge, not on any touch.

## [0.5.5] - 2014-08-29
- New experimental option: "Make the Launcher available inside other apps". Swiping in from the bottom half of the left screen edge shows a floating Launcher with your pinned apps; tapping its Dash button returns home with the Dash open.
- The "Take more samples" and "Use HSV" colour calculation options now actually take effect (they were ignored before).
- Fixed the dynamic colour for wallpapers without any usable pixels; a neutral dark grey is now used.
- The Dash background gradient now spans the full width of the Dash.

## [0.5.4] - 2014-08-26
- Now runs on Android 2.2 and newer (was 4.0). Some features, such as Panel transparency, need a newer Android version.
- The wallpaper can now be changed from Preferences, which also shows a preview of the current wallpaper.
- Added zoom and fade animations when opening an app from the home screen and when returning to it.
- Added animations when apps appear in the Dash and the Launcher (Android 3.0+).
- Added Russian and Dutch translations.
- Fixed the "Panel opacity" option showing in Preferences when "Show Panel" was off.
- Fixed a crash (out of memory) while blurring large wallpapers; the wallpaper is now darkened instead of blurred when this happens.

## [0.5.3] - 2014-08-24
- Pressing Back on the home screen no longer closes the launcher when it is the default home app.
- Fixed the Dash close button being out of place after the Launcher width had been changed.
- Lowered the default Launcher width.
- Fixed the icon background colour of greyscale icons ignoring the transparency setting.
- The larger layout for wide screens now starts at 480dp width instead of 400dp.

## [0.5.2] - 2014-08-24
- Live wallpapers are now supported. The launcher draws over the system wallpaper, and with a live wallpaper the Dash darkens the background instead of blurring it. The dynamic colour falls back to a translucent dark grey.
- New Preferences: "Launcher width" (the size of the Launcher and its icons), "Show Panel" and "Panel opacity". The Panel becomes fully opaque while the Dash is open.

## [0.5.1] - 2014-08-23
- The whole app was rewritten as a native Android app, replacing the old HTML/web-view interface. It is faster and looks and behaves differently.
- Apps load in the background, with a progress indicator in the Launcher; the Dash button appears when loading finishes.
- Long-press an app in the Dash to pin it to the Launcher; long-press a Launcher icon to unpin it. A short message confirms each change. The old app info page in the Dash is gone.
- The Dash shows all installed apps alphabetically and filters them as you type in the search box. The wallpaper is blurred while the Dash is open, and Back closes the Dash.
- New Preferences screen, opened from the Launcher's Preferences icon or the Panel's cog:
  - Launcher icon background opacity.
  - Dynamic Unity background colour (from the wallpaper), or a colour you pick yourself.
  - Unity background opacity.
  - Colour calculation options ("Take more samples", "Use HSV").
- New About screen with credits.
- Errors now show in a dialog with details you can send to the developer.

## [0.4.1] - 2014-08-13
- Fixed long-pressing elsewhere on the screen opening the app info page for the last app you touched.

## [0.4.0] - 2014-08-05
- The list of installed apps now loads in the background. A loading screen shows first, and a spinner stays in the Launcher until the Dash button is ready.
- Wallpaper is blurred, and Launcher icons are greyed out, while the Dash is open.
- Fixed Dash search on Android 4.4, including deleting characters with backspace.
- Tapping pinned apps in the Launcher now launches them, and the app info page's Launch button works for pinned apps.
- The layout now scales up on larger screens (e.g. tablets): bigger Launcher and icons, and a centred search box.
- Fixed app icons going missing when their cached image file was empty.
- Updated the Dash button, icon background and window-close artwork, and the fallback wallpaper.

## [0.4.0b] - 2014-03-30

The starting point: the version imported from Launchpad, before any of the changes above.

- An Ubuntu Unity-style home screen, "Ubuntu Launcher", built as an HTML page inside a web view. It shows the Unity panel at the top and the launcher on the left, over the current wallpaper.
- The launcher and dash take a "chameleonic" tint from the wallpaper's average colour.
- The dash lists installed apps and has a Home/Applications lens ribbon and a search box.
- Long-pressing an app opens an app info page in the dash, with Pin to Launcher/Remove from Launcher and Launch buttons.
- The panel cog and the launcher's Preferences icon open a menu with a shortcut to Android Settings. There were no preferences yet.
