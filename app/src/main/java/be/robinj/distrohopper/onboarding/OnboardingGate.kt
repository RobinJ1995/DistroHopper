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
		val firstRun = !prefs.getBoolean(Preference.SETUP_STARTED, false)
			&& !prefs.getBoolean(Preference.SETUP_COMPLETED, false)
		val defaultPinsEligible = !prefs.getBoolean(
			Preference.DEFAULT_PINS_AUTO_INELIGIBLE, false)

		prefs.edit {
			putBoolean(Preference.SETUP_STARTED.getName(), true)
			if (firstRun && defaultPinsEligible) {
				putBoolean(Preference.DEFAULT_PINS_PENDING.getName(), true)
			}
		}
	}

	@JvmStatic
	fun markCompleted(prefs: PreferencesRepository) {
		prefs.edit { putBoolean(Preference.SETUP_COMPLETED.getName(), true) }
	}

	/** Dev tool: make the wizard show again on the next launch. */
	@JvmStatic
	fun reset(prefs: PreferencesRepository) {
		prefs.edit {
			remove(Preference.SETUP_COMPLETED.getName())
			remove(Preference.DEFAULT_PINS_PENDING.getName())
			// Keeps the existing theme preference from re-grandfathering //
			putBoolean(Preference.SETUP_STARTED.getName(), true)
		}
	}
}
