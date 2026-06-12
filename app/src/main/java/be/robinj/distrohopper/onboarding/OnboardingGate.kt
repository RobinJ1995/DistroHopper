package be.robinj.distrohopper.onboarding

import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.PreferencesRepository

/**
 * Decides whether the first-run wizard ([OnboardingActivity]) should be shown.
 * Users who customised the app before the wizard existed (a theme preference
 * is present) are grandfathered in without seeing it.
 */
object OnboardingGate {
	@JvmStatic
	fun shouldShow(prefs: PreferencesRepository): Boolean {
		if (prefs.getBoolean(Preference.SETUP_COMPLETED, false)) {
			return false
		}

		// A theme preference can only identify a pre-wizard install if the
		// wizard never ran: the wizard's own theme page writes it too, and a
		// user killed mid-wizard must still get the remaining pages. //
		if (prefs.getString(Preference.THEME, null) != null
			&& !prefs.getBoolean(Preference.SETUP_STARTED, false)) {
			markCompleted(prefs)

			return false
		}

		return true
	}

	/** Recorded by the wizard before it writes anything else (see [shouldShow]). */
	@JvmStatic
	fun markStarted(prefs: PreferencesRepository) {
		prefs.edit { putBoolean(Preference.SETUP_STARTED.getName(), true) }
	}

	@JvmStatic
	fun markCompleted(prefs: PreferencesRepository) {
		prefs.edit { putBoolean(Preference.SETUP_COMPLETED.getName(), true) }
	}
}
