package be.robinj.distrohopper

import android.content.SharedPreferences
import be.robinj.distrohopper.preferences.Preference

/**
 * Central decision logic for ACRA crash reporting.
 *
 * Reporting is only active when credentials were baked in at build time
 * ([BuildConfig.ACRA_CONFIGURED]) AND the user has not opted out. The user
 * preference defaults to on, so existing installs keep reporting unless the
 * option is unticked. When no credentials were provided at build time, reporting
 * is forced off regardless of the (hidden) preference.
 */
object CrashReporting {
    @JvmStatic
    fun isEnabled(acraConfigured: Boolean, prefs: SharedPreferences): Boolean =
        acraConfigured && prefs.getBoolean(Preference.CRASH_REPORTING_ENABLED.getName(), true)
}
