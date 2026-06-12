package be.robinj.distrohopper.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.RenderEffect
import android.graphics.Shader
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.LinearLayout
import androidx.core.view.OneShotPreDrawListener
import be.robinj.distrohopper.R
import be.robinj.distrohopper.ViewFinder
import be.robinj.distrohopper.desktop.Wallpaper
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.PreferencesRepository
import be.robinj.distrohopper.theme.DashAnimation
import be.robinj.distrohopper.theme.Location
import be.robinj.distrohopper.theme.Theme
import be.robinj.distrohopper.widgets.WidgetsContainer
import kotlin.math.hypot
import kotlin.math.min

/**
 * Applies the visual side of opening/closing the dash on behalf of
 * DashController. The wallpaper/widget blur and the panel opacity always
 * change gradually; how the dash itself appears is the theme's choice
 * (dash_animation):
 *  - NONE: the dash snaps in and out instantly.
 *  - GNOME: the dim overlay fades in and each app icon expands out of the BFB
 *    (slightly staggered by distance, nearest first).
 *  - CINNAMON: the dash slides in from the launcher's edge of the screen.
 *  - ELEMENTARY: the dash fades and zooms in from the Applications label.
 *  - UNITY: the dash fades in.
 * Everything is reversed on close.
 */
class DashAnimator(
	private val activity: Activity,
	private val viewFinder: ViewFinder,
	private val theme: Theme,
	private val prefs: PreferencesRepository,
) {
	private var running: AnimatorSet? = null
	private var pendingOpen: OneShotPreDrawListener? = null
	private var currentBlurRadius = 0F

	private val animation: DashAnimation
		get() = DashAnimation.of(this.activity.resources.getInteger(this.theme.dash_animation))

	fun open(blurRadiusPx: Int) {
		val reversal = this.cancelRunning()
		val llDash = this.viewFinder.get<LinearLayout>(R.id.llDash)

		if (this.animation == DashAnimation.NONE) {
			this.openInstantly()
			this.start(opening = true, freshOpen = false, blurRadiusPx)
			return
		}

		this.withLayoutTransitionsSuppressed {
			llDash.visibility = View.VISIBLE
			this.viewFinder.get<FrameLayout>(R.id.flWallpaperOverlay).visibility = View.INVISIBLE
			this.viewFinder.get<FrameLayout>(R.id.flWallpaperOverlayWhenDashOpened).visibility =
				View.VISIBLE
		}

		if (reversal) {
			this.start(opening = true, freshOpen = false, blurRadiusPx)
		} else {
			/*
			 * A fresh open needs the dash laid out first: the start transforms
			 * depend on view sizes/positions (and the grid only gets children once
			 * laid out), and they must be in place before the first frame. Nothing
			 * is drawn before the pre-draw of the traversal this VISIBLE triggered.
			 */
			this.pendingOpen = OneShotPreDrawListener.add(llDash) {
				this.pendingOpen = null
				this.start(opening = true, freshOpen = true, blurRadiusPx)
			}
		}
	}

	/**
	 * Reverses the open effects. [teardown] (hide the dash, restore the
	 * overlays) runs synchronously for NONE, at animation end otherwise — or
	 * not at all if a re-open cancels the close mid-flight. The blur always
	 * ramps down gradually, NONE included.
	 */
	fun close(blurRadiusPx: Int, teardown: () -> Unit) {
		this.cancelRunning()

		if (this.animation == DashAnimation.NONE) {
			teardown()
			this.viewFinder.get<LinearLayout>(R.id.llPanel).alpha = this.panelRestingAlpha()
			this.start(opening = false, freshOpen = false, blurRadiusPx)
			return
		}

		this.start(opening = false, freshOpen = false, blurRadiusPx, teardown)
	}

	private fun openInstantly() {
		this.viewFinder.get<LinearLayout>(R.id.llDash).visibility = View.VISIBLE
		this.viewFinder.get<LinearLayout>(R.id.llPanel).alpha = 1F

		this.viewFinder.get<FrameLayout>(R.id.flWallpaperOverlay).visibility = View.INVISIBLE
		this.viewFinder.get<FrameLayout>(R.id.flWallpaperOverlayWhenDashOpened).visibility =
			View.VISIBLE
	}

	/** @return whether an animation was in flight (i.e. the new call reverses it). */
	private fun cancelRunning(): Boolean {
		this.pendingOpen?.removeListener()
		this.pendingOpen = null

		val wasRunning = this.running != null
		this.running?.cancel()
		this.running = null

		return wasRunning
	}

	private fun start(opening: Boolean, freshOpen: Boolean, blurRadiusPx: Int,
			teardown: (() -> Unit)? = null) {
		val mode = this.animation
		val duration = if (opening) OPEN_DURATION_MS else CLOSE_DURATION_MS
		val interpolator = if (opening) OPEN_INTERPOLATOR else CLOSE_INTERPOLATOR
		val llDash = this.viewFinder.get<LinearLayout>(R.id.llDash)
		val flOverlayWhenOpened =
			this.viewFinder.get<FrameLayout>(R.id.flWallpaperOverlayWhenDashOpened)
		val gvDashHomeApps = this.viewFinder.get<GridView>(R.id.gvDashHomeApps)

		val animators = mutableListOf<Animator>()
		this.buildBlurAnimator(opening, blurRadiusPx, duration)?.let { animators += it }

		if (mode != DashAnimation.NONE) {
			if (opening && freshOpen) {
				flOverlayWhenOpened.alpha = 0F
			}
			animators += alphaAnimator(this.viewFinder.get<LinearLayout>(R.id.llPanel),
				if (opening) 1F else this.panelRestingAlpha(), duration)
			animators += alphaAnimator(flOverlayWhenOpened, if (opening) 1F else 0F, duration)
			animators += when (mode) {
				DashAnimation.GNOME -> this.buildGnomeAnimators(opening, freshOpen, llDash,
					gvDashHomeApps, duration)
				DashAnimation.CINNAMON -> this.buildCinnamonAnimators(opening, freshOpen, llDash,
					duration)
				DashAnimation.ELEMENTARY -> this.buildElementaryAnimators(opening, freshOpen,
					llDash, duration)
				else -> this.buildUnityAnimators(opening, freshOpen, llDash, duration)
			}
		}

		if (animators.isEmpty()) { // NONE with a zero blur radius //
			return
		}
		animators.forEach { it.interpolator = interpolator }

		this.running = AnimatorSet().also { set ->
			set.playTogether(animators)
			set.addListener(object : AnimatorListenerAdapter() {
				private var cancelled = false

				override fun onAnimationCancel(animation: Animator) {
					this.cancelled = true
				}

				override fun onAnimationEnd(animation: Animator) {
					if (running === set) {
						running = null
					}
					if (this.cancelled) { // The reversing call owns the views now //
						return
					}

					if (opening) {
						resetIconTransforms(gvDashHomeApps)
					} else {
						withLayoutTransitionsSuppressed { teardown?.invoke() }
						resetDashTransforms(llDash)
						flOverlayWhenOpened.alpha = 1F
						resetIconTransforms(gvDashHomeApps)
						finishBlur()
					}
				}
			})
			set.start()
		}
	}

	private fun buildBlurAnimator(opening: Boolean, blurRadiusPx: Int, duration: Long): Animator? {
		if (blurRadiusPx <= 0) {
			return null
		}

		val wallpaper = this.viewFinder.get<Wallpaper>(R.id.wpWallpaper)
		val widgets = this.viewFinder.get<WidgetsContainer>(R.id.vgWidgets)
		val target = if (opening) blurRadiusPx.toFloat() else 0F

		return ValueAnimator.ofFloat(this.currentBlurRadius, target).also { animator ->
			animator.duration = duration
			animator.addUpdateListener {
				val radius = it.animatedValue as Float

				this.currentBlurRadius = radius
				wallpaper.applyBlurFraction(this.activity.window, radius / blurRadiusPx,
					blurRadiusPx)
				widgets.setRenderEffect(if (radius >= 0.5F) { // createBlurEffect() rejects 0 //
					RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
				} else {
					null
				})
			}
		}
	}

	private fun buildGnomeAnimators(opening: Boolean, freshOpen: Boolean, llDash: View,
			grid: GridView, duration: Long): List<Animator> {
		if (freshOpen) {
			llDash.alpha = 0F
		}

		return listOf(alphaAnimator(llDash, if (opening) 1F else 0F, duration)) +
			this.buildIconAnimators(opening, freshOpen, grid,
				if (opening) OPEN_ICON_DURATION_MS else CLOSE_ICON_DURATION_MS)
	}

	private fun buildCinnamonAnimators(opening: Boolean, freshOpen: Boolean, llDash: View,
			duration: Long): List<Animator> {
		val (offscreenX, offscreenY) = when (this.launcherEdge()) {
			Location.LEFT -> -llDash.width.toFloat() to 0F
			Location.RIGHT -> llDash.width.toFloat() to 0F
			Location.TOP -> 0F to -llDash.height.toFloat()
			Location.BOTTOM -> 0F to llDash.height.toFloat()
			Location.NONE -> 0F to 0F
		}
		if (offscreenX == 0F && offscreenY == 0F) { // No launcher edge to slide in from //
			return this.buildUnityAnimators(opening, freshOpen, llDash, duration)
		}

		if (freshOpen) {
			llDash.translationX = offscreenX
			llDash.translationY = offscreenY
		}

		return listOf(ObjectAnimator.ofPropertyValuesHolder(llDash,
			PropertyValuesHolder.ofFloat(View.TRANSLATION_X, if (opening) 0F else offscreenX),
			PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, if (opening) 0F else offscreenY),
		).setDuration(duration))
	}

	private fun buildElementaryAnimators(opening: Boolean, freshOpen: Boolean, llDash: View,
			duration: Long): List<Animator> {
		val label = this.viewFinder.get<View>(R.id.tvPanelBfb)

		if (label.isShown && label.width > 0) { // Zoom from the Applications label's centre //
			val labelLocation = IntArray(2)
			val dashLocation = IntArray(2)
			label.getLocationOnScreen(labelLocation)
			llDash.getLocationOnScreen(dashLocation)

			llDash.pivotX = labelLocation[0] + label.width / 2F -
				(dashLocation[0] - llDash.translationX)
			llDash.pivotY = labelLocation[1] + label.height / 2F -
				(dashLocation[1] - llDash.translationY)
		} else { // The label sits in the top-left corner by default //
			llDash.pivotX = 0F
			llDash.pivotY = 0F
		}

		if (freshOpen) {
			llDash.alpha = 0F
			llDash.scaleX = ZOOM_START_SCALE
			llDash.scaleY = ZOOM_START_SCALE
		}

		val targetScale = if (opening) 1F else ZOOM_START_SCALE

		return listOf(
			alphaAnimator(llDash, if (opening) 1F else 0F, duration),
			ObjectAnimator.ofPropertyValuesHolder(llDash,
				PropertyValuesHolder.ofFloat(View.SCALE_X, targetScale),
				PropertyValuesHolder.ofFloat(View.SCALE_Y, targetScale),
			).setDuration(duration))
	}

	private fun buildUnityAnimators(opening: Boolean, freshOpen: Boolean, llDash: View,
			duration: Long): List<Animator> {
		if (freshOpen) {
			llDash.alpha = 0F
		}

		return listOf(alphaAnimator(llDash, if (opening) 1F else 0F, duration))
	}

	private fun buildIconAnimators(opening: Boolean, freshOpen: Boolean, grid: GridView,
			duration: Long): List<Animator> {
		val children = (0 until grid.childCount).map(grid::getChildAt)
		if (children.isEmpty()) {
			return emptyList()
		}

		val bfb = this.viewFinder.get<View>(R.id.lalBfb)
		val bfbShown = bfb.isShown && bfb.width > 0
		val bfbLocation = IntArray(2)
		if (bfbShown) {
			bfb.getLocationOnScreen(bfbLocation)
		}
		val bfbCentreX = bfbLocation[0] + bfb.width / 2F
		val bfbCentreY = bfbLocation[1] + bfb.height / 2F

		val genies = children.map { child ->
			val location = IntArray(2)
			child.getLocationOnScreen(location)
			/*
			 * getLocationOnScreen() includes any in-flight translation; undo it to
			 * get the child's resting (laid-out) centre.
			 */
			val centreX = location[0] - child.translationX + child.width / 2F
			val centreY = location[1] - child.translationY + child.height / 2F

			if (bfbShown) {
				Genie(child, bfbCentreX - centreX, bfbCentreY - centreY, GENIE_START_SCALE,
					hypot(bfbCentreX - centreX, bfbCentreY - centreY))
			} else { // No BFB to expand from; zoom each icon in place instead //
				Genie(child, 0F, 0F, NO_BFB_START_SCALE, hypot(centreX, centreY))
			}
		}.sortedBy { if (opening) it.distance else -it.distance }

		val staggerStep = if (genies.size > 1) {
			min(ICON_STAGGER_STEP_MS, ICON_STAGGER_MAX_MS / (genies.size - 1))
		} else {
			0L
		}

		return genies.mapIndexed { i, genie ->
			if (freshOpen) {
				genie.child.translationX = genie.translationX
				genie.child.translationY = genie.translationY
				genie.child.scaleX = genie.scale
				genie.child.scaleY = genie.scale
			}

			ObjectAnimator.ofPropertyValuesHolder(genie.child,
				PropertyValuesHolder.ofFloat(View.TRANSLATION_X,
					if (opening) 0F else genie.translationX),
				PropertyValuesHolder.ofFloat(View.TRANSLATION_Y,
					if (opening) 0F else genie.translationY),
				PropertyValuesHolder.ofFloat(View.SCALE_X, if (opening) 1F else genie.scale),
				PropertyValuesHolder.ofFloat(View.SCALE_Y, if (opening) 1F else genie.scale),
			).also {
				it.duration = duration
				it.startDelay = i * staggerStep
			}
		}
	}

	/*
	 * Single-value animators animate from the property's current value, which
	 * keeps mid-flight reversals smooth.
	 */
	private fun alphaAnimator(view: View, target: Float, duration: Long): Animator =
		ObjectAnimator.ofFloat(view, View.ALPHA, target).setDuration(duration)

	private fun panelRestingAlpha(): Float =
		this.prefs.getInt(Preference.PANEL_OPACITY, 100).toFloat() / 100F

	private fun launcherEdge(): Location =
		Location.of(this.prefs.getInt(Preference.LAUNCHER_EDGE,
			this.activity.resources.getInteger(this.theme.launcher_location)))

	private fun resetDashTransforms(llDash: View) {
		llDash.alpha = 1F
		llDash.translationX = 0F
		llDash.translationY = 0F
		llDash.scaleX = 1F
		llDash.scaleY = 1F
		llDash.resetPivot()
	}

	/* The grid recycles item views, so transforms must never outlive an animation. */
	private fun resetIconTransforms(grid: GridView) {
		for (i in 0 until grid.childCount) {
			val child = grid.getChildAt(i)
			child.translationX = 0F
			child.translationY = 0F
			child.scaleX = 1F
			child.scaleY = 1F
		}
	}

	private fun finishBlur() {
		this.viewFinder.get<Wallpaper>(R.id.wpWallpaper).unblur(this.activity.window)
		this.viewFinder.get<WidgetsContainer>(R.id.vgWidgets).setRenderEffect(null)
		this.currentBlurRadius = 0F
	}

	/*
	 * The generic LayoutTransitions installed by LayoutTransitionConfigurer would
	 * fade these same visibility changes a second time. They only fire at mutation
	 * time, so suppressing them around the change is enough.
	 */
	private fun withLayoutTransitionsSuppressed(block: () -> Unit) {
		val containers = listOf<ViewGroup>(
			this.viewFinder.get(R.id.llLauncherAndDashContainer),
			this.viewFinder.get(R.id.flWallpaperOverlayContainer))
		val saved = containers.map { it.layoutTransition }

		containers.forEach { it.layoutTransition = null }
		try {
			block()
		} finally {
			containers.zip(saved).forEach { (container, transition) ->
				container.layoutTransition = transition
			}
		}
	}

	private class Genie(val child: View, val translationX: Float, val translationY: Float,
		val scale: Float, val distance: Float)

	companion object {
		private const val OPEN_DURATION_MS = 280L
		private const val CLOSE_DURATION_MS = 240L
		private const val OPEN_ICON_DURATION_MS = 240L
		private const val CLOSE_ICON_DURATION_MS = 200L
		private const val ICON_STAGGER_STEP_MS = 12L
		private const val ICON_STAGGER_MAX_MS = 100L
		private const val GENIE_START_SCALE = 0.15F
		private const val NO_BFB_START_SCALE = 0.6F
		private const val ZOOM_START_SCALE = 0.2F
		private val OPEN_INTERPOLATOR: TimeInterpolator = PathInterpolator(0.4F, 0F, 0.2F, 1F)
		private val CLOSE_INTERPOLATOR: TimeInterpolator = PathInterpolator(0.4F, 0F, 1F, 1F)
	}
}
