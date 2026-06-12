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
		val app = this.app("com.example.a", "A", "Alpha")

		assertTrue(this.repository.add(app, true))
		assertFalse(this.repository.add(app, true))
		assertEquals(1, this.repository.size())
	}

	@Test fun sortOrdersByLabelAndEmitsASnapshot() {
		this.repository.add(this.app("com.example.b", "B", "Bravo"), false)
		this.repository.add(this.app("com.example.a", "A", "Alpha"), false)

		this.repository.sort()

		assertEquals(listOf("Alpha", "Bravo"), this.repository.installedLive.map { it.label })
		assertEquals(listOf("Alpha", "Bravo"), this.repository.installedLive.map { it.label })
	}

	@Test fun searchPrefersPrefixMatchesAndHonoursMaxResults() {
		this.repository.add(this.app("com.example.a", "A", "Alpha"), false)
		this.repository.add(this.app("com.example.b", "B", "Kalpha"), false)
		this.repository.add(this.app("com.example.c", "C", "Beta"), false)

		val results = this.repository.search("alpha", Integer.MAX_VALUE)
		assertEquals(listOf("Alpha", "Kalpha"), results.map { it.label })

		assertEquals(1, this.repository.search("alpha", 1).size)
		assertEquals(3, this.repository.search("", 1).size) // maxResults ignored when empty
	}

	@Test fun pinUnpinRoundTripsAndEmits() {
		val app = this.app("com.example.a", "A", "Alpha")
		this.repository.add(app, false)

		assertTrue(this.repository.pin(app))
		assertFalse(this.repository.pin(app)) // already pinned
		assertTrue(this.repository.isPinned(app))
		assertEquals(listOf(app), this.repository.pinnedLive)

		assertTrue(this.repository.unpin(app))
		assertFalse(this.repository.isPinned(app))
		assertTrue(this.repository.pinnedLive.isEmpty())
	}

	@Test fun savePinnedAppsPersistsInOrder() {
		val alpha = this.app("com.example.a", "A", "Alpha")
		val beta = this.app("com.example.b", "B", "Beta")
		this.repository.pin(alpha)
		this.repository.pin(beta)
		this.repository.movePinnedApp(0, 1)

		this.repository.savePinnedApps()

		val prefs = Preferences.getSharedPreferences(this.context, Preferences.PINNED_APPS)
		assertEquals("com.example.b\nB", prefs.getString("0", null))
		assertEquals("com.example.a\nA", prefs.getString("1", null))
		assertNull(prefs.getString("2", null))
	}

	@Test fun findersLocateAppsByIdentity() {
		val alpha = this.app("com.example.a", "AlphaActivity", "Alpha")
		this.repository.add(alpha, false)

		assertEquals(alpha,
			this.repository.findAppByPackageAndActivityName("com.example.a", "AlphaActivity"))
		assertNull(this.repository.findAppByPackageAndActivityName("com.example.a", "Other"))
		assertEquals(listOf(alpha), this.repository.findAppsByPackageName("com.example.a"))
		assertEquals(alpha,
			this.repository.installedAppsMap()[alpha.packageAndActivityName])
	}
}
