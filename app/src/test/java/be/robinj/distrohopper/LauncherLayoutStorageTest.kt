package be.robinj.distrohopper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.folder.Folder
import be.robinj.distrohopper.folder.FolderMember
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LauncherLayoutStorageTest {
	private val context: Context = ApplicationProvider.getApplicationContext()
	private val prefs = Preferences.getSharedPreferences(this.context, Preferences.LAUNCHER_LAYOUT)

	@Before fun setUp() {
		this.prefs.edit().clear().commit()
	}

	private fun folder(id: String, vararg keys: String): Folder =
		Folder(id, keys.map { FolderMember.AppMember(it) as FolderMember })

	@Test fun roundTripsFoldersPerDesktop() {
		val data = mapOf(
			0 to LauncherLayoutStorage.DesktopLayout(
				folders = listOf(this.folder("folder-1", "a\nA", "b\nB")),
			),
			2 to LauncherLayoutStorage.DesktopLayout(
				folders = listOf(this.folder("folder-2", "c\nC", "d\nD")),
			),
		)

		LauncherLayoutStorage.write(this.prefs, data)

		assertEquals(data, LauncherLayoutStorage.read(this.prefs))
	}

	@Test fun readsEmptyWhenUnsetOrUnreadable() {
		assertTrue(LauncherLayoutStorage.read(this.prefs).isEmpty())

		this.prefs.edit().putString(LauncherLayoutStorage.KEY, "{ not json").commit()
		assertTrue(LauncherLayoutStorage.read(this.prefs).isEmpty())
	}
}
