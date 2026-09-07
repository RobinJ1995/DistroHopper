package be.robinj.distrohopper.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class WidgetHostTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
	@After fun tearDown() { scenario.close() }

	@Test fun configureResultRecoversThePendingWidgetFromTheResultIntent() {
		scenario.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			WidgetTestSupport.layoutGrid(grid)
			val host = WidgetTestSupport.host(activity, grid)
			val widgetManager = AppWidgetManager.getInstance(activity)
			Shadows.shadowOf(widgetManager)
				.addBoundWidget(7, WidgetTestSupport.providerInfo())

			// The pending state lives in memory only; after process death the
			// recreated host must recover the widget id from the result intent //
			host.onConfigureResult(Activity.RESULT_OK,
				Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 7))

			assertEquals(1, grid.childCount)
			assertEquals(7, (grid.getChildAt(0) as WidgetContainer).appWidgetId)
		}
	}

	@Test fun restoreShrinksAWidgetLeftBiggerThanTheGridEvenWithNowhereToRepackIt() {
		scenario.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			WidgetTestSupport.layoutGrid(grid)
			val host = WidgetTestSupport.host(activity, grid)
			val widgetManager = AppWidgetManager.getInstance(activity)
			Shadows.shadowOf(widgetManager)
				.addBoundWidget(1, WidgetTestSupport.providerInfo())
			Shadows.shadowOf(widgetManager)
				.addBoundWidget(2, WidgetTestSupport.providerInfo())

			// Widget 1 fills the desktop, so the oversized widget 2 — a size saved
			// against a bigger grid, before the user picked a smaller preset — has
			// nowhere to be re-packed. Its span must still come back inside the
			// grid: the drag snap works out "cols minus span" and underflows //
			DesktopLayoutTestStore.saveWidgets(activity.applicationContext, listOf(
				WidgetLayout(1, 0, 0, WidgetGrid.COLS, WidgetGrid.ROWS, 0),
				WidgetLayout(2, 0, 0, WidgetGrid.COLS + 2, WidgetGrid.ROWS + 2, 0),
			))

			host.restoreWidgets()

			val oversized = (0 until grid.childCount)
				.map { grid.getChildAt(it) as WidgetContainer }
				.single { it.appWidgetId == 2 }
			val lp = oversized.layoutParams as WidgetsContainer.LayoutParams
			assertEquals(WidgetGrid.COLS, lp.colSpan)
			assertEquals(WidgetGrid.ROWS, lp.rowSpan)
			assertEquals(0, lp.col)
			assertEquals(0, lp.row)
		}
	}

	@Test fun configureResultWithoutPendingStateOrResultIntentDoesNothing() {
		scenario.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			WidgetTestSupport.layoutGrid(grid)
			val host = WidgetTestSupport.host(activity, grid)

			host.onConfigureResult(Activity.RESULT_OK, null)

			assertEquals(0, grid.childCount)
		}
	}
}
