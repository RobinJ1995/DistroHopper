package be.robinj.distrohopper.desktop.launcher.service

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.PowerManager
import android.util.AttributeSet
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.annotation.VisibleForTesting
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.HomeRole
import be.robinj.distrohopper.R
import be.robinj.distrohopper.desktop.launcher.AppLauncher
import be.robinj.distrohopper.desktop.launcher.LauncherIconGrid
import be.robinj.distrohopper.home.LauncherTileColour
import be.robinj.distrohopper.preferences.LauncherServiceSensitivity
import be.robinj.distrohopper.preferences.LauncherServiceZone
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import be.robinj.distrohopper.theme.Location

/**
 * The floating launcher itself: an overlay window holding a copy of the dock
 * that can be pulled out from the screen edge while another app is in front.
 *
 * **One window, two shapes.** While hidden it is a thin strip along the docked
 * edge — only as wide as the sensitivity's hot zone and only as long as the
 * chosen zone — so it takes as little as possible away from the app underneath.
 * The moment a finger goes down on it the window grows to fill the screen, so
 * the bar can be drawn sliding in and a tap outside can dismiss it; when it
 * settles closed again it shrinks back to the strip. Growing it on ACTION_DOWN
 * (rather than adding a second window later) is what makes the gesture continue
 * to work: the input dispatcher hands the whole gesture to whichever window took
 * the DOWN, so the finger keeps being tracked far outside the strip it started in.
 *
 * Coordinates are taken raw (screen-absolute) throughout, since the window
 * resizing underneath the gesture moves the local origin.
 *
 * The bar follows the active theme and the user's launcher edge exactly as the
 * home screen's dock does: same background, same tile colour, same icon size,
 * reoriented for the edge it is docked on.
 */
class FloatingLauncherWindow(private val context: Context) {
	private val windowManager = this.context.getSystemService(WindowManager::class.java)
	private val root = FrameLayout(this.context)
	private val scrim = View(this.context)
	private val bar = LinearLayout(this.context)

	private var items: List<FloatingLauncherItem> = emptyList()
	private var edge = Location.LEFT
	private var zone = LauncherServiceZone.FULL
	private var sensitivity = LauncherServiceSensitivity.MEDIUM

	private var attached = false
	private var opened = false
	private var dragging = false
	private var progress = 0F
	private var barLengthPx = 0
	private var downX = 0F
	private var downY = 0F
	private var animator: ValueAnimator? = null

	private val touchSlop = ViewConfiguration.get(this.context).scaledTouchSlop

	init {
		this.scrim.setBackgroundColor(Color.BLACK)
		this.scrim.alpha = 0F
		this.scrim.visibility = View.GONE
		this.scrim.setOnClickListener { this.settle(false) }
		// Only a settled-open bar is dismissed by a tap outside it. Until then the
		// scrim must not take touches: setOnClickListener makes a view clickable,
		// and a clickable scrim would swallow the DOWN that starts the next pull //
		this.scrim.isClickable = false

		this.root.addView(this.scrim, FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
		this.root.addView(this.bar)
		this.root.setOnTouchListener { _, event -> this.onGrabberTouch(event) }

		/*
		 * Ask the system to keep its own edge gestures (back) off the strip, so a
		 * pull from a side edge is ours rather than a race with the back swipe.
		 * Best effort: the platform caps how much of an edge may be excluded, and
		 * only the strip is worth asking for — an open bar covers the screen, where
		 * an exclusion would mean nothing.
		 */
		this.root.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
			view.systemGestureExclusionRects = if (this.opened) emptyList()
				else listOf(Rect(0, 0, view.width, view.height))
		}
	}

	/** Whether the bar is currently pulled out (or on its way there). */
	val isOpen: Boolean
		get() = this.opened

	/** The overlay's root — what a test dispatches its pull through. */
	@VisibleForTesting
	internal val overlay: View
		get() = this.root

	/** The bar itself: its translation is how far out it has come. */
	@VisibleForTesting
	internal val dock: LinearLayout
		get() = this.bar

	/**
	 * Puts the strip on screen (rebuilding the bar from the current settings),
	 * or refreshes an already-visible one. A rebuild is skipped while the user
	 * has the bar open, so it can never change shape under their finger.
	 */
	fun show(items: List<FloatingLauncherItem>) {
		this.items = items

		if (this.attached && this.opened) {
			return
		}

		this.build()

		if (this.attached) {
			this.windowManager.updateViewLayout(this.root, this.closedParams())
		} else {
			this.windowManager.addView(this.root, this.closedParams())
			this.attached = true
		}
	}

