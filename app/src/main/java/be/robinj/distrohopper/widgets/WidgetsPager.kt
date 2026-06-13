package be.robinj.distrohopper.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.os.PowerManager
import android.util.AttributeSet
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
 */
class WidgetsPager @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
) : ViewGroup(context, attrs) {
	var currentPage = 0
		private set

	private var insetLeft = 0
	private var insetTop = 0
	private var insetRight = 0
	private var insetBottom = 0

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

	init {
		this.pageAt(0)
	}

	/** One past the last desktop holding a widget, so a single empty one trails. */
	val pageCount: Int
		get() = (this.highestOccupiedPage() + 2).coerceIn(1, MAX_PAGES)

	val currentPageContainer: WidgetsContainer
		get() = this.pageAt(this.currentPage)

	/** The page at [index], creating it (and any before it) as needed. */
	fun pageAt(index: Int): WidgetsContainer {
		while (this.childCount <= index) {
			val page = WidgetsContainer(this.context, null)
			page.setPadding(this.insetLeft, this.insetTop, this.insetRight, this.insetBottom)
			this.addView(page)
		}

		return this.getChildAt(index) as WidgetsContainer
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

		if (!animate || this.width <= 0 || !this.animationsEnabled) {
			this.scrollTo(targetX, 0)

			return
		}

		this.settle = ValueAnimator.ofInt(this.scrollX, targetX).also { animator ->
			animator.duration = SETTLE_DURATION_MS
			animator.interpolator = DecelerateInterpolator()
			animator.addUpdateListener { this.scrollTo(it.animatedValue as Int, 0) }
			animator.start()
		}
	}

	//# Finger-tracked panning (used by HomeGestureController and the interception below) #//

	fun panBegin() {
		this.settle?.cancel()
		this.settle = null
		this.panStartScrollX = this.scrollX
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

		val target = if (abs(velocityX) > this.flingVelocityPx) {
			// A leftward fling (negative velocity) moves to the next page //
			if (velocityX < 0F) {
				fraction.toInt() + 1
			} else {
				(fraction + FLING_BACK_LEEWAY).toInt()
			}
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
				this.velocityTracker = VelocityTracker.obtain().also { it.addMovement(ev) }
			}
			MotionEvent.ACTION_MOVE -> {
				this.velocityTracker?.addMovement(ev)

				val dx = ev.x - this.downX
				val dy = ev.y - this.downY
				if (!this.hasEditModeChild()
						&& abs(dx) > this.touchSlop && abs(dx) > abs(dy) * 2F) {
					this.panning = true
					this.panOriginX = ev.x
					this.panBegin()

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

	private fun highestOccupiedPage(): Int {
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
		/*
		 * A rightward fling settles on the page being scrolled back towards;
		 * the leeway keeps a fling thrown just past a page boundary from
		 * skipping an extra page back.
		 */
		private const val FLING_BACK_LEEWAY = 0.999F
	}
}
