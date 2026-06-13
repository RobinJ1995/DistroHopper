package be.robinj.distrohopper.widgets

import android.view.View
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

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

		val persisted = WidgetPersistence(activity.applicationContext).load()
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

	@Test fun preDesktopsSavesLoadOntoTheFirstDesktop() {
		this.onActivity { activity ->
			activity.getSharedPreferences(
				be.robinj.distrohopper.preferences.Preferences.WIDGETS, 0).edit()
				.putString("widgets", """[{"id":1,"col":0,"row":0,"colSpan":2,"rowSpan":2}]""")
				.commit()

			val loaded = WidgetPersistence(activity.applicationContext).load().single()

			assertEquals(0, loaded.page)
		}
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

	private fun motionEvent(action: Int, x: Float, y: Float, timeMs: Long) =
		android.view.MotionEvent.obtain(0, timeMs, action, x, y, 0)
}
