package be.robinj.distrohopper.home

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import be.robinj.distrohopper.R
import be.robinj.distrohopper.ViewFinder
import be.robinj.distrohopper.desktop.Wallpaper
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.PreferencesRepository
import be.robinj.distrohopper.theme.Theme
import be.robinj.distrohopper.widgets.WidgetsContainer
import java.util.function.Consumer

/**
 * Opens and closes the dash: shows/hides the dash itself, blurs the system
 * wallpaper (cross-window) and the widgets (RenderEffect), and adjusts the
 * panel. The visual work (instant or animated, per the theme's dash_animation)
 * is delegated to DashAnimator. Extracted from HomeActivity; the
 * customise-mode relaunch on close stays there, as it manipulates the
 * activity's intent.
 */
class DashController(
	private val activity: Activity,
	private val viewFinder: ViewFinder,
	private val theme: Theme,
	private val prefs: PreferencesRepository,
) {
	var isOpen: Boolean = false
		private set

	/** Average wallpaper colour; set once the wallpaper has been analysed. */
	var chameleonicBgColour: Int = Color.argb(25, 0, 0, 0)

	private val animator = DashAnimator(this.activity, this.viewFinder, this.theme)

	/** Cross-window blur can be toggled at runtime (e.g. battery saver). */
	val crossWindowBlurListener: Consumer<Boolean> = Consumer {
		if (this.isOpen) {
			this.viewFinder.get<Wallpaper>(R.id.wpWallpaper).blur(this.activity.window,
				this.activity.resources.getDimensionPixelSize(this.theme.dash_blur_radius))
		}
	}

	fun open() {
		if (this.isOpen) {
			return
		}

		val llPanel = this.viewFinder.get<LinearLayout>(R.id.llPanel)

		this.animator.open(
			this.activity.resources.getDimensionPixelSize(this.theme.dash_blur_radius))
		llPanel.alpha = 1F

		if (this.activity.resources.getInteger(this.theme.panel_close_location) != -1)
			this.viewFinder.get<ImageButton>(llPanel, R.id.ibPanelDashClose).visibility = View.VISIBLE

		if (this.activity.resources.getBoolean(this.theme.panel_background_dynamic_when_dash_opened)) {
			llPanel.setBackgroundColor(this.chameleonicBgColour)
			this.viewFinder.get<LinearLayout>(R.id.llStatusBar)
				.setBackgroundColor(this.chameleonicBgColour)
		}

		this.isOpen = true
	}

	fun close() {
		if (! this.isOpen) {
			return
		}

		val llPanel = this.viewFinder.get<LinearLayout>(R.id.llPanel)
		val etDashSearch = this.viewFinder.get<EditText>(R.id.etDashSearch)

		this.animator.close(
			this.activity.resources.getDimensionPixelSize(this.theme.dash_blur_radius)) {
			this.viewFinder.get<LinearLayout>(R.id.llDash).visibility = View.GONE
			this.viewFinder.get<Wallpaper>(R.id.wpWallpaper).unblur(this.activity.window)
			this.viewFinder.get<WidgetsContainer>(R.id.vgWidgets).setRenderEffect(null)

			this.viewFinder.get<FrameLayout>(R.id.flWallpaperOverlay).visibility = View.VISIBLE
			this.viewFinder.get<FrameLayout>(R.id.flWallpaperOverlayWhenDashOpened).visibility =
				View.INVISIBLE
		}
		etDashSearch.setText("")
		etDashSearch.clearFocus()

		if (this.activity.resources.getInteger(this.theme.panel_close_location) != -1)
			this.viewFinder.get<ImageButton>(llPanel, R.id.ibPanelDashClose).visibility = View.INVISIBLE

		llPanel.alpha = this.prefs.getInt(Preference.PANEL_OPACITY, 100).toFloat() / 100F

		if (this.activity.resources.getBoolean(this.theme.panel_background_dynamic_when_dash_opened)) {
			llPanel.setBackgroundResource(this.theme.panel_background)
			this.viewFinder.get<LinearLayout>(R.id.llStatusBar).setBackgroundColor(
				this.activity.resources.getColor(android.R.color.black))
		}

		val imm = this.activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
		imm?.hideSoftInputFromWindow(
			this.activity.window.decorView.rootView.windowToken, 0)

		this.isOpen = false
	}
}
