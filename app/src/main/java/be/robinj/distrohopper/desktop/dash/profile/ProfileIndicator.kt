package be.robinj.distrohopper.desktop.dash.profile

import android.os.UserHandle

/**
 * The dash's profile (profile) tab indicator. One implementation per theme
 * style (see ProfileIndicatorStyle): the Unity ribbon glyphs, the GNOME
 * panel pill, etc. The dash profile pager drives these as the user swipes;
 * tapping a slot calls back to switch pages.
 */
interface ProfileIndicator {
	/** (Re)build for these profiles, with [selected] the current page. */
	fun bind(profiles: List<UserHandle?>, selected: Int)

	/** Smooth scroll progress: settled on [position], [positionOffset] (0..1) towards the next. */
	fun onPageScrolled(position: Int, positionOffset: Float)

	/** Settled on a page. */
	fun onPageSelected(position: Int)

	/** The dash opened or closed (indicators that only show while open react here). */
	fun onDashOpenChanged(open: Boolean) {}

	/** Removes the indicator's views (e.g. when reverting to a single profile). */
	fun clear()
}
