package be.robinj.distrohopper.preferences;

import android.content.SharedPreferences;

public enum Preference {
	PANEL_EDGE("panel_edge_v2"),
	PANEL_OPACITY("panel_opacity"),
	LAUNCHER_EDGE("launcher_edge_v2"),
	LAUNCHER_SHOW_RUNNING_APPS("launcher_running_show"),
	LAUNCHERICON_WIDTH("launchericon_width"),
	LAUNCHERICON_OPACITY("launchericon_opacity"),
	LAUNCHERSERVICE_ENABLED("launcherservice_enabled"),
	DASH_OPEN_ON_READY("dash_ready_show"),
	DASH_SEARCH_FULL("dashsearch_full"),
	DASH_SEARCH_LENSES_MAX_RESULTS("dashsearch_lenses_maxresults"),
	DASHICON_WIDTH("dashicon_width", 24),
	WALLPAPER_BLUR_MODE("unitywallpaper_blur"),
	PRIMARY_COLOUR("unitybackground_colour"),
	PRIMARY_COLOUR_OPACITY("unitybackground_opacity"),
	PRIMARY_COLOUR_DYAMIC("unitybackground_dynamic"),
	WIDGETS_ENABLED("widgets_enabled"),
	THEME("theme"),
	DEV("dev"),
	DEV_LOG_TOASTER("dev_log_toaster");

	private final String name;
	private final Object defaultValue;

	Preference(final String name, final Object defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;
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

	@Override
	public String toString() {
		return this.getName();
	}
}
