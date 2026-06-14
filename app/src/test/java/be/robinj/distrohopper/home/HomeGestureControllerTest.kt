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
import be.robinj.distrohopper.widgets.WidgetTestSupport
import be.robinj.distrohopper.widgets.WidgetsPager
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

		init {
			val container = DependencyContainer.of(this.activity)
			this.dash = DashController(this.activity, this.activity.viewFinder,
				container.themeManager.current, container.prefs)
			this.viewModel = HomeViewModel(container)
			this.gestures = HomeGestureController(this.activity, this.activity.viewFinder,
				this.dash, this.viewModel, { false })
		}

		val slop = ViewConfiguration.get(this.activity).scaledTouchSlop.toFloat()

		/* A point on empty desktop space: clear of the launcher (left) and panel (top). */
		val emptyX = this.activity.findViewById<View>(R.id.rlContainer).width - 5F
		val emptyY = this.activity.findViewById<View>(R.id.rlContainer).height - 50F

		fun touch(action: Int, x: Float, y: Float, timeMs: Long): Boolean =
			this.touch(this.gestures, action, x, y, timeMs)

		fun touch(g: HomeGestureController, action: Int, x: Float, y: Float, timeMs: Long): Boolean {
			val event = MotionEvent.obtain(0, timeMs, action, x, y, 0)
			try {
				return g.onHomeTouchEvent(event)
			} finally {
				event.recycle()
			}
		}

		/* A controller wired up for the experimental notification-tray gesture. */
		fun notificationGestures(
			enabled: Boolean = true,
			serviceConnected: Boolean = true,
			onOpen: () -> Unit = {},
			onPrompt: () -> Unit = {},
		): HomeGestureController =
			HomeGestureController(this.activity, this.activity.viewFinder, this.dash,
				this.viewModel, { false }, { enabled }, { serviceConnected }, onOpen, onPrompt)
	}

	private fun onHarness(block: (Harness) -> Unit) {
		this.scenario.onActivity { block(Harness(it)) }
	}

	@Test fun swipingDownOnEmptySpaceDoesNothing() = this.onHarness { h ->
		// The notification-tray gesture is off by default; a downward swipe is inert //
		h.touch(MotionEvent.ACTION_DOWN, h.emptyX, h.emptyY - 200F, 0)
		val consumedMove = h.touch(MotionEvent.ACTION_MOVE, h.emptyX, h.emptyY - 100F, 50)
		val consumedUp = h.touch(MotionEvent.ACTION_UP, h.emptyX, h.emptyY - 100F, 100)

		assertFalse(consumedMove)
		assertFalse(consumedUp)
		assertFalse(h.dash.isOpen)
	}

	@Test fun swipingDownWithTheGestureEnabledOpensNotifications() = this.onHarness { h ->
		var opened = 0
		var prompted = 0
		val g = h.notificationGestures(onOpen = { opened++ }, onPrompt = { prompted++ })
		val distance = h.dash.swipeDistancePx

		h.touch(g, MotionEvent.ACTION_DOWN, h.emptyX, h.emptyY, 0)
		// Past the slop: the downward swipe is recognised //
		h.touch(g, MotionEvent.ACTION_MOVE, h.emptyX, h.emptyY + h.slop * 3F, 50)
		// Well past the commit threshold (a slow drag, so distance — not a fling — decides) //
		h.touch(g, MotionEvent.ACTION_MOVE, h.emptyX, h.emptyY + h.slop * 3F + distance * 0.6F, 400)
		val consumed = h.touch(g, MotionEvent.ACTION_UP,
			h.emptyX, h.emptyY + h.slop * 3F + distance * 0.6F, 800)

		assertTrue(consumed)
		assertEquals(1, opened)
		assertEquals(0, prompted)
		assertFalse(h.dash.isOpen) // The dash is untouched by the downward gesture //
	}

	@Test fun aFastFlickDownOpensNotificationsOnVelocityAlone() = this.onHarness { h ->
		var opened = 0
		val g = h.notificationGestures(onOpen = { opened++ })
		val distance = h.dash.swipeDistancePx

		h.touch(g, MotionEvent.ACTION_DOWN, h.emptyX, h.emptyY, 0)
		h.touch(g, MotionEvent.ACTION_MOVE, h.emptyX, h.emptyY + h.slop * 3F, 10)
		// A short but quick flick: under the distance threshold, the fling decides //
		h.touch(g, MotionEvent.ACTION_MOVE, h.emptyX, h.emptyY + h.slop * 3F + distance * 0.2F, 30)
		h.touch(g, MotionEvent.ACTION_UP,
			h.emptyX, h.emptyY + h.slop * 3F + distance * 0.2F, 40)

		assertEquals(1, opened)
	}

	@Test fun aShortSlowSwipeDownDoesNotOpenNotifications() = this.onHarness { h ->
		var opened = 0
		val g = h.notificationGestures(onOpen = { opened++ })

		h.touch(g, MotionEvent.ACTION_DOWN, h.emptyX, h.emptyY, 0)
		h.touch(g, MotionEvent.ACTION_MOVE, h.emptyX, h.emptyY + h.slop * 3F, 200)
		h.touch(g, MotionEvent.ACTION_MOVE, h.emptyX, h.emptyY + h.slop * 4F, 600)
		h.touch(g, MotionEvent.ACTION_UP, h.emptyX, h.emptyY + h.slop * 4F, 1000)

		assertEquals(0, opened)
	}

	@Test fun swipingDownWithoutTheServiceConnectedPromptsToEnableIt() = this.onHarness { h ->
		var opened = 0
		var prompted = 0
		val g = h.notificationGestures(
			serviceConnected = false, onOpen = { opened++ }, onPrompt = { prompted++ })
		val distance = h.dash.swipeDistancePx

		h.touch(g, MotionEvent.ACTION_DOWN, h.emptyX, h.emptyY, 0)
		h.touch(g, MotionEvent.ACTION_MOVE, h.emptyX, h.emptyY + h.slop * 3F, 50)
		h.touch(g, MotionEvent.ACTION_MOVE, h.emptyX, h.emptyY + h.slop * 3F + distance * 0.6F, 400)
		h.touch(g, MotionEvent.ACTION_UP,
			h.emptyX, h.emptyY + h.slop * 3F + distance * 0.6F, 800)

		assertEquals(0, opened)
		assertEquals(1, prompted) // Nudged towards the accessibility settings instead //
	}

	@Test fun aDisabledNotificationGestureLeavesTheDownSwipeInert() = this.onHarness { h ->
		var opened = 0
		var prompted = 0
		val g = h.notificationGestures(
			enabled = false, onOpen = { opened++ }, onPrompt = { prompted++ })
		val distance = h.dash.swipeDistancePx

		h.touch(g, MotionEvent.ACTION_DOWN, h.emptyX, h.emptyY, 0)
		h.touch(g, MotionEvent.ACTION_MOVE, h.emptyX, h.emptyY + h.slop * 3F + distance * 0.6F, 50)
		val consumedUp = h.touch(g, MotionEvent.ACTION_UP,
			h.emptyX, h.emptyY + h.slop * 3F + distance * 0.6F, 100)

		assertEquals(0, opened)
		assertEquals(0, prompted)
		assertFalse(consumedUp)
	}

	@Test fun enablingTheNotificationGestureLeavesSwipeUpForTheDashWorking() = this.onHarness { h ->
		val g = h.notificationGestures()
		val distance = h.dash.swipeDistancePx

		h.touch(g, MotionEvent.ACTION_DOWN, h.emptyX, h.emptyY, 0)
		h.touch(g, MotionEvent.ACTION_MOVE, h.emptyX, h.emptyY - h.slop * 3F, 50)
		h.touch(g, MotionEvent.ACTION_MOVE, h.emptyX, h.emptyY - h.slop * 3F - distance * 0.6F, 250)
		h.touch(g, MotionEvent.ACTION_UP, h.emptyX, h.emptyY - h.slop * 3F - distance * 0.6F, 300)
		ActivityTestSupport.drainTasks()

		assertTrue(h.dash.isOpen)
	}

	@Test fun swipingUpOnThePanelIsIgnored() = this.onHarness { h ->
		val llPanel = h.activity.findViewById<View>(R.id.llPanel)
		val location = IntArray(2)
		llPanel.getLocationInWindow(location)
		val x = location[0] + llPanel.width / 2F
		val y = location[1] + llPanel.height / 2F

		assertFalse(h.touch(MotionEvent.ACTION_DOWN, x, y, 0))
		h.touch(MotionEvent.ACTION_MOVE, x, y - 100F, 50)
		h.touch(MotionEvent.ACTION_UP, x, y - 100F, 100)

		assertFalse(h.dash.isOpen)
	}

	@Test fun horizontalSwipesDoNotOpenTheDash() = this.onHarness { h ->
		h.touch(MotionEvent.ACTION_DOWN, h.emptyX, h.emptyY, 0)
		h.touch(MotionEvent.ACTION_MOVE, h.emptyX - 150F, h.emptyY - 10F, 50)
		h.touch(MotionEvent.ACTION_UP, h.emptyX - 150F, h.emptyY - 10F, 100)

		assertFalse(h.dash.isOpen)
	}

	@Test fun aHorizontalSwipeOnASingleEmptyDesktopGoesNowhere() = this.onHarness { h ->
		val pager = h.activity.findViewById<WidgetsPager>(R.id.vgWidgets)

		h.touch(MotionEvent.ACTION_DOWN, h.emptyX, h.emptyY, 0)
		h.touch(MotionEvent.ACTION_MOVE, h.emptyX - 150F, h.emptyY - 10F, 200)
		h.touch(MotionEvent.ACTION_UP, h.emptyX - 150F, h.emptyY - 10F, 600)
		ActivityTestSupport.drainTasks()

		assertEquals(0, pager.currentPage)
		assertEquals(0, pager.scrollX)
	}

	@Test fun aHorizontalSwipePansToTheTrailingEmptyDesktop() = this.onHarness { h ->
		val pager = h.activity.findViewById<WidgetsPager>(R.id.vgWidgets)
		val host = WidgetTestSupport.host(h.activity)
		WidgetTestSupport.addWidget(h.activity, host, pager.pageAt(0), 42, 0, 0, 2, 2)
		pager.pagesChanged()
		ActivityTestSupport.drainTasks()
		val width = pager.width.toFloat()

		h.touch(MotionEvent.ACTION_DOWN, h.emptyX, h.emptyY, 0)
		h.touch(MotionEvent.ACTION_MOVE, h.emptyX - h.slop * 3F, h.emptyY, 200)
		// Mid-pan: the desktops track the finger //
		h.touch(MotionEvent.ACTION_MOVE, h.emptyX - h.slop * 3F - width * 0.6F, h.emptyY, 400)
		assertTrue(pager.scrollX > 0 && pager.scrollX < pager.width)

		h.touch(MotionEvent.ACTION_UP, h.emptyX - h.slop * 3F - width * 0.6F, h.emptyY, 800)
		ActivityTestSupport.drainTasks()

		assertEquals(1, pager.currentPage)
		assertEquals(pager.width, pager.scrollX)
		// ... but no further: only one empty desktop may trail the occupied one //
		assertEquals(2, pager.pageCount)
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
		// Mid-flight: the dash is part-way in, tracking the finger. The exact
		// transform is the theme's business; openness is the theme-agnostic measure //
		assertTrue(h.dash.swipeOpenness > 0F && h.dash.swipeOpenness < 1F)

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
		assertTrue(h.dash.swipeOpenness > 0F && h.dash.swipeOpenness < 1F)
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

	/*
	 * End-to-end through real touch dispatch: the widget pager is clickable
	 * (tap = exit edit mode), so touches on empty desktop space are consumed
	 * by it rather than bubbling up to Activity#onTouchEvent — its
	 * OnTouchListener must feed them into the gestures.
	 */
	@Test fun swipingUpThroughRealTouchDispatchOpensTheDash() {
		this.scenario.onActivity { activity ->
			val llDash = activity.findViewById<LinearLayout>(R.id.llDash)
			val decor = activity.window.decorView
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
				decor.dispatchTouchEvent(it)
				it.recycle()
			}
			ActivityTestSupport.drainTasks()

			assertEquals(View.VISIBLE, llDash.visibility)
		}
	}

	/* Pressing home delivers the HOME intent to onNewIntent, which calls this. */
	@Test fun returningToTheFirstDesktopAnimatesBackToPageOne() {
		this.scenario.onActivity { activity ->
			val pager = activity.findViewById<WidgetsPager>(R.id.vgWidgets)
			val host = WidgetTestSupport.host(activity)
			WidgetTestSupport.addWidget(activity, host, pager.pageAt(0), 42, 0, 0, 2, 2)
			pager.pagesChanged()
			ActivityTestSupport.drainTasks()
			pager.setCurrentPage(1, animate = false)

			activity.returnToFirstDesktop()
			ActivityTestSupport.drainTasks()

			assertEquals(0, pager.currentPage)
			assertEquals(0, pager.scrollX)
		}
	}
}
