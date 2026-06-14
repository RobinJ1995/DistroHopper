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
class DashLayoutStorageTest {
	private val context: Context = ApplicationProvider.getApplicationContext()
	private val prefs = Preferences.getSharedPreferences(this.context, Preferences.DASH_LAYOUT)

	@Before fun setUp() {
		this.prefs.edit().clear().commit()
	}

	private fun folder(id: String, vararg keys: String): Folder =
		Folder(id, keys.map { FolderMember.AppMember(it) as FolderMember })

	@Test fun roundTripsFoldersAndOrderPerProfile() {
		val data = mapOf(
			"personal" to DashLayoutStorage.ProfileLayout(
				folders = listOf(this.folder("folder-1", "a\nA", "b\nB")),
				order = listOf("folder:folder-1", "app:c\nC"),
			),
			"10" to DashLayoutStorage.ProfileLayout(
				folders = listOf(this.folder("folder-2", "w\nW\n10")),
				order = emptyList(),
			),
		)

		DashLayoutStorage.write(this.prefs, data)
		val read = DashLayoutStorage.read(this.prefs)

		assertEquals(data, read)
	}

	@Test fun readsEmptyWhenUnset() {
		assertTrue(DashLayoutStorage.read(this.prefs).isEmpty())
	}

	@Test fun discardsUnreadableJson() {
		this.prefs.edit().putString(DashLayoutStorage.KEY, "{ not json").commit()

		assertTrue(DashLayoutStorage.read(this.prefs).isEmpty())
	}
}
