package be.robinj.distrohopper.desktop.launcher.service

import android.app.Application
import android.os.Parcel
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowSettings

/**
 * The floating launcher's gating: it is opt-in, it needs the overlay
 * permission on top of that, and it is never started when either is missing.
 */
@RunWith(RobolectricTestRunner::class)
class LauncherServiceTest {
	private lateinit var application: Application

	@Before fun setUp() {
		this.application = ApplicationProvider.getApplicationContext()
		Preferences.getSharedPreferences(this.application).edit().clear().commit()
		ShadowSettings.setCanDrawOverlays(true)
		Shadows.shadowOf(this.application).clearStartedServices()
	}

	private fun enable(enabled: Boolean) {
		Preferences.getSharedPreferences(this.application).edit()
			.putBoolean(Preference.LAUNCHER_SERVICE_ENABLED.getName(), enabled)
			.commit()
	}

	@Test fun itIsOffUntilItIsSwitchedOn() {
		assertFalse(LauncherService.isEnabled(this.application))

		this.enable(true)

		assertTrue(LauncherService.isEnabled(this.application))
	}

	@Test fun beingSwitchedOnIsNotEnoughWithoutTheOverlayPermission() {
		this.enable(true)
		ShadowSettings.setCanDrawOverlays(false)

		assertTrue(LauncherService.isEnabled(this.application))
		assertFalse(LauncherService.isAvailable(this.application))
	}

	@Test fun syncStartsNothingWhileItIsSwitchedOff() {
		LauncherService.sync(this.application, ArrayList(), false)

		assertNull(Shadows.shadowOf(this.application).nextStartedService)
	}

	@Test fun syncStartsNothingWithoutTheOverlayPermission() {
		this.enable(true)
		ShadowSettings.setCanDrawOverlays(false)

		LauncherService.sync(this.application, ArrayList(), false)

		assertNull(Shadows.shadowOf(this.application).nextStartedService)
	}

	@Test fun syncStartsTheServiceWithThePinsAndTheHomeScreenState() {
		this.enable(true)

		val items = arrayListOf(
			FloatingLauncherItem("be.robinj.app", "be.robinj.app.Main", "App", null, -1L))
		LauncherService.sync(this.application, items, false)

		val started = Shadows.shadowOf(this.application).nextStartedService
		assertEquals(LauncherService::class.java.name, started.component?.className)
		assertFalse(started.getBooleanExtra(LauncherService.EXTRA_HOME_FOREGROUND, true))
		assertEquals(1,
			started.getParcelableArrayListExtra<FloatingLauncherItem>(
				LauncherService.EXTRA_ITEMS)?.size)
	}
}

/** The identity a pinned app keeps on its way to the service. */
@RunWith(RobolectricTestRunner::class)
class FloatingLauncherItemTest {
	@Test fun aPersonalAppIsKeyedLikeTheHomeScreenCachesIt() {
		val item = FloatingLauncherItem("be.robinj.app", "be.robinj.app.Main", "App", null, -1L)

		assertEquals("be.robinj.app\nbe.robinj.app.Main", item.key)
	}

	/** Another profile's copy of the same app is a different app; the serial keeps them apart. */
	@Test fun anotherProfilesCopyIsKeyedByItsProfileSerial() {
		val item = FloatingLauncherItem(
			"be.robinj.app", "be.robinj.app.Main", "App", android.os.Process.myUserHandle(), 11L)

		assertEquals("be.robinj.app\nbe.robinj.app.Main\n11", item.key)
	}

	@Test fun itSurvivesTheTripThroughAnIntent() {
		val item = FloatingLauncherItem("be.robinj.app", "be.robinj.app.Main", "App", null, -1L)

		val parcel = Parcel.obtain()
		item.writeToParcel(parcel, 0)
		parcel.setDataPosition(0)
		val restored = FloatingLauncherItem.CREATOR.createFromParcel(parcel)
		parcel.recycle()

		assertEquals(item.packageName, restored.packageName)
		assertEquals(item.activityName, restored.activityName)
		assertEquals(item.label, restored.label)
		assertEquals(item.key, restored.key)
	}
}
