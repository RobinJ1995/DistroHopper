package be.robinj.distrohopper.preferences;

import android.content.SharedPreferences;

public enum Preference {
	PANEL_EDGE("panel_edge_v2"),
	PANEL_OPACITY("panel_opacity"),
	LAUNCHER_EDGE("launcher_edge_v2"),
	LAUNCHER_MENU_BUTTON_VISIBLE("launcher_menu_button_visible"),
	LAUNCHER_SHOW_RUNNING_APPS("launcher_running_show"),
	LAUNCHER_APP_PIN_MODE("launcher_app_pin_mode", "desktop"),
	LAUNCHERICON_WIDTH("launchericon_width"),
	LAUNCHERSERVICE_ENABLED("launcherservice_enabled"),
	DASH_SEARCH_FULL("dashsearch_full"),
	DASH_SEARCH_FOCUS_ON_OPEN("dashsearch_focus_on_open", false),
	DASH_SEARCH_LENSES_MAX_RESULTS("dashsearch_lenses_maxresults"),
	DASH_GRID_COLUMNS("dash_grid_columns"),
	APP_SORT_ORDER("app_sort_order", "alphabetical"),
	CRASH_REPORTING_ENABLED("crash_reporting_enabled"),
	THEME("theme"),
	ICON_PACK("icon_pack"),
	ICON_SHAPE("icon_shape", "system"),
	TINTED_ICONS("tinted_icons", false),
	ICON_TINT("icon_tint", "wallpaper"),
	ICON_CONFIG_SIGNATURE("icon_config_signature"),
	SETUP_COMPLETED("setup_completed"),
	SETUP_STARTED("setup_started"),
	DEFAULT_PINS_PENDING("default_pins_pending"),
	DEFAULT_PINS_AUTO_INELIGIBLE("default_pins_auto_ineligible"),
	DEV("dev"),
	DEV_LOG_TOASTER("dev_log_toaster", null, DEV),
	DEV_WIDGET_RESIZE_ANY("dev_widget_resize_any", false, DEV);

	private final String name;
	private final Object defaultValue;
	private final Preference parent;

	Preference(final String name, final Object defaultValue, final Preference parent) {
		this.name = name;
		this.defaultValue = defaultValue;
		this.parent = parent;
	}

	Preference(final String name, final Object defaultValue) {
		this(name, defaultValue, null);
	}

	Preference(final String name) {
		this(name, null);
	}

	public String getName() {
		return this.name;
	}

	public <T> T getDefault() {
		return (T) this.defaultValue;
	}

	public Preference getParent() {
		return this.parent;
	}

	@Override
	public String toString() {
		return this.getName();
	}
}
