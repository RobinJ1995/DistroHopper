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

		if (prefs.getString(Preference.THEME, null) != null) {
			markCompleted(prefs)

			return false
		}

		return true
	}

	@JvmStatic
	fun markCompleted(prefs: PreferencesRepository) {
		prefs.edit { putBoolean(Preference.SETUP_COMPLETED.getName(), true) }
	}
}
