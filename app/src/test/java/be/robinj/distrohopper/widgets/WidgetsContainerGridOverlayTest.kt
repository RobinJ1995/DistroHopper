package be.robinj.distrohopper.widgets

import android.graphics.Canvas
import android.graphics.Paint
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.home.LauncherBarBinder
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import be.robinj.distrohopper.widgets.WidgetTestSupport.CELL
import be.robinj.distrohopper.widgets.WidgetTestSupport.CELL_H
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class WidgetsContainerGridOverlayTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
	@After fun tearDown() {
		scenario.onActivity { activity ->
			Preferences.getSharedPreferences(activity)
				.edit()
				.remove(Preference.DEV_SHOW_GRID_ON_DRAG.getName())
				.commit()
		}
		scenario.close()
	}

	/** A Canvas that only records the dots drawn by the grid overlay. */
	private class RecordingCanvas : Canvas() {
		val circles = mutableListOf<Pair<Float, Float>>()

		override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint) {
			this.circles.add(cx to cy)
		}
	}

	private fun enableOption(activity: HomeActivity) {
		Preferences.getSharedPreferences(activity)
			.edit()
			.putBoolean(Preference.DEV_SHOW_GRID_ON_DRAG.getName(), true)
			.commit()
	}

	@Test fun showGridOverlayIsANoOpWhenTheDevOptionIsOff() {
		scenario.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			WidgetTestSupport.layoutGrid(grid)

			grid.showGridOverlay()

			assertFalse(grid.isGridOverlayVisible)
		}
	}

	@Test fun showAndHideToggleTheOverlayWhenTheDevOptionIsOn() {
		scenario.onActivity { activity ->
			enableOption(activity)
			val grid = WidgetTestSupport.standaloneGrid(activity)
			WidgetTestSupport.layoutGrid(grid)

			grid.showGridOverlay()
			assertTrue(grid.isGridOverlayVisible)

			grid.hideGridOverlay()
			assertFalse(grid.isGridOverlayVisible)
		}
	}

	@Test fun visibleOverlayDrawsADotAtEveryGridIntersection() {
		scenario.onActivity { activity ->
			enableOption(activity)
			val grid = WidgetTestSupport.standaloneGrid(activity)
			WidgetTestSupport.layoutGrid(grid)
			grid.showGridOverlay()

			val canvas = RecordingCanvas()
			grid.draw(canvas)

			// One dot at every corner of the COLS x ROWS grid //
			assertEquals(
				(WidgetGrid.COLS + 1) * (WidgetGrid.ROWS + 1),
				canvas.circles.size)
			// The four extreme corners of the test grid //
			assertTrue(canvas.circles.contains(0F to 0F))
			assertTrue(canvas.circles.contains((WidgetGrid.COLS * CELL).toFloat() to 0F))
			assertTrue(canvas.circles.contains(0F to (WidgetGrid.ROWS * CELL_H).toFloat()))
			assertTrue(canvas.circles.contains(
				(WidgetGrid.COLS * CELL).toFloat() to (WidgetGrid.ROWS * CELL_H).toFloat()))
		}
	}

	@Test fun hiddenOverlayDrawsNoDots() {
		scenario.onActivity { activity ->
			enableOption(activity)
			val grid = WidgetTestSupport.standaloneGrid(activity)
			WidgetTestSupport.layoutGrid(grid)

			val canvas = RecordingCanvas()
			grid.draw(canvas)

			assertTrue(canvas.circles.isEmpty())
		}
	}

	@Test fun startingADragShowsTheOverlayAndStoppingHidesItWhenTheDevOptionIsOn() {
		scenario.onActivity { activity ->
			enableOption(activity)
			val page = activity.findViewById<WidgetsPager>(R.id.vgWidgets).pageAt(0)

			LauncherBarBinder.startedDragging(activity)
			assertTrue(page.isGridOverlayVisible)

			LauncherBarBinder.stoppedDragging(activity)
			assertFalse(page.isGridOverlayVisible)
		}
	}

	@Test fun startingADragDoesNotShowTheOverlayWhenTheDevOptionIsOff() {
		scenario.onActivity { activity ->
			val page = activity.findViewById<WidgetsPager>(R.id.vgWidgets).pageAt(0)

			LauncherBarBinder.startedDragging(activity)

			assertFalse(page.isGridOverlayVisible)
		}
	}
}
