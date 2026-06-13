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
		this.prefs.edit().putString("0", "a\nA").putString("1", "b\nB").commit()

		assertEquals(listOf(listOf("a\nA", "b\nB")), PinnedAppsStorage.read(this.prefs))
	}

	@Test fun readsThePerDesktopFormatDenselyAcrossGaps() {
		this.prefs.edit()
			.putString("0/0", "a\nA")
			.putString("2/0", "c\nC").putString("2/1", "d\nD")
			.commit()

		assertEquals(
			listOf(listOf("a\nA"), emptyList(), listOf("c\nC", "d\nD")),
			PinnedAppsStorage.read(this.prefs))
	}

	@Test fun writeGlobalRoundTripsViaTheFlatFormat() {
		PinnedAppsStorage.writeGlobal(this.prefs, listOf("a\nA", "b\nB"))

		assertEquals("a\nA", this.prefs.getString("0", null))
		assertEquals(listOf(listOf("a\nA", "b\nB")), PinnedAppsStorage.read(this.prefs))
	}

	@Test fun writePerDesktopRoundTripsViaThePagedFormat() {
		PinnedAppsStorage.writePerDesktop(this.prefs, listOf(listOf("a\nA"), listOf("b\nB", "c\nC")))

		assertEquals("b\nB", this.prefs.getString("1/0", null))
		assertEquals(
			listOf(listOf("a\nA"), listOf("b\nB", "c\nC")), PinnedAppsStorage.read(this.prefs))
	}
}
