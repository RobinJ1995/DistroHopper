package be.robinj.distrohopper

import be.robinj.distrohopper.desktop.dash.DashItem
import be.robinj.distrohopper.preferences.AppSortOrder

/**
 * Builds the [Comparator] that orders dash items ([DashItem]) for a given
 * non-[AppSortOrder.CUSTOM] order. The rule (per the feature spec):
 *
 *  - a folder is ranked by its highest-scoring member under the active order
 *    (alphabetically-first for [AppSortOrder.ALPHABETICAL], highest launch count
 *    / most recent for the usage orders) — so a folder interleaves with apps by
 *    that representative member regardless of the chosen order;
 *  - additionally, in [AppSortOrder.ALPHABETICAL] all folders sort ahead of all
 *    loose apps (the spec's "a folder is sorted higher than any individual app").
 *
 * The manual [AppSortOrder.CUSTOM] order is not a comparator — it is applied
 * directly from the stored arrangement by [DashLayoutRepository].
 */
object DashComparators {
	@JvmStatic
	fun forOrder(order: AppSortOrder, stats: AppUsageStats): Comparator<DashItem> {
		val appComparator = AppComparators.forOrder(order, stats)
		val byRepresentative = Comparator<DashItem> { a, b ->
			appComparator.compare(representative(a, appComparator), representative(b, appComparator))
		}

		if (order != AppSortOrder.ALPHABETICAL) {
			return byRepresentative
		}

		// Alphabetical: folders grouped ahead of loose apps, then by representative.
		return Comparator { a, b ->
			val groupA = if (a is DashItem.FolderItem) 0 else 1
			val groupB = if (b is DashItem.FolderItem) 0 else 1
			if (groupA != groupB) groupA - groupB else byRepresentative.compare(a, b)
		}
	}

	/** The member that decides a folder's rank: the "best" one under [appComparator]. */
	private fun representative(item: DashItem, appComparator: Comparator<App>): App = when (item) {
		is DashItem.AppItem -> item.app
		is DashItem.FolderItem -> item.apps.minWithOrNull(appComparator) ?: item.apps.first()
	}
}
