package be.robinj.distrohopper.home

import android.app.Activity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import be.robinj.distrohopper.R
import be.robinj.distrohopper.ViewFinder
import be.robinj.distrohopper.desktop.dash.SwipeToCloseLayout
import kotlin.math.abs

/**
 * The home screen's swipe gestures. On empty desktop space (HomeActivity
 * feeds in the touches no view claimed; the panel and launcher are excluded
 * by hit-testing) swiping down pulls down the system notification shade and
 * swiping up slides the dash in, tracking the finger — DashController and
 * DashAnimator do the visual work — committing or cancelling when it lifts.
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
	private enum class State { IDLE, PENDING, TRACKING_OPEN, INSTANT_OPEN, DONE }

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

	//# Home screen (empty desktop space) #//

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
					State.INSTANT_OPEN -> if (this.downY - ev.y > this.touchSlop * 4F) {
						this.dash.open()
						this.viewModel.openDash()
						this.state = State.DONE
					}
					else -> {}
				}
			}
			MotionEvent.ACTION_UP -> {
				if (this.state == State.IDLE) {
					return false
				}
				if (this.state == State.TRACKING_OPEN) {
					this.velocityTracker?.addMovement(ev)
					val velocityY = this.velocityTracker?.let {
						it.computeCurrentVelocity(1000)
						it.yVelocity
					} ?: 0F
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
				this.reset()
			}
			MotionEvent.ACTION_CANCEL -> {
				if (this.state == State.TRACKING_OPEN) {
					this.dash.swipeOpenEnd(commit = false)
				}
				this.reset()
			}
		}

		return this.state != State.IDLE
	}

	private fun maybeStart(ev: MotionEvent) {
		val dx = ev.x - this.downX
		val dy = ev.y - this.downY
		if (abs(dy) <= this.touchSlop || abs(dy) <= abs(dx) * 2F) {
			return // Not (yet) a mostly-vertical swipe //
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

	private fun hits(id: Int, ev: MotionEvent): Boolean {
		val view = this.viewFinder.get<View>(id)
		if (! view.isShown) {
			return false
		}

		val location = IntArray(2)
		view.getLocationInWindow(location)

		return ev.x >= location[0] && ev.x < location[0] + view.width
			&& ev.y >= location[1] && ev.y < location[1] + view.height
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
