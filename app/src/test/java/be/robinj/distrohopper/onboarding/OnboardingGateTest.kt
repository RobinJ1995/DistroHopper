package be.robinj.distrohopper.onboarding

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import be.robinj.distrohopper.preferences.PreferencesRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingGateTest {
    private lateinit var application: Application
    private lateinit var prefs: PreferencesRepository

    @Before fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        application.getSharedPreferences(Preferences.PREFERENCES, 0).edit().clear().commit()
        prefs = PreferencesRepository(application)
    }

    @Test fun shownOnFreshInstall() {
        assertTrue(OnboardingGate.shouldShow(prefs))
    }

    @Test fun notShownOnceCompleted() {
        OnboardingGate.markCompleted(prefs)

        assertFalse(OnboardingGate.shouldShow(prefs))
    }

    @Test fun existingUsersWithAThemeAreGrandfatheredIn() {
        application.getSharedPreferences(Preferences.PREFERENCES, 0).edit()
            .putString(Preference.THEME.getName(), "gnome").commit()

        assertFalse(OnboardingGate.shouldShow(prefs))
        // ... and the flag is backfilled so the theme heuristic is only needed once //
        assertTrue(prefs.getBoolean(Preference.SETUP_COMPLETED, false))
    }

    @Test fun resetShowsTheWizardAgainDespiteAnExistingTheme() {
        OnboardingGate.markStarted(prefs)
        OnboardingGate.markCompleted(prefs)
        application.getSharedPreferences(Preferences.PREFERENCES, 0).edit()
            .putString(Preference.THEME.getName(), "gnome").commit()

        OnboardingGate.reset(prefs)

        assertTrue(OnboardingGate.shouldShow(prefs))
    }

    @Test fun aThemeChosenInAnInterruptedWizardRunIsNotGrandfathered() {
        // As when the app is killed after the wizard's theme page but before Done/Skip //
        OnboardingGate.markStarted(prefs)
        application.getSharedPreferences(Preferences.PREFERENCES, 0).edit()
            .putString(Preference.THEME.getName(), "gnome").commit()

        assertTrue(OnboardingGate.shouldShow(prefs))
        assertFalse(prefs.getBoolean(Preference.SETUP_COMPLETED, false))
        assertTrue(prefs.getBoolean(Preference.DEFAULT_PINS_PENDING, false))
    }

    @Test fun firstWizardRunArmsDefaultPinsOnlyOnce() {
        OnboardingGate.markStarted(prefs)
        assertTrue(prefs.getBoolean(Preference.DEFAULT_PINS_PENDING, false))

        prefs.edit { remove(Preference.DEFAULT_PINS_PENDING.getName()) }
        OnboardingGate.markStarted(prefs)

        assertFalse(prefs.getBoolean(Preference.DEFAULT_PINS_PENDING, false))
    }

    @Test fun developerResetClearsAndDoesNotRearmDefaultPins() {
        OnboardingGate.markStarted(prefs)
        OnboardingGate.markCompleted(prefs)
        OnboardingGate.reset(prefs)

        assertFalse(prefs.getBoolean(Preference.DEFAULT_PINS_PENDING, false))
        OnboardingGate.markStarted(prefs)
        assertFalse(prefs.getBoolean(Preference.DEFAULT_PINS_PENDING, false))
    }

    @Test fun upgradedInstallDoesNotArmDefaultPins() {
        prefs.edit { putBoolean(Preference.DEFAULT_PINS_AUTO_INELIGIBLE.getName(), true) }
        OnboardingGate.markStarted(prefs)

        assertTrue(prefs.getBoolean(Preference.SETUP_STARTED, false))
        assertFalse(prefs.getBoolean(Preference.DEFAULT_PINS_PENDING, false))
    }
}
