package be.robinj.distrohopper.preferences

import android.app.Application
import android.content.Context
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowPowerManager

/**
 * The animations setting: how a stored value maps to a mode, and how each mode
 * resolves against the device's battery-saver state.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class AnimationModeTest {
	private lateinit var application: Application

	@Before fun setUp() {
		this.application = ApplicationProvider.getApplicationContext()
		Preferences.getSharedPreferences(this.application).edit().clear().commit()
	}

	private fun setPowerSaveMode(enabled: Boolean) {
		val powerManager =
			this.application.getSystemService(Context.POWER_SERVICE) as PowerManager
		Shadow.extract<ShadowPowerManager>(powerManager).setIsPowerSaveMode(enabled)
	}

	private fun store(mode: AnimationMode) {
		Preferences.getSharedPreferences(this.application).edit()
			.putString(Preference.ANIMATIONS.getName(), mode.value).commit()
	}

	@Test fun unsetResolvesToUnlessPowerSaving() {
		assertEquals(AnimationMode.UNLESS_POWER_SAVING, AnimationMode.of(null))
		assertEquals(AnimationMode.UNLESS_POWER_SAVING,
			AnimationMode.current(Preferences.getSharedPreferences(this.application)))
	}

	@Test fun anUnrecognisedValueResolvesToUnlessPowerSaving() {
		assertEquals(AnimationMode.UNLESS_POWER_SAVING, AnimationMode.of("sometimes"))
	}

	@Test fun everyValueRoundTripsThroughTheStoredString() {
		for (mode in AnimationMode.entries) {
			this.store(mode)

			assertEquals(mode,
				AnimationMode.current(Preferences.getSharedPreferences(this.application)))
		}
	}

	@Test fun theDefaultFollowsBatterySaver() {
		this.setPowerSaveMode(false)
		assertTrue(AnimationMode.animationsEnabled(this.application))

		this.setPowerSaveMode(true)
		assertFalse(AnimationMode.animationsEnabled(this.application))
	}

	@Test fun alwaysKeepsAnimationsInBatterySaver() {
		this.store(AnimationMode.ALWAYS)

		this.setPowerSaveMode(true)

		assertTrue(AnimationMode.animationsEnabled(this.application))
	}

	@Test fun offDisablesAnimationsOutsideBatterySaver() {
		this.store(AnimationMode.OFF)

		this.setPowerSaveMode(false)

		assertFalse(AnimationMode.animationsEnabled(this.application))
	}

	/** Read fresh, so a settings change lands without recreating the launcher. */
	@Test fun aChangedSettingTakesEffectImmediately() {
		this.setPowerSaveMode(true)
		assertFalse(AnimationMode.animationsEnabled(this.application))

		this.store(AnimationMode.ALWAYS)

		assertTrue(AnimationMode.animationsEnabled(this.application))
	}
}
