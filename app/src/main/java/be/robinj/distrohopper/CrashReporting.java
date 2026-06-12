package be.robinj.distrohopper;

import android.content.SharedPreferences;

import be.robinj.distrohopper.preferences.Preference;

/**
 * Central decision logic for ACRA crash reporting.
 *
 * Reporting is only active when credentials were baked in at build time
 * ({@code BuildConfig.ACRA_CONFIGURED}) AND the user has not opted out. The user
 * preference defaults to on, so existing installs keep reporting unless the
 * option is unticked. When no credentials were provided at build time, reporting
 * is forced off regardless of the (hidden) preference.
 */
public final class CrashReporting {
	private CrashReporting() {}

	public static boolean isEnabled(final boolean acraConfigured, final SharedPreferences prefs) {
		return acraConfigured
			&& prefs.getBoolean(Preference.CRASH_REPORTS_ENABLED.getName(), true);
	}
}
