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
import be.robinj.distrohopper.desktop.dash.DashGridSizer
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
		val lalAppInfo = this.viewFinder.get<AppLauncher>(llLauncher, R.id.lalAppInfo)
		val llDash = this.viewFinder.get<LinearLayout>(R.id.llDash)
		val llDashCustomise = this.viewFinder.get<LinearLayout>(llDash, R.id.llDashCustomise)
		val imgDashBackgroundGradient = this.viewFinder.get<ImageView>(llDash, R.id.imgDashBackgroundGradient)
		val etDashSearch = this.viewFinder.get<EditText>(llDash, R.id.etDashSearch)
		val llDashRibbon = this.viewFinder.get<LinearLayout>(llDash, R.id.llDashRibbon)
		val llPanel = this.viewFinder.get<LinearLayout>(R.id.llPanel)
		val tvPanelBfb = this.viewFinder.get<TextView>(llPanel, R.id.tvPanelBfb)
		val ibPanelDashClose = this.viewFinder.get<ImageButton>(llPanel, R.id.ibPanelDashClose)
		val ibPanelCog = this.viewFinder.get<ImageButton>(llPanel, R.id.ibPanelCog)

		// Apply theme
		llPanel.setBackgroundResource(this.theme.panel_background)
		this.viewFinder.get<LinearLayout>(R.id.llStatusBar)
			.setBackgroundResource(this.theme.statusbar_background_resolved(res, prefs))
		ibPanelCog.setImageResource(this.theme.panel_preferences_image)
		ibPanelDashClose.setImageResource(this.theme.panel_close_image)
		imgDashBackgroundGradient.setImageResource(this.theme.dash_background_gradient)
		lalPreferences.setIcon(res.getDrawable(this.theme.launcher_preferences_image, null))
		lalTrash.setIcon(res.getDrawable(this.theme.launcher_trash_image, null))
		lalAppInfo.setIcon(res.getDrawable(this.theme.launcher_appinfo_image, null))

		val llPanel_layoutParams = llPanel.layoutParams as RelativeLayout.LayoutParams
		llPanel_layoutParams.height = res.getDimension(this.theme.panel_height).toInt()

		val expandLlLauncher = res.getBoolean(this.theme.launcher_expand)
		val launcherEdge = Location.of(prefs.getInt(Preference.LAUNCHER_EDGE.getName(),
			res.getInteger(this.theme.launcher_location)))
		lalBfb.setIcon(res.getDrawable(when (launcherEdge) {
			Location.LEFT, Location.RIGHT -> this.theme.launcher_bfb_image_vertical
			else -> this.theme.launcher_bfb_image
		}, null))
		this.edgeController.applyLauncherEdge(launcherEdge, expandLlLauncher)
		this.applyDashColumns()

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

		// The menu button (BFB) is hidden either when the theme has no BFB or when
		// the user has hidden it on a theme that lets them; its resolved location
		// also picks which end of the launcher it sits at (see Theme).
		when (this.theme.launcherBfbLocationResolved(res, prefs)) {
			Location.NONE -> {
				// The loading spinner shares the wrapper with the BFB: while
				// startup loading still has it spinning, keep the wrapper up (only
				// the BFB itself stays hidden) so a hidden BFB doesn't take the
				// spinner down with it. StartupLoader collapses the wrapper once
				// loading finishes.
				val lalSpinner = this.viewFinder.get<View>(llBfbSpinnerWrapper, R.id.lalSpinner)
				lalBfb.visibility = View.GONE
				llBfbSpinnerWrapper.visibility =
					if (lalSpinner.visibility == View.VISIBLE) View.VISIBLE else View.GONE
			}
			Location.TOP, Location.LEFT ->
				llBfbSpinnerWrapper.visibility = View.VISIBLE
			Location.RIGHT, Location.BOTTOM -> {
				llBfbSpinnerWrapper.visibility = View.VISIBLE

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
		tvPanelBfb.setTextColor(res.getColor(this.theme.panel_bfb_text_colour, null))
		/*
		 * Colour resources have no intrinsic size, so themes without a BFB
		 * image (transparent) get no icon rather than an empty gap.
		 */
		tvPanelBfb.setCompoundDrawablesWithIntrinsicBounds(this.theme.panel_bfb_image, 0, 0, 0)
		tvPanelBfb.compoundDrawablePadding =
			(6 * this.activity.resources.displayMetrics.density).toInt()

		// The dash title now lives in each pager page (styled by ProfilePagerAdapter).

		etDashSearch.setBackgroundResource(this.theme.dash_search_background)
		etDashSearch.setTextColor(res.getColor(this.theme.dash_search_text_colour, null))
		/*
		 * Themes can pin the search field to a fixed width, centred (COSMIC's
		 * library search does not span the dash); 0 keeps it full-width.
		 */
		val searchWidth = res.getDimensionPixelSize(this.theme.dash_search_width)
		if (searchWidth > 0) {
			val searchParams = etDashSearch.layoutParams as LinearLayout.LayoutParams
			searchParams.width = searchWidth
			etDashSearch.layoutParams = searchParams
			this.viewFinder.get<LinearLayout>(llDash, R.id.llDashSearchContainer).gravity =
				android.view.Gravity.CENTER_HORIZONTAL
		}

		llDashRibbon.visibility =
			if (res.getBoolean(this.theme.dash_ribbon_show)) View.VISIBLE else View.GONE

		this.viewFinder.get<FrameLayout>(R.id.flWallpaperOverlay)
			.setBackgroundResource(this.theme.wallpaper_overlay)
		this.viewFinder.get<FrameLayout>(R.id.flWallpaperOverlayWhenDashOpened)
			.setBackgroundResource(this.theme.wallpaper_overlay_when_dash_opened)

		/*
		 * Customise mode's settings live on opaque cards and are white there
		 * whatever the theme; only its header and group labels sit on the bare
		 * dash, which is light on some themes (elementary, MATE) and dark on
		 * others, so those still take the theme's colour and shadow.
		 */
		val customiseTextColour = res.getColor(this.theme.dash_customise_text_colour, null)
		val customiseShadowColour = res.getColor(this.theme.dash_customise_text_shadow_colour, null)
		for (id in CUSTOMISE_TEXT_ON_DASH) {
			val textView = this.viewFinder.get<TextView>(llDashCustomise, id)

			textView.setTextColor(customiseTextColour)
			textView.setShadowLayer(5F, 2F, 2F, customiseShadowColour)
		}
	}

	/**
	 * Apply the dash grid's column count (see [DashGrid]) to the dash apps grid.
	 *
	 * The grid lives on the current pager page, which only exists once the dash
	 * has been laid out; null-safe so a theme apply before that still works.
	 * Once apps load the binder owns the pager and applies it per page; this is
	 * the pre-load path on the single laid-out grid.
	 */
	fun applyDashColumns() {
		this.activity.findViewById<GridView>(R.id.gvDashHomeApps)?.let { DashGridSizer.apply(it) }
	}

	private companion object {
		/** Customise mode's text that sits on the dash itself rather than on a card. */
		val CUSTOMISE_TEXT_ON_DASH = intArrayOf(
			R.id.tvCustomiseTitle,
			R.id.tvCustomiseDone,
			R.id.tvCustomiseGroupLauncher,
			R.id.tvCustomiseGroupDash,
			R.id.tvCustomiseGroupDesktop,
			R.id.tvCustomiseGroupPanel,
		)
	}
}
