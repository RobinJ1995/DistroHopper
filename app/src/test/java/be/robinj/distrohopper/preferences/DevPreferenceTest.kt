package be.robinj.distrohopper.preferences

import android.app.Application
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.R
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class DevPreferenceTest {
	private lateinit var application: Application

	@Before fun setUp() {
		this.application = ApplicationProvider.getApplicationContext()
		Preferences.getSharedPreferences(this.application).edit()
			.clear()
			.putBoolean(Preference.DEV.getName(), true)
			.putBoolean(Preference.DEV_LOG_TOASTER.getName(), true)
			.putBoolean(Preference.DEV_WIDGET_RESIZE_ANY.getName(), true)
			.commit()
	}

	@Test fun disablingDeveloperModeClearsDeveloperToggles() {
		ActivityScenario.launch(PreferencesActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				val fragment = activity.supportFragmentManager
					.findFragmentById(R.id.preferences_container)
					as PreferencesActivity.PreferencesFragment
				val dev = fragment.findPreference<SwitchPreferenceCompat>(Preference.DEV.getName())!!

				dev.performClick()
			}
		}

		val prefs = Preferences.getSharedPreferences(this.application)
		assertFalse(prefs.getBoolean(Preference.DEV.getName(), true))
		assertFalse(prefs.getBoolean(Preference.DEV_LOG_TOASTER.getName(), false))
		assertFalse(prefs.getBoolean(Preference.DEV_WIDGET_RESIZE_ANY.getName(), false))
	}
}
