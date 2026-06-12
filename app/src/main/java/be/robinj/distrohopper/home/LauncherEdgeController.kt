package be.robinj.distrohopper.home

import android.annotation.SuppressLint
import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import androidx.core.graphics.Insets
import be.robinj.distrohopper.R
import be.robinj.distrohopper.ViewFinder
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.PreferencesRepository
import be.robinj.distrohopper.theme.Location
import be.robinj.distrohopper.theme.Theme
import be.robinj.distrohopper.widgets.WidgetsContainer

/**
 * Owns the position of the launcher bar and the panel: moves and reorients
 * the view tree when the launcher is docked to another screen edge, and keeps
 * the widget area clear of both the launcher and the navigation bar.
 * Extracted from HomeActivity.
 */
class LauncherEdgeController(
	private val activity: Activity,
	private val viewFinder: ViewFinder,
	private val theme: Theme,
	private val prefs: PreferencesRepository,
) {
	var launcherEdge: Location = Location.NONE
		private set
	var navigationInsets: Insets = Insets.NONE

	@SuppressLint("ResourceType")
	fun applyLauncherEdge(edge: Location, expand: Boolean) {
		this.launcherEdge = edge

		val llPanel = this.viewFinder.get<LinearLayout>(R.id.llPanel)
		val ibPanelDashClose = this.viewFinder.get<ImageButton>(llPanel, R.id.ibPanelDashClose)
		val ibPanelCog = this.viewFinder.get<ImageButton>(llPanel, R.id.ibPanelCog)
		val llLauncherAndDashContainer = this.viewFinder.get<LinearLayout>(R.id.llLauncherAndDashContainer)
		val llLauncher = this.viewFinder.get<LinearLayout>(llLauncherAndDashContainer, R.id.llLauncher)
		val llDash = this.viewFinder.get<LinearLayout>(llLauncherAndDashContainer, R.id.llDash)
		val llLauncherAppsContainer = this.viewFinder.get<LinearLayout>(llLauncher, R.id.llLauncherAppsContainer)
		val llLauncherPinnedApps = this.viewFinder.get<LinearLayout>(R.id.llLauncherPinnedApps)
		val llLauncherRunningApps = this.viewFinder.get<LinearLayout>(R.id.llLauncherRunningApps)
		val llBfbSpinnerWrapper = this.viewFinder.get<LinearLayout>(llLauncher, R.id.llBfbSpinnerWrapper)
		val scrLauncherAppsContainer = this.viewFinder.get<ScrollView>(llLauncher, R.id.scrLauncherAppsContainer)
		val scrLauncherAppsContainerHorizontal = this.viewFinder.get<HorizontalScrollView>(llLauncher, R.id.scrLauncherAppsContainerHorizontal)
		var llLauncher_layoutParams = llLauncher.layoutParams as LinearLayout.LayoutParams

		val taLauncherMargins = this.activity.resources.obtainTypedArray(this.theme.launcher_margin)
		val launcherMargins = intArrayOf(
			taLauncherMargins.getLayoutDimension(0, 0),
			taLauncherMargins.getLayoutDimension(1, 0),
			taLauncherMargins.getLayoutDimension(2, 0),
			taLauncherMargins.getLayoutDimension(3, 0))
		val rotateLauncherMargins = edge.n

		val launcherMarginsRotated = IntArray(4)
		for (i in launcherMargins.indices)
			launcherMarginsRotated[(i + rotateLauncherMargins) % launcherMargins.size] = launcherMargins[i]

		when (edge) {
			Location.TOP -> {
				llLauncherAndDashContainer.orientation = LinearLayout.VERTICAL
				llLauncher.orientation = LinearLayout.HORIZONTAL
				llBfbSpinnerWrapper.orientation = LinearLayout.HORIZONTAL
				llLauncherAppsContainer.orientation = LinearLayout.HORIZONTAL
				llLauncherPinnedApps.orientation = LinearLayout.HORIZONTAL
				llLauncherRunningApps.orientation = LinearLayout.HORIZONTAL

				llLauncherAndDashContainer.gravity = Gravity.TOP or Gravity.CENTER

				llLauncher_layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

				scrLauncherAppsContainer.visibility = View.GONE
				scrLauncherAppsContainer.removeView(llLauncherAppsContainer)
				scrLauncherAppsContainerHorizontal.addView(llLauncherAppsContainer)
				scrLauncherAppsContainerHorizontal.visibility = View.VISIBLE

				val llLauncherPinnedApps_layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
				llLauncherPinnedApps_layoutParams.gravity = Gravity.LEFT
				llLauncherPinnedApps.layoutParams = llLauncherPinnedApps_layoutParams

				val llLauncherRunningApps_layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
				llLauncherRunningApps_layoutParams.gravity = Gravity.LEFT
				llLauncherRunningApps.layoutParams = llLauncherRunningApps_layoutParams
			}
			Location.BOTTOM -> {
				llLauncherAndDashContainer.orientation = LinearLayout.VERTICAL
				llLauncher.orientation = LinearLayout.HORIZONTAL
				llBfbSpinnerWrapper.orientation = LinearLayout.HORIZONTAL
				llLauncherAppsContainer.orientation = LinearLayout.HORIZONTAL
				llLauncherPinnedApps.orientation = LinearLayout.HORIZONTAL
				llLauncherRunningApps.orientation = LinearLayout.HORIZONTAL

				llLauncherAndDashContainer.gravity = Gravity.BOTTOM or Gravity.CENTER

				llLauncherAndDashContainer.removeView(llLauncher)
				llLauncherAndDashContainer.removeView(llDash)

				llLauncherAndDashContainer.addView(llDash)
				llLauncherAndDashContainer.addView(llLauncher)

				llLauncher_layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

				scrLauncherAppsContainer.visibility = View.GONE
				scrLauncherAppsContainer.removeView(llLauncherAppsContainer)
				scrLauncherAppsContainerHorizontal.addView(llLauncherAppsContainer)
				scrLauncherAppsContainerHorizontal.visibility = View.VISIBLE

				val llLauncherPinnedApps_layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
				llLauncherPinnedApps_layoutParams.gravity = Gravity.LEFT
				llLauncherPinnedApps.layoutParams = llLauncherPinnedApps_layoutParams

				val llLauncherRunningApps_layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
				llLauncherRunningApps_layoutParams.gravity = Gravity.LEFT
				llLauncherRunningApps.layoutParams = llLauncherRunningApps_layoutParams
			}
			Location.RIGHT -> {
				llLauncherAndDashContainer.gravity = Gravity.RIGHT or Gravity.CENTER

				llLauncherAndDashContainer.removeView(llLauncher)
				llLauncherAndDashContainer.removeView(llDash)

				llLauncherAndDashContainer.addView(llDash)
				llLauncherAndDashContainer.addView(llLauncher)
			}
			Location.LEFT -> {
				llLauncherAndDashContainer.gravity = Gravity.LEFT or Gravity.CENTER
			}
			else -> {}
		}

		if (! expand) {
			llLauncher_layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
			llLauncher.layoutParams = llLauncher_layoutParams
		}

		val panelSwapClosePreferencesWhenLauncherLocation =
			this.activity.resources.getIntArray(this.theme.panel_swap_close_preferences_when_launcher_location)
		if (panelSwapClosePreferencesWhenLauncherLocation.contains(edge.n)) {
			llPanel.removeView(ibPanelDashClose)
			llPanel.addView(ibPanelDashClose, llPanel.indexOfChild(ibPanelCog))
			llPanel.removeView(ibPanelCog)
			llPanel.addView(ibPanelCog, 0)
		}

		llLauncher_layoutParams.setMargins(launcherMarginsRotated[3], launcherMarginsRotated[0],
			launcherMarginsRotated[1], launcherMarginsRotated[2])
		llLauncher.layoutParams = llLauncher_layoutParams

		/*
		 * The dash must never draw over the launcher, even while a dash
		 * animation scales it beyond its own bounds (the MATE genie). For the
		 * bottom/right edges the child order already takes care of that, but
		 * for top/left the dash is the later sibling; a minimal Z lift keeps
		 * the launcher on top everywhere without casting a visible shadow.
		 */
		llLauncher.translationZ = 1F
	}

	fun applyPanelEdge(edge: Location) {
		val llPanel = this.viewFinder.get<LinearLayout>(R.id.llPanel)
		val llLauncherAndDashContainer =
			this.viewFinder.get<LinearLayout>(R.id.llLauncherAndDashContainer)
		val vgWidgets = this.viewFinder.get<View>(R.id.vgWidgets)
		val panelParams = llPanel.layoutParams as RelativeLayout.LayoutParams
		val containerParams =
			llLauncherAndDashContainer.layoutParams as RelativeLayout.LayoutParams
		val widgetsParams = vgWidgets.layoutParams as RelativeLayout.LayoutParams

		when (this.effectivePanelEdge(edge)) {
			Location.TOP -> {
				panelParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
				panelParams.addRule(RelativeLayout.BELOW, R.id.llStatusBar)
				containerParams.removeRule(RelativeLayout.ABOVE)
				containerParams.addRule(RelativeLayout.BELOW, R.id.llPanel)
				widgetsParams.removeRule(RelativeLayout.ABOVE)
				widgetsParams.addRule(RelativeLayout.BELOW, R.id.llPanel)
				widgetsParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)

				llPanel.alpha = this.prefs.getInt(Preference.PANEL_OPACITY, 100).toFloat() / 100F
			}
			Location.BOTTOM -> {
				panelParams.removeRule(RelativeLayout.BELOW)
				panelParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
				containerParams.addRule(RelativeLayout.BELOW, R.id.llStatusBar)
				containerParams.addRule(RelativeLayout.ABOVE, R.id.llPanel)
				widgetsParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
				widgetsParams.removeRule(RelativeLayout.BELOW)
				widgetsParams.addRule(RelativeLayout.BELOW, R.id.llStatusBar)
				widgetsParams.addRule(RelativeLayout.ABOVE, R.id.llPanel)

				llPanel.alpha = this.prefs.getInt(Preference.PANEL_OPACITY, 100).toFloat() / 100F
			}
			Location.NONE ->
				llPanel.visibility = View.GONE
			else -> {}
		}

		llPanel.layoutParams = panelParams
		llLauncherAndDashContainer.layoutParams = containerParams
		vgWidgets.layoutParams = widgetsParams
	}

	/*
	 * Themes that support a bottom panel (MATE) place the panel on whichever
	 * horizontal edge the launcher leaves free: at the bottom when the
	 * launcher is docked to the top, at the top otherwise. The user's only
	 * say is whether the panel is shown at all.
	 */
	private fun effectivePanelEdge(edge: Location): Location {
		if (edge == Location.NONE) {
			return edge
		}

		val res = this.activity.resources
		if (! res.getIntArray(this.theme.panel_location_supported).contains(Location.BOTTOM.n)) {
			return edge
		}

		val launcherEdge = Location.of(this.prefs.getInt(Preference.LAUNCHER_EDGE,
			res.getInteger(this.theme.launcher_location)))
		return if (launcherEdge == Location.TOP) Location.BOTTOM else Location.TOP
	}

	/**
	 * Pad the widget area so that widgets only occupy the part of the home screen that is not
	 * covered by the launcher or the navigation bar. The panel is already excluded by the layout
	 * itself. The navigation insets are added separately because they pad the launcher's parent
	 * container, so they are not included in the launcher's own dimensions.
	 */
	fun updateWidgetAreaInsets(vgWidgets: WidgetsContainer, llLauncher: View) {
		val nav = this.navigationInsets

		when (this.launcherEdge) {
			Location.LEFT ->
				vgWidgets.setPadding(llLauncher.width + nav.left, 0, nav.right, nav.bottom)
			Location.RIGHT ->
				vgWidgets.setPadding(nav.left, 0, llLauncher.width + nav.right, nav.bottom)
			Location.TOP ->
				vgWidgets.setPadding(nav.left, llLauncher.height, nav.right, nav.bottom)
			Location.BOTTOM ->
				vgWidgets.setPadding(nav.left, 0, nav.right, llLauncher.height + nav.bottom)
			else ->
				vgWidgets.setPadding(nav.left, 0, nav.right, nav.bottom)
		}
	}
}
