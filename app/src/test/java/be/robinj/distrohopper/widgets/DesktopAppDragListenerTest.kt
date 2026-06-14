package be.robinj.distrohopper.widgets

import android.content.ClipData
import android.view.DragEvent
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.DragEvents
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.desktop.launcher.LauncherDragListener
import be.robinj.distrohopper.desktop.launcher.TrashDragListener
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
import org.robolectric.util.ReflectionHelpers

/**
 * The desktop grid's drop routing for apps: launcher icons and dash apps pinned
 * onto a desktop (and the launcher icon *moved* off its bar), desktop apps
 * repositioned, moved back to the bar, and trashed — plus widget/app collision.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class DesktopAppDragListenerTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { this.scenario = ActivityTestSupport.launchHome() }
	@After fun tearDown() { this.scenario.close() }

	private class Fixture(
		val receiver: View,
		val grid: WidgetsContainer,
		val pager: WidgetsPager,
		val host: DesktopAppHost,
		val listener: WidgetsContainer_DragListener,
	)

	/**
	 * A detached pager (so getLocationOnScreen is [0,0] and event coords are
	 * grid-space) whose [DesktopAppHost] is injected as the activity's, so the
	 * listener — which resolves the host from the activity — drives it.
	 */
	private fun fixture(activity: HomeActivity): Fixture {
		val root = FrameLayout(activity)
		val grid = WidgetTestSupport.standaloneGrid(activity)
		val pager = WidgetTestSupport.pagerOf(grid)
		val receiver = View(activity)

		root.addView(pager, FrameLayout.LayoutParams(GRID_SIZE, GRID_SIZE))
		root.addView(receiver, FrameLayout.LayoutParams(GRID_SIZE, GRID_SIZE))
		root.measure(
			View.MeasureSpec.makeMeasureSpec(GRID_SIZE, View.MeasureSpec.EXACTLY),
			View.MeasureSpec.makeMeasureSpec(GRID_SIZE, View.MeasureSpec.EXACTLY))
		root.layout(0, 0, GRID_SIZE, GRID_SIZE)

		val host = DesktopAppHost(activity, pager, activity.appManager.repository)
		ReflectionHelpers.setField(activity, "desktopAppHost", host)

		return Fixture(receiver, grid, pager, host,
			WidgetsContainer_DragListener(activity, pager))
	}

	private fun cell(col: Int, row: Int): Pair<Float, Float> =
		(col * CELL + CELL / 2).toFloat() to (row * CELL + CELL / 2).toFloat()

	private fun lp(view: View): WidgetsContainer.LayoutParams =
		view.layoutParams as WidgetsContainer.LayoutParams

	/** The clip a launcher-bar icon drag carries: its pinned index as the label. */
	private fun launcherPinClip(index: Int): ClipData =
		ClipData.newPlainText(index.toString(), "app")

	private fun drag(f: Fixture, action: Int, col: Int, row: Int,
			localState: Any? = null, clip: ClipData? = null) {
		val (x, y) = cell(col, row)
		f.listener.onDrag(f.receiver, DragEvents.obtain(action, x, y, localState,
			clipDescription = clip?.description, clipData = clip))
	}

	@Test fun dashAppDroppedOnTheDesktopIsPinnedThere() {
		this.scenario.onActivity { activity ->
			val f = this.fixture(activity)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")

			this.drag(f, DragEvent.ACTION_DRAG_LOCATION, 3, 2, localState = alpha)
			this.drag(f, DragEvent.ACTION_DROP, 3, 2, localState = alpha)

			val view = WidgetTestSupport.desktopAppsOn(f.grid).single()
			assertEquals(alpha.profileScopedKey, view.key)
			assertEquals(3, lp(view).col)
			assertEquals(2, lp(view).row)
		}
	}

	@Test fun launcherIconDroppedOnTheDesktopMovesOffTheBar() {
		this.scenario.onActivity { activity ->
			val f = this.fixture(activity)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")
			activity.appManager.pin(alpha, false, false, true)
			assertTrue(activity.appManager.isPinned(alpha))

			this.drag(f, DragEvent.ACTION_DROP, 4, 1,
				localState = null, clip = launcherPinClip(0))

			// Moved: now a desktop pin, gone from the bar //
			assertTrue(f.host.isPinnedOnDesktop(alpha))
			assertFalse(activity.appManager.isPinned(alpha))
			assertEquals(4, lp(WidgetTestSupport.desktopAppsOn(f.grid).single()).col)
		}
	}

	@Test fun desktopAppDroppedElsewhereOnItsGridIsRepositioned() {
		this.scenario.onActivity { activity ->
			val f = this.fixture(activity)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")
			f.host.pinAt(alpha, 0, 0, 0)
			val view = WidgetTestSupport.desktopAppsOn(f.grid).single()
			view.dragGrabOffsetX = CELL / 2
			view.dragGrabOffsetY = CELL / 2

			this.drag(f, DragEvent.ACTION_DROP, 6, 5, localState = view)

			assertEquals(6, lp(view).col)
			assertEquals(5, lp(view).row)
			assertEquals(1, WidgetTestSupport.desktopAppsOn(f.grid).size)
		}
	}

	@Test fun droppingAnAppOnAnOccupiedCellShowsAnInvalidTarget() {
		this.scenario.onActivity { activity ->
			val f = this.fixture(activity)
			val widgetHost = WidgetTestSupport.host(activity, f.grid)
			WidgetTestSupport.addWidget(activity, widgetHost, f.grid, 42, 3, 3, 2, 2)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")

			// Hover a dash app over the widget's cell //
			this.drag(f, DragEvent.ACTION_DRAG_LOCATION, 3, 3, localState = alpha)

			assertTrue(f.grid.isMoveTargetVisible)
			// Dropping there is rejected: no desktop app is created //
			this.drag(f, DragEvent.ACTION_DROP, 3, 3, localState = alpha)
			// pinAt falls back to a free cell rather than overlapping the widget //
			val view = WidgetTestSupport.desktopAppsOn(f.grid).single()
			assertFalse(lp(view).col in 3..4 && lp(view).row in 3..4)
		}
	}

	@Test fun draggingAnAppAlreadyPinnedElsewhereRelocatesIt() {
		this.scenario.onActivity { activity ->
			val f = this.fixture(activity)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")
			f.host.pinAt(alpha, 0, 0, 0)

			// Drop the same app (as a dash drag) onto another cell //
			this.drag(f, DragEvent.ACTION_DROP, 5, 5, localState = alpha)

			val view = WidgetTestSupport.desktopAppsOn(f.grid).single()
			assertEquals(5, lp(view).col)
			assertEquals(5, lp(view).row)
			assertEquals(1, WidgetTestSupport.desktopAppsOn(f.grid).size)
		}
	}

	@Test fun moveTargetValidatesAgainstBothWidgetsAndDesktopApps() {
		this.scenario.onActivity { activity ->
			val f = this.fixture(activity)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")
			f.host.pinAt(alpha, 2, 2, 0)
			val beta = WidgetTestSupport.app(activity, "com.example.beta")

			// Hovering a second app over the first app's cell is invalid //
			this.drag(f, DragEvent.ACTION_DRAG_LOCATION, 2, 2, localState = beta)

			assertTrue(f.grid.isMoveTargetVisible)
			assertEquals(2, f.grid.moveTargetCol)
			assertEquals(2, f.grid.moveTargetRow)
		}
	}

	@Test fun desktopAppDroppedOnTheBarMovesToTheLauncher() {
		this.scenario.onActivity { activity ->
			val f = this.fixture(activity)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")
			f.host.pinAt(alpha, 0, 0, 0)
			val view = WidgetTestSupport.desktopAppsOn(f.grid).single()
			val barListener = LauncherDragListener(activity.appManager)

			barListener.onDrag(activity.findViewById(R.id.llLauncher),
				DragEvents.obtain(DragEvent.ACTION_DROP, localState = view))

			// Moved: pinned to the bar, gone from the desktop //
			assertTrue(activity.appManager.isPinned(alpha))
			assertFalse(f.host.isPinnedOnDesktop(alpha))
		}
	}

	@Test fun aWidgetCannotBeMovedOntoADesktopAppCell() {
		this.scenario.onActivity { activity ->
			val f = this.fixture(activity)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")
			f.host.pinAt(alpha, 4, 4, 0)
			val widgetHost = WidgetTestSupport.host(activity, f.grid)
			val widget = WidgetTestSupport.addWidget(activity, widgetHost, f.grid, 42, 0, 0, 1, 1)
			widget.dragGrabOffsetX = CELL / 2
			widget.dragGrabOffsetY = CELL / 2

			// Try to drop the widget onto the desktop app's cell //
			this.drag(f, DragEvent.ACTION_DROP, 4, 4, localState = widget)

			// Rejected: the widget stays put, the app keeps its cell //
			assertEquals(0, lp(widget).col)
			assertEquals(0, lp(widget).row)
			assertEquals(4, lp(WidgetTestSupport.desktopAppsOn(f.grid).single()).col)
		}
	}

	@Test fun desktopAppDroppedOnTheTrashIsRemoved() {
		this.scenario.onActivity { activity ->
			val f = this.fixture(activity)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")
			f.host.pinAt(alpha, 0, 0, 0)
			val view = WidgetTestSupport.desktopAppsOn(f.grid).single()
			val trash = TrashDragListener(activity)

			trash.onDrag(activity.findViewById(R.id.lalTrash),
				DragEvents.obtain(DragEvent.ACTION_DROP, localState = view))

			assertFalse(f.host.isPinnedOnDesktop(alpha))
			assertEquals(0, WidgetTestSupport.desktopAppsOn(f.grid).size)
		}
	}
}
