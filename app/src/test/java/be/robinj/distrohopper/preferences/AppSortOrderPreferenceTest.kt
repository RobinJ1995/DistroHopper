package be.robinj.distrohopper.preferences

import android.app.Application
import androidx.preference.ListPreference
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.R
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

/**
 * The app-sorting-order preference summary shows both the hint and the
 * currently selected order (the ListPreference substitutes its `%s` with the
 * selected entry). Guards against the summary being hidden again behind a
 * SimpleSummaryProvider or the `%s` being dropped from the hint string.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class AppSortOrderPreferenceTest {
	private lateinit var application: Application

	@Before fun setUp() {
		this.application = ApplicationProvider.getApplicationContext()
		Preferences.getSharedPreferences(this.application).edit().clear().commit()
	}

	private fun sortOrderSummary(): String {
		var summary = ""
		ActivityScenario.launch(PreferencesActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				val fragment = activity.supportFragmentManager
					.findFragmentById(R.id.preferences_container)
					as PreferencesActivity.PreferencesFragment
				summary = fragment.findPreference<ListPreference>(
					Preference.APP_SORT_ORDER.getName())!!.summary.toString()
			}
		}

		return summary
	}

	@Test fun summaryShowsBothTheHintAndTheSelectedOrder() {
		Preferences.getSharedPreferences(this.application).edit()
			.putString(Preference.APP_SORT_ORDER.getName(), "most_used").commit()

		val summary = this.sortOrderSummary()

		assertTrue(summary, summary.contains("Choose how apps are ordered"))
		assertTrue(summary, summary.contains(
			this.application.getString(R.string.option_app_sort_order_most_used)))
	}

	@Test fun summaryReflectsTheDefaultOrderWhenUnset() {
		val summary = this.sortOrderSummary()

		assertTrue(summary, summary.contains(
			this.application.getString(R.string.option_app_sort_order_alphabetic)))
	}
}
