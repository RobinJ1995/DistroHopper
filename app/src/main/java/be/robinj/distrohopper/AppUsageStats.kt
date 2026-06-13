package be.robinj.distrohopper

import android.content.Context
import be.robinj.distrohopper.preferences.Preferences

/**
 * Per-app launch statistics — how often each app was launched and when it was
 * last launched — feeding the non-alphabetical dash sort orders
 * ([be.robinj.distrohopper.preferences.AppSortOrder]).
 *
 * Kept in its own SharedPreferences file so a launch never trips the main
 * "prefs" change listeners. Apps are keyed by their profile-scoped key, the
 * same identity used for pinned apps and the icon/label caches, so the same
 * package in two profiles is tracked separately.
 */
class AppUsageStats(context: Context) {
	private val prefs = Preferences.getSharedPreferences(context, Preferences.APP_USAGE)

	/** Bumps [key]'s launch count and stamps it as the most recently launched. */
	fun recordLaunch(key: String) {
		this.prefs.edit()
			.putInt(countKey(key), this.getLaunchCount(key) + 1)
			.putLong(lastUsedKey(key), System.currentTimeMillis())
			.apply()
	}

	/** Number of times [key] has been launched; 0 if never. */
	fun getLaunchCount(key: String): Int = this.prefs.getInt(countKey(key), 0)

	/** When [key] was last launched (epoch millis); 0 if never. */
	fun getLastUsed(key: String): Long = this.prefs.getLong(lastUsedKey(key), 0L)

	// "\n" separator: profile-scoped keys already contain it, but never as a
	// leading character, so the count/last prefixes can't collide.
	private fun countKey(key: String) = "count\n$key"
	private fun lastUsedKey(key: String) = "last\n$key"
}
