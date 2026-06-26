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
		// CUSTOM is a manual per-item arrangement applied by DashLayoutRepository;
		// as an app comparator it degrades to alphabetical (the stable tail order
		// for apps that have no manual position yet).
		AppSortOrder.ALPHABETICAL, AppSortOrder.CUSTOM -> this.alphabetical
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
