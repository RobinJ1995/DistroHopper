package be.robinj.distrohopper.home

import be.robinj.distrohopper.AppManager
import be.robinj.distrohopper.widgets.WidgetHost
import kotlin.math.max

/**
 * The single authority over the home screen's desktops, spanning their two
 * owners — widgets ([WidgetHost]) and per-desktop pinned launcher apps
 * ([AppManager]) — which share one 0-based desktop index. It derives how many
 * desktops exist (so `WidgetsPager` doesn't reach into the app model) and owns
 * the structural operations that must touch both at once.
 *
 * There is no delete-desktop UI yet; [deleteDesktop] is the data-layer
 * operation that one will call, kept here so widgets and pins can never drift
 * apart. Future insert/reorder operations belong here too.
 */
class Desktops(
	private val widgetHost: WidgetHost,
	private val appManager: AppManager,
) {
	/** Highest occupied desktop index across widgets and pins (or -1 when empty). */
	fun highestOccupiedDesktop(): Int =
		max(this.widgetHost.highestWidgetDesktop(), this.appManager.highestPinnedDesktop())

	/**
	 * Deletes desktop [page]: removes its widgets and its pinned apps, and shifts
	 * every higher desktop down by one.
	 */
	fun deleteDesktop(page: Int) {
		this.appManager.removePinnedDesktop(page)
		this.appManager.savePinnedApps()
		// Re-derives the pager (which reads the now-updated pinned desktops too) //
		this.widgetHost.removeWidgetPage(page)
	}
}
