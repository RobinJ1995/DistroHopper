package be.robinj.distrohopper.home

import android.animation.LayoutTransition
import android.content.res.Resources
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import be.robinj.distrohopper.R
import be.robinj.distrohopper.ViewFinder

/**
 * Configures the LayoutTransitions that animate views (dash grid, lens
 * results, launcher icons) appearing and disappearing. Extracted from
 * HomeActivity's onCreate().
 */
object LayoutTransitionConfigurer {
	@JvmStatic
	fun apply(viewFinder: ViewFinder, res: Resources) {
		// The dash app grid's transition is set per pager page in
		// ProfilePagerAdapter — the pages don't exist yet at this point.

		val lvDashHomeLensResults_transition = LayoutTransition()
		lvDashHomeLensResults_transition.setDuration(180L)
		lvDashHomeLensResults_transition.setStartDelay(LayoutTransition.APPEARING, 0)
		viewFinder.get<ListView>(R.id.lvDashHomeLensResults).layoutTransition = lvDashHomeLensResults_transition

		// Zero delays and a short duration so that, while reordering icons by
		// drag, the siblings slide over immediately as the empty slot moves.
		// DISAPPEARING must stay disabled: it would keep a removed icon as a
		// transient child until its animation ends, making the reorder's
		// immediate removeView+addView throw "child already has a parent" //
		val llLauncherPinnedApps_transition = LayoutTransition()
		llLauncherPinnedApps_transition.setDuration(180L)
		llLauncherPinnedApps_transition.disableTransitionType(LayoutTransition.DISAPPEARING)
		this.zeroStartDelays(llLauncherPinnedApps_transition)
		viewFinder.get<LinearLayout>(R.id.llLauncherPinnedApps).layoutTransition = llLauncherPinnedApps_transition

		val llLauncherRunningApps_transition = LayoutTransition()
		llLauncherRunningApps_transition.setStartDelay(LayoutTransition.APPEARING, 0)
		viewFinder.get<LinearLayout>(R.id.llLauncherRunningApps).layoutTransition = llLauncherRunningApps_transition

		val llDashSearchContainer_transition = LayoutTransition()
		this.zeroStartDelays(llDashSearchContainer_transition)
		viewFinder.get<LinearLayout>(R.id.llDashSearchContainer).layoutTransition = llDashSearchContainer_transition

		val llLauncherAndDashContainer_transition = LayoutTransition()
		this.zeroStartDelays(llLauncherAndDashContainer_transition)
		llLauncherAndDashContainer_transition.setDuration(
			res.getInteger(android.R.integer.config_shortAnimTime).toLong())
		// CHANGING animates the launcher's own bounds when it resizes for reasons
		// other than a child being added/removed here — chiefly a bottom dock
		// (elementary) collapsing/expanding as you swipe to a desktop whose pinned
		// set is empty/non-empty, instead of snapping to the new height.
		llLauncherAndDashContainer_transition.enableTransitionType(LayoutTransition.CHANGING)
		viewFinder.get<LinearLayout>(R.id.llLauncherAndDashContainer).layoutTransition = llLauncherAndDashContainer_transition

		val flWallpaperOverlayContainer_transition = LayoutTransition()
		this.zeroStartDelays(flWallpaperOverlayContainer_transition)
		flWallpaperOverlayContainer_transition.setDuration(
			res.getInteger(android.R.integer.config_shortAnimTime).toLong())
		viewFinder.get<FrameLayout>(R.id.flWallpaperOverlayContainer).layoutTransition = flWallpaperOverlayContainer_transition
	}

	private fun zeroStartDelays(transition: LayoutTransition) {
		transition.setStartDelay(LayoutTransition.APPEARING, 0)
		transition.setStartDelay(LayoutTransition.DISAPPEARING, 0)
		transition.setStartDelay(LayoutTransition.CHANGE_APPEARING, 0)
		transition.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0)
		transition.setStartDelay(LayoutTransition.CHANGING, 0)
	}
}
