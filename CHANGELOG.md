# Changelog

All notable user-facing changes. Test builds are folded into the following
release. Versions before 1.0 were released as Ubuntu Launcher.

## [Unreleased]
### Fixed
- Crash in Settings → Icons on Android 12 with tinted icons enabled

## [3.0.3] - 2026-09-17
### Fixed
- Crash on Android 14 with a work, clone or private profile

## [3.0.2] - 2026-09-07
### Changed
- Unloadable icon pack now falls back to system icons entirely instead of mixing both
### Fixed
- Crash when dragging a widget larger than the desktop grid
- Startup crash on some vendor ROMs when a profile has no badge
- Blank loading spinner on the menu button at startup

## [3.0.1] - 2026-09-01
### Added
- Per-app language selection in Android settings
### Fixed
- Battery drain when the home screen was closed during startup
- Non-US English locales showing American instead of British English

## [3.0.0] - 2026-08-09
### Added
- KDE Plasma theme
- MATE, COSMIC and Budgie themes (developer mode only)
- Multiple desktops
- Widgets, with resizing and a new widget picker
- Apps on the desktop
- Folders on the dash, launcher and desktop
- Drag apps between dash, launcher and desktop in one gesture
- App info drop target when dragging from the dash
- Configurable swipe up/down gestures, including opening the notification tray
- Work profile, Private space and clone profile support
- Dash sort order: alphabetical, recently used, most used
- Optional per-desktop pinned apps
- Font setting, including OpenDyslexic
- Icon shape and tinted icon settings
- Menu button and search widgets for any home screen
- Desktop grid size setting
- Settings entry in the dash
- Option to start searching when the dash opens
- Enter in dash search opens the first result
- Google Play Store and F-Droid search
- Favicons in DuckDuckGo results
- First-run setup wizard, with default pinned apps
- "Set as home screen" setting
- Per-theme dash animations
- Crash report opt-out
- Privacy policy and open source licences in About
- Themed app icon
- American English translation
### Changed
- Requires Android 12 or newer
- Dash blurs the wallpaper and widgets
- Wallpaper colours no longer need storage permission
- GNOME theme updated to GNOME 45+
- Cinnamon and Pantheon themes redrawn
- Themes renamed to Unity and Pantheon
- Settings button removed from the launcher
- Long-press on a dash app starts a drag instead of pinning
- Live preview when reordering the launcher
- Long-press on the desktop opens a Widgets/Customise/Settings menu
- Customise mode redesigned
- Pinned icon size uses presets; dash icon size is set in columns
- Local files search only searches folders you choose, and is off by default
- Faster dash search; web searches time out after 10 seconds
- Home closes the dash
- Settings, search sources and dialogs redesigned
- Crash reports collect less data
- Updated translations
### Fixed
- Content drawn under the status and navigation bars on Android 15+
- Blurry, small icons for most modern apps
- Apps occasionally showing another app's icon
- Customise mode sometimes failing to open
- Crash when no wallpaper picker is installed
- Oversized search result icons
- Trash icon flickering while dragging launcher icons
- Memory growing on every theme change
### Removed
- Reddit, Ask Ubuntu, Stack Overflow, Super User and Server Fault search
- "Open Dash by default" option
- Contribute screen (links moved to About)
- Storage and wake-lock permissions

## [2.7.0] - 2025-10-18
### Added
- Icon pack support

## [2.6.5] - 2025-10-14
### Fixed
- Crash on launch when the wallpaper couldn't be read
- Crash with apps that have an empty icon

## [2.6.4] - 2025-10-14
### Changed
- Requires Android 10 or newer
### Fixed
- Occasional crashes when apps were installed, removed or pinned

## [2.6.3] - 2023-03-27
### Fixed
- Unity wallpaper colour on Android 13+
- Crash when calculating the colour of some icons and wallpapers
- Crash when an app fails to launch
- Crash when leaving the home screen with widgets enabled

## [2.6.2] - 2023-03-27
### Changed
- Requires Android 6.0 or newer
- Updated Dutch translation
### Removed
- Donations

## [2.6.1] - 2020-06-19
### Fixed
- Dash search results missing their icons

## [2.6.0] - 2020-06-12
### Added
- Dash icon size setting
### Fixed
- Panel height ignoring the theme

## [2.5.0] - 2020-06-05
### Changed
- Requires Android 4.4 or newer
- Crash reports sent to the developer's own server
### Removed
- Google Analytics

## [2.4.0] - 2019-06-02
### Added
- Cinnamon launcher can be placed on any edge
### Changed
- Faster dash search
- Sharper Cinnamon search bar
- Updated Russian translation
### Removed
- Screenshots in the theme picker

## [2.3.0] - 2019-06-01
### Added
- Cinnamon theme
### Fixed
- Loading spinner on the dash button not showing progress

## [2.2.0] - 2019-03-31
### Changed
- Updated app icons are picked up within a week
- Updated translations
### Removed
- Outdated American English translation

