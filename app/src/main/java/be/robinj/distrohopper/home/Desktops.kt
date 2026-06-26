package be.robinj.distrohopper.home

import be.robinj.distrohopper.AppManager
import be.robinj.distrohopper.widgets.DesktopAppHost
import be.robinj.distrohopper.widgets.DesktopFolderHost
import be.robinj.distrohopper.widgets.WidgetHost
import kotlin.math.max

/**
 * The single authority over the home screen's desktops, spanning their three
 * owners — widgets ([WidgetHost]), per-desktop pinned launcher apps
 * ([AppManager]) and apps pinned to the desktop itself ([DesktopAppHost]) —
 * which share one 0-based desktop index. It derives how many desktops exist (so
 * `WidgetsPager` doesn't reach into the app model) and owns the structural
 * operations that must touch all of them at once.
 *
 * [deleteDesktop] is the data-layer operation that removes one explicitly;
 * [removeEmptyDesktops] is the automatic clean-up that runs after any
 * structural change so empty desktops in the middle of the row never linger
 * (the trailing empty one is the pager's own doing — see `WidgetsPager`).
 * Future insert/reorder operations belong here too.
 */
class Desktops(
	private val widgetHost: WidgetHost,
	private val appManager: AppManager,
	private val desktopAppHost: DesktopAppHost,
	private val desktopFolderHost: DesktopFolderHost,
) {
	/** Guards against re-entering [removeEmptyDesktops] from the `pagesChanged` it triggers. */
	private var compacting = false

	/** Highest occupied desktop index across widgets, pins, desktop apps and folders (or -1). */
	fun highestOccupiedDesktop(): Int =
		max(
			max(this.widgetHost.highestWidgetDesktop(), this.appManager.highestPinnedDesktop()),
			max(this.desktopAppHost.highestDesktop(), this.desktopFolderHost.highestDesktop()))

	/**
	 * Whether desktop [page] holds nothing the user can see on it: no widgets,
	 * desktop apps or desktop folders, and no per-desktop launcher pins. Launcher
	 * pins only count in per-desktop pin mode — in global mode the bar is shared
	 * across desktops, so it can never keep one alive (mirrors how
	 * [AppManager.highestPinnedDesktop] ignores pins in global mode).
	 */
	fun isDesktopEmpty(page: Int): Boolean =
		! this.widgetHost.hasWidgetsOnDesktop(page) &&
			! this.desktopAppHost.hasAppsOnDesktop(page) &&
			! this.desktopFolderHost.hasFoldersOnDesktop(page) &&
			! (this.appManager.isPerDesktopPins() && this.appManager.pinnedOn(page).isNotEmpty())

	/**
	 * Removes every empty desktop within the occupied range, shifting the rest
	 * down so no gaps remain. The single trailing empty desktop is left alone (it
	 * sits above [highestOccupiedDesktop] and so is never iterated): there is
	 * always an empty desktop at the end for the user to fill.
	 *
	 * Called after any structural change (via `WidgetsPager.pagesChanged`), so
	 * deleting the last item from a desktop drops the desktop itself. Iterating
	 * top-down keeps the not-yet-visited (lower) indices stable as higher ones
	 * shift down. [deleteDesktop] re-enters `pagesChanged`, so a guard stops it
	 * recursing back in here.
	 */
	fun removeEmptyDesktops() {
		if (this.compacting) {
			return
		}

		this.compacting = true
		try {
			for (page in this.highestOccupiedDesktop() downTo 0) {
				if (this.isDesktopEmpty(page)) {
					this.deleteDesktop(page)
				}
			}
		} finally {
			this.compacting = false
		}
	}

	/**
	 * Deletes desktop [page]: removes its widgets, its pinned apps, its desktop
	 * apps and its folders, and shifts every higher desktop down by one.
	 */
	fun deleteDesktop(page: Int) {
		this.appManager.removePinnedDesktop(page)
		this.appManager.savePinnedApps()
		this.desktopFolderHost.removeDesktopPage(page)
		this.desktopAppHost.removeDesktopPage(page)
		// Re-derives the pager (which reads the now-updated pinned desktops too) //
		this.widgetHost.removeWidgetPage(page)
	}
}