	/** Takes the whole thing off screen (the home screen is in front, or it was switched off). */
	fun hide() {
		this.animator?.cancel()
		this.animator = null

		if (this.attached) {
			this.windowManager.removeViewImmediate(this.root)
			this.attached = false
		}

		this.opened = false
		this.dragging = false
		this.progress = 0F
	}

	/** Re-reads the settings and rebuilds, e.g. after a rotation. */
	fun refresh() {
		if (this.attached && ! this.opened) {
			this.show(this.items)
		}
	}

	// --- Building ---------------------------------------------------------------

	private fun build() {
		val res = this.context.resources
		val theme = DependencyContainer.of(this.context).themeManager.current
		val prefs = Preferences.getSharedPreferences(this.context)

		this.edge = FloatingLauncherGeometry.edgeOrDefault(LauncherIconGrid.launcherEdge(this.context))
		this.zone = LauncherServiceZone.of(
			prefs.getString(Preference.LAUNCHER_SERVICE_ZONE.getName(),
				Preference.LAUNCHER_SERVICE_ZONE.getDefault()))
		this.sensitivity = LauncherServiceSensitivity.of(
			prefs.getString(Preference.LAUNCHER_SERVICE_SENSITIVITY.getName(),
				Preference.LAUNCHER_SERVICE_SENSITIVITY.getDefault()))

		val vertical = FloatingLauncherGeometry.isVertical(this.edge)
		val tileColour = LauncherTileColour.resolve(this.context, theme)

		this.bar.removeAllViews()
		this.bar.orientation = if (vertical) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL

		if (res.getBoolean(theme.launcher_background_dynamic)) {
			this.bar.setBackgroundColor(tileColour)
		} else {
			val backgrounds = res.obtainTypedArray(theme.launcher_background)
			this.bar.setBackgroundResource(
				backgrounds.getResourceId(this.edge.n, R.color.transparent))
			backgrounds.recycle()
		}

		/*
		 * The menu button is the floating launcher's only route to the dash, so it
		 * is always shown — even on a theme (or a user setting) that hides it on
		 * the home screen, where the dash is a swipe away instead. Its artwork
		 * still follows the theme and the edge, like the dock's.
		 */
		val bfb = LayoutInflater.from(this.context)
			.inflate(R.layout.widget_service_launcher_bfb, this.bar, false) as AppLauncher
		bfb.setIcon(res.getDrawable(
			if (vertical) theme.launcher_bfb_image_vertical else theme.launcher_bfb_image, null))
		bfb.init()
		bfb.colour = tileColour
		bfb.setOnClickListener { this.openDash() }
		this.bar.addView(bfb)

		val strip = LinearLayout(this.context)
		strip.orientation = this.bar.orientation
		for (item in this.items) {
			strip.addView(this.tile(item, tileColour))
		}

		val scroller: ViewGroup = if (vertical) {
			ScrollView(this.context).also { it.isVerticalScrollBarEnabled = false }
		} else {
			HorizontalScrollView(this.context).also { it.isHorizontalScrollBarEnabled = false }
		}
		scroller.addView(strip, ViewGroup.LayoutParams(
			if (vertical) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT,
			if (vertical) ViewGroup.LayoutParams.WRAP_CONTENT else ViewGroup.LayoutParams.MATCH_PARENT))

		/*
		 * The icons take the rest of the bar and scroll when there are more than
		 * fit — but only on a theme whose launcher spans the edge. A dock that
		 * wraps its contents (GNOME) has no "rest" to take: a weighted child in a
		 * wrap_content LinearLayout has nothing to be given a share of and would
		 * collapse to nothing.
		 */
		val expand = res.getBoolean(theme.launcher_expand)
		val along = if (expand) 0 else ViewGroup.LayoutParams.WRAP_CONTENT
		val scrollerParams = LinearLayout.LayoutParams(
			if (vertical) ViewGroup.LayoutParams.MATCH_PARENT else along,
			if (vertical) along else ViewGroup.LayoutParams.MATCH_PARENT,
			if (expand) 1F else 0F)
		this.bar.addView(scroller, scrollerParams)

		this.applyBarLayout(expand)
		this.measureBar()
		this.applyProgress(0F)
	}

