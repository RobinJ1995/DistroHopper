package be.robinj.distrohopper

import be.robinj.distrohopper.preferences.AppSortOrder

/**
 * Builds the [Comparator] for a given dash [AppSortOrder]. The usage-based
 * orders sort by their score descending (most recent / most launched first)
 * and fall back to the alphabetical comparator as a secondary key, so a group
 * of apps with the same score stays alphabetically ordered.
 */
object AppComparators {
	private val alphabetical = AppComparatorAlphabetical()

	@JvmStatic
	fun forOrder(order: AppSortOrder, stats: AppUsageStats): Comparator<App> = when (order) {
		AppSortOrder.ALPHABETICAL -> this.alphabetical
		AppSortOrder.MOST_RECENTLY_USED ->
			Comparator<App> { a, b ->
				stats.getLastUsed(b.profileScopedKey).compareTo(stats.getLastUsed(a.profileScopedKey))
			}.then(this.alphabetical)
		AppSortOrder.MOST_USED ->
			Comparator<App> { a, b ->
				stats.getLaunchCount(b.profileScopedKey).compareTo(stats.getLaunchCount(a.profileScopedKey))
			}.then(this.alphabetical)
	}
}
