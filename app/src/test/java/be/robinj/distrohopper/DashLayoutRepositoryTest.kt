package be.robinj.distrohopper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.desktop.dash.DashItem
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The dash's folder + custom-order model, exercised without launching the dash.
 */
@RunWith(RobolectricTestRunner::class)
class DashLayoutRepositoryTest {
	private val context: Context = ApplicationProvider.getApplicationContext()
	private lateinit var appRepository: AppRepository
	private lateinit var repository: DashLayoutRepository

	private fun app(label: String): App =
		App(this.context, null,
			ActivityTestSupport.resolveInfo("com.example.${label.lowercase()}", label, label))

	private fun add(label: String): App = this.app(label).also { this.appRepository.add(it, false) }

	private fun setSortOrder(value: String) =
		Preferences.getSharedPreferences(this.context).edit()
			.putString("app_sort_order", value).commit()

	/** Labels of the personal profile's dash items; folders as "[m1,m2]". */
	private fun items(): List<String> = this.repository.dashItems(null).map {
		when (it) {
			is DashItem.AppItem -> it.app.label
			is DashItem.FolderItem -> "[${it.apps.joinToString(",") { a -> a.label }}]"
		}
	}

	private fun folderId(): String =
		(this.repository.dashItems(null).first { it is DashItem.FolderItem }
			as DashItem.FolderItem).folder.id

	@Before fun setUp() {
		Preferences.getSharedPreferences(this.context, Preferences.DASH_LAYOUT).edit().clear().commit()
		this.setSortOrder("alphabetical")
		this.appRepository = AppRepository(this.context)
		this.repository = DashLayoutRepository(this.context, this.appRepository)
		this.repository.load()
	}

	@Test fun createFolderRemovesLooseAppsAndShowsFolderFirst() {
		val a = this.add("Alpha")
		val b = this.add("Bravo")
		this.add("Charlie")

		this.repository.createFolder(a, b)

		// Alphabetical: the folder (members Alpha, Bravo) sorts ahead of loose Charlie.
		assertEquals(listOf("[Alpha,Bravo]", "Charlie"), this.items())
	}

	@Test fun addToFolderIsCappedAtNineAndShowsAToastableFalse() {
		val a = this.add("App0")
		val b = this.add("App1")
		val id = this.repository.createFolder(a, b)!!
		// Fill up to 9.
		for (i in 2 until 9) {
			assertTrue(this.repository.addToFolder(id, this.add("App$i")))
		}
		// The 10th is rejected.
		assertFalse(this.repository.addToFolder(id, this.add("App9")))
	}

	@Test fun removingDownToOneAppDissolvesTheFolder() {
		val a = this.add("Alpha")
		val b = this.add("Bravo")
		val id = this.repository.createFolder(a, b)!!

		this.repository.removeFromFolder(id, a.profileScopedKey)

		// Folder dissolved: both apps loose again.
		assertEquals(listOf("Alpha", "Bravo"), this.items())
	}

	@Test fun deleteFolderReturnsMembersAsLooseApps() {
		val a = this.add("Alpha")
		val b = this.add("Bravo")
		this.add("Charlie")
		val id = this.repository.createFolder(a, b)!!

		this.repository.deleteFolder(id)

		assertEquals(listOf("Alpha", "Bravo", "Charlie"), this.items())
	}

	@Test fun customOrderIsAppliedAndPersisted() {
		this.setSortOrder("custom")
		this.add("Alpha")
		this.add("Bravo")
		this.add("Charlie")

		// Move Charlie (index 2) to the front.
		this.repository.moveItem(null, 2, 0)
		assertEquals(listOf("Charlie", "Alpha", "Bravo"), this.items())

		// Reload from disk: the manual order survives.
		val reloaded = DashLayoutRepository(this.context, this.appRepository)
		reloaded.load()
		assertEquals(listOf("Charlie", "Alpha", "Bravo"),
			reloaded.dashItems(null).map { (it as DashItem.AppItem).app.label })
	}

	@Test fun reconcileDropsUninstalledMembersAndDissolves() {
		val a = this.add("Alpha")
		val b = this.add("Bravo")
		this.repository.createFolder(a, b)

		// Bravo uninstalled: the folder drops to one app and dissolves.
		this.appRepository.remove(b)
		this.repository.reconcile()

		assertEquals(listOf("Alpha"), this.items())
	}

	@Test fun foldersPersistAcrossReload() {
		val a = this.add("Alpha")
		val b = this.add("Bravo")
		this.repository.createFolder(a, b)

		val reloaded = DashLayoutRepository(this.context, this.appRepository)
		reloaded.load()

		assertEquals(listOf("[Alpha,Bravo]"),
			reloaded.dashItems(null).map { "[${(it as DashItem.FolderItem).apps.joinToString(",") { a -> a.label }}]" })
	}
}
