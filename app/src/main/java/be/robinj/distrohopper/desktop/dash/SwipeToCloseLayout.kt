package be.robinj.distrohopper.desktop.dash

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.widget.LinearLayout
import be.robinj.distrohopper.R
import kotlin.math.abs

/**
 * The dash's container: a LinearLayout that recognises a downward swipe over
 * the dash and hands it to the [delegate] (HomeGestureController) to track a
 * swipe-to-close. The swipe only starts once the finger has travelled past
 * the touch slop, mostly vertically and downwards, and neither the app grid
 * nor the lens results can scroll up any further — so swiping within a
 * scrolled list first scrolls it back to the top, and only then starts
 * pulling the dash closed.
 */
class SwipeToCloseLayout @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
	interface Delegate {
		/** Whether a downward swipe may begin closing the dash right now. */
		fun dashSwipeEnabled(): Boolean
		/** @return whether a tracked close began (false = nothing to track). */
		fun dashSwipeStarted(): Boolean
		/** [dyPx] is the downward distance travelled since the swipe started. */
		fun dashSwipeMoved(dyPx: Float)
		fun dashSwipeEnded(dyPx: Float, velocityY: Float)
		fun dashSwipeCancelled()
	}

	var delegate: Delegate? = null

	private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
	private var downX = 0F
	private var downY = 0F
	private var originY = 0F
	private var tracking = false
	private var velocityTracker: VelocityTracker? = null

	override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
		when (ev.actionMasked) {
			MotionEvent.ACTION_DOWN -> this.begin(ev)
			MotionEvent.ACTION_MOVE -> return this.maybeStart(ev)
			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> this.clear()
		}

		return false
	}

	override fun onTouchEvent(ev: MotionEvent): Boolean {
		when (ev.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				// Touches on the dash's own background reach here directly
				// (no child claimed them); claim them so the MOVEs follow //
				this.begin(ev)

				return this.delegate != null
			}
			MotionEvent.ACTION_MOVE -> {
				if (! this.tracking) {
					this.maybeStart(ev)
				} else {
					this.velocityTracker?.addMovement(ev)
					this.delegate?.dashSwipeMoved(ev.y - this.originY)
				}

				return true
			}
			MotionEvent.ACTION_UP -> {
				if (this.tracking) {
					val velocityY = this.velocityTracker?.let {
						it.addMovement(ev)
						it.computeCurrentVelocity(1000)
						it.yVelocity
					} ?: 0F
					this.delegate?.dashSwipeEnded(ev.y - this.originY, velocityY)
				}
				this.clear()

				return true
			}
			MotionEvent.ACTION_CANCEL -> {
				if (this.tracking) {
					this.delegate?.dashSwipeCancelled()
				}
				this.clear()

				return true
			}
		}

		return super.onTouchEvent(ev)
	}

	private fun begin(ev: MotionEvent) {
		this.clear()
		this.downX = ev.x
		this.downY = ev.y
		this.velocityTracker = VelocityTracker.obtain().also { it.addMovement(ev) }
	}

	private fun maybeStart(ev: MotionEvent): Boolean {
		val delegate = this.delegate ?: return false
		if (! delegate.dashSwipeEnabled()) {
			return false
		}

		this.velocityTracker?.addMovement(ev)

		val dx = ev.x - this.downX
		val dy = ev.y - this.downY
		if (dy > this.touchSlop && dy > abs(dx) * 2F && ! this.contentCanScrollUp()
				&& delegate.dashSwipeStarted()) {
			this.tracking = true
			this.originY = ev.y

			return true
		}

		return false
	}

	private fun contentCanScrollUp(): Boolean =
		listOf(R.id.gvDashHomeApps, R.id.lvDashHomeLensResults).any {
			val view = this.findViewById<android.view.View>(it)
			view != null && view.isShown && view.canScrollVertically(-1)
		}

	private fun clear() {
		this.tracking = false
		this.velocityTracker?.recycle()
		this.velocityTracker = null
	}
}
