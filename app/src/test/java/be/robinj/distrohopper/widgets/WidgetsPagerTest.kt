package be.robinj.distrohopper.widgets

import android.content.Context
import android.os.PowerManager
import android.view.View
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
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
 * The widget desktop row: there is always exactly one empty desktop after the
 * last occupied one (never more), capped at MAX_PAGES, and widget placements
 * persist per desktop.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class WidgetsPagerTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { this.scenario = ActivityTestSupport.launchHome() }

	@After fun tearDown() { this.scenario.close() }

	private fun onActivity(block: (HomeActivity) -> Unit) {
		this.scenario.onActivity { block(it) }
	}

	private fun layoutPager(pager: WidgetsPager, pages: Int = pager.pageCount) {
		pager.pageAt(pages - 1)
		pager.measure(
			View.MeasureSpec.makeMeasureSpec(WidgetTestSupport.GRID_SIZE, View.MeasureSpec.EXACTLY),
			View.MeasureSpec.makeMeasureSpec(WidgetTestSupport.GRID_SIZE, View.MeasureSpec.EXACTLY))
		pager.layout(0, 0, WidgetTestSupport.GRID_SIZE, WidgetTestSupport.GRID_SIZE)
	}

	@Test fun anEmptyDesktopRowIsASingleDesktop() = this.onActivity { activity ->
		val pager = WidgetTestSupport.pagerOf(WidgetTestSupport.standaloneGrid(activity))

		assertEquals(1, pager.pageCount)
	}

	@Test fun aWidgetOnTheLastDesktopYieldsOneTrailingEmptyDesktop() =
		this.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			val pager = WidgetTestSupport.pagerOf(grid)
			val host = WidgetTestSupport.host(activity, grid)

			WidgetTestSupport.addWidget(activity, host, grid, 42, 0, 0, 2, 2)
			pager.pagesChanged()

			assertEquals(2, pager.pageCount)

			WidgetTestSupport.addWidget(activity, host, pager.pageAt(1), 43, 0, 0, 2, 2)
			pager.pagesChanged()

			assertEquals(3, pager.pageCount)
		}

	@Test fun theOccupiedDesktopSupplierDrivesThePageCount() = this.onActivity { activity ->
		val pager = WidgetTestSupport.pagerOf(WidgetTestSupport.standaloneGrid(activity))
		pager.occupiedDesktopSupplier = { 2 } // e.g. pins reach desktop 2, no widgets //
		pager.pagesChanged()

		// Desktops 0..2 occupied + one trailing empty //
		assertEquals(4, pager.pageCount)
	}

	@Test fun theDesktopCountIsCapped() = this.onActivity { activity ->
		val grid = WidgetTestSupport.standaloneGrid(activity)
		val pager = WidgetTestSupport.pagerOf(grid)
		val host = WidgetTestSupport.host(activity, grid)

		WidgetTestSupport.addWidget(activity, host,
			pager.pageAt(WidgetsPager.MAX_PAGES - 1), 42, 0, 0, 2, 2)
		pager.pagesChanged()

		assertEquals(WidgetsPager.MAX_PAGES, pager.pageCount)
	}

	@Test fun removingTheLastWidgetShrinksTheRowAndSnapsTheCurrentPageBack() =
		this.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			val pager = WidgetTestSupport.pagerOf(grid)
			val host = WidgetTestSupport.host(activity, grid)
			val container = WidgetTestSupport.addWidget(activity, host,
				pager.pageAt(2), 42, 0, 0, 2, 2)
			pager.pagesChanged()
			this.layoutPager(pager)
			pager.setCurrentPage(3, animate = false)
			assertEquals(4, pager.pageCount)

			pager.pageAt(2).removeView(container) // Bypasses the host; pagesChanged() drives it //
			pager.pagesChanged()
			ActivityTestSupport.drainTasks()

			assertEquals(1, pager.pageCount)
			assertEquals(0, pager.currentPage)
		}

	@Test fun widgetPlacementsPersistPerDesktop() = this.onActivity { activity ->
		val grid = WidgetTestSupport.standaloneGrid(activity)
		val pager = WidgetTestSupport.pagerOf(grid)
		val host = WidgetTestSupport.host(activity, grid)
		WidgetTestSupport.addWidget(activity, host, grid, 42, 0, 0, 2, 2)
		WidgetTestSupport.addWidget(activity, host, pager.pageAt(2), 43, 4, 3, 4, 1)

		host.persist()

		val persisted = DesktopLayoutTestStore.widgets(activity.applicationContext)
		assertEquals(listOf(
			WidgetLayout(42, 0, 0, 2, 2, 0),
			WidgetLayout(43, 4, 3, 4, 1, 2),
		), persisted)
	}

	@Test fun panningFollowsTheFingerAndSettlesOnTheNearestDesktop() =
		this.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			val pager = WidgetTestSupport.pagerOf(grid)
			val host = WidgetTestSupport.host(activity, grid)
			WidgetTestSupport.addWidget(activity, host, grid, 42, 0, 0, 2, 2)
			pager.pagesChanged()
			this.layoutPager(pager)

			pager.panBegin()
			pager.panBy(WidgetTestSupport.GRID_SIZE * 0.6F)
			assertEquals((WidgetTestSupport.GRID_SIZE * 0.6F).toInt(), pager.scrollX)

			pager.panSettle(0F)
			ActivityTestSupport.drainTasks()

			assertEquals(1, pager.currentPage)
			assertEquals(WidgetTestSupport.GRID_SIZE, pager.scrollX)
		}

	@Test fun aFlingMovesOneDesktopOverRegardlessOfDistance() =
		this.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			val pager = WidgetTestSupport.pagerOf(grid)
			val host = WidgetTestSupport.host(activity, grid)
			WidgetTestSupport.addWidget(activity, host, grid, 42, 0, 0, 2, 2)
			pager.pagesChanged()
			this.layoutPager(pager)

			pager.panBegin()
			pager.panBy(WidgetTestSupport.GRID_SIZE * 0.1F)
			pager.panSettle(-10_000F) // Hard leftward fling //
			ActivityTestSupport.drainTasks()

			assertEquals(1, pager.currentPage)
		}

	@Test fun panningIsClampedToTheDesktopRow() = this.onActivity { activity ->
		val pager = WidgetTestSupport.pagerOf(WidgetTestSupport.standaloneGrid(activity))
		this.layoutPager(pager)

		// A single (empty) desktop: there is nowhere to pan to //
		pager.panBegin()
		pager.panBy(WidgetTestSupport.GRID_SIZE * 0.6F)

		assertEquals(0, pager.scrollX)
	}

	@Test fun interceptsHorizontalSwipesOverWidgets() = this.onActivity { activity ->
		val grid = WidgetTestSupport.standaloneGrid(activity)
		val pager = WidgetTestSupport.pagerOf(grid)
		val host = WidgetTestSupport.host(activity, grid)
		WidgetTestSupport.addWidget(activity, host, grid, 42, 0, 0, 2, 2)
		pager.pagesChanged()
		this.layoutPager(pager)

		val down = motionEvent(android.view.MotionEvent.ACTION_DOWN, 400F, 400F, 0)
		val move = motionEvent(android.view.MotionEvent.ACTION_MOVE, 300F, 405F, 50)
		try {
			assertEquals(false, pager.onInterceptTouchEvent(down))
			assertTrue(pager.onInterceptTouchEvent(move))
		} finally {
			down.recycle()
			move.recycle()
		}
	}

	@Test fun interceptsVerticalSwipesOverWidgetsAndForwardsThem() = this.onActivity { activity ->
		val grid = WidgetTestSupport.standaloneGrid(activity)
		val pager = WidgetTestSupport.pagerOf(grid)
		val host = WidgetTestSupport.host(activity, grid)
		WidgetTestSupport.addWidget(activity, host, grid, 42, 0, 0, 2, 2)
		pager.pagesChanged()
		this.layoutPager(pager)

		var forwarded = false
		pager.swipeGestureForwarder = { forwarded = true; false }

		val down = motionEvent(android.view.MotionEvent.ACTION_DOWN, 400F, 400F, 0)
		// Swipe straight down (the direction the widget used to swallow) //
		val move = motionEvent(android.view.MotionEvent.ACTION_MOVE, 405F, 500F, 50)
		try {
			assertEquals(false, pager.onInterceptTouchEvent(down))
			assertTrue(pager.onInterceptTouchEvent(move))
			assertTrue(forwarded) // The home gestures were primed with the DOWN //
		} finally {
			down.recycle()
			move.recycle()
		}
	}

	@Test fun doesNotInterceptWhileAWidgetIsInEditMode() = this.onActivity { activity ->
		val grid = WidgetTestSupport.standaloneGrid(activity)
		val pager = WidgetTestSupport.pagerOf(grid)
		val host = WidgetTestSupport.host(activity, grid)
		val container = WidgetTestSupport.addWidget(activity, host, grid, 42, 0, 0, 2, 2)
		pager.pagesChanged()
		this.layoutPager(pager)
		container.editMode = true

		val down = motionEvent(android.view.MotionEvent.ACTION_DOWN, 400F, 400F, 0)
		val move = motionEvent(android.view.MotionEvent.ACTION_MOVE, 300F, 405F, 50)
		try {
			pager.onInterceptTouchEvent(down)
			assertEquals(false, pager.onInterceptTouchEvent(move))
		} finally {
			down.recycle()
			move.recycle()
		}
	}

	@Test fun thePageIndicatorFlashesWhileSwipingAndFadesAfterSettling() =
		this.onActivity { activity ->
			// The real (attached) pager, so its delayed hide reaches the looper //
			val pager = activity.findViewById<WidgetsPager>(R.id.vgWidgets)
			val host = WidgetTestSupport.host(activity, pager.pageAt(0))
			WidgetTestSupport.addWidget(activity, host, pager.pageAt(0), 42, 0, 0, 2, 2)
			pager.pagesChanged()
			// Battery saver makes the fade-out snap, so the end state is deterministic //
			val powerManager = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
			Shadow.extract<ShadowPowerManager>(powerManager).setIsPowerSaveMode(true)

			pager.panBegin()
			assertTrue(pager.isPageIndicatorShowing) // Up for the duration of the drag //

			pager.panBy(100F)
			pager.panSettle(0F)
			ActivityTestSupport.drainTasks() // Runs the delayed hide //

			assertFalse(pager.isPageIndicatorShowing)
		}

	@Test fun thePageIndicatorStaysHiddenWithASingleDesktop() = this.onActivity { activity ->
		val pager = WidgetTestSupport.pagerOf(WidgetTestSupport.standaloneGrid(activity))
		this.layoutPager(pager)

		pager.panBegin()

		assertFalse(pager.isPageIndicatorShowing)
	}

	@Test fun thePageIndicatorRowStaysFixedInTheViewportAsPagesScroll() =
		this.onActivity { activity ->
			val pager = WidgetTestSupport.pagerOf(WidgetTestSupport.standaloneGrid(activity))
			this.layoutPager(pager)

			val onFirstDesktop = pager.indicatorContentCentreX()
			pager.scrollTo(WidgetTestSupport.GRID_SIZE, 0) // Scroll one full page over //
			val onSecondDesktop = pager.indicatorContentCentreX()

			// It carries scrollX, so the row lands at the same on-screen spot //
			assertEquals(WidgetTestSupport.GRID_SIZE.toFloat(),
				onSecondDesktop - onFirstDesktop, 0.5F)
		}

	@Test fun aBackwardFlingReturnsOneDesktop() = this.onActivity { activity ->
		val grid = WidgetTestSupport.standaloneGrid(activity)
		val pager = WidgetTestSupport.pagerOf(grid)
		val host = WidgetTestSupport.host(activity, grid)
		WidgetTestSupport.addWidget(activity, host, grid, 42, 0, 0, 2, 2)
		pager.pagesChanged()
		this.layoutPager(pager)
		pager.setCurrentPage(1, animate = false) // Start on the second desktop //

		pager.panBegin()
		pager.panBy(-WidgetTestSupport.GRID_SIZE * 0.1F) // Nudge back toward the first //
		pager.panSettle(10_000F) // Hard rightward (positive) fling = backward //
		ActivityTestSupport.drainTasks()

		assertEquals(0, pager.currentPage)
	}

	private fun motionEvent(action: Int, x: Float, y: Float, timeMs: Long) =
		android.view.MotionEvent.obtain(0, timeMs, action, x, y, 0)
}
