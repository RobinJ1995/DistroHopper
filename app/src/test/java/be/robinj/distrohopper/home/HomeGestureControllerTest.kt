package be.robinj.distrohopper.home

import android.content.Context
import android.os.PowerManager
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowPowerManager

/**
 * The home screen swipe gestures: swipe down on empty desktop space for the
 * notification shade, swipe up for a finger-tracked dash open, and (as
 * SwipeToCloseLayout's delegate) swipe down on the dash to close it again.
 * Runs under the PAUSED looper so the settle animators advance in
 * drainTasks().
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class HomeGestureControllerTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { this.scenario = ActivityTestSupport.launchHome() }

	@After fun tearDown() { this.scenario.close() }

	private class Harness(val activity: HomeActivity) {
		val dash: DashController
		val viewModel: HomeViewModel
		val gestures: HomeGestureController
		var shadeExpansions = 0

		init {
			val container = DependencyContainer.of(this.activity)
			this.dash = DashController(this.activity, this.activity.viewFinder,
				container.themeManager.current, container.prefs)
			this.viewModel = HomeViewModel(container)
			this.gestures = HomeGestureController(this.activity, this.activity.viewFinder,
				this.dash, this.viewModel, { false }, { this.shadeExpansions++ })
		}

		val slop = ViewConfiguration.get(this.activity).scaledTouchSlop.toFloat()

		/* A point on empty desktop space: clear of the launcher (left) and panel (top). */
		val emptyX = this.activity.findViewById<View>(R.id.rlContainer).width - 5F
		val emptyY = this.activity.findViewById<View>(R.id.rlContainer).height - 50F

		fun touch(action: Int, x: Float, y: Float, timeMs: Long): Boolean {
			val event = MotionEvent.obtain(0, timeMs, action, x, y, 0)
			try {
				return this.gestures.onHomeTouchEvent(event)
			} finally {
				event.recycle()
			}
		}
	}

	private fun onHarness(block: (Harness) -> Unit) {
		this.scenario.onActivity { block(Harness(it)) }
	}

	@Test fun swipingDownOnEmptySpaceExpandsTheNotificationShade() = this.onHarness { h ->
		h.touch(MotionEvent.ACTION_DOWN, h.emptyX, h.emptyY - 200F, 0)
		h.touch(MotionEvent.ACTION_MOVE, h.emptyX, h.emptyY - 100F, 50)
		h.touch(MotionEvent.ACTION_UP, h.emptyX, h.emptyY - 100F, 100)

		assertEquals(1, h.shadeExpansions)
		assertFalse(h.dash.isOpen)
	}

	@Test fun swipingDownOnThePanelIsIgnored() = this.onHarness { h ->
		val llPanel = h.activity.findViewById<View>(R.id.llPanel)
		val location = IntArray(2)
		llPanel.getLocationInWindow(location)
		val x = location[0] + llPanel.width / 2F
		val y = location[1] + llPanel.height / 2F

		assertFalse(h.touch(MotionEvent.ACTION_DOWN, x, y, 0))
		h.touch(MotionEvent.ACTION_MOVE, x, y + 100F, 50)
		h.touch(MotionEvent.ACTION_UP, x, y + 100F, 100)

		assertEquals(0, h.shadeExpansions)
	}

	@Test fun horizontalSwipesAreIgnored() = this.onHarness { h ->
		h.touch(MotionEvent.ACTION_DOWN, h.emptyX, h.emptyY, 0)
		h.touch(MotionEvent.ACTION_MOVE, h.emptyX - 150F, h.emptyY - 10F, 50)
		h.touch(MotionEvent.ACTION_UP, h.emptyX - 150F, h.emptyY - 10F, 100)

		assertEquals(0, h.shadeExpansions)
		assertFalse(h.dash.isOpen)
	}

	@Test fun swipingUpTracksTheDashAndCommitsPastTheThreshold() = this.onHarness { h ->
		val llDash = h.activity.findViewById<LinearLayout>(R.id.llDash)

		h.touch(MotionEvent.ACTION_DOWN, h.emptyX, h.emptyY, 0)
		// Past the slop: the swipe is recognised and tracking begins //
		h.touch(MotionEvent.ACTION_MOVE, h.emptyX, h.emptyY - h.slop * 3F, 50)
		assertEquals(View.VISIBLE, llDash.visibility)
		assertFalse(h.dash.isOpen) // Not committed yet //

		val distance = h.dash.swipeDistancePx
		h.touch(MotionEvent.ACTION_MOVE, h.emptyX, h.emptyY - h.slop * 3F - distance * 0.3F, 250)
		// Mid-flight: the dash is part-way in, tracking the finger //
		assertTrue(llDash.translationY > 0F && llDash.translationY < distance)

		h.touch(MotionEvent.ACTION_MOVE, h.emptyX, h.emptyY - h.slop * 3F - distance * 0.6F, 450)
		h.touch(MotionEvent.ACTION_UP, h.emptyX, h.emptyY - h.slop * 3F - distance * 0.6F, 500)
		ActivityTestSupport.drainTasks()

		assertTrue(h.dash.isOpen)
		assertTrue(h.viewModel.dashOpen.value)
		assertEquals(View.VISIBLE, llDash.visibility)
		assertEquals(0F, llDash.translationY, 0.001F)
	}

	@Test fun aShortSlowSwipeUpSettlesBackClosed() = this.onHarness { h ->
		val llDash = h.activity.findViewById<LinearLayout>(R.id.llDash)

		h.touch(MotionEvent.ACTION_DOWN, h.emptyX, h.emptyY, 0)
		h.touch(MotionEvent.ACTION_MOVE, h.emptyX, h.emptyY - h.slop * 3F, 200)
		h.touch(MotionEvent.ACTION_MOVE, h.emptyX, h.emptyY - h.slop * 4F, 600)
		h.touch(MotionEvent.ACTION_UP, h.emptyX, h.emptyY - h.slop * 4F, 1000)
		ActivityTestSupport.drainTasks()

		assertFalse(h.dash.isOpen)
		assertFalse(h.viewModel.dashOpen.value)
		assertEquals(View.GONE, llDash.visibility)
		assertEquals(0F, llDash.translationY, 0.001F)
	}

	@Test fun batterySaverOpensInstantlyAtTheTriggerDistance() = this.onHarness { h ->
		val powerManager = h.activity.getSystemService(Context.POWER_SERVICE) as PowerManager
		Shadow.extract<ShadowPowerManager>(powerManager).setIsPowerSaveMode(true)

		h.touch(MotionEvent.ACTION_DOWN, h.emptyX, h.emptyY, 0)
		h.touch(MotionEvent.ACTION_MOVE, h.emptyX, h.emptyY - h.slop * 3F, 50)
		assertFalse(h.dash.isOpen) // Recognised, but not yet past the trigger distance //
		h.touch(MotionEvent.ACTION_MOVE, h.emptyX, h.emptyY - h.slop * 5F, 100)

		assertTrue(h.dash.isOpen)
		assertTrue(h.viewModel.dashOpen.value)
		h.touch(MotionEvent.ACTION_UP, h.emptyX, h.emptyY - h.slop * 5F, 150)
	}

	@Test fun touchesWhileTheDashIsOpenAreIgnored() = this.onHarness { h ->
		h.dash.open()
		ActivityTestSupport.drainTasks()

		assertFalse(h.touch(MotionEvent.ACTION_DOWN, h.emptyX, h.emptyY, 0))
		h.touch(MotionEvent.ACTION_MOVE, h.emptyX, h.emptyY + 100F, 50)
		h.touch(MotionEvent.ACTION_UP, h.emptyX, h.emptyY + 100F, 100)

		assertEquals(0, h.shadeExpansions)
		assertTrue(h.dash.isOpen)
	}

	@Test fun swipingTheDashDownClosesIt() = this.onHarness { h ->
		val llDash = h.activity.findViewById<LinearLayout>(R.id.llDash)
		h.dash.open()
		h.viewModel.openDash()
		ActivityTestSupport.drainTasks()

		assertTrue(h.gestures.dashSwipeEnabled())
		assertTrue(h.gestures.dashSwipeStarted())
		val distance = h.dash.swipeDistancePx
		h.gestures.dashSwipeMoved(distance * 0.6F)
		// Mid-flight: the dash is part-way out, tracking the finger //
		assertTrue(llDash.translationY > 0F && llDash.translationY < distance)
		h.gestures.dashSwipeEnded(distance * 0.6F, 0F)
		ActivityTestSupport.drainTasks()

		assertFalse(h.dash.isOpen)
		assertFalse(h.viewModel.dashOpen.value)
		assertEquals(View.GONE, llDash.visibility)
		assertEquals(0F, llDash.translationY, 0.001F)
	}

	@Test fun aShortDashSwipeDownSettlesBackOpen() = this.onHarness { h ->
		val llDash = h.activity.findViewById<LinearLayout>(R.id.llDash)
		h.dash.open()
		h.viewModel.openDash()
		ActivityTestSupport.drainTasks()

		assertTrue(h.gestures.dashSwipeStarted())
		h.gestures.dashSwipeMoved(h.dash.swipeDistancePx * 0.1F)
		h.gestures.dashSwipeEnded(h.dash.swipeDistancePx * 0.1F, 0F)
		ActivityTestSupport.drainTasks()

		assertTrue(h.dash.isOpen)
		assertTrue(h.viewModel.dashOpen.value)
		assertEquals(View.VISIBLE, llDash.visibility)
		assertEquals(0F, llDash.translationY, 0.001F)
	}

	@Test fun batterySaverClosesInstantlyWhenTheSwipeStarts() = this.onHarness { h ->
		h.dash.open()
		h.viewModel.openDash()

		val powerManager = h.activity.getSystemService(Context.POWER_SERVICE) as PowerManager
		Shadow.extract<ShadowPowerManager>(powerManager).setIsPowerSaveMode(true)

		assertFalse(h.gestures.dashSwipeStarted()) // Nothing to track //
		assertFalse(h.dash.isOpen)
		assertFalse(h.viewModel.dashOpen.value)
	}

	/* End-to-end: the activity's own wiring feeds unclaimed touches into the gestures. */
	@Test fun swipingUpOnTheActivityOpensItsDash() {
		this.scenario.onActivity { activity ->
			val llDash = activity.findViewById<LinearLayout>(R.id.llDash)
			val container = activity.findViewById<View>(R.id.rlContainer)
			val x = container.width - 5F
			val y = container.height - 50F
			val distance = container.height * 0.6F

			listOf(
				MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, y, 0),
				MotionEvent.obtain(0, 100, MotionEvent.ACTION_MOVE, x, y - distance / 2F, 0),
				MotionEvent.obtain(0, 200, MotionEvent.ACTION_MOVE, x, y - distance, 0),
				MotionEvent.obtain(0, 250, MotionEvent.ACTION_UP, x, y - distance, 0),
			).forEach {
				activity.onTouchEvent(it)
				it.recycle()
			}
			ActivityTestSupport.drainTasks()

			assertEquals(View.VISIBLE, llDash.visibility)
		}
	}
}
