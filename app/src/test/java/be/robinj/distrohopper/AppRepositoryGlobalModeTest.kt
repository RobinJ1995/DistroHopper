package be.robinj.distrohopper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** AppRepository's global pin behaviour (the GLOBAL launcher pin mode): every desktop collapses onto desktop 0. */
@RunWith(RobolectricTestRunner::class)
class AppRepositoryGlobalModeTest {
	private val context: Context = ApplicationProvider.getApplicationContext()
	private val repository = AppRepository(this.context)

	private fun app(packageName: String): App =
		App(this.context, null,
			ActivityTestSupport.resolveInfo(packageName, packageName.uppercase(), packageName))

	@Before fun setUp() {
		Preferences.getSharedPreferences(this.context, Preferences.PINNED_APPS).edit().clear().commit()
		Preferences.getSharedPreferences(this.context).edit()
			.putString(Preference.LAUNCHER_APP_PIN_MODE.getName(), "global").commit()
		this.repository.loadPinnedApps() // perDesktop = false, empty //
	}

	@Test fun pinsAreSharedAcrossEveryDesktop() {
		val a = this.app("a")
		val b = this.app("b")

		this.repository.pin(a, 0)
		this.repository.pin(b, 1) // collapses onto the same page as desktop 0 //

		assertEquals(listOf(a, b), this.repository.pinnedOn(0))
		assertEquals(listOf(a, b), this.repository.pinnedOn(1))
		assertEquals(listOf(a, b), this.repository.pinnedOn(5))
	}

	@Test fun unpinFromAnyDesktopAffectsTheSharedList() {
		val a = this.app("a")
		this.repository.pin(a, 0)

		this.repository.unpin(a, 3) // still the shared page //

		assertFalse(this.repository.isPinnedOn(a, 0))
		assertFalse(this.repository.isPinnedOn(a, 3))
	}

	@Test fun highestPinnedDesktopIsAlwaysAbsentInGlobalMode() {
		val a = this.app("a")
		this.repository.pin(a, 4)

		assertEquals(-1, this.repository.highestPinnedDesktop())
	}

	@Test fun removingADesktopLeavesTheSharedPinsUntouched() {
		val a = this.app("a")
		val b = this.app("b")
		this.repository.pin(a, 0)
		this.repository.pin(b, 1)

		this.repository.removePinnedDesktop(0) // no-op: pins aren't desktop-bound //

		assertEquals(listOf(a, b), this.repository.pinnedOn(0))
	}

	@Test fun noDesktopOpsAlwaysTargetTheSharedPage() {
		val a = this.app("a")
		this.repository.setCurrentDesktop(3)

		this.repository.pin(a) // no-desktop overload //

		assertTrue(this.repository.isPinnedOn(a, 0))
		assertTrue(this.repository.isPinnedOn(a, 3))
		assertEquals(listOf(a), this.repository.pinned.value)
	}

	@Test fun savePersistsTheFlatGlobalFormat() {
		val a = this.app("a")
		val b = this.app("b")
		this.repository.pin(a, 0)
		this.repository.pin(b, 2)

		this.repository.savePinnedApps()

		val prefs = Preferences.getSharedPreferences(this.context, Preferences.PINNED_APPS)
		assertEquals(
			listOf(listOf(a.packageAndActivityName, b.packageAndActivityName)),
			PinnedAppsStorage.read(prefs))
	}
}
