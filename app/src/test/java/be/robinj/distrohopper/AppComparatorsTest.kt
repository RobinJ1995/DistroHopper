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
		this.seed(this.app("Zeta"), count = 9, lastUsed = 9_000L)

		assertEquals(listOf("Alpha", "Zeta"),
			this.sorted(AppSortOrder.ALPHABETICAL, this.app("Zeta"), this.app("Alpha")))
	}

	@Test fun mostUsedRanksByLaunchCountThenAlphabetically() {
		val alpha = this.app("Alpha")
		val beta = this.app("Beta")
		val gamma = this.app("Gamma")

		// Gamma launched most; Alpha and Beta never launched (tied -> alphabetical).
		this.seed(gamma, count = 5, lastUsed = 100L)

		assertEquals(listOf("Gamma", "Alpha", "Beta"),
			this.sorted(AppSortOrder.MOST_USED, beta, gamma, alpha))
	}

	@Test fun mostRecentlyUsedRanksByLastLaunchThenAlphabetically() {
		val alpha = this.app("Alpha")
		val beta = this.app("Beta")
		val gamma = this.app("Gamma")

		// Beta used after Alpha; Gamma never used and trails the rest.
		this.seed(alpha, count = 1, lastUsed = 100L)
		this.seed(beta, count = 1, lastUsed = 200L)

		assertEquals(listOf("Beta", "Alpha", "Gamma"),
			this.sorted(AppSortOrder.MOST_RECENTLY_USED, gamma, alpha, beta))
	}

	@Test fun recordLaunchAccumulatesCountAndStampsLastUsed() {
		val key = this.app("Alpha").profileScopedKey

		assertEquals(0, this.stats.getLaunchCount(key))
		assertEquals(0L, this.stats.getLastUsed(key))

		this.stats.recordLaunch(key)
		this.stats.recordLaunch(key)

		assertEquals(2, this.stats.getLaunchCount(key))
		assertTrue(this.stats.getLastUsed(key) > 0L)
	}
}
