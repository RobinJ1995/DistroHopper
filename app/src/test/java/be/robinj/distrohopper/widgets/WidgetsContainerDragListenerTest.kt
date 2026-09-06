package be.robinj.distrohopper.widgets

import android.view.DragEvent
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.DragEvents
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.widgets.WidgetTestSupport.CELL
import be.robinj.distrohopper.widgets.WidgetTestSupport.GRID_SIZE
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
class WidgetsContainerDragListenerTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
	@After fun tearDown() { scenario.close() }

	private class Fixture(
		val receiver: View,
		val grid: WidgetsContainer,
		val container: WidgetContainer,
		val listener: WidgetsContainer_DragListener,
	)

	private fun fixture(activity: HomeActivity): Fixture {
		val root = FrameLayout(activity)
		val grid = WidgetTestSupport.standaloneGrid(activity)
		val receiver = View(activity)
		val host = WidgetTestSupport.host(activity, grid)
		val container = WidgetTestSupport.addWidget(activity, host, grid, 42, 2, 2, 2, 2)

		val gridHeight = WidgetTestSupport.gridHeight
		root.addView(WidgetTestSupport.pagerOf(grid),
			FrameLayout.LayoutParams(GRID_SIZE, gridHeight).apply {
				leftMargin = CELL
				topMargin = 2 * CELL
			})
		root.addView(receiver, FrameLayout.LayoutParams(GRID_SIZE, gridHeight))
		root.measure(
			View.MeasureSpec.makeMeasureSpec(GRID_SIZE, View.MeasureSpec.EXACTLY),
			View.MeasureSpec.makeMeasureSpec(gridHeight, View.MeasureSpec.EXACTLY),
		)
		root.layout(0, 0, GRID_SIZE, gridHeight)

		container.dragGrabOffsetX = CELL / 2
		container.dragGrabOffsetY = CELL / 2

		return Fixture(receiver, grid, container,
			WidgetsContainer_DragListener(activity, WidgetTestSupport.pagerOf(grid)))
	}

	// The root FrameLayout is detached from the activity window so
	// View.getLocationOnScreen() returns [0, 0] for all views in Robolectric.
	// Event coordinates are therefore in the grid's own coordinate space directly.
	private fun receiverPositionForCell(col: Int, row: Int): Pair<Float, Float> =
		(col * CELL + CELL / 2).toFloat() to
			(row * CELL + CELL / 2).toFloat()

	private fun lp(container: WidgetContainer): WidgetsContainer.LayoutParams =
		container.layoutParams as WidgetsContainer.LayoutParams

	@Test fun dragLocationShowsTheSnappedGridTarget() {
		scenario.onActivity { activity ->
			val fixture = fixture(activity)
			val (x, y) = receiverPositionForCell(5, 4)

			assertTrue(fixture.listener.onDrag(fixture.receiver,
				DragEvents.obtain(DragEvent.ACTION_DRAG_LOCATION, x, y, fixture.container)))

			assertTrue(fixture.grid.isMoveTargetVisible)
			assertEquals(5, fixture.grid.moveTargetCol)
			assertEquals(4, fixture.grid.moveTargetRow)
		}
	}

	@Test fun droppingMovesAndPersistsTheWidgetThenHidesTheTarget() {
		scenario.onActivity { activity ->
			val fixture = fixture(activity)
			val (x, y) = receiverPositionForCell(5, 4)
			fixture.listener.onDrag(fixture.receiver,
				DragEvents.obtain(DragEvent.ACTION_DRAG_LOCATION, x, y, fixture.container))

			fixture.listener.onDrag(fixture.receiver,
				DragEvents.obtain(DragEvent.ACTION_DROP, x, y, fixture.container))

			assertEquals(5, lp(fixture.container).col)
			assertEquals(4, lp(fixture.container).row)
			assertFalse(fixture.grid.isMoveTargetVisible)
			val persisted = DesktopLayoutTestStore.widgets(activity.applicationContext).single()
			assertEquals(5, persisted.col)
			assertEquals(4, persisted.row)
		}
	}

	@Test fun draggingAWidgetBiggerThanTheGridSnapsToTheOriginInsteadOfCrashing() {
		scenario.onActivity { activity ->
			val fixture = fixture(activity)
			// A size saved against a bigger grid, before the user picked a smaller
			// preset: the snap bound ("cols minus span") goes negative //
			lp(fixture.container).colSpan = WidgetGrid.COLS + 1
			lp(fixture.container).rowSpan = WidgetGrid.ROWS + 1
			val (x, y) = receiverPositionForCell(5, 4)

			assertTrue(fixture.listener.onDrag(fixture.receiver,
				DragEvents.obtain(DragEvent.ACTION_DRAG_LOCATION, x, y, fixture.container)))

			assertEquals(0, fixture.grid.moveTargetCol)
			assertEquals(0, fixture.grid.moveTargetRow)
		}
	}

	@Test fun leavingTheDesktopHidesTheLandingTarget() {
		scenario.onActivity { activity ->
			val fixture = fixture(activity)
			val (x, y) = receiverPositionForCell(5, 4)
			fixture.listener.onDrag(fixture.receiver,
				DragEvents.obtain(DragEvent.ACTION_DRAG_LOCATION, x, y, fixture.container))

			fixture.listener.onDrag(fixture.receiver,
				DragEvents.obtain(DragEvent.ACTION_DRAG_EXITED, localState = fixture.container))

			assertFalse(fixture.grid.isMoveTargetVisible)
		}
	}
}
