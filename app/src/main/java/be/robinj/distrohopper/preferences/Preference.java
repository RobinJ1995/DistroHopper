package be.robinj.distrohopper.preferences;

import android.content.SharedPreferences;

public enum Preference {
	PANEL_EDGE("panel_edge_v2"),
	PANEL_OPACITY("panel_opacity"),
	LAUNCHER_EDGE("launcher_edge_v2"),
	LAUNCHER_SHOW_RUNNING_APPS("launcher_running_show"),
	LAUNCHER_APP_PIN_MODE("launcher_app_pin_mode", "desktop"),
	LAUNCHERICON_WIDTH("launchericon_width"),
	LAUNCHERSERVICE_ENABLED("launcherservice_enabled"),
	DASH_SEARCH_FULL("dashsearch_full"),
	DASH_SEARCH_LENSES_MAX_RESULTS("dashsearch_lenses_maxresults"),
	DASHICON_WIDTH("dashicon_width", 24),
	CRASH_REPORTING_ENABLED("crash_reporting_enabled"),
	THEME("theme"),
	ICON_PACK("icon_pack"),
	SETUP_COMPLETED("setup_completed"),
	SETUP_STARTED("setup_started"),
	DEFAULT_PINS_PENDING("default_pins_pending"),
	DEFAULT_PINS_AUTO_INELIGIBLE("default_pins_auto_ineligible"),
	DEV("dev"),
	DEV_LOG_TOASTER("dev_log_toaster", null, true),
	DEV_WIDGET_RESIZE_ANY("dev_widget_resize_any", false, true);

	private final String name;
	private final Object defaultValue;
	private final boolean devChild;

	Preference(final String name, final Object defaultValue, final boolean devChild) {
		this.name = name;
		this.defaultValue = defaultValue;
		this.devChild = devChild;
	}

	Preference(final String name, final Object defaultValue) {
		this(name, defaultValue, false);
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

	public boolean isDevChild() {
		return this.devChild;
	}

	@Override
	public String toString() {
		return this.getName();
	}
}
