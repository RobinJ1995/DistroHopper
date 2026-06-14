package be.robinj.distrohopper.widgets

import android.appwidget.AppWidgetProviderInfo
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import be.robinj.distrohopper.widgets.WidgetTestSupport.CELL
import be.robinj.distrohopper.widgets.WidgetTestSupport.CELL_H
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
class WidgetContainerTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
	@After fun tearDown() {
		scenario.onActivity { activity ->
			Preferences.getSharedPreferences(activity)
				.edit()
				.remove(Preference.DEV_WIDGET_RESIZE_ANY.getName())
				.remove(Preference.DEV_SHOW_GRID_ON_DRAG.getName())
				.commit()
		}
		scenario.close()
	}

	private class Fixture(
		val grid: WidgetsContainer,
		val host: WidgetHost,
		val container: WidgetContainer,
	)

	/** A 2x2-cell widget at cell (2, 2) on a laid-out 8x8 grid of 100px cells. */
	private fun widgetAt22(
		activity: HomeActivity,
		info: AppWidgetProviderInfo? = WidgetTestSupport.providerInfo(),
	): Fixture {
		val grid = WidgetTestSupport.standaloneGrid(activity)
		val host = WidgetTestSupport.host(activity, grid)
		val container = WidgetTestSupport.addWidget(activity, host, grid, 42, 2, 2, 2, 2, info)
		WidgetTestSupport.layoutGrid(grid)
		return Fixture(grid, host, container)
	}

	private fun lp(container: WidgetContainer): WidgetsContainer.LayoutParams =
		container.layoutParams as WidgetsContainer.LayoutParams

	private fun motionEvent(action: Int, x: Float, y: Float): MotionEvent =
		MotionEvent.obtain(0, SystemClock.uptimeMillis(), action, x, y, 0)

	private fun touch(container: WidgetContainer, edgeId: Int, action: Int, x: Float, y: Float): Boolean =
		container.onTouch(container.findViewById(edgeId), motionEvent(action, x, y))

	@Test fun strayMovesAfterTheSystemDragStartsDoNotRestartTheDrag() {
		scenario.onActivity { activity ->
			val container = widgetAt22(activity).container
			container.editMode = true
			val lalTrash = activity.findViewById<View>(R.id.lalTrash)

			touch(container, R.id.widgetOverlayCenter, MotionEvent.ACTION_DOWN, 300F, 300F)
			touch(container, R.id.widgetOverlayCenter, MotionEvent.ACTION_MOVE, 350F, 300F)
			assertEquals(View.VISIBLE, lalTrash.visibility)

			be.robinj.distrohopper.home.LauncherBarBinder.stoppedDragging(activity)
			assertTrue(touch(container, R.id.widgetOverlayCenter,
				MotionEvent.ACTION_MOVE, 360F, 300F))
			assertEquals(View.GONE, lalTrash.visibility)
		}
	}

	@Test fun editModeShowsTheOverlayAndDimsTheWidget() {
		scenario.onActivity { activity ->
			val container = widgetAt22(activity).container

			container.editMode = true

			assertEquals(View.VISIBLE,
				container.findViewById<View>(R.id.widgetOverlayCenter).visibility)
			assertEquals(0.8F,
				container.findViewById<View>(R.id.widgetContainer).alpha, 0.001F)
			assertEquals(View.VISIBLE, container.findViewById<View>(R.id.llEdgeLeft).visibility)
			assertEquals(View.VISIBLE, container.findViewById<View>(R.id.llEdgeTop).visibility)

			container.editMode = false

			assertEquals(View.GONE,
				container.findViewById<View>(R.id.widgetOverlayCenter).visibility)
			assertEquals(1.0F,
				container.findViewById<View>(R.id.widgetContainer).alpha, 0.001F)
		}
	}

	@Test fun horizontalOnlyResizeHidesTheVerticalEdges() {
		scenario.onActivity { activity ->
			val container = widgetAt22(activity,
				WidgetTestSupport.providerInfo(AppWidgetProviderInfo.RESIZE_HORIZONTAL)).container

			container.editMode = true

			assertEquals(View.VISIBLE, container.findViewById<View>(R.id.llEdgeLeft).visibility)
			assertEquals(View.VISIBLE, container.findViewById<View>(R.id.llEdgeRight).visibility)
			assertEquals(View.GONE, container.findViewById<View>(R.id.llEdgeTop).visibility)
			assertEquals(View.GONE, container.findViewById<View>(R.id.llEdgeBottom).visibility)
		}
	}

	@Test fun developerResizeOverrideShowsAllResizeEdges() {
		scenario.onActivity { activity ->
			Preferences.getSharedPreferences(activity)
				.edit()
				.putBoolean(Preference.DEV_WIDGET_RESIZE_ANY.getName(), true)
				.commit()
			val container = widgetAt22(activity,
				WidgetTestSupport.providerInfo(AppWidgetProviderInfo.RESIZE_HORIZONTAL)).container

			container.editMode = true

			assertEquals(View.VISIBLE, container.findViewById<View>(R.id.llEdgeLeft).visibility)
			assertEquals(View.VISIBLE, container.findViewById<View>(R.id.llEdgeRight).visibility)
			assertEquals(View.VISIBLE, container.findViewById<View>(R.id.llEdgeTop).visibility)
			assertEquals(View.VISIBLE, container.findViewById<View>(R.id.llEdgeBottom).visibility)
		}
	}

	@Test fun developerResizeOverrideIgnoresProviderSizeLimits() {
		scenario.onActivity { activity ->
			Preferences.getSharedPreferences(activity)
				.edit()
				.putBoolean(Preference.DEV_WIDGET_RESIZE_ANY.getName(), true)
				.commit()
			val container = widgetAt22(activity,
				WidgetTestSupport.providerInfo(maxResizeWidth = 2 * CELL)).container

			touch(container, R.id.llEdgeRight, MotionEvent.ACTION_DOWN, 400F, 300F)
			touch(container, R.id.llEdgeRight, MotionEvent.ACTION_MOVE, 400F + CELL, 300F)
			touch(container, R.id.llEdgeRight, MotionEvent.ACTION_UP, 400F + CELL, 300F)

			assertEquals(3, lp(container).colSpan)
		}
	}

	@Test fun onlyOneWidgetCanBeInEditModeAtATime() {
		scenario.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			val host = WidgetTestSupport.host(activity, grid)
			val first = WidgetTestSupport.addWidget(activity, host, grid, 42, 0, 0, 2, 2)
			val second = WidgetTestSupport.addWidget(activity, host, grid, 43, 4, 4, 2, 2)
			WidgetTestSupport.layoutGrid(grid)

			first.editMode = true
			assertTrue(grid.hasEditModeChild())

			second.editMode = true
			assertFalse(first.editMode)
			assertTrue(second.editMode)

			grid.exitEditMode()
			assertFalse(second.editMode)
			assertFalse(grid.hasEditModeChild())
		}
	}

	@Test fun smallMovesBelowTheTouchSlopDoNotStartAResize() {
		scenario.onActivity { activity ->
			val container = widgetAt22(activity).container

			assertTrue(touch(container, R.id.llEdgeRight, MotionEvent.ACTION_DOWN, 400F, 300F))
			assertTrue(touch(container, R.id.llEdgeRight, MotionEvent.ACTION_MOVE, 401F, 300F))

			assertEquals(-1, lp(container).previewWidthPx)
		}
	}

	@Test fun draggingTheRightEdgeGrowsAndCommitsTheColumnSpan() {
		scenario.onActivity { activity ->
			val fixture = widgetAt22(activity)
			val container = fixture.container

			touch(container, R.id.llEdgeRight, MotionEvent.ACTION_DOWN, 400F, 300F)
			touch(container, R.id.llEdgeRight, MotionEvent.ACTION_MOVE, 400F + CELL, 300F)

			assertEquals(2 * CELL + CELL, lp(container).previewWidthPx)

			touch(container, R.id.llEdgeRight, MotionEvent.ACTION_UP, 400F + CELL, 300F)

			assertEquals(3, lp(container).colSpan)
			assertEquals(2, lp(container).rowSpan)
			assertEquals(2, lp(container).col)
			assertEquals(-1, lp(container).previewWidthPx)

			// The new span is persisted
			val persisted = WidgetPersistence(activity.applicationContext).load().single()
			assertEquals(3, persisted.colSpan)
		}
	}

	@Test fun draggingTheLeftEdgeMovesTheLeftSideAndKeepsTheRightFixed() {
		scenario.onActivity { activity ->
			val container = widgetAt22(activity).container

			touch(container, R.id.llEdgeLeft, MotionEvent.ACTION_DOWN, 200F, 300F)
			touch(container, R.id.llEdgeLeft, MotionEvent.ACTION_MOVE, 200F - CELL, 300F)

			assertEquals(CELL, lp(container).previewLeftPx)
			assertEquals(3 * CELL, lp(container).previewWidthPx)

			touch(container, R.id.llEdgeLeft, MotionEvent.ACTION_UP, 200F - CELL, 300F)

			assertEquals(1, lp(container).col)
			assertEquals(3, lp(container).colSpan)
		}
	}

	@Test fun draggingTheBottomEdgeGrowsTheRowSpan() {
		scenario.onActivity { activity ->
			val container = widgetAt22(activity).container
			val bottomY = (4 * CELL_H).toFloat() // widget at row=2 with span=2: bottom = 4*cellHeight

			touch(container, R.id.llEdgeBottom, MotionEvent.ACTION_DOWN, 300F, bottomY)
			touch(container, R.id.llEdgeBottom, MotionEvent.ACTION_MOVE, 300F, bottomY + 2 * CELL_H)
			touch(container, R.id.llEdgeBottom, MotionEvent.ACTION_UP, 300F, bottomY + 2 * CELL_H)

			assertEquals(4, lp(container).rowSpan)
			assertEquals(2, lp(container).row)
		}
	}

	@Test fun draggingTheTopEdgeMovesTheTopAndKeepsTheBottomFixed() {
		scenario.onActivity { activity ->
			val container = widgetAt22(activity).container

			touch(container, R.id.llEdgeTop, MotionEvent.ACTION_DOWN, 300F, 200F)
			touch(container, R.id.llEdgeTop, MotionEvent.ACTION_MOVE, 300F, 200F - CELL)
			touch(container, R.id.llEdgeTop, MotionEvent.ACTION_UP, 300F, 200F - CELL)

			assertEquals(1, lp(container).row)
			assertEquals(3, lp(container).rowSpan)
		}
	}

	@Test fun resizeIsClampedToTheGridBounds() {
		scenario.onActivity { activity ->
			val container = widgetAt22(activity).container

			touch(container, R.id.llEdgeRight, MotionEvent.ACTION_DOWN, (4 * CELL).toFloat(), 300F)
			// Way beyond the right edge of the 800px grid
			touch(container, R.id.llEdgeRight, MotionEvent.ACTION_MOVE, (4 * CELL + 20 * CELL).toFloat(), 300F)

			// Clamped to gridRight - startLeft = GRID_SIZE - 2*cellWidth
			assertEquals(GRID_SIZE - 2 * CELL, lp(container).previewWidthPx)
		}
	}

	@Test fun resizingShowsTheGridOverlayWhenTheDevOptionIsOnAndHidesItOnRelease() {
		scenario.onActivity { activity ->
			Preferences.getSharedPreferences(activity)
				.edit()
				.putBoolean(Preference.DEV_SHOW_GRID_ON_DRAG.getName(), true)
				.commit()
			val fixture = widgetAt22(activity)
			val container = fixture.container

			touch(container, R.id.llEdgeRight, MotionEvent.ACTION_DOWN, 400F, 300F)
			touch(container, R.id.llEdgeRight, MotionEvent.ACTION_MOVE, 400F + CELL, 300F)
			assertTrue(fixture.grid.isGridOverlayVisible)

			touch(container, R.id.llEdgeRight, MotionEvent.ACTION_UP, 400F + CELL, 300F)
			assertFalse(fixture.grid.isGridOverlayVisible)
		}
	}

	@Test fun cancellingAResizeAlsoHidesTheGridOverlay() {
		scenario.onActivity { activity ->
			Preferences.getSharedPreferences(activity)
				.edit()
				.putBoolean(Preference.DEV_SHOW_GRID_ON_DRAG.getName(), true)
				.commit()
			val fixture = widgetAt22(activity)
			val container = fixture.container

			touch(container, R.id.llEdgeRight, MotionEvent.ACTION_DOWN, 400F, 300F)
			touch(container, R.id.llEdgeRight, MotionEvent.ACTION_MOVE, 400F + CELL, 300F)
			assertTrue(fixture.grid.isGridOverlayVisible)

			touch(container, R.id.llEdgeRight, MotionEvent.ACTION_CANCEL, 400F + CELL, 300F)
			assertFalse(fixture.grid.isGridOverlayVisible)
		}
	}

	@Test fun resizingDoesNotShowTheGridOverlayWhenTheDevOptionIsOff() {
		scenario.onActivity { activity ->
			val fixture = widgetAt22(activity)
			val container = fixture.container

			touch(container, R.id.llEdgeRight, MotionEvent.ACTION_DOWN, 400F, 300F)
			touch(container, R.id.llEdgeRight, MotionEvent.ACTION_MOVE, 400F + CELL, 300F)

			assertFalse(fixture.grid.isGridOverlayVisible)
		}
	}

	@Test fun cancelClearsThePreviewWithoutCommitting() {
		scenario.onActivity { activity ->
			val container = widgetAt22(activity).container

			touch(container, R.id.llEdgeRight, MotionEvent.ACTION_DOWN, 400F, 300F)
			touch(container, R.id.llEdgeRight, MotionEvent.ACTION_MOVE, 400F + CELL, 300F)
			touch(container, R.id.llEdgeRight, MotionEvent.ACTION_CANCEL, 400F + CELL, 300F)

			assertEquals(2, lp(container).colSpan)
			assertEquals(-1, lp(container).previewWidthPx)
		}
	}

	@Test fun resizeOntoAnotherWidgetRevertsOnRelease() {
		scenario.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			val host = WidgetTestSupport.host(activity, grid)
			val container = WidgetTestSupport.addWidget(activity, host, grid, 42, 2, 2, 2, 2)
			WidgetTestSupport.addWidget(activity, host, grid, 43, 4, 2, 2, 2)
			WidgetTestSupport.layoutGrid(grid)

			touch(container, R.id.llEdgeRight, MotionEvent.ACTION_DOWN, 400F, 300F)
			touch(container, R.id.llEdgeRight, MotionEvent.ACTION_MOVE, 400F + CELL, 300F)
			touch(container, R.id.llEdgeRight, MotionEvent.ACTION_UP, 400F + CELL, 300F)

			// Would overlap the neighbour at column 4, so nothing changes
			assertEquals(2, lp(container).colSpan)
			assertEquals(2, lp(container).col)
		}
	}

	@Test fun commitMoveMovesToAFreeCellAndPersists() {
		scenario.onActivity { activity ->
			val container = widgetAt22(activity).container

			container.commitMove(5, 6)

			assertEquals(5, lp(container).col)
			assertEquals(6, lp(container).row)
			assertTrue(container.parent.isLayoutRequested)
			val persisted = WidgetPersistence(activity.applicationContext).load().single()
			assertEquals(5, persisted.col)
			assertEquals(6, persisted.row)
		}
	}

	@Test fun commitMoveOntoAnOccupiedCellReverts() {
		scenario.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			val host = WidgetTestSupport.host(activity, grid)
			val container = WidgetTestSupport.addWidget(activity, host, grid, 42, 2, 2, 2, 2)
			WidgetTestSupport.addWidget(activity, host, grid, 43, 4, 4, 2, 2)
			WidgetTestSupport.layoutGrid(grid)

			container.commitMove(4, 4)

			assertEquals(2, lp(container).col)
			assertEquals(2, lp(container).row)
		}
	}

	@Test fun commitMoveOutsideTheGridReverts() {
		scenario.onActivity { activity ->
			val container = widgetAt22(activity).container

			container.commitMove(7, 7) // 2x2 widget cannot fit at (7, 7) on an 8x8 grid

			assertEquals(2, lp(container).col)
			assertEquals(2, lp(container).row)
		}
	}

	@Test fun movingTheOverlayHandsOffToTheDragFramework() {
		scenario.onActivity { activity ->
			val container = widgetAt22(activity).container
			container.editMode = true

			// Widget at col=2,row=2 so left=2*CELL, top=2*CELL_H.
			// Grab 50px+CELL into the widget from the left, 50px down from the top.
			// dragGrabOffset is recorded from the MOVE event (which starts the system drag).
			val moveX = (50 + 3 * CELL).toFloat()  // 2*CELL (widget left) + CELL + 50
			val moveY = (50 + 2 * CELL_H).toFloat() // 2*CELL_H (widget top) + 50
			touch(container, R.id.widgetOverlayCenter, MotionEvent.ACTION_DOWN, (50 + 2 * CELL).toFloat(), moveY)
			touch(container, R.id.widgetOverlayCenter, MotionEvent.ACTION_MOVE, moveX, moveY)

			assertEquals(50 + CELL, container.dragGrabOffsetX)
			assertEquals(50, container.dragGrabOffsetY)
			assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.lalTrash).visibility)
		}
	}
}
