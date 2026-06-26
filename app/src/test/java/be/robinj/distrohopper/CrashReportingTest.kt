package be.robinj.distrohopper

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CrashReportingTest {
    private lateinit var prefs: SharedPreferences

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        prefs = context.getSharedPreferences(Preferences.PREFERENCES, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    private fun setReportsEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(Preference.CRASH_REPORTING_ENABLED.getName(), enabled).commit()

    @Test fun disabledWhenCredentialsAbsentEvenIfUserOptedIn() {
        setReportsEnabled(true)
        assertFalse(CrashReporting.isEnabled(false, prefs))
    }

    @Test fun disabledWhenCredentialsAbsentAndPreferenceUnset() {
        assertFalse(CrashReporting.isEnabled(false, prefs))
    }

    @Test fun enabledWhenConfiguredAndUserOptedIn() {
        setReportsEnabled(true)
        assertTrue(CrashReporting.isEnabled(true, prefs))
    }

    @Test fun disabledWhenConfiguredButUserOptedOut() {
        setReportsEnabled(false)
        assertFalse(CrashReporting.isEnabled(true, prefs))
    }

    @Test fun enabledByDefaultWhenConfiguredAndPreferenceUnset() {
        // The preference defaults to on, so a fresh configured install reports. //
        assertTrue(CrashReporting.isEnabled(true, prefs))
    }
}
