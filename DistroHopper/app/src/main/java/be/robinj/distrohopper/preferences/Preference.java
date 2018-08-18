package be.robinj.distrohopper.preferences;

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
	WALLPAPER_BLUR_MODE("unitywallpaper_blur"),
	PRIMARY_COLOUR("unitybackground_colour"),
	PRIMARY_COLOUR_OPACITY("unitybackground_opacity"),
	PRIMARY_COLOUR_DYAMIC("unitybackground_dynamic"),
	WIDGETS_ENABLED("widgets_enabled"),
	THEME("theme"),
	DEV("dev"),
	DEV_LOG_TOASTER("dev_log_toaster");

	private final String name;

	Preference(final String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public String toString() {
		return this.getName();
	}
}
