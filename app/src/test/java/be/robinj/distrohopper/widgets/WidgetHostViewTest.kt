package be.robinj.distrohopper.widgets

import android.content.Context
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode
import java.time.Duration

/**
 * The widget long-press that enters edit mode must behave like a stock
 * long-press: a finger that travels past touch slop (e.g. swiping between
 * desktops) aborts it, while a finger that stays put still triggers it.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class WidgetHostViewTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { this.scenario = ActivityTestSupport.launchHome() }
	@After fun tearDown() { this.scenario.close() }

	private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()

	/**
	 * Reports window focus so the long-press check's window guards pass
	 * deterministically, independent of how Robolectric models window focus —
	 * the point under test is the touch-slop cancellation, not the guards.
	 */
	private class FocusedWidgetHostView(context: Context, host: WidgetHost) :
		WidgetHostView(context, host) {
		override fun hasWindowFocus(): Boolean = true
	}

	private fun setUpWidget(activity: HomeActivity): Pair<WidgetHostView, WidgetContainer> {
		val grid = WidgetTestSupport.standaloneGrid(activity)
		val host = WidgetTestSupport.host(activity, grid)
		val hostView = FocusedWidgetHostView(activity.applicationContext, host)
		hostView.setAppWidget(42, WidgetTestSupport.providerInfo())
		val container = WidgetContainer(activity, host, hostView)
		grid.addView(container, WidgetsContainer.LayoutParams(2, 2, 2, 2))
		hostView.setOnLongClickListener(WidgetHostView_LongClickListener(container))

		// Attach the grid's pager to the resumed activity's (already-attached)
		// content view so the long-press check posted via View.postDelayed runs on
		// the looper — a detached view defers it to its run-queue, which never
		// flushes here //
		activity.findViewById<android.widget.FrameLayout>(android.R.id.content)
			.addView(WidgetTestSupport.pagerOf(grid))
		WidgetTestSupport.layoutGrid(grid)

		return hostView to container
	}

	private fun event(action: Int, x: Float, y: Float): MotionEvent =
		MotionEvent.obtain(0, SystemClock.uptimeMillis(), action, x, y, 0)

	private fun idlePastLongPress() =
		shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(this.longPressTimeout + 50))

	@Test fun aStationaryLongPressEntersEditMode() { this.scenario.onActivity { activity ->
		val (hostView, container) = this.setUpWidget(activity)

		hostView.onInterceptTouchEvent(event(MotionEvent.ACTION_DOWN, 100F, 100F))
		this.idlePastLongPress()

		assertTrue(container.editMode)
	} }

	@Test fun swipingPastTouchSlopDoesNotEnterEditMode() { this.scenario.onActivity { activity ->
		val (hostView, container) = this.setUpWidget(activity)
		val slop = ViewConfiguration.get(activity).scaledTouchSlop

		hostView.onInterceptTouchEvent(event(MotionEvent.ACTION_DOWN, 100F, 100F))
		// A horizontal swipe between desktops moves the finger well past touch slop //
		hostView.onInterceptTouchEvent(event(MotionEvent.ACTION_MOVE, 100F + slop + 10F, 105F))
		this.idlePastLongPress()

		assertFalse(container.editMode)
	} }
}
