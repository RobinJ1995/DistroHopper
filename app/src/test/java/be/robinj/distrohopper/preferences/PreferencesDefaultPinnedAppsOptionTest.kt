package be.robinj.distrohopper.preferences

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.R
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class PreferencesDefaultPinnedAppsOptionTest {
	private lateinit var application: Application

	@Before fun setUp() {
		this.application = ApplicationProvider.getApplicationContext()
		Preferences.getSharedPreferences(this.application).edit()
			.clear()
			.putBoolean(Preference.DEV.getName(), true)
			.commit()
	}

	@Test fun clickingTheDeveloperOptionQueuesDefaultPins() {
		Preferences.getSharedPreferences(this.application).edit()
			.putBoolean(Preference.DEFAULT_PINS_AUTO_INELIGIBLE.getName(), true)
			.commit()

		ActivityScenario.launch(PreferencesActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				val fragment = activity.supportFragmentManager
					.findFragmentById(R.id.preferences_container)
					as PreferencesActivity.PreferencesFragment

				fragment.findPreference<androidx.preference.Preference>("dummy_pin_default_apps")!!
					.performClick()
			}
		}

		assertTrue(
			Preferences.getSharedPreferences(this.application)
				.getBoolean(Preference.DEFAULT_PINS_PENDING.getName(), false),
		)
	}
}
