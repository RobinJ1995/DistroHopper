package be.robinj.distrohopper.broadcast

import android.app.Application
import android.content.Intent
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
class AppUpgradeReceiverTest {
	private lateinit var application: Application

	@Before fun setUp() {
		this.application = ApplicationProvider.getApplicationContext()
		Preferences.getSharedPreferences(this.application).edit().clear().commit()
	}

	@Test fun packageReplacementDisablesAutomaticDefaultPinsAndClearsPendingWork() {
		Preferences.getSharedPreferences(this.application).edit()
			.putBoolean(Preference.DEFAULT_PINS_PENDING.getName(), true)
			.commit()

		AppUpgradeReceiver().onReceive(
			this.application,
			Intent(Intent.ACTION_MY_PACKAGE_REPLACED),
		)

		val prefs = Preferences.getSharedPreferences(this.application)
		assertTrue(prefs.getBoolean(Preference.DEFAULT_PINS_AUTO_INELIGIBLE.getName(), false))
		assertFalse(prefs.getBoolean(Preference.DEFAULT_PINS_PENDING.getName(), false))
	}

	@Test fun unrelatedBroadcastDoesNothing() {
		AppUpgradeReceiver().onReceive(this.application, Intent(Intent.ACTION_BOOT_COMPLETED))

		val prefs = Preferences.getSharedPreferences(this.application)
		assertFalse(prefs.getBoolean(Preference.DEFAULT_PINS_AUTO_INELIGIBLE.getName(), false))
	}
}
