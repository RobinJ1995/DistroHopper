package be.robinj.distrohopper

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PinnedAppsStorageTest {
	private val context: Context = ApplicationProvider.getApplicationContext()
	private val prefs: SharedPreferences
		get() = Preferences.getSharedPreferences(this.context, Preferences.PINNED_APPS)

	@Before fun setUp() { this.prefs.edit().clear().commit() }

	@Test fun readReturnsEmptyWhenNothingIsStored() {
		assertEquals(emptyList<List<String>>(), PinnedAppsStorage.read(this.prefs))
	}

	@Test fun readsTheLegacyFlatFormatAsDesktopZero() {
		this.prefs.edit()
			.putString("0", "noot.noot.pingu\nPinguActivity")
			.putString("1", "noot.noot.pinga\nPingaActivity")
			.commit()

		assertEquals(
			listOf(listOf("noot.noot.pingu\nPinguActivity", "noot.noot.pinga\nPingaActivity")),
			PinnedAppsStorage.read(this.prefs))
	}

	@Test fun readsThePerDesktopFormatDenselyAcrossGaps() {
		this.prefs.edit()
			.putString("0/0", "noot.noot.pingu\nPinguActivity")
			.putString("2/0", "ie.craggy.ted\nTedActivity")
			.putString("2/1", "ie.craggy.dougal\nDougalActivity")
			.commit()

		assertEquals(
			listOf(
				listOf("noot.noot.pingu\nPinguActivity"),
				emptyList(),
				listOf("ie.craggy.ted\nTedActivity", "ie.craggy.dougal\nDougalActivity"),
			),
			PinnedAppsStorage.read(this.prefs))
	}

	@Test fun writeGlobalRoundTripsViaTheFlatFormat() {
		PinnedAppsStorage.writeGlobal(this.prefs,
			listOf("noot.noot.pingu\nPinguActivity", "noot.noot.pinga\nPingaActivity"))

		assertEquals("noot.noot.pingu\nPinguActivity", this.prefs.getString("0", null))
		assertEquals(
			listOf(listOf("noot.noot.pingu\nPinguActivity", "noot.noot.pinga\nPingaActivity")),
			PinnedAppsStorage.read(this.prefs))
	}

	@Test fun writePerDesktopRoundTripsViaThePagedFormat() {
		PinnedAppsStorage.writePerDesktop(this.prefs, listOf(
			listOf("noot.noot.pingu\nPinguActivity"),
			listOf("ie.craggy.ted\nTedActivity", "ie.craggy.dougal\nDougalActivity"),
		))

		assertEquals("ie.craggy.ted\nTedActivity", this.prefs.getString("1/0", null))
		assertEquals(
			listOf(
				listOf("noot.noot.pingu\nPinguActivity"),
				listOf("ie.craggy.ted\nTedActivity", "ie.craggy.dougal\nDougalActivity"),
			),
			PinnedAppsStorage.read(this.prefs))
	}
}
