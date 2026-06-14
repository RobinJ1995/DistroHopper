package be.robinj.distrohopper.widgets

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.PowerManager
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Horizontal pager of widget desktops: each child is a full-size
 * [WidgetsContainer] page, laid out side by side and scrolled between.
 * There is always exactly one empty desktop after the last one holding a
 * widget (so swiping right past the end lands on a fresh desktop, but empty
 * desktops never pile up), capped at [MAX_PAGES]; widgets dropped on the
 * trailing desktop grow the row by one. Page views and the count are
 * re-derived in [pagesChanged] whenever widgets come or go.
 *
 * Swipes over empty desktop space are driven externally (HomeGestureController
 * calls [panBegin]/[panBy]/[panSettle]); swipes that start on a widget are
 * intercepted here once they lock to the horizontal axis, and feed the same
 * pan. Interception stays out of the way while a widget is in edit mode, so
 * its resize handles keep receiving horizontal drags.
 *
 * A row of dots (drawn in [dispatchDraw]) appears briefly while the desktops
 * are swiped between, showing which one is in view; it snaps in on a swipe
 * and fades out shortly after settling.
 */
class WidgetsPager @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
) : ViewGroup(context, attrs) {
	fun interface PageScrollListener {
		fun onScroll(fromPage: Int, toPage: Int, fraction: Float)
	}

	fun interface PageSettledListener {
		fun onSettled(page: Int)
	}

	var currentPage = 0
		private set

	/**
	 * Highest occupied desktop index across widgets *and* pins (or -1) — what the
	 * desktop count is derived from. Defaults to the pager's own widgets;
	 * HomeActivity points it at `home/Desktops`, the single authority that
	 * combines widgets with pinned apps, so the pager stays decoupled from the
	 * app model.
	 */
	var occupiedDesktopSupplier: () -> Int = { this.highestWidgetPage() }

	/** Continuous scroll between desktops (during pan and settle), for the launcher morph. */
	var onPageScroll: PageScrollListener? = null

	/** Fired once a desktop is settled on (the launcher rebuilds to it). */
	var onPageSettled: PageSettledListener? = null

	/**
	 * Feeds a touch stream to the home-screen swipe gestures (HomeActivity wires
	 * it to HomeGestureController). Used to hand off swipe-ups that start on a
	 * widget so the dash opens just as it does on empty desktop space — see
	 * [onInterceptTouchEvent]. Sideways pans are still handled here directly.
	 */
	var swipeGestureForwarder: ((MotionEvent) -> Boolean)? = null

	private var insetLeft = 0
	private var insetTop = 0
	private var insetRight = 0
	private var insetBottom = 0
	private var displayRotation = 0

	private var settle: ValueAnimator? = null

	private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
	private val flingVelocityPx =
		FLING_VELOCITY_DP_S * context.resources.displayMetrics.density
	private var downX = 0F
	private var downY = 0F
	private var panOriginX = 0F
	private var panning = false
	private var panStartScrollX = 0
	private var velocityTracker: VelocityTracker? = null
	private var downEvent: MotionEvent? = null

	/*
	 * A row of dots overlaid on the desktops while they are being swiped
	 * between, showing which desktop is in view. It snaps in on a swipe and
	 * fades out shortly after settling; the active dot tracks the scroll
	 * position so it slides between dots as the pages do.
	 */
	private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val indicatorDotRadius = this.dp(INDICATOR_DOT_RADIUS_DP)
	private val indicatorSpacing = this.dp(INDICATOR_SPACING_DP)
	private val indicatorBottomMargin = this.dp(INDICATOR_BOTTOM_MARGIN_DP)
	private var indicatorAlpha = 0F
	private var indicatorFade: ValueAnimator? = null
	private val hidePageIndicator = Runnable { this.fadePageIndicatorOut() }

	init {
		this.pageAt(0)
	}

	private fun dp(value: Float): Float =
		TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
			this.context.resources.displayMetrics)

	/** One past the last occupied desktop, so a single empty one trails. */
	val pageCount: Int
		get() = (this.occupiedDesktopSupplier() + 2).coerceIn(1, MAX_PAGES)

	val currentPageContainer: WidgetsContainer
		get() = this.pageAt(this.currentPage)

	/** The page at [index], creating it (and any before it) as needed. */
	fun pageAt(index: Int): WidgetsContainer {
		while (this.childCount <= index) {
			val page = WidgetsContainer(this.context, null)
			page.setPadding(this.insetLeft, this.insetTop, this.insetRight, this.insetBottom)
			page.setDisplayRotation(this.displayRotation)
			this.addView(page)
		}

		return this.getChildAt(index) as WidgetsContainer
	}

	/**
	 * Propagates a display rotation change to all pages so they re-layout
	 * with the correct portrait-to-display transform. Call from
	 * [be.robinj.distrohopper.HomeActivity.onConfigurationChanged].
	 */
	fun setDisplayRotation(rotation: Int) {
		this.displayRotation = rotation
		for (i in 0 until this.childCount) {
			(this.getChildAt(i) as? WidgetsContainer)?.setDisplayRotation(rotation)
		}
	}

	/**
	 * Re-derives the desktop row after widgets were added or removed: trailing
	 * empty page views beyond the single allowed one are dropped, the trailing
	 * empty desktop is (re)created, and a now-out-of-range current page snaps
	 * back into range.
	 */
	fun pagesChanged() {
		val count = this.pageCount

		while (this.childCount > count) {
			this.removeViewAt(this.childCount - 1)
		}
		this.pageAt(count - 1)

		if (this.currentPage > count - 1) {
			this.setCurrentPage(count - 1, animate = true)
		}
	}

	fun setCurrentPage(page: Int, animate: Boolean) {
		this.settle?.cancel()
		this.settle = null

		val target = page.coerceIn(0, this.pageCount - 1)
		this.currentPage = target
		val targetX = target * this.width

		this.showPageIndicator(autoHide = true) // Settling on a page: flash then fade //

		if (!animate || this.width <= 0 || !this.animationsEnabled) {
			this.scrollTo(targetX, 0)
			this.onPageSettled?.onSettled(target)

			return
		}

		this.settle = ValueAnimator.ofInt(this.scrollX, targetX).also { animator ->
			animator.duration = SETTLE_DURATION_MS
			animator.interpolator = DecelerateInterpolator()
			animator.addUpdateListener { this.scrollTo(it.animatedValue as Int, 0) }
			animator.addListener(object : AnimatorListenerAdapter() {
				private var cancelled = false

				override fun onAnimationCancel(animation: Animator) {
					this.cancelled = true
				}

				override fun onAnimationEnd(animation: Animator) {
					if (this@WidgetsPager.settle === animator) {
						this@WidgetsPager.settle = null
					}
					if (! this.cancelled) {
						this@WidgetsPager.onPageSettled?.onSettled(target)
					}
				}
			})
			animator.start()
		}
	}

	//# Finger-tracked panning (used by HomeGestureController and the interception below) #//

	fun panBegin() {
		this.settle?.cancel()
		this.settle = null
		this.panStartScrollX = this.scrollX
		this.showPageIndicator(autoHide = false) // Stays up for the duration of the drag //
	}

	/** [dxPx] is the distance panned towards higher pages (finger moving left). */
	fun panBy(dxPx: Float) {
		val maxScrollX = (this.pageCount - 1) * this.width

		this.scrollTo((this.panStartScrollX + dxPx).roundToInt().coerceIn(0, maxScrollX), 0)
	}

	/** Snaps to the nearest page, or one over in the fling's direction. */
	fun panSettle(velocityX: Float) {
		val width = max(1, this.width)
		val fraction = this.scrollX.toFloat() / width

		/*
		 * floor(fraction) is the page to the left of the current scroll
		 * position. A fast fling settles on the neighbour in the fling's
		 * direction (forward = finger left = negative velocity = the page to
		 * the right; back = finger right = positive velocity = the page to the
		 * left); a slow drag just snaps to whichever page is nearest.
		 */
		val base = fraction.toInt() // fraction is never negative (scrollX is clamped >= 0) //
		val target = if (abs(velocityX) > this.flingVelocityPx) {
			if (velocityX < 0F) base + 1 else base
		} else {
			fraction.roundToInt()
		}

		this.setCurrentPage(target, animate = true)
	}

	//# Swipes starting on a widget #//

	override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
		when (ev.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				this.clearGesture()
				this.downX = ev.x
				this.downY = ev.y
				this.downEvent = MotionEvent.obtain(ev)
				this.velocityTracker = VelocityTracker.obtain().also { it.addMovement(ev) }
			}
			MotionEvent.ACTION_MOVE -> {
				this.velocityTracker?.addMovement(ev)

				val dx = ev.x - this.downX
				val dy = ev.y - this.downY
				if (this.hasEditModeChild()) {
					// A widget is being resized/moved: leave its handles the touches //
					return false
				}

				if (abs(dx) > this.touchSlop && abs(dx) > abs(dy) * 2F) {
					this.panning = true
					this.panOriginX = ev.x
					this.panBegin()

					return true
				}

				// Swipe up that started on a widget: hand the stream to the home
				// gestures (priming them with the original DOWN they never saw, since
				// the widget consumed it) so the dash opens just like on empty space //
				if (abs(dy) > this.touchSlop && abs(dy) > abs(dx) * 2F && dy < 0F) {
					this.downEvent?.let { this.swipeGestureForwarder?.invoke(it) }
					this.recycleDownEvent()

					return true
				}
			}
			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> this.clearGesture()
		}

		return false
	}

	override fun onTouchEvent(ev: MotionEvent): Boolean {
		if (this.panning) {
			when (ev.actionMasked) {
				MotionEvent.ACTION_MOVE -> {
					this.velocityTracker?.addMovement(ev)
					this.panBy(this.panOriginX - ev.x)
				}
				MotionEvent.ACTION_UP -> {
					val velocityX = this.velocityTracker?.let {
						it.addMovement(ev)
						it.computeCurrentVelocity(1000)
						it.xVelocity
					} ?: 0F
					this.panSettle(velocityX)
					this.clearGesture()
				}
				MotionEvent.ACTION_CANCEL -> {
					this.panSettle(0F)
					this.clearGesture()
				}
			}

			return true
		}

		return super.onTouchEvent(ev)
	}

	private fun clearGesture() {
		this.panning = false
		this.velocityTracker?.recycle()
		this.velocityTracker = null
		this.recycleDownEvent()
	}

	private fun recycleDownEvent() {
		this.downEvent?.recycle()
		this.downEvent = null
	}

	//# Aggregates over the pages #//

	/** Grid placements of every widget on every desktop, with their page set. */
	fun collectLayouts(exclude: View?): List<WidgetLayout> {
		val layouts = mutableListOf<WidgetLayout>()

		for (i in 0 until this.childCount) {
			(this.getChildAt(i) as WidgetsContainer).collectLayouts(exclude).forEach {
				it.page = i
				layouts.add(it)
			}
		}

		return layouts
	}

	fun hasEditModeChild(): Boolean =
		(0 until this.childCount).any {
			(this.getChildAt(it) as WidgetsContainer).hasEditModeChild()
		}

	fun exitEditMode() {
		for (i in 0 until this.childCount) {
			(this.getChildAt(i) as WidgetsContainer).exitEditMode()
		}
	}

	/** Shows the grid-intersection overlay on every desktop (no-op unless the dev option is on). */
	fun showGridOverlay() {
		for (i in 0 until this.childCount) {
			(this.getChildAt(i) as WidgetsContainer).showGridOverlay()
		}
	}

	fun hideGridOverlay() {
		for (i in 0 until this.childCount) {
			(this.getChildAt(i) as WidgetsContainer).hideGridOverlay()
		}
	}

	/**
	 * Keeps each desktop clear of the launcher and the navigation bar; applied
	 * as padding on every page (existing and future) rather than the pager
	 * itself, so the per-page grid maths stay self-contained.
	 */
	fun setPageInsets(left: Int, top: Int, right: Int, bottom: Int) {
		this.insetLeft = left
		this.insetTop = top
		this.insetRight = right
		this.insetBottom = bottom

		for (i in 0 until this.childCount) {
			this.getChildAt(i).setPadding(left, top, right, bottom)
		}
	}

	private val animationsEnabled: Boolean
		get() = this.context.getSystemService(PowerManager::class.java)
			?.isPowerSaveMode != true

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		this.setMeasuredDimension(
			getDefaultSize(this.suggestedMinimumWidth, widthMeasureSpec),
			getDefaultSize(this.suggestedMinimumHeight, heightMeasureSpec))

		for (i in 0 until this.childCount) {
			this.getChildAt(i).measure(
				MeasureSpec.makeMeasureSpec(this.measuredWidth, MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(this.measuredHeight, MeasureSpec.EXACTLY))
		}
	}

	override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
		val width = r - l
		val height = b - t

		for (i in 0 until this.childCount) {
			this.getChildAt(i).layout(i * width, 0, (i + 1) * width, height)
		}

		// A resize (e.g. rotation) must not leave the scroll between pages //
		if (changed && this.settle == null && !this.panning) {
			this.scrollTo(this.currentPage * width, 0)
		}
	}

	override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
		super.onScrollChanged(l, t, oldl, oldt)

		val callback = this.onPageScroll ?: return
		val width = max(1, this.width)
		val position = l.toFloat() / width
		val count = this.pageCount
		val from = position.toInt().coerceIn(0, count - 1)
		val to = (from + 1).coerceAtMost(count - 1)

		callback.onScroll(from, to, (position - from).coerceIn(0F, 1F))
	}

	//# Page indicator #//

	/** Snaps the indicator into view (when there is more than one desktop). */
	private fun showPageIndicator(autoHide: Boolean) {
		this.removeCallbacks(this.hidePageIndicator)
		this.indicatorFade?.cancel()
		this.indicatorFade = null

		if (this.pageCount <= 1) { // Nothing to indicate //
			this.indicatorAlpha = 0F
			this.invalidate()

			return
		}

		this.indicatorAlpha = 1F
		this.invalidate()

		if (autoHide) {
			this.postDelayed(this.hidePageIndicator, INDICATOR_HIDE_DELAY_MS)
		}
	}

	private fun fadePageIndicatorOut() {
		this.indicatorFade?.cancel()

		if (!this.animationsEnabled || this.width == 0) {
			this.indicatorAlpha = 0F
			this.invalidate()

			return
		}

		this.indicatorFade = ValueAnimator.ofFloat(this.indicatorAlpha, 0F).also { animator ->
			animator.duration = INDICATOR_FADE_MS
			animator.addUpdateListener {
				this.indicatorAlpha = it.animatedValue as Float
				this.invalidate()
			}
			animator.start()
		}
	}

	/** Whether the page indicator is currently visible (drives drawing; exposed for tests). */
	internal val isPageIndicatorShowing: Boolean
		get() = this.indicatorAlpha > 0F

	/*
	 * Content-space x that places the dot row at the centre of the viewport
	 * (the launcher-free part of the width). Drawing happens in scrolled
	 * content space, so it carries scrollX — keeping the row viewport-fixed as
	 * the desktops scroll. Exposed for tests.
	 */
	internal fun indicatorContentCentreX(): Float =
		this.scrollX + (this.insetLeft + (this.width - this.insetRight)) / 2F

	override fun dispatchDraw(canvas: Canvas) {
		super.dispatchDraw(canvas)

		val count = this.pageCount
		if (this.indicatorAlpha <= 0F || count <= 1) {
			return
		}

		/*
		 * dispatchDraw paints in the pager's scrolled content space (x = 0 is
		 * the left edge of the first desktop), so the row's screen position is
		 * offset by scrollX to keep it fixed in the viewport while the desktops
		 * scroll underneath. Centred within the part of the width left clear of
		 * the launcher, and lifted above the navigation bar.
		 */
		val span = (count - 1) * this.indicatorSpacing
		val centreX = this.indicatorContentCentreX()
		val firstX = centreX - span / 2F
		val cy = this.height - this.insetBottom - this.indicatorBottomMargin

		for (i in 0 until count) {
			this.indicatorPaint.color = Color.argb(
				(INDICATOR_INACTIVE_ALPHA * this.indicatorAlpha).toInt(), 255, 255, 255)
			canvas.drawCircle(firstX + i * this.indicatorSpacing, cy,
				this.indicatorDotRadius, this.indicatorPaint)
		}

		// The active dot tracks the scroll position, sliding between dots //
		val position = this.scrollX.toFloat() / max(1, this.width)
		val from = position.toInt().coerceIn(0, count - 1)
		val to = (from + 1).coerceAtMost(count - 1)
		val activeX = (firstX + from * this.indicatorSpacing) +
			(to - from) * this.indicatorSpacing * (position - from)

		this.indicatorPaint.color = Color.argb(
			(255 * this.indicatorAlpha).toInt(), 255, 255, 255)
		canvas.drawCircle(activeX, cy, this.indicatorDotRadius, this.indicatorPaint)
	}

	/** Highest desktop index holding a widget (or -1); pins are folded in by the supplier. */
	fun highestWidgetPage(): Int {
		for (i in this.childCount - 1 downTo 0) {
			val page = this.getChildAt(i) as WidgetsContainer

			for (j in 0 until page.childCount) {
				if (page.getChildAt(j) is WidgetContainer) {
					return i
				}
			}
		}

		return -1
	}

	companion object {
		/** A sane upper bound on how many desktops can ever exist. */
		const val MAX_PAGES = 16
		private const val SETTLE_DURATION_MS = 250L
		/** Above this speed the fling direction decides which page to settle on. */
		private const val FLING_VELOCITY_DP_S = 500F

		private const val INDICATOR_DOT_RADIUS_DP = 3.5F
		private const val INDICATOR_SPACING_DP = 14F
		private const val INDICATOR_BOTTOM_MARGIN_DP = 24F
		private const val INDICATOR_INACTIVE_ALPHA = 90 // out of 255 //
		private const val INDICATOR_FADE_MS = 250L
		private const val INDICATOR_HIDE_DELAY_MS = 900L
	}
}
