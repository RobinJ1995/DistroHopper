package be.robinj.distrohopper.home

import android.app.Activity
import android.graphics.Color
import android.widget.LinearLayout
import be.robinj.distrohopper.R
import be.robinj.distrohopper.ViewFinder
import be.robinj.distrohopper.desktop.Wallpaper
import be.robinj.distrohopper.desktop.launcher.AppLauncher
import be.robinj.distrohopper.theme.Theme

/**
 * Applies the wallpaper's average colour to the launcher items and the
 * launcher/dash backgrounds, for themes with chameleonic colouring.
 * Extracted from HomeActivity's asyncInitWallpaperDone().
 */
class WallpaperColourApplier(
	private val activity: Activity,
	private val viewFinder: ViewFinder,
	private val theme: Theme,
	private val edgeController: LauncherEdgeController,
) {
	/** @return the chameleonic background colour derived from the wallpaper. */
	fun apply(wpWallpaper: Wallpaper): Int {
		val res = this.activity.resources

		val colour_opacity = res.getInteger(this.theme.launcher_applauncher_backgroundcolour_opacity)
		val bgColour_opacity = res.getInteger(this.theme.dynamic_background_opacity)

		val colour: Int
		val bgColour: Int
		if (wpWallpaper.isLiveWallpaper) {
			colour = Color.argb(40, 40, 40, 40)
			bgColour = Color.argb(bgColour_opacity, 40, 40, 40)
		} else {
			colour = wpWallpaper.getAverageColour(colour_opacity)
			bgColour = wpWallpaper.getAverageColour(bgColour_opacity)
		}

		val llLauncher = this.viewFinder.get<LinearLayout>(R.id.llLauncher)
		val llDash = this.viewFinder.get<LinearLayout>(R.id.llDash)

		if (res.getBoolean(this.theme.launcher_applauncher_backgroundcolour_dynamic)) {
			this.viewFinder.get<AppLauncher>(R.id.lalBfb).colour = colour
			this.viewFinder.get<AppLauncher>(R.id.lalPreferences).colour = colour
			this.viewFinder.get<AppLauncher>(R.id.lalSpinner).colour = colour
			this.viewFinder.get<AppLauncher>(R.id.lalTrash).colour = colour
		}

		val launcherBackgroundResources = res.obtainTypedArray(this.theme.launcher_background)
		if (res.getBoolean(this.theme.launcher_background_dynamic))
			llLauncher.setBackgroundColor(bgColour)
		else
			llLauncher.setBackgroundResource(launcherBackgroundResources.getResourceId(
				this.edgeController.launcherEdge.n, R.color.transparent))

		if (res.getBoolean(this.theme.dash_background_dynamic)) {
			llDash.setBackgroundColor(bgColour)
		} else if (this.theme.dash_background_edge != 0) {
			// A directional dash (Budgie's ear) picks its background by the
			// launcher edge, falling back to the single dash_background.
			val perEdge = res.obtainTypedArray(this.theme.dash_background_edge)
			val edgeRes = perEdge.getResourceId(
				this.edgeController.launcherEdge.n, this.theme.dash_background)
			perEdge.recycle()
			llDash.setBackgroundResource(edgeRes)
		} else {
			llDash.setBackgroundResource(this.theme.dash_background)
		}

		return bgColour
	}
}
