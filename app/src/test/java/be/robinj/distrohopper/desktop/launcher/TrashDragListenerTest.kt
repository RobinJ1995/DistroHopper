package be.robinj.distrohopper.desktop.launcher

import android.content.ClipData
import android.graphics.Color
import android.view.DragEvent
import android.view.View
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.DragEvents
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.widgets.DesktopLayoutTestStore
import be.robinj.distrohopper.widgets.WidgetTestSupport
import be.robinj.distrohopper.widgets.WidgetsPager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class TrashDragListenerTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() {
		ShadowAlertDialog.reset()
		scenario = ActivityTestSupport.launchHome()
	}
	@After fun tearDown() { scenario.close() }

	private fun appClip(pinnedIndex: Int): ClipData =
		ClipData.newPlainText(pinnedIndex.toString(), "app")

	@Test fun enteringTheTrashTurnsItRedAndEntersDragMode() {
		scenario.onActivity { activity ->
			val lalTrash = activity.findViewById<AppLauncher>(R.id.lalTrash)
			val listener = TrashDragListener(activity)

			assertTrue(listener.onDrag(lalTrash,
				DragEvents.obtain(DragEvent.ACTION_DRAG_ENTERED)))

			assertEquals(Color.rgb(255, 40, 40), lalTrash.colour)
			assertEquals(View.VISIBLE, lalTrash.visibility)
		}
	}

	@Test fun exitingTheTrashRestoresItsColour() {
		scenario.onActivity { activity ->
			val lalTrash = activity.findViewById<AppLauncher>(R.id.lalTrash)
			val originalColour = lalTrash.colour
			val listener = TrashDragListener(activity)

			listener.onDrag(lalTrash, DragEvents.obtain(DragEvent.ACTION_DRAG_ENTERED))
			listener.onDrag(lalTrash, DragEvents.obtain(DragEvent.ACTION_DRAG_EXITED))

			assertEquals(originalColour, lalTrash.colour)
		}
	}

	@Test fun droppingAPinnedAppUnpinsItAndLeavesDragMode() {
		scenario.onActivity { activity ->
			val manager = activity.appManager
			val app = manager.findAppsByPackageName("com.example.alpha").first()
			manager.pin(app, false, true)
			val lalTrash = activity.findViewById<AppLauncher>(R.id.lalTrash)
			val originalColour = lalTrash.colour
			val listener = TrashDragListener(activity)

			listener.onDrag(lalTrash, DragEvents.obtain(DragEvent.ACTION_DRAG_ENTERED))
			listener.onDrag(lalTrash, DragEvents.obtain(DragEvent.ACTION_DROP,
				clipDescription = appClip(0).description, clipData = appClip(0)))

			assertEquals(0, manager.pinned.size)
			assertEquals(originalColour, lalTrash.colour)
			assertEquals(View.GONE, lalTrash.visibility)
		}
	}

	@Test fun droppingAWidgetRemovesItFromTheGridAndFromPersistence() {
		scenario.onActivity { activity ->
			val grid = activity.findViewById<WidgetsPager>(R.id.vgWidgets).pageAt(0)
			val host = WidgetTestSupport.host(activity)
			val container = WidgetTestSupport.addWidget(activity, host, grid, 42, 0, 0, 2, 2)
			host.persist()
			assertEquals(1, DesktopLayoutTestStore.widgets(activity.applicationContext).size)
			val listener = TrashDragListener(activity)

			listener.onDrag(activity.findViewById<AppLauncher>(R.id.lalTrash),
				DragEvents.obtain(DragEvent.ACTION_DROP, localState = container))

			assertEquals(0, grid.childCount)
			assertEquals(0, DesktopLayoutTestStore.widgets(activity.applicationContext).size)
		}
	}

	@Test fun aMalformedDropShowsTheErrorDialogInsteadOfCrashing() {
		scenario.onActivity { activity ->
			val clip = ClipData.newPlainText("not-a-number", "app")
			val listener = TrashDragListener(activity)

			assertTrue(listener.onDrag(activity.findViewById<AppLauncher>(R.id.lalTrash),
				DragEvents.obtain(DragEvent.ACTION_DROP,
					clipDescription = clip.description, clipData = clip)))
		}

		ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
		assertNotNull(ShadowAlertDialog.getLatestAlertDialog())
	}
}
