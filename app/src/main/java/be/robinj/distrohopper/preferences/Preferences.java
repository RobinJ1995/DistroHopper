package be.robinj.distrohopper.preferences;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {
	public static final String PREFERENCES = "prefs";
	public static final String PINNED_APPS = "pinned";
	public static final String LENSES = "lenses";
	public static final String APP_USAGE = "app_usage";
	public static final String DASH_LAYOUT = "dash_layout";
	public static final String LAUNCHER_LAYOUT = "launcher_layout";
	// The desktop's widgets, pinned apps and folders share one file (see DesktopLayoutStorage).
	public static final String DESKTOP_LAYOUT = "desktop_layout";

	/**
	 * The preferences file belonging to one lens, named after its
	 * {@code Lens.getKey()}. Each lens owns its own file rather than sharing the
	 * main "prefs" one, so a lens can persist user-specific state (the folders
	 * the Local files lens may search, for instance) without it landing in
	 * anything scoped to "prefs" — crash reports included.
	 */
	public static String forLens(final String lensKey) {
		return "lens_" + lensKey + "_prefs";
	}

	public static SharedPreferences getSharedPreferences(final Context context) {
		return Preferences.getSharedPreferences(context, Preferences.PREFERENCES);
	}

	public static SharedPreferences getSharedPreferences(final Context context, final String file) {
		return context.getSharedPreferences(file, Context.MODE_PRIVATE);
	}
}
