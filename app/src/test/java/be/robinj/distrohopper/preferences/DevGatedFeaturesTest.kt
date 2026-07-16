package be.robinj.distrohopper.preferences

import android.app.Application
import android.widget.LinearLayout
import androidx.preference.ListPreference
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.R
import be.robinj.distrohopper.theme.Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

/**
 * Features gated behind developer mode until they are ready for the general
 * public: the dev-only themes (MATE, COSMIC, Budgie) and the custom
 * (drag-to-reorder) dash sort order.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class DevGatedFeaturesTest {
	private lateinit var application: Application

	@Before fun setUp() {
		this.application = ApplicationProvider.getApplicationContext()
		Preferences.getSharedPreferences(this.application).edit().clear().commit()
	}

	private fun setDev(enabled: Boolean) =
		Preferences.getSharedPreferences(this.application).edit()
			.putBoolean(Preference.DEV.getName(), enabled).commit()

	private fun sortOrderValues(): List<String> {
		var values = listOf<String>()
		ActivityScenario.launch(PreferencesActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				val fragment = activity.supportFragmentManager
					.findFragmentById(R.id.preferences_container)
					as PreferencesActivity.PreferencesFragment
				values = fragment.findPreference<ListPreference>(
					Preference.APP_SORT_ORDER.getName())!!.entryValues.map { it.toString() }
			}
		}

		return values
	}

	private fun themeCardNames(): List<String> {
		var names = listOf<String>()
		ActivityScenario.launch(ThemePreferencesActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				val container = activity.findViewById<LinearLayout>(R.id.llThemeCards)
				names = (0 until container.childCount)
					.map { (container.getChildAt(it).tag as Theme).getName() }
			}
		}

		return names
	}

	@Test fun customSortOrderIsOfferedOnlyInDeveloperMode() {
		this.setDev(false)
		assertFalse(this.sortOrderValues().contains(AppSortOrder.CUSTOM.value))

		this.setDev(true)
		assertTrue(this.sortOrderValues().contains(AppSortOrder.CUSTOM.value))
	}

	@Test fun storedCustomSortOrderFallsBackWhenDeveloperModeIsOff() {
		this.setDev(false)
		Preferences.getSharedPreferences(this.application).edit()
			.putString(Preference.APP_SORT_ORDER.getName(), AppSortOrder.CUSTOM.value).commit()

		this.sortOrderValues() // Opening the preferences screen applies the fallback //

		assertEquals(AppSortOrder.ALPHABETICAL,
			AppSortOrder.current(Preferences.getSharedPreferences(this.application)))
	}

	@Test fun disablingDeveloperModeRemovesTheCustomSortOrderOption() {
		this.setDev(true)
		Preferences.getSharedPreferences(this.application).edit()
			.putString(Preference.APP_SORT_ORDER.getName(), AppSortOrder.CUSTOM.value).commit()

		ActivityScenario.launch(PreferencesActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				val fragment = activity.supportFragmentManager
					.findFragmentById(R.id.preferences_container)
					as PreferencesActivity.PreferencesFragment
				val sortPref = fragment.findPreference<ListPreference>(
					Preference.APP_SORT_ORDER.getName())!!
				assertTrue(sortPref.entryValues
					.map { it.toString() }.contains(AppSortOrder.CUSTOM.value))

				fragment.findPreference<SwitchPreferenceCompat>(Preference.DEV.getName())!!
					.performClick()

				assertFalse(sortPref.entryValues
					.map { it.toString() }.contains(AppSortOrder.CUSTOM.value))
				assertEquals(AppSortOrder.ALPHABETICAL.value, sortPref.value)
			}
		}
	}

	@Test fun themePickerHidesDevOnlyThemesWithoutDeveloperMode() {
		this.setDev(false)
		val names = this.themeCardNames()

		assertTrue(names.contains("default"))
		assertFalse(names.contains("mate"))
		assertFalse(names.contains("cosmic"))
		assertFalse(names.contains("budgie"))
	}

	@Test fun themePickerOffersDevOnlyThemesInDeveloperMode() {
		this.setDev(true)

		assertTrue(this.themeCardNames()
			.containsAll(listOf("default", "mate", "cosmic", "budgie")))
	}
}
