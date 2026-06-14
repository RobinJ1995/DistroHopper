package be.robinj.distrohopper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * AppRepository is the Activity-free model half of AppManager; these tests
 * exercise it without launching HomeActivity.
 */
@RunWith(RobolectricTestRunner::class)
class AppRepositoryTest {
	private val context: Context = ApplicationProvider.getApplicationContext()
	private val repository = AppRepository(this.context)

	private fun app(packageName: String, activityName: String, label: String): App =
		App(this.context, null,
			ActivityTestSupport.resolveInfo(packageName, activityName, label))

	@Before fun setUp() {
		Preferences.getSharedPreferences(this.context, Preferences.PINNED_APPS)
			.edit().clear().commit()
	}

	@Test fun addRejectsDuplicatesWhenAsked() {
		val app = this.app("noot.noot.pingu", "PinguActivity", "Pingu")

		assertTrue(this.repository.add(app, true))
		assertFalse(this.repository.add(app, true))
		assertEquals(1, this.repository.size())
	}

	@Test fun sortOrdersByLabelAndEmitsASnapshot() {
		this.repository.add(this.app("noot.noot.pinga", "PingaActivity", "Pinga"), false)
		this.repository.add(this.app("noot.noot.pingu", "PinguActivity", "Pingu"), false)

		this.repository.sort()

		assertEquals(listOf("Pinga", "Pingu"), this.repository.installedLive.map { it.label })
		assertEquals(listOf("Pinga", "Pingu"), this.repository.installed.value.map { it.label })
	}

	private fun setSortOrder(value: String) =
		Preferences.getSharedPreferences(this.context).edit()
			.putString("app_sort_order", value).commit()

	private fun seedUsage(app: App, count: Int, lastUsed: Long) =
		Preferences.getSharedPreferences(this.context, Preferences.APP_USAGE).edit()
			.putInt("count\n${app.profileScopedKey}", count)
			.putLong("last\n${app.profileScopedKey}", lastUsed)
			.commit()

	@Test fun sortByMostUsedRanksByLaunchCountThenAlphabetically() {
		this.setSortOrder("most_used")
		val pinga = this.app("noot.noot.pinga", "PingaActivity", "Pinga")
		val pingu = this.app("noot.noot.pingu", "PinguActivity", "Pingu")
		val robby = this.app("noot.noot.robby", "RobbyActivity", "Robby")
		this.repository.add(robby, false)
		this.repository.add(pinga, false)
		this.repository.add(pingu, false)
		this.seedUsage(robby, 5, 100L) // Pinga en Pingu nooit gelanceerd (gelijk -> alfabetisch).

		this.repository.sort()

		assertEquals(listOf("Robby", "Pinga", "Pingu"),
			this.repository.installedLive.map { it.label })
	}

	@Test fun sortByMostRecentlyUsedRanksByLastLaunchThenAlphabetically() {
		this.setSortOrder("recent")
		val pinga = this.app("noot.noot.pinga", "PingaActivity", "Pinga")
		val pingu = this.app("noot.noot.pingu", "PinguActivity", "Pingu")
		val robby = this.app("noot.noot.robby", "RobbyActivity", "Robby")
		this.repository.add(robby, false)
		this.repository.add(pinga, false)
		this.repository.add(pingu, false)
		this.seedUsage(pinga, 1, 100L)
		this.seedUsage(pingu, 1, 200L) // Robby nooit gelanceerd, hangt er achteraan.

		this.repository.sort()

		assertEquals(listOf("Pingu", "Pinga", "Robby"),
			this.repository.installedLive.map { it.label })
	}

	@Test fun searchPrefersPrefixMatchesAndHonoursMaxResults() {
		this.repository.add(this.app("ie.craggy.jack", "JackActivity", "Jack"), false)
		this.repository.add(this.app("ie.craggy.blackjack", "BlackjackActivity", "Blackjack"), false)
		this.repository.add(this.app("ie.craggy.dougal", "DougalActivity", "Dougal"), false)

		val results = this.repository.search("jack", Integer.MAX_VALUE)
		assertEquals(listOf("Jack", "Blackjack"), results.map { it.label })

		assertEquals(1, this.repository.search("jack", 1).size)
		assertEquals(3, this.repository.search("", 1).size) // maxResults ignored when empty
	}

	@Test fun pinUnpinRoundTripsAndEmits() {
		val app = this.app("noot.noot.pingu", "PinguActivity", "Pingu")
		this.repository.add(app, false)

		assertTrue(this.repository.pin(app))
		assertFalse(this.repository.pin(app)) // already pinned
		assertTrue(this.repository.isPinned(app))
		assertEquals(listOf(app), this.repository.pinned.value)

		assertTrue(this.repository.unpin(app))
		assertFalse(this.repository.isPinned(app))
		assertTrue(this.repository.pinned.value.isEmpty())
	}

	@Test fun savePinnedAppsPersistsInOrder() {
		val pingu = this.app("noot.noot.pingu", "PinguActivity", "Pingu")
		val pinga = this.app("noot.noot.pinga", "PingaActivity", "Pinga")
		this.repository.pin(pingu)
		this.repository.pin(pinga)
		this.repository.movePinnedApp(0, 1)

		this.repository.savePinnedApps()

		val prefs = Preferences.getSharedPreferences(this.context, Preferences.PINNED_APPS)
		assertEquals("noot.noot.pinga\nPingaActivity", prefs.getString("0", null))
		assertEquals("noot.noot.pingu\nPinguActivity", prefs.getString("1", null))
		assertNull(prefs.getString("2", null))
	}

	@Test fun isUsageBasedSortOrderTracksThePreference() {
		val prefs = Preferences.getSharedPreferences(this.context)

		prefs.edit().putString("app_sort_order", "alphabetical").commit()
		assertFalse(this.repository.isUsageBasedSortOrder())

		prefs.edit().putString("app_sort_order", "recent").commit()
		assertTrue(this.repository.isUsageBasedSortOrder())

		prefs.edit().putString("app_sort_order", "most_used").commit()
		assertTrue(this.repository.isUsageBasedSortOrder())
	}

	@Test fun findersLocateAppsByIdentity() {
		val pingu = this.app("noot.noot.pingu", "PinguActivity", "Pingu")
		this.repository.add(pingu, false)

		assertEquals(pingu,
			this.repository.findAppByPackageAndActivityName("noot.noot.pingu", "PinguActivity"))
		assertNull(this.repository.findAppByPackageAndActivityName("noot.noot.pingu", "Other"))
		assertEquals(listOf(pingu), this.repository.findAppsByPackageName("noot.noot.pingu"))
		assertEquals(pingu,
			this.repository.installedAppsMap()[pingu.packageAndActivityName])
	}
}
