package be.robinj.distrohopper.preferences;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {
	public static final String PREFERENCES = "prefs";
	public static final String PINNED_APPS = "pinned";
	public static final String LENSES = "lenses";

	public static SharedPreferences getSharedPreferences(final Context context) {
		return Preferences.getSharedPreferences(context, Preferences.PREFERENCES);
	}

	public static SharedPreferences getSharedPreferences(final Context context, final String file) {
		return context.getSharedPreferences(file, Context.MODE_PRIVATE);
	}
}
