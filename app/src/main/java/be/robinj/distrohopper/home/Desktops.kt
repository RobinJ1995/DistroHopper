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
 * There is no delete-desktop UI yet; [deleteDesktop] is the data-layer
 * operation that one will call, kept here so widgets and pins can never drift
 * apart. Future insert/reorder operations belong here too.
 */
class Desktops(
	private val widgetHost: WidgetHost,
	private val appManager: AppManager,
	private val desktopAppHost: DesktopAppHost,
	private val desktopFolderHost: DesktopFolderHost,
) {
	/** Highest occupied desktop index across widgets, pins, desktop apps and folders (or -1). */
	fun highestOccupiedDesktop(): Int =
		max(
			max(this.widgetHost.highestWidgetDesktop(), this.appManager.highestPinnedDesktop()),
			max(this.desktopAppHost.highestDesktop(), this.desktopFolderHost.highestDesktop()))

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
