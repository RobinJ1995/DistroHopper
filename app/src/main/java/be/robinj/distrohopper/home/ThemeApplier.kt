package be.robinj.distrohopper.home

import android.app.Activity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import be.robinj.distrohopper.R
import be.robinj.distrohopper.ViewFinder
import be.robinj.distrohopper.desktop.launcher.AppLauncher
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import be.robinj.distrohopper.theme.Location
import be.robinj.distrohopper.theme.Theme

/**
 * Applies the active theme's resources to the launcher, panel, dash, and
 * wallpaper overlay views, and positions the launcher on its themed edge.
 * Extracted from HomeActivity's applyTheme().
 */
class ThemeApplier(
	private val activity: Activity,
	private val viewFinder: ViewFinder,
	private val theme: Theme,
	private val edgeController: LauncherEdgeController,
) {
	fun apply() {
		val res = this.activity.resources
		val prefs = Preferences.getSharedPreferences(this.activity)

		// Get views
		val llLauncher = this.viewFinder.get<LinearLayout>(R.id.llLauncher)
		val llBfbSpinnerWrapper = this.viewFinder.get<LinearLayout>(llLauncher, R.id.llBfbSpinnerWrapper)
		val lalBfb = this.viewFinder.get<AppLauncher>(llBfbSpinnerWrapper, R.id.lalBfb)
		val lalPreferences = this.viewFinder.get<AppLauncher>(llLauncher, R.id.lalPreferences)
		val lalTrash = this.viewFinder.get<AppLauncher>(llLauncher, R.id.lalTrash)
		val llDash = this.viewFinder.get<LinearLayout>(R.id.llDash)
		val llDashCustomise = this.viewFinder.get<LinearLayout>(llDash, R.id.llDashCustomise)
		val imgDashBackgroundGradient = this.viewFinder.get<ImageView>(llDash, R.id.imgDashBackgroundGradient)
		val tvDashHomeTitle = this.viewFinder.get<TextView>(llDash, R.id.tvDashHomeTitle)
		val etDashSearch = this.viewFinder.get<EditText>(llDash, R.id.etDashSearch)
		val llDashRibbon = this.viewFinder.get<LinearLayout>(llDash, R.id.llDashRibbon)
		val llPanel = this.viewFinder.get<LinearLayout>(R.id.llPanel)
		val tvPanelBfb = this.viewFinder.get<TextView>(llPanel, R.id.tvPanelBfb)
		val ibPanelDashClose = this.viewFinder.get<ImageButton>(llPanel, R.id.ibPanelDashClose)
		val ibPanelCog = this.viewFinder.get<ImageButton>(llPanel, R.id.ibPanelCog)

		// Apply theme
		llPanel.setBackgroundResource(this.theme.panel_background)
		ibPanelCog.setImageResource(this.theme.panel_preferences_image)
		ibPanelDashClose.setImageResource(this.theme.panel_close_image)
		imgDashBackgroundGradient.setImageResource(this.theme.dash_background_gradient)
		lalBfb.setIcon(res.getDrawable(this.theme.launcher_bfb_image))
		lalPreferences.setIcon(res.getDrawable(this.theme.launcher_preferences_image))
		lalTrash.setIcon(res.getDrawable(this.theme.launcher_trash_image))

		val llPanel_layoutParams = llPanel.layoutParams as RelativeLayout.LayoutParams
		llPanel_layoutParams.height = res.getDimension(this.theme.panel_height).toInt()

		val expandLlLauncher = res.getBoolean(this.theme.launcher_expand)
		val launcherEdge = Location.of(prefs.getInt(Preference.LAUNCHER_EDGE.getName(),
			res.getInteger(this.theme.launcher_location)))
		this.edgeController.applyLauncherEdge(launcherEdge, expandLlLauncher)
		this.applyDashIconWidth(prefs.getInt(Preference.DASHICON_WIDTH.getName(),
			Preference.DASHICON_WIDTH.getDefault()))

		when (this.theme.lalPreferences_getLocation(res, prefs)!!) {
			Location.NONE ->
				lalPreferences.visibility = View.GONE
			Location.TOP, Location.LEFT -> {
				val posLlBfbSpinnerWrapper = llLauncher.indexOfChild(llBfbSpinnerWrapper)
				val posLalPreferences = if (posLlBfbSpinnerWrapper == 0) 1 else 0

				llLauncher.removeView(lalPreferences)
				llLauncher.addView(lalPreferences, posLalPreferences)
			}
			Location.RIGHT, Location.BOTTOM ->
				lalPreferences.visibility = View.VISIBLE
		}

		when (Location.of(res.getInteger(this.theme.launcher_bfb_location))) {
			Location.NONE ->
				llBfbSpinnerWrapper.visibility = View.GONE
			Location.TOP, Location.LEFT ->
				llBfbSpinnerWrapper.visibility = View.VISIBLE
			Location.RIGHT, Location.BOTTOM -> {
				val posLalPreferences = llLauncher.indexOfChild(lalPreferences)
				val posLalTrash = llLauncher.indexOfChild(lalTrash)
				val posLlBfbSpinnerWrapper =
					(if (posLalPreferences > 1) posLalPreferences else posLalTrash) - 1

				llLauncher.removeView(llBfbSpinnerWrapper)
				llLauncher.addView(llBfbSpinnerWrapper, posLlBfbSpinnerWrapper)
			}
		}

		when (Location.of(res.getInteger(this.theme.panel_bfb_location))) {
			Location.NONE ->
				tvPanelBfb.visibility = View.GONE
			Location.LEFT ->
				tvPanelBfb.visibility = View.VISIBLE
			else -> {}
		}

		tvPanelBfb.setText(res.getString(this.theme.panel_bfb_text))
		tvPanelBfb.setTextColor(res.getColor(this.theme.panel_bfb_text_colour))

		tvDashHomeTitle.setTextColor(res.getColor(this.theme.dash_applauncher_text_colour))
		tvDashHomeTitle.setShadowLayer(5F, 2F, 2F, res.getColor(this.theme.dash_applauncher_text_shadow_colour))

		etDashSearch.setBackgroundResource(this.theme.dash_search_background)
		etDashSearch.setTextColor(res.getColor(this.theme.dash_search_text_colour))

		llDashRibbon.visibility =
			if (res.getBoolean(this.theme.dash_ribbon_show)) View.VISIBLE else View.GONE

		this.viewFinder.get<FrameLayout>(R.id.flWallpaperOverlay)
			.setBackgroundResource(this.theme.wallpaper_overlay)
		this.viewFinder.get<FrameLayout>(R.id.flWallpaperOverlayWhenDashOpened)
			.setBackgroundResource(this.theme.wallpaper_overlay_when_dash_opened)

		// I don't like this, but it's just too much of a pain to do it properly.
		for (i in 0 until llDashCustomise.childCount) {
			val container = llDashCustomise.getChildAt(i) as? LinearLayout ?: continue

			for (j in 0 until container.childCount) {
				val textView = container.getChildAt(j) as? TextView ?: continue

				textView.setTextColor(res.getColor(this.theme.dash_customise_text_colour))
				textView.setShadowLayer(5F, 2F, 2F, res.getColor(this.theme.dash_customise_text_shadow_colour))
			}
		}
	}

	/**
	 * Set the width of icons in the Dash.
	 * @param width The value of the [Preference.DASHICON_WIDTH] user preference.
	 */
	fun applyDashIconWidth(width: Int) {
		val density = this.activity.resources.displayMetrics.density

		this.viewFinder.get<GridView>(R.id.gvDashHomeApps).setColumnWidth(Math.round((80 // 80 is the minimum
			+ width)
			* density)) // Adjust for the screen's pixel density
	}
}
