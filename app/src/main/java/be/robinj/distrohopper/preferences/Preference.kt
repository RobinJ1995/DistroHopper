package be.robinj.distrohopper.preferences

import be.robinj.distrohopper.desktop.launcher.LauncherIconGrid

/**
 * Every persisted setting, paired with its SharedPreferences key (and, where it
 * has one, a default value and a parent toggle that gates it). The key is what's
 * actually written to disk — some carry a `_v2` suffix from a past migration.
 */
enum class Preference(
	private val key: String,
	private val defaultValue: Any? = null,
	private val parent: Preference? = null,
) {
	/** Which screen edge the panel sits on, or None to hide it. */
	PANEL_EDGE("panel_edge_v2"),
	/** Panel background opacity, 0–100. */
	PANEL_OPACITY("panel_opacity"),
	/** Which screen edge the launcher/dock sits on. */
	LAUNCHER_EDGE("launcher_edge_v2"),
	/** Where the menu button (BFB) sits in the launcher: none/start/end. */
	LAUNCHER_BFB_LOCATION("launcher_bfb_location"),
	/** Whether running (unpinned) apps are shown in the launcher. */
	LAUNCHER_SHOW_RUNNING_APPS("launcher_running_show"),
	/** Whether pinned launcher apps are shared globally or kept per desktop. */
	LAUNCHER_APP_PIN_MODE("launcher_app_pin_mode", "global"),
	/**
	 * Pinned-icon size preset (index 0 = Tiny … 4 = Huge, defaulting to the middle, "Default").
	 * The pixel size is computed at runtime from this; see
	 * [be.robinj.distrohopper.desktop.launcher.LauncherIconGrid]. A new key (the old
	 * `launchericon_width` raw-dp value is intentionally not migrated).
	 */
	LAUNCHER_ICON_PRESET("launcher_icon_preset", LauncherIconGrid.DEFAULT_PRESET),
	/** Whether the accessibility-based launcher service is enabled. */
	LAUNCHERSERVICE_ENABLED("launcherservice_enabled"),
	/** Whether dash search also queries the (slower) full set of lenses. */
	DASH_SEARCH_FULL("dashsearch_full"),
	/** Whether opening the dash focuses the search field (raising the keyboard). */
	DASH_SEARCH_FOCUS_ON_OPEN("dashsearch_focus_on_open", false),
	/** Maximum results each lens contributes to dash search. */
	DASH_SEARCH_LENSES_MAX_RESULTS("dashsearch_lenses_maxresults"),
	/** Number of icon columns across the dash app grid. */
	DASH_GRID_COLUMNS("dash_grid_columns"),
	/** How dash apps are ordered (alphabetical, usage, custom, …). */
	APP_SORT_ORDER("app_sort_order", "alphabetical"),
	/** Whether opt-in crash reporting is enabled. */
	CRASH_REPORTING_ENABLED("crash_reporting_enabled"),
	/** The selected distro theme (its lowercase class name). */
	THEME("theme"),
	/** The selected icon pack (package name), or unset for system icons. */
	ICON_PACK("icon_pack"),
	/** The selected font family, or "system" for the device default. */
	FONT("font", "system"),
	/** The icon mask shape (system, circle, squircle, …). */
	ICON_SHAPE("icon_shape", "system"),
	/** Whether icons are recoloured with a single tint. */
	TINTED_ICONS("tinted_icons", false),
	/** The source colour for tinted icons (wallpaper or a fixed colour). */
	ICON_TINT("icon_tint", "wallpaper"),
	/** Signature of the current icon config; a mismatch invalidates cached icons. */
	ICON_CONFIG_SIGNATURE("icon_config_signature"),
	/** Whether onboarding has been completed. */
	SETUP_COMPLETED("setup_completed"),
	/** Whether onboarding has been started. */
	SETUP_STARTED("setup_started"),
	/** Whether the first-run default pinned apps still need to be applied. */
	DEFAULT_PINS_PENDING("default_pins_pending"),
	/** Whether this install is ineligible for automatic default pins. */
	DEFAULT_PINS_AUTO_INELIGIBLE("default_pins_auto_ineligible"),
	/** Developer mode master toggle; gates the DEV_* settings below. */
	DEV("dev"),
	/** Dev: surface internal log messages as on-screen toasts. */
	DEV_LOG_TOASTER("dev_log_toaster", null, DEV),
	/** Dev: allow freely resizing any widget, ignoring its declared limits. */
	DEV_WIDGET_RESIZE_ANY("dev_widget_resize_any", false, DEV),
	/** Dev: show a dot at every grid intersection while dragging or resizing. */
	DEV_SHOW_GRID_ON_DRAG("dev_show_grid_on_drag", false, DEV),
	/** What the home-screen swipe-up gesture does (a GestureAction value). */
	GESTURE_SWIPE_UP("gesture_swipe_up", "open_dash"),
	/** What the home-screen swipe-down gesture does (a GestureAction value). */
	GESTURE_SWIPE_DOWN("gesture_swipe_down", "none");

	fun getName(): String = this.key

	@Suppress("UNCHECKED_CAST")
	fun <T> getDefault(): T = this.defaultValue as T

	fun getParent(): Preference? = this.parent

	override fun toString(): String = this.getName()
}
