package be.robinj.distrohopper.preferences;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {
	public static final String PREFERENCES = "prefs";
	public static final String PINNED_APPS = "pinned";
	public static final String LENSES = "lenses";
	public static final String WIDGETS = "widgets";
	public static final String DESKTOP_APPS = "desktop_apps";
	public static final String APP_USAGE = "app_usage";
	public static final String DASH_LAYOUT = "dash_layout";

	public static SharedPreferences getSharedPreferences(final Context context) {
		return Preferences.getSharedPreferences(context, Preferences.PREFERENCES);
	}

	public static SharedPreferences getSharedPreferences(final Context context, final String file) {
		return context.getSharedPreferences(file, Context.MODE_PRIVATE);
	}
}
