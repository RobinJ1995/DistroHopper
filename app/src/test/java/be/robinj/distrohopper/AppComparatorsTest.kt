package be.robinj.distrohopper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.AppSortOrder
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The dash sort orders: alphabetical, most-recently-used and most-used, all
 * with an alphabetical secondary key. Driven without launching HomeActivity.
 *
 * The comparator scenarios seed the usage store directly (matching its on-disk
 * key format) so timestamps are explicit and the ordering is deterministic,
 * rather than relying on two real-clock launches landing in distinct millis.
 *
 */
@RunWith(RobolectricTestRunner::class)
class AppComparatorsTest {
	private val context: Context = ApplicationProvider.getApplicationContext()
	private val stats = AppUsageStats(this.context)

	private fun app(label: String): App =
		App(this.context, null,
			ActivityTestSupport.resolveInfo("com.example.${label.lowercase()}", label, label))

	/** Writes usage straight to the store's file, bypassing the real-time clock. */
	private fun seed(app: App, count: Int, lastUsed: Long) {
		Preferences.getSharedPreferences(this.context, Preferences.APP_USAGE)
			.edit()
			.putInt("count\n${app.profileScopedKey}", count)
			.putLong("last\n${app.profileScopedKey}", lastUsed)
			.commit()
	}

	private fun sorted(order: AppSortOrder, vararg apps: App): List<String> =
		apps.toMutableList()
			.apply { sortWith(AppComparators.forOrder(order, stats)) }
			.map { it.label }

	@Before fun setUp() {
		Preferences.getSharedPreferences(this.context, Preferences.APP_USAGE)
			.edit().clear().commit()
	}

	@Test fun alphabeticalIgnoresUsage() {
		this.seed(this.app("Noel"), count = 9, lastUsed = 9_000L)

		assertEquals(listOf("Dougal", "Noel"),
			this.sorted(AppSortOrder.ALPHABETICAL, this.app("Noel"), this.app("Dougal")))
	}

	@Test fun mostUsedRanksByLaunchCountThenAlphabetically() {
		val dougal = this.app("Dougal")
		val jack = this.app("Jack")
		val ted = this.app("Ted")

		// Ted 't meeste gelanceerd; Dougal en Jack nooit gelanceerd (gelijk -> alfabetisch).
		this.seed(ted, count = 5, lastUsed = 100L)

		assertEquals(listOf("Ted", "Dougal", "Jack"),
			this.sorted(AppSortOrder.MOST_USED, jack, ted, dougal))
	}

	@Test fun mostRecentlyUsedRanksByLastLaunchThenAlphabetically() {
		val dougal = this.app("Dougal")
		val jack = this.app("Jack")
		val ted = this.app("Ted")

		// Jack gebruikt na Dougal; Ted nooit gebruikt en bungelt er achteraan.
		this.seed(dougal, count = 1, lastUsed = 100L)
		this.seed(jack, count = 1, lastUsed = 200L)

		assertEquals(listOf("Jack", "Dougal", "Ted"),
			this.sorted(AppSortOrder.MOST_RECENTLY_USED, ted, dougal, jack))
	}

	@Test fun recordLaunchAccumulatesCountAndStampsLastUsed() {
		val key = this.app("Dougal").profileScopedKey

		assertEquals(0, this.stats.getLaunchCount(key))
		assertEquals(0L, this.stats.getLastUsed(key))

		this.stats.recordLaunch(key)
		this.stats.recordLaunch(key)

		assertEquals(2, this.stats.getLaunchCount(key))
		assertTrue(this.stats.getLastUsed(key) > 0L)
	}
}