	private fun tile(item: FloatingLauncherItem, tileColour: Int): AppLauncher {
		val tile = AppLauncher(this.context, null as AttributeSet?)

		// The bar is icons only, so the label is what a screen reader has to go on //
		tile.contentDescription = item.label
		item.icon(this.context)?.let { tile.setIcon(it) }
		tile.init()
		tile.colour = tileColour
		tile.setOnClickListener { view ->
			view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
			item.launch(this.context)
			this.settle(false)
		}

		return tile
	}

	/**
	 * Sizes and places the bar within the (full-screen) window: along the docked
	 * edge, spanning it or wrapping its contents as the theme's `launcher_expand`
	 * says, and padded clear of the system bars.
	 */
	private fun applyBarLayout(expand: Boolean) {
		val vertical = FloatingLauncherGeometry.isVertical(this.edge)
		val along = if (expand) ViewGroup.LayoutParams.MATCH_PARENT
			else ViewGroup.LayoutParams.WRAP_CONTENT

		val params = FrameLayout.LayoutParams(
			if (vertical) ViewGroup.LayoutParams.WRAP_CONTENT else along,
			if (vertical) along else ViewGroup.LayoutParams.WRAP_CONTENT)
		params.gravity = when (this.edge) {
			Location.RIGHT -> Gravity.RIGHT or Gravity.CENTER_VERTICAL
			Location.TOP -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
			Location.BOTTOM -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
			else -> Gravity.LEFT or Gravity.CENTER_VERTICAL
		}
		this.bar.layoutParams = params

		// A bar that spans the edge runs behind the system bars and is padded clear
		// of them; one that wraps its contents is centred well away from both, and
		// padding it would only pad the dock's own background out //
		if (! expand) {
			this.bar.setPadding(0, 0, 0, 0)

			return
		}

		val insets = this.windowManager.currentWindowMetrics.windowInsets
			.getInsets(WindowInsets.Type.systemBars())
		if (vertical) {
			this.bar.setPadding(0, insets.top, 0, insets.bottom)
		} else {
			this.bar.setPadding(insets.left, 0, insets.right, 0)
		}
	}

	/**
	 * The bar's own length across the edge, measured against the full screen —
	 * the distance it has to travel to come out. It cannot be read off the laid
	 * out view: while closed the window is only a strip wide, so that is all the
	 * bar would ever be given.
	 */
	private fun measureBar() {
		val bounds = this.windowManager.currentWindowMetrics.bounds

		this.bar.measure(
			View.MeasureSpec.makeMeasureSpec(bounds.width(), View.MeasureSpec.AT_MOST),
			View.MeasureSpec.makeMeasureSpec(bounds.height(), View.MeasureSpec.AT_MOST))

		this.barLengthPx = if (FloatingLauncherGeometry.isVertical(this.edge))
			this.bar.measuredWidth else this.bar.measuredHeight
	}

	// --- Window shapes ----------------------------------------------------------

	private fun baseParams(): WindowManager.LayoutParams {
		val params = WindowManager.LayoutParams(
			WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
			PixelFormat.TRANSLUCENT)

		// Lay out against the whole display, so the strip's placement is the plain
		// screen geometry the zone fractions describe (and an open bar can dim the
		// system bar areas too); the bar itself is padded clear of the bars above. //
		params.fitInsetsTypes = 0

		return params
	}

	/** The thin grabbable strip: the hot zone's width, over the chosen stretch of the edge. */
	private fun closedParams(): WindowManager.LayoutParams {
		val params = this.baseParams()
		val bounds = this.windowManager.currentWindowMetrics.bounds
		val hotZone = this.sensitivity.hotZonePx(this.context.resources.displayMetrics.density)

		if (FloatingLauncherGeometry.isVertical(this.edge)) {
			params.width = hotZone
			params.height = this.zone.lengthPx(bounds.height())
			params.gravity = (if (this.edge == Location.RIGHT) Gravity.RIGHT else Gravity.LEFT) or
				Gravity.TOP
			params.y = this.zone.offsetPx(bounds.height())
		} else {
			params.height = hotZone
			params.width = this.zone.lengthPx(bounds.width())
			params.gravity = (if (this.edge == Location.BOTTOM) Gravity.BOTTOM else Gravity.TOP) or
				Gravity.LEFT
			params.x = this.zone.offsetPx(bounds.width())
		}

		return params
	}

	/** The full-screen shape used while the bar is being pulled out or is open. */
	private fun openParams(): WindowManager.LayoutParams {
		val params = this.baseParams()
		params.width = WindowManager.LayoutParams.MATCH_PARENT
		params.height = WindowManager.LayoutParams.MATCH_PARENT
		params.gravity = Gravity.TOP or Gravity.LEFT

		return params
	}

