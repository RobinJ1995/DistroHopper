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

/** AppRepository's per-desktop pin behaviour (the DESKTOP launcher pin mode). */
@RunWith(RobolectricTestRunner::class)
class AppRepositoryPerDesktopTest {
	private val context: Context = ApplicationProvider.getApplicationContext()
	private val repository = AppRepository(this.context)

	private fun app(packageName: String): App =
		App(this.context, null,
			ActivityTestSupport.resolveInfo(packageName, packageName.uppercase(), packageName))

	@Before fun setUp() {
		Preferences.getSharedPreferences(this.context, Preferences.PINNED_APPS).edit().clear().commit()
		Preferences.getSharedPreferences(this.context).edit()
			.putString(Preference.LAUNCHER_APP_PIN_MODE.getName(), "desktop").commit()
		this.repository.loadPinnedApps() // perDesktop = true, empty //
	}

	@Test fun pinsAreKeptPerDesktop() {
		val a = this.app("a")
		val b = this.app("b")

		this.repository.pin(a, 0)
		this.repository.pin(b, 1)

		assertEquals(listOf(a), this.repository.pinnedOn(0))
		assertEquals(listOf(b), this.repository.pinnedOn(1))
		assertEquals(1, this.repository.highestPinnedDesktop())
	}

	@Test fun theSameAppCanBePinnedOnSeveralDesktops() {
		val a = this.app("a")

		this.repository.pin(a, 0)
		this.repository.pin(a, 2)

		assertTrue(this.repository.isPinnedOn(a, 0))
		assertFalse(this.repository.isPinnedOn(a, 1))
		assertTrue(this.repository.isPinnedOn(a, 2))
	}

	@Test fun unpinAffectsOnlyTheGivenDesktop() {
		val a = this.app("a")
		this.repository.pin(a, 0)
		this.repository.pin(a, 1)

		this.repository.unpin(a, 0)

		assertFalse(this.repository.isPinnedOn(a, 0))
		assertTrue(this.repository.isPinnedOn(a, 1))
	}

	@Test fun removingADesktopShiftsHigherOnesDown() {
		val a = this.app("a")
		val b = this.app("b")
		this.repository.pin(a, 0)
		this.repository.pin(b, 1)

		this.repository.removePinnedDesktop(0)

		assertEquals(listOf(b), this.repository.pinnedOn(0))
		assertEquals(0, this.repository.highestPinnedDesktop())
	}

	@Test fun noDesktopOpsTargetTheCurrentDesktop() {
		val a = this.app("a")
		this.repository.setCurrentDesktop(1)

		this.repository.pin(a) // no-desktop overload //

		assertTrue(this.repository.isPinnedOn(a, 1))
		assertFalse(this.repository.isPinnedOn(a, 0))
		assertEquals(listOf(a), this.repository.pinned.value) // snapshot of the current desktop //
	}

	@Test fun savePersistsThePerDesktopFormat() {
		val a = this.app("a")
		val b = this.app("b")
		this.repository.pin(a, 0)
		this.repository.pin(b, 1)

		this.repository.savePinnedApps()

		val prefs = Preferences.getSharedPreferences(this.context, Preferences.PINNED_APPS)
		assertEquals(
			listOf(listOf(a.packageAndActivityName), listOf(b.packageAndActivityName)),
			PinnedAppsStorage.read(prefs))
	}
}
