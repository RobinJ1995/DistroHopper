package be.robinj.distrohopper.home

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.PinnedAppsStorage
import be.robinj.distrohopper.preferences.LauncherPinMode
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PinnedAppsMigrationTest {
	private val context: Context = ApplicationProvider.getApplicationContext()
	private val prefs: SharedPreferences
		get() = Preferences.getSharedPreferences(this.context, Preferences.PINNED_APPS)

	@Before fun setUp() { this.prefs.edit().clear().commit() }

	@Test fun globalToDesktopKeepsTheGlobalsAsDesktopZero() {
		this.prefs.edit()
			.putString("0", "noot.noot.pingu\nPinguActivity")
			.putString("1", "ie.craggy.ted\nTedActivity")
			.commit()

		PinnedAppsMigration.migrate(this.context, LauncherPinMode.DESKTOP)

		assertEquals("noot.noot.pingu\nPinguActivity", this.prefs.getString("0/0", null)) // Per-desktop format now //
		assertEquals(
			listOf(listOf("noot.noot.pingu\nPinguActivity", "ie.craggy.ted\nTedActivity")),
			PinnedAppsStorage.read(this.prefs))
	}

	@Test fun desktopToGlobalFlattensInOrderAndDeDuplicates() {
		this.prefs.edit()
			.putString("0/0", "noot.noot.pingu\nPinguActivity")
			.putString("1/0", "ie.craggy.ted\nTedActivity")
			.putString("1/1", "noot.noot.pingu\nPinguActivity") // pingu is on desktops 0 and 1 //
			.commit()

		PinnedAppsMigration.migrate(this.context, LauncherPinMode.GLOBAL)

		assertEquals(
			listOf(listOf("noot.noot.pingu\nPinguActivity", "ie.craggy.ted\nTedActivity")),
			PinnedAppsStorage.read(this.prefs))
		assertEquals("noot.noot.pingu\nPinguActivity", this.prefs.getString("0", null))
		assertEquals("ie.craggy.ted\nTedActivity", this.prefs.getString("1", null))
		assertNull(this.prefs.getString("2", null))
	}
}
