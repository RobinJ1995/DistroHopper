package be.robinj.distrohopper.home

import android.app.Activity
import android.content.Context
import android.animation.ValueAnimator
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
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
import be.robinj.distrohopper.preferences.Preferences
import be.robinj.distrohopper.preferences.PreferencesRepository
import be.robinj.distrohopper.theme.Theme
import java.util.function.Consumer

/**
 * Opens and closes the dash: shows/hides the dash itself, blurs the system
 * wallpaper (cross-window) and the widgets (RenderEffect), and adjusts the
 * panel. The visual work (the gradual blur ramp plus the theme's
 * dash_animation) is delegated to DashAnimator. The swipe*() family is the
 * finger-tracked variant driven by HomeGestureController: begin/update follow
 * the finger, and end either commits (applying the same chrome and isOpen
 * change as open()/close()) or settles back. Extracted from HomeActivity;
 * the customise-mode relaunch on close stays there, as it manipulates the
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

	private val animator = DashAnimator(this.activity, this.viewFinder, this.theme, this.prefs)

	/*
	 * Fading the panel and status bar between their resting and dash-opened
	 * backgrounds keeps them in step with the dash overlay's fade, instead of
	 * flashing the bare wallpaper through. The dash-opened background sits at
	 * full alpha underneath and only the resting one fades on top, so a pair
	 * of identical backgrounds stays rock solid (a crossfade would dip towards
	 * translucency halfway). Reused across open/close so a mid-flight reversal
	 * resumes from the current fade position.
	 */
	private val panelFade by lazy {
		BackgroundFade(this.activity.resources, this.theme.panel_background,
			this.theme.panel_background_when_dash_opened)
	}
	/*
	 * The themed dash-opened status bar background only applies in the
	 * normal configuration (resting on statusbar_background). When the
	 * resolver picks the panel-not-on-top background instead, the status bar
	 * keeps it through dash open/close — otherwise opening the dash would
	 * paint the opaque dash-opened layer under the (possibly transparent)
	 * resting layer, and closing could never restore it.
	 */
	private val statusBarFade by lazy {
		val resting = this.theme.statusbar_background_resolved(this.activity.resources,
			Preferences.getSharedPreferences(this.activity))
		BackgroundFade(this.activity.resources, resting,
			if (resting == this.theme.statusbar_background) {
				this.theme.statusbar_background_when_dash_opened
			} else {
				resting
			})
	}

	private class BackgroundFade(res: Resources, restingRes: Int, dashOpenedRes: Int) {
		/*
		 * mutate(): drawables from the same resource share their constant
		 * state, so without it the alpha fade would also affect the
		 * dash-opened layer below whenever both resolve to the same colour.
		 */
		private val resting = res.getDrawable(restingRes).mutate()
		val drawable = LayerDrawable(arrayOf(res.getDrawable(dashOpenedRes), this.resting))
		private var fraction = 0F // 0 = resting, 1 = dash opened
		private var animator: ValueAnimator? = null

		fun animateTo(dashOpened: Boolean, durationMs: Long) {
			this.animator?.cancel()
			if (durationMs == 0L) {
				this.fraction = if (dashOpened) 1F else 0F
				this.resting.alpha = ((1F - this.fraction) * 255F).toInt()
				this.animator = null
				return
			}
			this.animator = ValueAnimator.ofFloat(this.fraction, if (dashOpened) 1F else 0F)
				.also { animator ->
					animator.duration = durationMs
					animator.addUpdateListener {
						this.fraction = it.animatedValue as Float
						this.resting.alpha = ((1F - this.fraction) * 255F).toInt()
					}
					animator.start()
				}
		}
	}

	/** Cross-window blur can be toggled at runtime (e.g. battery saver). */
	val crossWindowBlurListener: Consumer<Boolean> = Consumer {
		if (this.isOpen) {
			this.viewFinder.get<Wallpaper>(R.id.wpWallpaper).blur(this.activity.window,
				this.activity.resources.getDimensionPixelSize(this.theme.dash_blur_radius))
		}
	}

	@JvmOverloads
	fun open(forceSearchFocus: Boolean = false) {
		if (this.isOpen) {
			// The dash may already be open because viewModel.openDash() drove the
			// state binder's flagless open() before this flag-carrying call ran
			// (e.g. a BFB search-widget tap). Still honour the search focus.
			if (forceSearchFocus) this.focusSearchField()
			return
		}

		this.animator.open(this.blurRadiusPx())
		this.applyOpenedChrome(forceSearchFocus)

		this.isOpen = true
	}

	fun close() {
		if (! this.isOpen) {
			return
		}

		this.animator.close(this.blurRadiusPx()) { this.teardownDashViews() }
		this.applyClosedChrome()

		this.isOpen = false
	}

	/**
	 * Begins a finger-tracked open; the visual side lives in DashAnimator.
	 * Returns false when there is nothing to track (already open, or battery
	 * saver) — the caller falls back to the instant open().
	 */
	fun swipeOpenBegin(): Boolean {
		if (this.isOpen) {
			return false
		}

		return this.animator.swipeBegin(opening = true, this.blurRadiusPx())
	}

	/**
	 * Records the screen point an opening swipe began at, so the icon genie can
	 * expand out of it when the theme has no visible BFB to grow from.
	 */
	fun setSwipeOrigin(x: Float, y: Float) = this.animator.setSwipeOrigin(x, y)

	/** Like swipeOpenBegin(), but for swiping an open dash closed. */
	fun swipeCloseBegin(): Boolean {
		if (! this.isOpen) {
			return false
		}

		return this.animator.swipeBegin(opening = false, this.blurRadiusPx())
	}

	fun swipeUpdate(openness: Float) = this.animator.swipeUpdate(openness)

	/** The slide distance a swipe tracks over, for finger-to-fraction mapping. */
	val swipeDistancePx: Float
		get() = this.animator.swipeDistancePx

	/** Current openness of the in-flight swipe (0 = closed, 1 = open). */
	val swipeOpenness: Float
		get() = this.animator.swipeOpenness

	/**
	 * Ends a finger-tracked open: [commit] settles the dash fully open and
	 * applies the opened chrome, otherwise it settles back closed.
	 */
	fun swipeOpenEnd(commit: Boolean, forceSearchFocus: Boolean = false) {
		if (this.isOpen || ! this.animator.swipeInProgress) {
			return
		}

		if (commit) {
			this.animator.swipeSettle(open = true)
			this.applyOpenedChrome(forceSearchFocus)
			this.isOpen = true
		} else {
			this.animator.swipeSettle(open = false) { this.teardownDashViews() }
		}
	}

	/** Ends a finger-tracked close, mirroring swipeOpenEnd(). */
	fun swipeCloseEnd(commit: Boolean) {
		if (! this.isOpen || ! this.animator.swipeInProgress) {
			return
		}

		if (commit) {
			this.animator.swipeSettle(open = false) { this.teardownDashViews() }
			this.applyClosedChrome()
			this.isOpen = false
		} else {
			this.animator.swipeSettle(open = true)
		}
	}

	private fun blurRadiusPx(): Int =
		this.activity.resources.getDimensionPixelSize(this.theme.dash_blur_radius)

	/** The non-dash side of opening: the panel's close button, backgrounds and search focus. */
	private fun applyOpenedChrome(forceSearchFocus: Boolean = false) {
		val llPanel = this.viewFinder.get<LinearLayout>(R.id.llPanel)

		if (forceSearchFocus || this.prefs.getBoolean(Preference.DASH_SEARCH_FOCUS_ON_OPEN, false))
			this.focusSearchField()

		if (this.activity.resources.getInteger(this.theme.panel_close_location) != -1)
			this.viewFinder.get<ImageButton>(llPanel, R.id.ibPanelDashClose).visibility = View.VISIBLE

		if (this.activity.resources.getBoolean(this.theme.panel_background_dynamic_when_dash_opened)) {
			llPanel.setBackgroundColor(this.chameleonicBgColour)
			this.viewFinder.get<LinearLayout>(R.id.llStatusBar)
				.setBackgroundColor(this.chameleonicBgColour)
		} else {
			val duration = if (this.animator.animationsEnabled) DashAnimator.OPEN_DURATION_MS else 0L
			llPanel.background = this.panelFade.drawable
			this.viewFinder.get<LinearLayout>(R.id.llStatusBar).background =
				this.statusBarFade.drawable
			this.panelFade.animateTo(dashOpened = true, duration)
			this.statusBarFade.animateTo(dashOpened = true, duration)
		}
	}

	/** Focuses the dash search field and raises the keyboard, deferred until laid out. */
	private fun focusSearchField() {
		val llDashContent = this.viewFinder.get<LinearLayout>(R.id.llDashContent)
		val etDashSearch = this.viewFinder.get<EditText>(R.id.etDashSearch)
		// Defer until the dash view is laid out/visible so the focus and
		// keyboard request take effect. Skip in customise mode, where
		// llDashContent (which holds the search field) is hidden — otherwise
		// the keyboard would pop up over the customise controls.
		etDashSearch.post {
			if (llDashContent.visibility == View.VISIBLE && etDashSearch.requestFocus()) {
				val imm = this.activity.getSystemService(Context.INPUT_METHOD_SERVICE)
					as InputMethodManager?
				imm?.showSoftInput(etDashSearch, InputMethodManager.SHOW_IMPLICIT)
			}
		}
	}

	/** Hides the dash and restores the overlays once a close has settled. */
	private fun teardownDashViews() {
		this.viewFinder.get<LinearLayout>(R.id.llDash).visibility = View.GONE

		this.viewFinder.get<FrameLayout>(R.id.flWallpaperOverlay).visibility = View.VISIBLE
		this.viewFinder.get<FrameLayout>(R.id.flWallpaperOverlayWhenDashOpened).visibility =
			View.INVISIBLE
	}

	/** The non-dash side of closing: search field, close button, backgrounds, IME. */
	private fun applyClosedChrome() {
		val llPanel = this.viewFinder.get<LinearLayout>(R.id.llPanel)
		val etDashSearch = this.viewFinder.get<EditText>(R.id.etDashSearch)

		etDashSearch.setText("")
		etDashSearch.clearFocus()

		if (this.activity.resources.getInteger(this.theme.panel_close_location) != -1)
			this.viewFinder.get<ImageButton>(llPanel, R.id.ibPanelDashClose).visibility = View.INVISIBLE

		if (this.activity.resources.getBoolean(this.theme.panel_background_dynamic_when_dash_opened)) {
			llPanel.setBackgroundResource(this.theme.panel_background)
			this.viewFinder.get<LinearLayout>(R.id.llStatusBar)
				.setBackgroundResource(this.theme.statusbar_background_resolved(
					this.activity.resources, Preferences.getSharedPreferences(this.activity)))
		} else {
			val duration = if (this.animator.animationsEnabled) DashAnimator.CLOSE_DURATION_MS else 0L
			this.panelFade.animateTo(dashOpened = false, duration)
			this.statusBarFade.animateTo(dashOpened = false, duration)
		}

		val imm = this.activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
		imm?.hideSoftInputFromWindow(
			this.activity.window.decorView.rootView.windowToken, 0)
	}
}
