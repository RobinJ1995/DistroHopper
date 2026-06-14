package be.robinj.distrohopper.preferences

import android.app.Application
import android.graphics.drawable.ColorDrawable
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.R
import be.robinj.distrohopper.cache.AppIconCache
import be.robinj.distrohopper.cache.AppLabelCache
import be.robinj.distrohopper.cache.ExpiringCache
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
			.putBoolean(Preference.DEV_SHOW_GRID_ON_DRAG.getName(), true)
			.commit()
	}

	@Test fun disablingDeveloperModeClearsDeveloperToggles() {
		ActivityScenario.launch(PreferencesActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				val fragment = activity.supportFragmentManager
					.findFragmentById(R.id.preferences_container)
					as PreferencesActivity.PreferencesFragment
				val dev = fragment.findPreference<SwitchPreferenceCompat>(Preference.DEV.getName())!!
				val logToaster = fragment.findPreference<SwitchPreferenceCompat>(
					Preference.DEV_LOG_TOASTER.getName())!!
				val widgetResize = fragment.findPreference<SwitchPreferenceCompat>(
					Preference.DEV_WIDGET_RESIZE_ANY.getName())!!
				val showGrid = fragment.findPreference<SwitchPreferenceCompat>(
					Preference.DEV_SHOW_GRID_ON_DRAG.getName())!!

				assertTrue(logToaster.isChecked)
				assertTrue(widgetResize.isChecked)
				assertTrue(showGrid.isChecked)

				dev.performClick()

				assertFalse(logToaster.isChecked)
				assertFalse(widgetResize.isChecked)
				assertFalse(showGrid.isChecked)
			}
		}

		val prefs = Preferences.getSharedPreferences(this.application)
		assertFalse(prefs.getBoolean(Preference.DEV.getName(), true))
		assertFalse(prefs.contains(Preference.DEV_LOG_TOASTER.getName()))
		assertFalse(prefs.contains(Preference.DEV_WIDGET_RESIZE_ANY.getName()))
		assertFalse(prefs.contains(Preference.DEV_SHOW_GRID_ON_DRAG.getName()))
	}

	@Test fun openingPreferencesWithDeveloperModeOffClearsStaleDeveloperToggles() {
		Preferences.getSharedPreferences(this.application).edit()
			.putBoolean(Preference.DEV.getName(), false)
			.commit()

		ActivityScenario.launch(PreferencesActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				val fragment = activity.supportFragmentManager
					.findFragmentById(R.id.preferences_container)
					as PreferencesActivity.PreferencesFragment
				val logToaster = fragment.findPreference<SwitchPreferenceCompat>(
					Preference.DEV_LOG_TOASTER.getName())!!
				val widgetResize = fragment.findPreference<SwitchPreferenceCompat>(
					Preference.DEV_WIDGET_RESIZE_ANY.getName())!!
				val showGrid = fragment.findPreference<SwitchPreferenceCompat>(
					Preference.DEV_SHOW_GRID_ON_DRAG.getName())!!

				assertFalse(logToaster.isChecked)
				assertFalse(widgetResize.isChecked)
				assertFalse(showGrid.isChecked)
			}
		}

		val prefs = Preferences.getSharedPreferences(this.application)
		assertFalse(prefs.contains(Preference.DEV_LOG_TOASTER.getName()))
		assertFalse(prefs.contains(Preference.DEV_WIDGET_RESIZE_ANY.getName()))
		assertFalse(prefs.contains(Preference.DEV_SHOW_GRID_ON_DRAG.getName()))
	}

	@Test fun clickingClearCacheClearsLabelIconAndExpirationCaches() {
		val labelCache = AppLabelCache(this.application)
		val iconCache = ExpiringCache(
			this.application,
			AppIconCache(this.application),
			AppIconCache.EXPIRATION,
		)
		labelCache["pingu-label"] = "Noot"
		iconCache["pingu-icon"] = ColorDrawable(0xff00ff)

		ActivityScenario.launch(PreferencesActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				val fragment = activity.supportFragmentManager
					.findFragmentById(R.id.preferences_container)
					as PreferencesActivity.PreferencesFragment

				fragment.findPreference<androidx.preference.Preference>("dummy_clear_cache")!!
					.performClick()
			}
		}

		val freshLabelCache = AppLabelCache(this.application)
		val freshIconCache = ExpiringCache(
			this.application,
			AppIconCache(this.application),
			AppIconCache.EXPIRATION,
		)
		assertFalse(freshLabelCache.containsKey("pingu-label"))
		assertFalse(freshIconCache.containsKey("pingu-icon"))
	}
}
