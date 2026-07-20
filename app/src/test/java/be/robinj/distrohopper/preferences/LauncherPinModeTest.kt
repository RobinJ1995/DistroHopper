package be.robinj.distrohopper.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LauncherPinModeTest {
	private val context: Context = ApplicationProvider.getApplicationContext()

	@Test fun ofMapsTheKnownValues() {
		assertEquals(LauncherPinMode.DESKTOP, LauncherPinMode.of("desktop"))
		assertEquals(LauncherPinMode.GLOBAL, LauncherPinMode.of("global"))
	}

	@Test fun ofFallsBackToGlobalForUnknownOrNull() {
		assertEquals(LauncherPinMode.GLOBAL, LauncherPinMode.of(null))
		assertEquals(LauncherPinMode.GLOBAL, LauncherPinMode.of("garbage"))
		assertEquals(LauncherPinMode.GLOBAL, LauncherPinMode.of(""))
	}

	@Test fun currentDefaultsToGlobalWhenUnset() {
		val prefs = Preferences.getSharedPreferences(this.context)
		prefs.edit().remove(Preference.LAUNCHER_APP_PIN_MODE.getName()).commit()

		assertEquals(LauncherPinMode.GLOBAL, LauncherPinMode.current(prefs))
	}

	@Test fun currentReadsAnExplicitDesktopValue() {
		val prefs = Preferences.getSharedPreferences(this.context)
		prefs.edit().putString(Preference.LAUNCHER_APP_PIN_MODE.getName(), "desktop").commit()

		assertEquals(LauncherPinMode.DESKTOP, LauncherPinMode.current(prefs))
	}
}
