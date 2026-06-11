package be.robinj.distrohopper.home

import android.animation.LayoutTransition
import android.content.res.Resources
import android.widget.FrameLayout
import android.widget.GridView
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
		val gvDashHomeApps_transition = LayoutTransition()
		gvDashHomeApps_transition.setDuration(180L)
		gvDashHomeApps_transition.setStartDelay(LayoutTransition.APPEARING, 0)
		viewFinder.get<GridView>(R.id.gvDashHomeApps).layoutTransition = gvDashHomeApps_transition

		val lvDashHomeLensResults_transition = LayoutTransition()
		lvDashHomeLensResults_transition.setDuration(180L)
		lvDashHomeLensResults_transition.setStartDelay(LayoutTransition.APPEARING, 0)
		viewFinder.get<ListView>(R.id.lvDashHomeLensResults).layoutTransition = lvDashHomeLensResults_transition

		val llLauncherPinnedApps_transition = LayoutTransition()
		llLauncherPinnedApps_transition.setStartDelay(LayoutTransition.APPEARING, 0)
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
