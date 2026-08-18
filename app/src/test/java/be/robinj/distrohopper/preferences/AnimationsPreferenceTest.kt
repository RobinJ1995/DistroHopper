package be.robinj.distrohopper.preferences

import android.app.Application
import androidx.preference.ListPreference
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

/**
 * The animations preference offers exactly the [AnimationMode] values and shows
 * both its hint and the current choice (the ListPreference substitutes its `%s`
 * with the selected entry), like the app-sorting-order one.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class AnimationsPreferenceTest {
	private lateinit var application: Application

	@Before fun setUp() {
		this.application = ApplicationProvider.getApplicationContext()
		Preferences.getSharedPreferences(this.application).edit().clear().commit()
	}

	private fun <T : Any> onAnimationsPreference(block: (ListPreference) -> T): T {
		var result: T? = null
		ActivityScenario.launch(PreferencesActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				val fragment = activity.supportFragmentManager
					.findFragmentById(R.id.preferences_container)
					as PreferencesActivity.PreferencesFragment
				result = block(fragment.findPreference(Preference.ANIMATIONS.getName())!!)
			}
		}

		return result!!
	}

	@Test fun theEntryValuesMirrorTheAnimationModes() {
		val values = this.onAnimationsPreference { it.entryValues.map(CharSequence::toString) }

		assertEquals(AnimationMode.entries.map(AnimationMode::value), values)
	}

	@Test fun summaryShowsBothTheHintAndTheSelectedMode() {
		Preferences.getSharedPreferences(this.application).edit()
			.putString(Preference.ANIMATIONS.getName(), AnimationMode.ALWAYS.value).commit()

		val summary = this.onAnimationsPreference { it.summary.toString() }

		assertTrue(summary, summary.contains("Choose when DistroHopper plays"))
		assertTrue(summary,
			summary.contains(this.application.getString(R.string.option_animations_always)))
	}

	@Test fun summaryReflectsTheDefaultModeWhenUnset() {
		val summary = this.onAnimationsPreference { it.summary.toString() }

		assertTrue(summary, summary.contains(this.application.getString(
			R.string.option_animations_unless_power_saving)))
	}
}
