package be.robinj.distrohopper.home

import android.app.Activity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import be.robinj.distrohopper.R
import be.robinj.distrohopper.ViewFinder
import be.robinj.distrohopper.desktop.dash.SwipeToCloseLayout
import be.robinj.distrohopper.widgets.WidgetsPager
import kotlin.math.abs

/**
 * The home screen's swipe gestures. On empty desktop space (the widget
 * pager's OnTouchListener feeds in its touches, and HomeActivity those no
 * view claimed; the panel and launcher are excluded by hit-testing):
 *  - swiping down pulls down the system notification shade;
 *  - swiping up slides the dash in, tracking the finger — DashController and
 *    DashAnimator do the visual work — committing or cancelling when it
 *    lifts;
 *  - swiping sideways pans between the widget desktops (WidgetsPager does
 *    the same for swipes that start on a widget).
 * As SwipeToCloseLayout's delegate it likewise tracks the open dash being
 * swiped back closed (the layout only starts that swipe once the dash's
 * content is scrolled to the top). In battery saver there is nothing to
 * track, so the dash just opens/closes instantly at the trigger distance.
 * Swipe-to-close stays out of customise mode: closing there relaunches the
 * activity, which is not something to trigger from a drag.
 */
class HomeGestureController @JvmOverloads constructor(
	private val activity: Activity,
	private val viewFinder: ViewFinder,
	private val dash: DashController,
	private val viewModel: HomeViewModel,
	private val customiseMode: () -> Boolean,
	private val expandNotificationShade: () -> Unit = { NotificationShade.expand(activity) },
) : SwipeToCloseLayout.Delegate {
	private enum class State { IDLE, PENDING, TRACKING_OPEN, TRACKING_PAGES, INSTANT_OPEN, DONE }

	private val touchSlop = ViewConfiguration.get(activity).scaledTouchSlop
	private val flingVelocityPx =
		FLING_VELOCITY_DP_S * activity.resources.displayMetrics.density

	private var state = State.IDLE
	private var downX = 0F
	private var downY = 0F
	private var startOpenness = 0F
	private var velocityTracker: VelocityTracker? = null

	private var closeTracking = false
	private var closeStartOpenness = 1F

	private val pager: WidgetsPager
		get() = this.viewFinder.get(R.id.vgWidgets)

	//# Home screen (empty desktop space) #//

	/**
	 * Feed in a touch stream; returns whether the event was consumed by a
	 * gesture. DOWNs are never consumed (so the views' own taps and
	 * long-presses keep working when this runs as an OnTouchListener), and the
	 * stream may arrive from different views (coordinates are only ever
	 * compared within one stream; hit-testing uses raw coordinates).
	 */
	fun onHomeTouchEvent(ev: MotionEvent): Boolean {
		when (ev.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				this.reset()
				if (this.dash.isOpen
						|| this.hits(R.id.llPanel, ev) || this.hits(R.id.llLauncher, ev)) {
					return false
				}

				this.state = State.PENDING
				this.downX = ev.x
				this.downY = ev.y
				this.velocityTracker = VelocityTracker.obtain().also { it.addMovement(ev) }

				return false
			}
			MotionEvent.ACTION_MOVE -> {
				if (this.state == State.IDLE) {
					return false
				}
				this.velocityTracker?.addMovement(ev)

				when (this.state) {
					State.PENDING -> this.maybeStart(ev)
					State.TRACKING_OPEN -> this.dash.swipeUpdate(this.startOpenness
						+ (this.downY - ev.y) / this.dash.swipeDistancePx)
					State.TRACKING_PAGES -> this.pager.panBy(this.downX - ev.x)
					State.INSTANT_OPEN -> if (this.downY - ev.y > this.touchSlop * 4F) {
						this.dash.open()
						this.viewModel.openDash()
						this.state = State.DONE
					}
					else -> {}
				}
			}
			MotionEvent.ACTION_UP -> {
				val consumed = this.state != State.IDLE && this.state != State.PENDING
				this.velocityTracker?.addMovement(ev)

				when (this.state) {
					State.TRACKING_OPEN -> {
						val velocityY = this.currentVelocity { it.yVelocity }
						val commit = if (abs(velocityY) > this.flingVelocityPx) {
							velocityY < 0F // Flung in the opening (upward) direction //
						} else {
							this.dash.swipeOpenness > COMMIT_FRACTION
						}

						this.dash.swipeOpenEnd(commit)
						if (commit) {
							this.viewModel.openDash()
						}
					}
					State.TRACKING_PAGES ->
						this.pager.panSettle(this.currentVelocity { it.xVelocity })
					else -> {}
				}
				this.reset()

				return consumed
			}
			MotionEvent.ACTION_CANCEL -> {
				when (this.state) {
					State.TRACKING_OPEN -> this.dash.swipeOpenEnd(commit = false)
					State.TRACKING_PAGES -> this.pager.panSettle(0F)
					else -> {}
				}
				this.reset()

				return false
			}
		}

		return this.state != State.IDLE && this.state != State.PENDING
	}

	private fun maybeStart(ev: MotionEvent) {
		val dx = ev.x - this.downX
		val dy = ev.y - this.downY

		if (abs(dx) > this.touchSlop && abs(dx) > abs(dy) * 2F) {
			// Sideways: pan between the widget desktops //
			this.pager.panBegin()
			this.state = State.TRACKING_PAGES
			this.downX = ev.x // Track from where the swipe was recognised //

			return
		}

		if (abs(dy) <= this.touchSlop || abs(dy) <= abs(dx) * 2F) {
			return // Not (yet) locked to either axis //
		}

		if (dy > 0F) { // Downwards: pull down the notification shade //
			this.expandNotificationShade()
			this.state = State.DONE
		} else if (this.dash.swipeOpenBegin()) { // Upwards: pull in the dash //
			this.state = State.TRACKING_OPEN
			this.startOpenness = this.dash.swipeOpenness
			this.downY = ev.y // Track from where the swipe was recognised //
		} else { // Battery saver: nothing to track; open at the trigger distance //
			this.state = State.INSTANT_OPEN
		}
	}

	private fun currentVelocity(axis: (VelocityTracker) -> Float): Float =
		this.velocityTracker?.let {
			it.computeCurrentVelocity(1000)
			axis(it)
		} ?: 0F

	/*
	 * Raw (screen) coordinates: the stream may come from the widget pager's
	 * OnTouchListener or from the activity, whose local coordinate spaces
	 * differ.
	 */
	private fun hits(id: Int, ev: MotionEvent): Boolean {
		val view = this.viewFinder.get<View>(id)
		if (! view.isShown) {
			return false
		}

		val location = IntArray(2)
		view.getLocationOnScreen(location)

		return ev.rawX >= location[0] && ev.rawX < location[0] + view.width
			&& ev.rawY >= location[1] && ev.rawY < location[1] + view.height
	}

	private fun reset() {
		this.state = State.IDLE
		this.velocityTracker?.recycle()
		this.velocityTracker = null
	}

	//# Dash swipe-to-close (SwipeToCloseLayout.Delegate) #//

	override fun dashSwipeEnabled(): Boolean = this.dash.isOpen && ! this.customiseMode()

	override fun dashSwipeStarted(): Boolean {
		if (this.dash.swipeCloseBegin()) {
			this.closeTracking = true
			this.closeStartOpenness = this.dash.swipeOpenness

			return true
		}

		// Battery saver: nothing to track; close instantly instead //
		this.closeTracking = false
		this.dash.close()
		this.viewModel.closeDash()

		return false
	}

	override fun dashSwipeMoved(dyPx: Float) {
		if (this.closeTracking) {
			this.dash.swipeUpdate(this.closeStartOpenness - dyPx / this.dash.swipeDistancePx)
		}
	}

	override fun dashSwipeEnded(dyPx: Float, velocityY: Float) {
		if (! this.closeTracking) {
			return
		}
		this.closeTracking = false

		val commit = if (abs(velocityY) > this.flingVelocityPx) {
			velocityY > 0F // Flung in the closing (downward) direction //
		} else {
			this.dash.swipeOpenness < 1F - COMMIT_FRACTION
		}

		this.dash.swipeCloseEnd(commit)
		if (commit) {
			this.viewModel.closeDash()
		}
	}

	override fun dashSwipeCancelled() {
		if (this.closeTracking) {
			this.closeTracking = false
			this.dash.swipeCloseEnd(commit = false)
		}
	}

	companion object {
		/** How far (as a fraction of the slide distance) before a release commits. */
		private const val COMMIT_FRACTION = 0.35F
		/** Above this speed the fling direction decides, regardless of distance. */
		private const val FLING_VELOCITY_DP_S = 500F
	}
}
