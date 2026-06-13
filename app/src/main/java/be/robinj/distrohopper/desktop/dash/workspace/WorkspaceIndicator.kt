package be.robinj.distrohopper.desktop.dash.workspace

import android.os.UserHandle

/**
 * The dash's workspace (profile) tab indicator. One implementation per theme
 * style (see WorkspaceIndicatorStyle): the Unity ribbon glyphs, the GNOME
 * panel pill, etc. The dash workspace pager drives these as the user swipes;
 * tapping a slot calls back to switch pages.
 */
interface WorkspaceIndicator {
	/** (Re)build for these workspaces, with [selected] the current page. */
	fun bind(workspaces: List<UserHandle?>, selected: Int)

	/** Smooth scroll progress: settled on [position], [positionOffset] (0..1) towards the next. */
	fun onPageScrolled(position: Int, positionOffset: Float)

	/** Settled on a page. */
	fun onPageSelected(position: Int)

	/** The dash opened or closed (indicators that only show while open react here). */
	fun onDashOpenChanged(open: Boolean) {}

	/** Removes the indicator's views (e.g. when reverting to a single workspace). */
	fun clear()
}
