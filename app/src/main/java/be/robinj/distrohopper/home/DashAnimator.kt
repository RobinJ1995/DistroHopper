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
import be.robinj.distrohopper.theme.DashAnimation
import be.robinj.distrohopper.theme.Theme
import be.robinj.distrohopper.widgets.WidgetsContainer
import kotlin.math.hypot
import kotlin.math.min

/**
 * Applies the visual side of opening/closing the dash on behalf of
 * DashController. Which effect runs is the theme's choice (dash_animation):
 * NONE reproduces the instant behaviour, GENIE fades the dim overlay in, ramps
 * the blur up, and expands each app icon out of the BFB (slightly staggered by
 * distance, nearest first) — all reversed on close.
 */
class DashAnimator(
	private val activity: Activity,
	private val viewFinder: ViewFinder,
	private val theme: Theme,
) {
	private var running: AnimatorSet? = null
	private var pendingOpen: OneShotPreDrawListener? = null
	private var currentBlurRadius = 0F

	private val animation: DashAnimation
		get() = DashAnimation.of(this.activity.resources.getInteger(this.theme.dash_animation))

	fun open(blurRadiusPx: Int) {
		if (this.animation == DashAnimation.NONE) {
			this.openInstantly(blurRadiusPx)
			return
		}

		val reversal = this.cancelRunning()
		val llDash = this.viewFinder.get<LinearLayout>(R.id.llDash)
		val gvDashHomeApps = this.viewFinder.get<GridView>(R.id.gvDashHomeApps)
		val flOverlayWhenOpened =
			this.viewFinder.get<FrameLayout>(R.id.flWallpaperOverlayWhenDashOpened)

		this.withLayoutTransitionsSuppressed {
			if (! reversal) {
				llDash.alpha = 0F
				flOverlayWhenOpened.alpha = 0F
			}
			llDash.visibility = View.VISIBLE
			this.viewFinder.get<FrameLayout>(R.id.flWallpaperOverlay).visibility = View.INVISIBLE
			flOverlayWhenOpened.visibility = View.VISIBLE
		}

		if (reversal || (gvDashHomeApps.childCount > 0 && gvDashHomeApps.isLaidOut)) {
			this.start(opening = true, freshOpen = ! reversal, blurRadiusPx, gvDashHomeApps)
		} else {
			/*
			 * The grid only gets children once the dash has been laid out, and the
			 * genie start transforms must be in place before the first frame.
			 */
			this.pendingOpen = OneShotPreDrawListener.add(gvDashHomeApps) {
				this.pendingOpen = null
				this.start(opening = true, freshOpen = true, blurRadiusPx, gvDashHomeApps)
			}
		}
	}

	/**
	 * Reverses the open effects. [teardown] (hide the dash, unblur, restore the
	 * overlays) runs synchronously when nothing animates, at animation end
	 * otherwise — or not at all if a re-open cancels the close mid-flight.
	 */
	fun close(blurRadiusPx: Int, teardown: () -> Unit) {
		if (this.animation == DashAnimation.NONE) {
			teardown()
			return
		}

		this.cancelRunning()
		this.start(opening = false, freshOpen = false, blurRadiusPx,
			this.viewFinder.get(R.id.gvDashHomeApps), teardown)
	}

	private fun openInstantly(blurRadiusPx: Int) {
		this.viewFinder.get<LinearLayout>(R.id.llDash).visibility = View.VISIBLE
		this.viewFinder.get<Wallpaper>(R.id.wpWallpaper).blur(this.activity.window, blurRadiusPx)
		this.viewFinder.get<WidgetsContainer>(R.id.vgWidgets).setRenderEffect(
			RenderEffect.createBlurEffect(blurRadiusPx.toFloat(), blurRadiusPx.toFloat(),
				Shader.TileMode.CLAMP))

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

	private fun start(opening: Boolean, freshOpen: Boolean, blurRadiusPx: Int, grid: GridView,
			teardown: (() -> Unit)? = null) {
		val duration = if (opening) OPEN_DURATION_MS else CLOSE_DURATION_MS
		val interpolator = if (opening) OPEN_INTERPOLATOR else CLOSE_INTERPOLATOR
		val llDash = this.viewFinder.get<LinearLayout>(R.id.llDash)
		val flOverlayWhenOpened =
			this.viewFinder.get<FrameLayout>(R.id.flWallpaperOverlayWhenDashOpened)
		val targetAlpha = if (opening) 1F else 0F

		val animators = mutableListOf<Animator>()
		animators += ObjectAnimator.ofFloat(llDash, View.ALPHA, targetAlpha)
			.setDuration(duration)
		animators += ObjectAnimator.ofFloat(flOverlayWhenOpened, View.ALPHA, targetAlpha)
			.setDuration(duration)
		this.buildBlurAnimator(opening, blurRadiusPx, duration)?.let { animators += it }
		animators += this.buildIconAnimators(opening, freshOpen, grid,
			if (opening) OPEN_ICON_DURATION_MS else CLOSE_ICON_DURATION_MS)
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
						resetIconTransforms(grid)
					} else {
						withLayoutTransitionsSuppressed { teardown?.invoke() }
						llDash.alpha = 1F
						flOverlayWhenOpened.alpha = 1F
						resetIconTransforms(grid)
						currentBlurRadius = 0F
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

			/*
			 * Single-value holders animate from each property's current value, which
			 * keeps mid-flight reversals smooth.
			 */
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

	private data class Genie(val child: View, val translationX: Float, val translationY: Float,
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
		private val OPEN_INTERPOLATOR: TimeInterpolator = PathInterpolator(0.4F, 0F, 0.2F, 1F)
		private val CLOSE_INTERPOLATOR: TimeInterpolator = PathInterpolator(0.4F, 0F, 1F, 1F)
	}
}
