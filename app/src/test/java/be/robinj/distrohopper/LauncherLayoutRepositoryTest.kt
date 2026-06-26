package be.robinj.distrohopper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.desktop.launcher.LauncherItem
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The launcher's folder + manual-order model, over a desktop's pinned apps. */
@RunWith(RobolectricTestRunner::class)
class LauncherLayoutRepositoryTest {
	private val context: Context = ApplicationProvider.getApplicationContext()
	private lateinit var appRepository: AppRepository
	private lateinit var repository: LauncherLayoutRepository

	private fun app(label: String): App =
		App(this.context, null,
			ActivityTestSupport.resolveInfo("com.example.${label.lowercase()}", label, label))

	/** Adds [label] to the installed list and pins it on desktop 0. */
	private fun pin(label: String): App = this.app(label).also {
		this.appRepository.add(it, false)
		this.appRepository.pin(it, 0)
	}

	private fun items(): List<String> = this.repository.launcherItems(0).map {
		when (it) {
			is LauncherItem.LauncherApp -> it.app.label
			is LauncherItem.LauncherFolder -> "[${it.apps.joinToString(",") { a -> a.label }}]"
		}
	}

	@Before fun setUp() {
		Preferences.getSharedPreferences(this.context, Preferences.LAUNCHER_LAYOUT).edit().clear().commit()
		Preferences.getSharedPreferences(this.context, Preferences.PINNED_APPS).edit().clear().commit()
		this.appRepository = AppRepository(this.context)
		this.repository = LauncherLayoutRepository(this.context, this.appRepository)
		this.repository.load()
	}

	@Test fun loosePinnedAppsRenderInPinOrder() {
		this.pin("Alpha"); this.pin("Bravo"); this.pin("Charlie")

		assertEquals(listOf("Alpha", "Bravo", "Charlie"), this.items())
	}

	@Test fun createFolderGroupsTwoPinnedApps() {
		val a = this.pin("Alpha")
		val b = this.pin("Bravo")
		this.pin("Charlie")

		this.repository.createFolder(0, a, b)

		assertEquals(listOf("[Alpha,Bravo]", "Charlie"), this.items())
	}

	@Test fun addToFolderIsCappedAtNine() {
		val a = this.pin("App0")
		val b = this.pin("App1")
		val id = this.repository.createFolder(0, a, b)!!
		for (i in 2 until 9) {
			assertTrue(this.repository.addToFolder(id, this.pin("App$i")))
		}
		assertFalse(this.repository.addToFolder(id, this.pin("App9")))
	}

	@Test fun removingDownToOneAppDissolvesTheFolderKeepingBothPinned() {
		val a = this.pin("Alpha")
		val b = this.pin("Bravo")
		val id = this.repository.createFolder(0, a, b)!!

		this.repository.removeFromFolder(id, a.profileScopedKey)

		assertEquals(listOf("Alpha", "Bravo"), this.items())
		assertTrue(this.appRepository.isPinnedOn(a, 0))
		assertTrue(this.appRepository.isPinnedOn(b, 0))
	}

	@Test fun deleteFolderUnpinsItsMembers() {
		val a = this.pin("Alpha")
		val b = this.pin("Bravo")
		this.pin("Charlie")
		val id = this.repository.createFolder(0, a, b)!!

		this.repository.deleteFolder(id)

		// Members are gone from the launcher entirely; only Charlie remains.
		assertEquals(listOf("Charlie"), this.items())
		assertFalse(this.appRepository.isPinnedOn(a, 0))
		assertFalse(this.appRepository.isPinnedOn(b, 0))
	}

	@Test fun foldersPersistAcrossReloadAndRenderAtFirstMemberPosition() {
		this.pin("Alpha")
		val b = this.pin("Bravo")
		this.pin("Charlie")
		val d = this.pin("Delta")
		// A folder of Bravo + Delta renders at Bravo's (earlier) position.
		this.repository.createFolder(0, b, d)

		assertEquals(listOf("Alpha", "[Bravo,Delta]", "Charlie"), this.items())

		val reloaded = LauncherLayoutRepository(this.context, this.appRepository)
		reloaded.load()
		assertEquals(listOf("Alpha", "[Bravo,Delta]", "Charlie"),
			reloaded.launcherItems(0).map {
				when (it) {
					is LauncherItem.LauncherApp -> it.app.label
					is LauncherItem.LauncherFolder -> "[${it.apps.joinToString(",") { a -> a.label }}]"
				}
			})
	}

	@Test fun reconcileDropsUnpinnedMembersAndDissolves() {
		val a = this.pin("Alpha")
		val b = this.pin("Bravo")
		this.repository.createFolder(0, a, b)

		this.appRepository.unpin(b, 0)
		this.repository.reconcile()

		assertEquals(listOf("Alpha"), this.items())
	}
}
