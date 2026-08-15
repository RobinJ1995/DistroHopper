package be.robinj.distrohopper.preferences

import android.app.Application
import androidx.preference.ListPreference
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.R
import be.robinj.distrohopper.theme.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowSettings

/**
 * The floating launcher's option in the Gestures settings: it cannot be
 * switched on without the draw-over-other-apps permission (and unticks itself
 * if that is later withdrawn), and its zone options are named after the edge
 * the launcher is actually docked on.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class PreferencesLauncherServiceOptionTest {
	private lateinit var application: Application

	@Before fun setUp() {
		this.application = ApplicationProvider.getApplicationContext()
		Preferences.getSharedPreferences(this.application).edit().clear().commit()
	}

	private fun fragment(activity: PreferencesActivity): PreferencesActivity.PreferencesFragment =
		activity.supportFragmentManager.findFragmentById(R.id.preferences_container)
			as PreferencesActivity.PreferencesFragment

	private fun toggle(activity: PreferencesActivity): SwitchPreferenceCompat =
		this.fragment(activity).findPreference(Preference.LAUNCHER_SERVICE_ENABLED.getName())!!

	private fun zone(activity: PreferencesActivity): ListPreference =
		this.fragment(activity).findPreference(Preference.LAUNCHER_SERVICE_ZONE.getName())!!

	@Test fun switchingItOnWithoutTheOverlayPermissionDoesNotStick() {
		ShadowSettings.setCanDrawOverlays(false)

		ActivityScenario.launch(PreferencesActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				val toggle = this.toggle(activity)

				// What a tap does: the change goes through the listener first //
				assertFalse(toggle.callChangeListener(true))
				assertFalse(toggle.isChecked)
			}
		}
	}

	@Test fun switchingItOnWithTheOverlayPermissionIsAccepted() {
		ShadowSettings.setCanDrawOverlays(true)

		ActivityScenario.launch(PreferencesActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				assertTrue(this.toggle(activity).callChangeListener(true))
			}
		}
	}

	@Test fun aWithdrawnOverlayPermissionUnticksIt() {
		Preferences.getSharedPreferences(this.application).edit()
			.putBoolean(Preference.LAUNCHER_SERVICE_ENABLED.getName(), true)
			.commit()
		ShadowSettings.setCanDrawOverlays(false)

		ActivityScenario.launch(PreferencesActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				assertFalse(this.toggle(activity).isChecked)
			}
		}

		assertFalse(Preferences.getSharedPreferences(this.application)
			.getBoolean(Preference.LAUNCHER_SERVICE_ENABLED.getName(), true))
	}

	@Test fun aSideLauncherNamesItsZonesTopToBottom() {
		Preferences.getSharedPreferences(this.application).edit()
			.putInt(Preference.LAUNCHER_EDGE.getName(), Location.LEFT.n)
			.commit()

		ActivityScenario.launch(PreferencesActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				assertEquals(this.application.getString(R.string.launcherservice_zone_top),
					this.zone(activity).entries[1].toString())
			}
		}
	}

	@Test fun aTopOrBottomLauncherNamesItsZonesLeftToRight() {
		Preferences.getSharedPreferences(this.application).edit()
			.putInt(Preference.LAUNCHER_EDGE.getName(), Location.BOTTOM.n)
			.commit()

		ActivityScenario.launch(PreferencesActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				assertEquals(this.application.getString(R.string.launcherservice_zone_left),
					this.zone(activity).entries[1].toString())
			}
		}
	}

	/** Whatever the edge, the values behind the labels are the stored ones. */
	@Test fun theZoneValuesAreTheOnesLauncherServiceZoneMaps() {
		ActivityScenario.launch(PreferencesActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				val zone = this.zone(activity)

				assertEquals(zone.entries.size, zone.entryValues.size)
				assertEquals(LauncherServiceZone.entries.map { it.value },
					zone.entryValues.map { it.toString() })
			}
		}
	}
}