## [2.1.0] - 2018-12-03
### Changed
- Dash closes when dragging a pinned app
- Search progress indicator appears after a short delay
- Updated Russian translation
### Fixed
- Crash when opening a local file search result
- Trash icon turning into the settings icon when dropping an app
### Removed
- Google+ search

## [2.0.1] - 2018-10-13
### Changed
- GNOME dash button hidden while dragging
### Fixed
- Crash when long-pressing a local file search result
- Settings icon appearing in the GNOME and elementary OS launcher after dragging

## [2.0.0] - 2018-10-10
### Added
- Customise mode for editing the home screen in place
- Choice of launcher edge per theme
- Option to hide the Unity panel
- Serbian translation
### Changed
- Requires Android 4.1 or newer
- Much faster startup
- "Ubuntu" theme renamed to "Ubuntu Unity"
- Pinned icon size moved to customise mode
- New settings look on Android 5.0+
- Updated translations
### Fixed
- Crash when pinning apps with adaptive icons
- Wrong colours for adaptive icons, and for the wallpaper on OnePlus phones
- GNOME launcher missing on the right edge
- DistroHopper listing itself in the dash
- Possible freeze
### Removed
- "Take more samples" and "Use HSV" colour settings

## [1.0.1] - 2017-03-31
### Fixed
- Crash after changing the theme
- Features broken in 1.0
### Removed
- Experimental launcher overlay and widget developer options

## [1.0] - 2017-03-29
### Added
- Renamed to DistroHopper; installs separately from Ubuntu Launcher
- Themes: Ubuntu, elementary OS and GNOME
- Search sources for the dash: local files, DuckDuckGo, Stack Exchange sites, GitHub, Reddit and Google+
- Search sources settings screen
- Dash and launcher update automatically when apps are installed or removed
- Developer mode with a debug log viewer
- Arabic, Czech, French, Hungarian, Indonesian, Swedish, Ukrainian and Simplified Chinese translations
### Changed
- Requires Android 4.0 or newer
- Dash fades in and out
- Larger dash icons
- Redesigned settings, About and Contribute screens
- Donations through Google Play instead of PayPal
- Launcher overlay moved to developer options
- Updated translations
### Fixed
- Crash when returning from settings
- Launcher stuck in drag mode when the screen turned off
### Removed
- Running apps in the launcher
- Launcher edge, panel, opacity, colour, blur and advanced search settings (now set by the theme)

## [0.5.12] - 2014-11-02
### Added
- Option to hide running apps
- Option to open the dash by default
- Contribute screen

## [0.5.11] - 2014-11-01
### Added
- Launcher on the right edge
### Changed
- Updated Dutch, Portuguese and US English translations

## [0.5.10] - 2014-10-27
### Added
- Wallpaper blur method setting
- US English translation
### Changed
- Updated Portuguese translation
### Fixed
- Sliders not showing their default values

## [0.5.9.1] - 2014-10-15
### Fixed
- Settings crash on tablets

## [0.5.9] - 2014-10-15
### Added
- German, Spanish, Italian, Portuguese and Turkish translations
### Changed
- Redesigned settings
- Translators credited in About
- Updated Dutch, Japanese, Polish and Russian translations

## [0.5.8] - 2014-09-05
### Added
- Dash search matches anywhere in an app's name
- Japanese and Polish translations

## [0.5.7] - 2014-09-03
### Added
- Drag to reorder and unpin launcher icons
- Running apps in the launcher

## [0.5.6] - 2014-09-02
### Changed
- Translucent status bar on Android 4.4+
- Wallpaper shown behind settings and About
- Launcher overlay only opens on an inward swipe

## [0.5.5] - 2014-08-29
### Added
- Experimental launcher overlay inside other apps
### Fixed
- Colour calculation settings having no effect
- Wallpaper colour for wallpapers with no usable pixels
- Dash background not spanning the full width

## [0.5.4] - 2014-08-26
### Added
- Change wallpaper from settings
- App open and return animations
- Russian and Dutch translations
### Changed
- Requires Android 2.2 or newer (was 4.0)
### Fixed
- Panel opacity setting shown with the panel hidden
- Crash with large wallpapers

## [0.5.3] - 2014-08-24
### Changed
- Smaller default launcher width
- Large phones no longer get the tablet layout
### Fixed
- Back closing the launcher when it is the default home app
- Misplaced dash close button after changing launcher width
- Greyscale icon backgrounds ignoring transparency

## [0.5.2] - 2014-08-24
### Added
- Live wallpaper support
- Launcher width, panel visibility and panel opacity settings

## [0.5.1] - 2014-08-23
### Added
- Settings for launcher opacity and Unity background colour
- About screen
- Error reporting dialog
### Changed
- Much faster, redesigned home screen
- Long-press to pin and unpin apps

## [0.4.1] - 2014-08-13
### Fixed
- Long-press opening app info for the wrong app

## [0.4.0] - 2014-08-05
### Added
- Loading screen while apps load
- Larger layout for tablets
### Changed
- Wallpaper blurred while the dash is open
- Updated artwork
### Fixed
- Dash search on Android 4.4
- Tapping pinned apps not launching them
- Missing app icons