	// --- Gesture ----------------------------------------------------------------

	private fun onGrabberTouch(event: MotionEvent): Boolean {
		if (! this.attached) {
			return false
		}

		when (event.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				if (this.opened) {
					return false
				}

				this.animator?.cancel()
				this.downX = event.rawX
				this.downY = event.rawY
				this.dragging = false
				// Grow before the finger has moved, so the bar is laid out and ready
				// by the time the first pull arrives //
				this.windowManager.updateViewLayout(this.root, this.openParams())

				return true
			}
			MotionEvent.ACTION_MOVE -> {
				val pulled = this.pulled(event)
				if (! this.dragging) {
					if (pulled < this.touchSlop) {
						return true
					}

					this.dragging = true
				}

				this.applyProgress(
					FloatingLauncherGeometry.progress(pulled, this.barLengthPx))

				return true
			}
			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				val open = event.actionMasked == MotionEvent.ACTION_UP &&
					FloatingLauncherGeometry.settleOpen(this.pulled(event), this.barLengthPx,
						this.sensitivity.pullPx(this.context.resources.displayMetrics.density))
				this.dragging = false
				this.settle(open)

				return true
			}
		}

		return false
	}

	private fun pulled(event: MotionEvent): Float = FloatingLauncherGeometry.pulled(
		this.edge, this.downX, this.downY, event.rawX, event.rawY)

	private fun applyProgress(progress: Float) {
		this.progress = progress

		val offset = FloatingLauncherGeometry.translation(this.edge, this.barLengthPx, progress)
		if (FloatingLauncherGeometry.isVertical(this.edge)) {
			this.bar.translationX = offset
		} else {
			this.bar.translationY = offset
		}

		this.scrim.alpha = progress * SCRIM_ALPHA
		this.scrim.visibility = if (progress > 0F) View.VISIBLE else View.GONE
	}

	/** Animates to fully open or fully closed and lands the window in the matching shape. */
	private fun settle(open: Boolean) {
		this.animator?.cancel()

		val target = if (open) 1F else 0F
		if (! this.animationsEnabled) {
			this.applyProgress(target)
			this.settled(open)

			return
		}

		this.animator = ValueAnimator.ofFloat(this.progress, target).apply {
			this.duration = FloatingLauncherGeometry.settleDurationMs(
				this@FloatingLauncherWindow.progress, target, SETTLE_MS)
			this.interpolator = DecelerateInterpolator()
			this.addUpdateListener {
				this@FloatingLauncherWindow.applyProgress(it.animatedValue as Float)
			}
			// cancel() also reports an end, and the thing that cancels a settle is
			// always something taking over from it (a new pull, or hiding the whole
			// window) — landing the cancelled settle's state on top of it would
			// undo exactly that //
			this.addListener(object : AnimatorListenerAdapter() {
				private var cancelled = false

				override fun onAnimationCancel(animation: Animator) {
					this.cancelled = true
				}

				override fun onAnimationEnd(animation: Animator) {
					if (! this.cancelled) {
						this@FloatingLauncherWindow.settled(open)
					}
				}
			})
			this.start()
		}
	}

	private fun settled(open: Boolean) {
		this.animator = null
		this.opened = open
		this.applyProgress(if (open) 1F else 0F)

		// Only an open bar swallows touches outside itself (to be dismissed by
		// them); closed, the window shrinks back to its strip //
		this.scrim.isClickable = open
		if (! open && this.attached) {
			this.windowManager.updateViewLayout(this.root, this.closedParams())
		}
	}

	private val animationsEnabled: Boolean
		get() = this.context.getSystemService(PowerManager::class.java)?.isPowerSaveMode != true

	/**
	 * Takes the user to the dash. With DistroHopper as the home screen that means
	 * going home first (the HOME intent, exactly as the home key would), so the
	 * dash opens over the home screen it belongs to; otherwise there is no home to
	 * go to and DistroHopper is opened on its own.
	 */
	private fun openDash() {
		val intent = if (HomeRole.isHeld(this.context)) {
			Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
		} else {
			Intent(this.context, HomeActivity::class.java)
		}

		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		intent.putExtra("openDash", true)

		this.context.startActivity(intent)
		this.settle(false)
	}

	companion object {
		/** How dark the screen behind a fully open bar goes. */
		private const val SCRIM_ALPHA = 0.45F
		/** Settling animation length (ms) for the full travel. */
		private const val SETTLE_MS = 220L
	}
}
