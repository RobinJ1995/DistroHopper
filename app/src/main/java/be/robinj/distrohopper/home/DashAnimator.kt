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
import android.os.PowerManager
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
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
import be.robinj.distrohopper.widgets.WidgetsPager
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Applies the visual side of opening/closing the dash on behalf of
 * DashController. The wallpaper/widget blur and the panel opacity normally
 * change gradually; how the dash itself appears is the theme's choice
 * (dash_animation):
 *  - NONE: the dash snaps in and out instantly.
 *  - GNOME: the dim overlay fades in and each app icon expands out of the BFB
 *    (slightly staggered by distance, nearest first).
 *  - CINNAMON: the dash slides in from the launcher's edge of the screen.
 *  - ELEMENTARY: the dash fades and zooms in from the Applications label.
 *  - UNITY: the dash fades in.
 *  - MATE: the whole dash fades and zooms out of the BFB.
 *  - COSMIC: the dash fades in with a slight zoom.
 * Everything is reversed on close. Battery saver bypasses all transitions
 * and applies the final state immediately.
 *
 * Swipe gestures run the same theme presets, but finger-tracked rather than
 * clock-driven: swipeBegin() captures the theme's open-animation geometry and
 * swipeUpdate() applies its transform at the swipe's openness (0 = closed,
 * 1 = open) — the per-theme transforms the time-based builders above produce,
 * expressed as a function of progress (buildSwipeApplier) — while the blur,
 * panel opacity and dim overlay track the same fraction; swipeSettle() eases
 * the remainder when the finger lifts. The geometry helpers (collectGenies,
 * cinnamonOffscreen, applyElementaryPivot, mateGeometry) are shared by both
 * the builders and the appliers.
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
	private var swipe: SwipeSession? = null

	private val animation: DashAnimation
		get() = DashAnimation.of(this.activity.resources.getInteger(this.theme.dash_animation))

	internal val animationsEnabled: Boolean
		get() = !this.activity.getSystemService(PowerManager::class.java).isPowerSaveMode

	fun open(blurRadiusPx: Int) {
		val reversal = this.cancelRunning()
		val llDash = this.viewFinder.get<LinearLayout>(R.id.llDash)

		if (!this.animationsEnabled) {
			this.withLayoutTransitionsSuppressed { this.openInstantly() }
			this.applyBlurRadius(blurRadiusPx.toFloat(), blurRadiusPx)
			return
		}

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
	 * not at all if a re-open cancels the close mid-flight. The blur normally
	 * ramps down gradually, NONE included; battery saver settles it immediately.
	 */
	fun close(blurRadiusPx: Int, teardown: () -> Unit) {
		this.cancelRunning()

		if (!this.animationsEnabled) {
			this.withLayoutTransitionsSuppressed { teardown() }
			this.viewFinder.get<LinearLayout>(R.id.llPanel).alpha = this.panelRestingAlpha()
			this.resetDashTransforms(this.viewFinder.get(R.id.llDash))
			this.viewFinder.get<FrameLayout>(R.id.flWallpaperOverlayWhenDashOpened).alpha = 1F
			this.resetIconTransforms(this.viewFinder.get(R.id.gvDashHomeApps))
			this.finishBlur()
			return
		}

		if (this.animation == DashAnimation.NONE) {
			teardown()
			this.viewFinder.get<LinearLayout>(R.id.llPanel).alpha = this.panelRestingAlpha()
			this.start(opening = false, freshOpen = false, blurRadiusPx)
			return
		}

		this.start(opening = false, freshOpen = false, blurRadiusPx, teardown)
	}

	internal val swipeInProgress: Boolean
		get() = this.swipe != null

	/** The slide distance a swipe tracks over, for finger-to-fraction mapping. */
	internal val swipeDistancePx: Float
		get() = this.swipe?.distance ?: this.slideDistancePx()

	/** Current openness of the in-flight swipe (0 = closed, 1 = open). */
	internal val swipeOpenness: Float
		get() = this.swipe?.openness ?: 0F

	/**
	 * Begins a finger-tracked transition that scrubs the theme's own open
	 * animation by the swipe's progress. When opening from fully closed the
	 * dash is made visible first and its geometry captured on the next layout
	 * pass (so a stray frame or two may show a plain fade before the theme
	 * transform takes over); a close, or a swipe taking over a still-settling
	 * one, captures immediately. Returns false in battery saver, where there
	 * is nothing to track — the caller falls back to the instant open()/close().
	 */
	fun swipeBegin(opening: Boolean, blurRadiusPx: Int): Boolean {
		if (!this.animationsEnabled) {
			return false
		}

		val resumeOpenness = this.swipe?.openness
		this.cancelRunning()

		val llDash = this.viewFinder.get<LinearLayout>(R.id.llDash)
		val session = SwipeSession(blurRadiusPx, this.slideDistancePx())

		if (resumeOpenness == null && opening) {
			// Fresh open from fully closed: make the dash visible, then capture
			// the theme geometry once it has been laid out //
			this.withLayoutTransitionsSuppressed {
				llDash.visibility = View.VISIBLE
				this.viewFinder.get<FrameLayout>(R.id.flWallpaperOverlay).visibility =
					View.INVISIBLE
				this.viewFinder.get<FrameLayout>(R.id.flWallpaperOverlayWhenDashOpened)
					.visibility = View.VISIBLE
			}
			this.resetDashTransforms(llDash)
			this.resetIconTransforms(this.viewFinder.get(R.id.gvDashHomeApps))
			session.pendingCapture = OneShotPreDrawListener.add(llDash) {
				session.pendingCapture = null
				session.applier = this.buildSwipeApplier()
				this.applySwipe(session) // Apply the theme transform now it is captured //
			}
		} else {
			// The dash is already open and laid out (a close, or a swipe taking
			// over a settling one): capture the geometry synchronously //
			session.applier = this.buildSwipeApplier()
		}

		this.swipe = session
		this.swipeUpdate(resumeOpenness ?: if (opening) 0F else 1F)

		return true
	}

	fun swipeUpdate(openness: Float) {
		val swipe = this.swipe ?: return

		swipe.openness = openness.coerceIn(0F, 1F)
		this.applySwipe(swipe)
	}

	/**
	 * Animates the in-flight swipe the rest of the way to fully [open] or
	 * fully closed. [teardown] (hide the dash, restore the overlays) runs
	 * once a settle towards closed completes, mirroring close().
	 */
	fun swipeSettle(open: Boolean, teardown: (() -> Unit)? = null) {
		val swipe = this.swipe ?: return
		val target = if (open) 1F else 0F
		val base = if (open) OPEN_DURATION_MS else CLOSE_DURATION_MS

		swipe.settle = ValueAnimator.ofFloat(swipe.openness, target).also { animator ->
			animator.duration = (base * abs(target - swipe.openness)).toLong()
			animator.interpolator = if (open) OPEN_INTERPOLATOR else CLOSE_INTERPOLATOR
			animator.addUpdateListener {
				swipe.openness = it.animatedValue as Float
				this.applySwipe(swipe)
			}
			animator.addListener(object : AnimatorListenerAdapter() {
				private var cancelled = false

				override fun onAnimationCancel(animation: Animator) {
					this.cancelled = true
				}

				override fun onAnimationEnd(animation: Animator) {
					if (this@DashAnimator.swipe === swipe) {
						this@DashAnimator.swipe = null
					}
					if (this.cancelled) { // The taking-over call owns the views now //
						return
					}

					if (open) {
						resetDashTransforms(viewFinder.get(R.id.llDash))
						resetIconTransforms(viewFinder.get(R.id.gvDashHomeApps))
					} else {
						withLayoutTransitionsSuppressed { teardown?.invoke() }
						resetDashTransforms(viewFinder.get(R.id.llDash))
						resetIconTransforms(viewFinder.get(R.id.gvDashHomeApps))
						viewFinder.get<FrameLayout>(R.id.flWallpaperOverlayWhenDashOpened)
							.alpha = 1F
						viewFinder.get<LinearLayout>(R.id.llPanel).alpha = panelRestingAlpha()
						finishBlur()
					}
				}
			})
			animator.start()
		}
	}

	private fun applySwipe(swipe: SwipeSession) {
		val resting = this.panelRestingAlpha()

		this.viewFinder.get<FrameLayout>(R.id.flWallpaperOverlayWhenDashOpened).alpha =
			swipe.openness
		this.viewFinder.get<LinearLayout>(R.id.llPanel).alpha =
			resting + (1F - resting) * swipe.openness
		if (swipe.blurRadiusPx > 0) {
			this.applyBlurRadius(
				BLUR_OPEN_INTERPOLATOR.getInterpolation(swipe.openness) * swipe.blurRadiusPx,
				swipe.blurRadiusPx)
		}

		val applier = swipe.applier
		if (applier != null) {
			applier(swipe.openness)
		} else { // Geometry not captured yet (first frame of a fresh open): plain fade //
			this.viewFinder.get<LinearLayout>(R.id.llDash).alpha = swipe.openness
		}
	}

	/*
	 * The dash slides over the launcher-and-dash container's full height; the
	 * dash's own height can't be used as it is still zero on a fresh open
	 * (the dash only gets laid out once made visible).
	 */
	private fun slideDistancePx(): Float =
		max(1, this.viewFinder.get<View>(R.id.llLauncherAndDashContainer).height).toFloat()

	private fun openInstantly() {
		val llDash = this.viewFinder.get<LinearLayout>(R.id.llDash)
		val overlay = this.viewFinder.get<FrameLayout>(R.id.flWallpaperOverlayWhenDashOpened)

		llDash.visibility = View.VISIBLE
		this.viewFinder.get<LinearLayout>(R.id.llPanel).alpha = 1F

		this.viewFinder.get<FrameLayout>(R.id.flWallpaperOverlay).visibility = View.INVISIBLE
		overlay.visibility = View.VISIBLE
		overlay.alpha = 1F
		this.resetDashTransforms(llDash)
		this.resetIconTransforms(this.viewFinder.get(R.id.gvDashHomeApps))
	}

	/** @return whether an animation was in flight (i.e. the new call reverses it). */
	private fun cancelRunning(): Boolean {
		this.pendingOpen?.removeListener()
		this.pendingOpen = null

		val wasRunning = this.running != null || this.swipe != null
		this.running?.cancel()
		this.running = null
		this.swipe?.let {
			it.settle?.cancel()
			it.pendingCapture?.removeListener()
			/*
			 * The theme transforms the swipe applied are not the resting state a
			 * taking-over open/close animation expects; clear them (the dash and
			 * the recycled icon views) so it starts from a clean slate.
			 */
			this.resetDashTransforms(this.viewFinder.get(R.id.llDash))
			this.resetIconTransforms(this.viewFinder.get(R.id.gvDashHomeApps))
		}
		this.swipe = null

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
		val blurAnimator = this.buildBlurAnimator(opening, blurRadiusPx, duration)
			?.also { animators += it }

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
				DashAnimation.MATE -> this.buildMateAnimators(opening, freshOpen, llDash,
					duration)
				DashAnimation.COSMIC -> this.buildCosmicAnimators(opening, freshOpen, llDash,
					duration)
				else -> this.buildUnityAnimators(opening, freshOpen, llDash, duration)
			}
		}

		if (animators.isEmpty()) { // NONE with a zero blur radius //
			return
		}
		animators.forEach { it.interpolator = interpolator }
		/*
		 * The blur reads as binary above a fairly small radius, so an eased-out
		 * ramp looks like an instant jump (especially with large blur radii):
		 * nearly the whole perceptible sharp-to-blurred range sits in the first
		 * few percent of the radius ramp. Easing the radius in (and out in
		 * reverse) keeps it in the perceptible range for most of the duration.
		 */
		blurAnimator?.interpolator =
			if (opening) BLUR_OPEN_INTERPOLATOR else BLUR_CLOSE_INTERPOLATOR

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
		val widgets = this.viewFinder.get<WidgetsPager>(R.id.vgWidgets)
		val target = if (opening) blurRadiusPx.toFloat() else 0F

		return ValueAnimator.ofFloat(this.currentBlurRadius, target).also { animator ->
			animator.duration = duration
			animator.addUpdateListener {
				val radius = it.animatedValue as Float
				this.applyBlurRadius(radius, blurRadiusPx, wallpaper, widgets)
			}
		}
	}

	private fun applyBlurRadius(radius: Float, blurRadiusPx: Int,
			wallpaper: Wallpaper = this.viewFinder.get(R.id.wpWallpaper),
			widgets: WidgetsPager = this.viewFinder.get(R.id.vgWidgets)) {
		if (blurRadiusPx <= 0) {
			return
		}

		this.currentBlurRadius = radius
		wallpaper.applyBlurFraction(this.activity.window, radius / blurRadiusPx, blurRadiusPx)
		widgets.setRenderEffect(if (radius >= 0.5F) { // createBlurEffect() rejects 0 //
			RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
		} else {
			null
		})
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
		val (offscreenX, offscreenY) = this.cinnamonOffscreen(llDash)
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
		this.applyElementaryPivot(llDash)

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

	/*
	 * The whole dash genies out of the BFB (the menu button): on close it
	 * first squeezes horizontally into the button's column, then gets pulled
	 * vertically into the button (opening plays the phases in reverse). A
	 * true genie warps the surface along curves, which would require
	 * snapshotting the dash into a mesh-distorted bitmap; the staggered
	 * squeeze-then-slurp reads very similarly at these durations.
	 */
	private fun buildMateAnimators(opening: Boolean, freshOpen: Boolean, llDash: View,
			duration: Long): List<Animator> {
		val genieDuration = duration * GENIE_DURATION_SCALE / 100L
		val phase = genieDuration * GENIE_PHASE_PERCENT / 100L
		val overlap = genieDuration - phase

		val geo = this.mateGeometry(llDash)
		val endScaleX = geo.endScaleX
		val endScaleY = geo.endScaleY
		val verticalLauncher = geo.verticalLauncher

		if (freshOpen) {
			llDash.alpha = 0F
			llDash.scaleX = endScaleX
			llDash.scaleY = endScaleY
		}

		val scaleX = ObjectAnimator.ofFloat(llDash, View.SCALE_X,
			if (opening) 1F else endScaleX).setDuration(phase)
		val scaleY = ObjectAnimator.ofFloat(llDash, View.SCALE_Y,
			if (opening) 1F else endScaleY).setDuration(phase)
		val squeeze = if (verticalLauncher) scaleY else scaleX
		val slurp = if (verticalLauncher) scaleX else scaleY
		val alpha = alphaAnimator(llDash, if (opening) 1F else 0F, genieDuration / 4)

		if (opening) { // Pulled out of the button: slurp phase first, then unsqueeze //
			slurp.startDelay = 0L
			squeeze.startDelay = overlap
			alpha.startDelay = 0L
		} else { // Slurped into the button: squeeze first, then pull in //
			squeeze.startDelay = 0L
			slurp.startDelay = overlap
			alpha.startDelay = genieDuration - genieDuration / 4
		}

		return listOf(scaleX, scaleY, alpha)
	}

	/* The dash fades in with a slight zoom from its resting size. */
	private fun buildCosmicAnimators(opening: Boolean, freshOpen: Boolean, llDash: View,
			duration: Long): List<Animator> {
		llDash.resetPivot()

		if (freshOpen) {
			llDash.alpha = 0F
			llDash.scaleX = COSMIC_START_SCALE
			llDash.scaleY = COSMIC_START_SCALE
		}

		val targetScale = if (opening) 1F else COSMIC_START_SCALE

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
		val genies = this.collectGenies(grid, opening)
		if (genies.isEmpty()) {
			return emptyList()
		}

		val staggerStep = iconStaggerStep(genies.size)

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

	//# Theme geometry, shared by the time-based builders above and the #//
	//# finger-tracked appliers below.                                  #//

	/* The icons' genie targets (offset towards the BFB and start scale), nearest first. */
	private fun collectGenies(grid: GridView, opening: Boolean): List<Genie> {
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

		return children.map { child ->
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
	}

	private fun iconStaggerStep(count: Int): Long =
		if (count > 1) min(ICON_STAGGER_STEP_MS, ICON_STAGGER_MAX_MS / (count - 1)) else 0L

	/* The off-screen offset the dash slides in from, per launcher edge (0,0 = none). */
	private fun cinnamonOffscreen(llDash: View): Pair<Float, Float> =
		when (this.launcherEdge()) {
			Location.LEFT -> -llDash.width.toFloat() to 0F
			Location.RIGHT -> llDash.width.toFloat() to 0F
			Location.TOP -> 0F to -llDash.height.toFloat()
			Location.BOTTOM -> 0F to llDash.height.toFloat()
			Location.NONE -> 0F to 0F
		}

	/* Pivots the dash at the Applications label's centre, or its top-left by default. */
	private fun applyElementaryPivot(llDash: View) {
		val label = this.viewFinder.get<View>(R.id.tvPanelBfb)

		if (label.isShown && label.width > 0) {
			val labelLocation = IntArray(2)
			val dashLocation = IntArray(2)
			label.getLocationOnScreen(labelLocation)
			llDash.getLocationOnScreen(dashLocation)

			llDash.pivotX = labelLocation[0] + label.width / 2F -
				(dashLocation[0] - llDash.translationX)
			llDash.pivotY = labelLocation[1] + label.height / 2F -
				(dashLocation[1] - llDash.translationY)
		} else {
			llDash.pivotX = 0F
			llDash.pivotY = 0F
		}
	}

	/*
	 * The collapsed dash matches the BFB's own bounds exactly, so the dash
	 * looks like it expands out of (and gets slurped back into) the button
	 * itself. Scaling by s about pivot P maps the dash's edge to P * (1 - s),
	 * so P = target_edge / (1 - s) puts the collapsed dash's edge on the BFB's
	 * edge (and, with s = bfb / dash, its far edge on the BFB's far edge). Sets
	 * the dash's pivot as a side effect and returns the end scales plus which
	 * axis the squeeze runs along. The squeeze happens along the launcher's
	 * axis first: a horizontal launcher squeezes to the button's width then
	 * pulls down/up into it; a vertical launcher squeezes to its height then
	 * pulls sideways.
	 */
	private fun mateGeometry(llDash: View): MateGeometry {
		val bfb = this.viewFinder.get<View>(R.id.lalBfb)
		val endScaleX: Float
		val endScaleY: Float

		if (bfb.isShown && bfb.width > 0 && llDash.width > 0 && llDash.height > 0) {
			val bfbLocation = IntArray(2)
			val dashLocation = IntArray(2)
			bfb.getLocationOnScreen(bfbLocation)
			llDash.getLocationOnScreen(dashLocation)
			val dashScreenX = dashLocation[0] - llDash.translationX
			val dashScreenY = dashLocation[1] - llDash.translationY

			/*
			 * The launcher's own padding leaves the BFB a few dp short of the
			 * screen edge; the collapsed dash hugs the edge instead of showing
			 * that sliver of a gap.
			 */
			val hug = EDGE_HUG_DP * this.activity.resources.displayMetrics.density
			var targetLeft = bfbLocation[0].toFloat()
			var targetWidth = bfb.width.toFloat()
			if (targetLeft < hug) {
				targetWidth += targetLeft
				targetLeft = 0F
			}
			var targetTop = bfbLocation[1].toFloat()
			var targetHeight = bfb.height.toFloat()
			if (targetTop < hug) {
				targetHeight += targetTop
				targetTop = 0F
			}
			val screenWidth = this.activity.resources.displayMetrics.widthPixels
			val screenHeight = this.activity.resources.displayMetrics.heightPixels
			if (screenWidth - (targetLeft + targetWidth) < hug) {
				targetWidth = screenWidth - targetLeft
			}
			if (screenHeight - (targetTop + targetHeight) < hug) {
				targetHeight = screenHeight - targetTop
			}

			endScaleX = targetWidth / llDash.width
			endScaleY = targetHeight / llDash.height
			llDash.pivotX = (targetLeft - dashScreenX) / (1F - endScaleX)
			llDash.pivotY = (targetTop - dashScreenY) / (1F - endScaleY)
		} else { // No BFB to slurp into; collapse towards the dash's centre //
			endScaleX = GENIE_END_SCALE_FALLBACK
			endScaleY = GENIE_END_SCALE_FALLBACK
			llDash.resetPivot()
		}

		val verticalLauncher =
			this.launcherEdge() == Location.LEFT || this.launcherEdge() == Location.RIGHT

		return MateGeometry(endScaleX, endScaleY, verticalLauncher)
	}

	/**
	 * Captures the active theme's open-animation geometry once and returns a
	 * function applying the dash's transform for a given openness (0 = closed,
	 * 1 = open) — the theme's own open animation, driven by the swipe's
	 * progress instead of a clock. Must be called once the dash is laid out
	 * (the geometry reads child positions). Each applier fully defines the
	 * transforms it owns across the whole range, so it overrides the pre-capture
	 * fade in [applySwipe].
	 */
	private fun buildSwipeApplier(): (Float) -> Unit {
		val llDash = this.viewFinder.get<LinearLayout>(R.id.llDash)

		return when (this.animation) {
			DashAnimation.GNOME -> {
				val grid = this.viewFinder.get<GridView>(R.id.gvDashHomeApps)
				val genies = this.collectGenies(grid, opening = true)
				val staggerStep = this.iconStaggerStep(genies.size)
				val span = (OPEN_ICON_DURATION_MS + (genies.size - 1).coerceAtLeast(0) * staggerStep)
					.toFloat();

				{ openness ->
					llDash.alpha = openness
					val time = openness * span
					genies.forEachIndexed { i, genie ->
						val localO = ((time - i * staggerStep) / OPEN_ICON_DURATION_MS)
							.coerceIn(0F, 1F)
						genie.child.translationX = genie.translationX * (1F - localO)
						genie.child.translationY = genie.translationY * (1F - localO)
						genie.child.scaleX = lerp(genie.scale, 1F, localO)
						genie.child.scaleY = lerp(genie.scale, 1F, localO)
					}
				}
			}
			DashAnimation.CINNAMON -> {
				val (offscreenX, offscreenY) = this.cinnamonOffscreen(llDash)
				if (offscreenX == 0F && offscreenY == 0F) {
					this.fadeApplier(llDash) // No launcher edge to slide in from //
				} else {
					{ openness ->
						llDash.alpha = 1F
						llDash.translationX = offscreenX * (1F - openness)
						llDash.translationY = offscreenY * (1F - openness)
					}
				}
			}
			DashAnimation.ELEMENTARY -> {
				this.applyElementaryPivot(llDash);

				{ openness ->
					llDash.alpha = openness
					val scale = lerp(ZOOM_START_SCALE, 1F, openness)
					llDash.scaleX = scale
					llDash.scaleY = scale
				}
			}
			DashAnimation.MATE -> {
				val geo = this.mateGeometry(llDash)
				val phaseFraction = GENIE_PHASE_PERCENT / 100F
				val overlapFraction = 1F - phaseFraction;

				{ openness ->
					val slurpO = (openness / phaseFraction).coerceIn(0F, 1F)
					val squeezeO = ((openness - overlapFraction) / phaseFraction).coerceIn(0F, 1F)
					llDash.alpha = (openness / GENIE_ALPHA_FRACTION).coerceIn(0F, 1F)
					if (geo.verticalLauncher) {
						llDash.scaleX = lerp(geo.endScaleX, 1F, slurpO)
						llDash.scaleY = lerp(geo.endScaleY, 1F, squeezeO)
					} else {
						llDash.scaleX = lerp(geo.endScaleX, 1F, squeezeO)
						llDash.scaleY = lerp(geo.endScaleY, 1F, slurpO)
					}
				}
			}
			DashAnimation.COSMIC -> {
				llDash.resetPivot();

				{ openness ->
					llDash.alpha = openness
					val scale = lerp(COSMIC_START_SCALE, 1F, openness)
					llDash.scaleX = scale
					llDash.scaleY = scale
				}
			}
			else -> this.fadeApplier(llDash) // UNITY / NONE //
		}
	}

	private fun fadeApplier(llDash: View): (Float) -> Unit = { openness ->
		llDash.alpha = openness
	}

	private fun lerp(from: Float, to: Float, fraction: Float): Float =
		from + (to - from) * fraction

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
		this.viewFinder.get<WidgetsPager>(R.id.vgWidgets).setRenderEffect(null)
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

	private class MateGeometry(val endScaleX: Float, val endScaleY: Float,
		val verticalLauncher: Boolean)

	private class SwipeSession(val blurRadiusPx: Int, val distance: Float) {
		var openness = 0F
		var settle: ValueAnimator? = null
		/* The theme transform for a given openness; null until the dash is laid out. */
		var applier: ((Float) -> Unit)? = null
		var pendingCapture: OneShotPreDrawListener? = null
	}

	companion object {
		internal const val OPEN_DURATION_MS = 280L
		internal const val CLOSE_DURATION_MS = 240L
		private const val OPEN_ICON_DURATION_MS = 240L
		private const val CLOSE_ICON_DURATION_MS = 200L
		private const val ICON_STAGGER_STEP_MS = 12L
		private const val ICON_STAGGER_MAX_MS = 100L
		private const val GENIE_START_SCALE = 0.15F
		private const val GENIE_END_SCALE_FALLBACK = 0.06F
		private const val EDGE_HUG_DP = 16F
		private const val GENIE_DURATION_SCALE = 160L // percent of the base duration //
		private const val GENIE_PHASE_PERCENT = 62L // each phase's share, so they overlap //
		private const val GENIE_ALPHA_FRACTION = 0.25F // alpha fades over the first quarter //
		private const val NO_BFB_START_SCALE = 0.6F
		private const val ZOOM_START_SCALE = 0.2F
		private const val COSMIC_START_SCALE = 0.9F
		private val OPEN_INTERPOLATOR: TimeInterpolator = PathInterpolator(0.4F, 0F, 0.2F, 1F)
		private val CLOSE_INTERPOLATOR: TimeInterpolator = PathInterpolator(0.4F, 0F, 1F, 1F)
		private val BLUR_OPEN_INTERPOLATOR: TimeInterpolator = AccelerateInterpolator(2.5F)
		private val BLUR_CLOSE_INTERPOLATOR: TimeInterpolator = DecelerateInterpolator(2.5F)
	}
}
