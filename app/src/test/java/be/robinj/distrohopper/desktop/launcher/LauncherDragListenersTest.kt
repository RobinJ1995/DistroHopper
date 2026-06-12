package be.robinj.distrohopper.desktop.launcher

import android.content.ClipData
import android.view.DragEvent
import android.view.View
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.DragEvents
import be.robinj.distrohopper.R
import be.robinj.distrohopper.preferences.Preferences
import be.robinj.distrohopper.widgets.WidgetTestSupport
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertNull
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class LauncherDragListenersTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() {
		ShadowAlertDialog.reset()
		scenario = ActivityTestSupport.launchHome()
	}
	@After fun tearDown() { scenario.close() }

	@Test fun launcherIgnoresWidgetDragsSoTheyReachTheirOwnListeners() {
		scenario.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			val container = WidgetTestSupport.addWidget(
				activity, WidgetTestSupport.host(activity, grid), grid, 42, 0, 0, 2, 2)
			val listener = LauncherDragListener(activity.appManager)

			assertFalse(listener.onDrag(activity.findViewById(R.id.llLauncher),
				DragEvents.obtain(DragEvent.ACTION_DRAG_ENTERED, localState = container)))
		}
	}

	@Test fun pinnedAppIconsIgnoreWidgetDragsInsteadOfCrashing() {
		scenario.onActivity { activity ->
			val manager = activity.appManager
			val alpha = manager.findAppsByPackageName("com.example.alpha").first()
			manager.pin(alpha, true, false, true)
			val alphaLauncher = activity
				.findViewById<LinearLayout>(R.id.llLauncherPinnedApps)
				.findViewWithTag<AppLauncher>(alpha)
			val grid = WidgetTestSupport.standaloneGrid(activity)
			val container = WidgetTestSupport.addWidget(
				activity, WidgetTestSupport.host(activity, grid), grid, 42, 0, 0, 2, 2)
			// The clip label is the non-numeric "widget", which used to reach
			// Integer.parseInt() and pop the crash dialog //
			val clip = ClipData.newPlainText("widget", "42")
			val listener = AppLauncherDragListener(manager)

			assertFalse(listener.onDrag(alphaLauncher,
				DragEvents.obtain(DragEvent.ACTION_DROP,
					clipDescription = clip.description, clipData = clip,
					localState = container)))

			assertEquals(listOf(alpha), manager.pinned)
			assertNull("a widget drop on a pinned icon must not show an error dialog",
				ShadowAlertDialog.getLatestAlertDialog())
		}
	}

	@Test fun launcherEnterAndDropToggleDragMode() {
		scenario.onActivity { activity ->
			val llLauncher = activity.findViewById<LinearLayout>(R.id.llLauncher)
			val lalTrash = activity.findViewById<View>(R.id.lalTrash)
			val listener = LauncherDragListener(activity.appManager)

			assertTrue(listener.onDrag(llLauncher,
				DragEvents.obtain(DragEvent.ACTION_DRAG_ENTERED)))
			assertEquals(View.VISIBLE, lalTrash.visibility)

			assertTrue(listener.onDrag(llLauncher,
				DragEvents.obtain(DragEvent.ACTION_DROP)))
			assertEquals(View.GONE, lalTrash.visibility)
		}
	}

	@Test fun droppingOnAnotherPinnedAppReordersAndPersistsThePinnedApps() {
		scenario.onActivity { activity ->
			val manager = activity.appManager
			val alpha = manager.findAppsByPackageName("com.example.alpha").first()
			val beta = manager.findAppsByPackageName("com.example.beta").first()
			manager.pin(alpha, true, false, true)
			manager.pin(beta, true, false, true)
			val betaLauncher = activity
				.findViewById<LinearLayout>(R.id.llLauncherPinnedApps)
				.findViewWithTag<AppLauncher>(beta)
			val clip = ClipData.newPlainText("0", "app") // alpha's pinned index
			val listener = AppLauncherDragListener(manager)

			assertTrue(listener.onDrag(betaLauncher,
				DragEvents.obtain(DragEvent.ACTION_DROP,
					clipDescription = clip.description, clipData = clip)))

			assertEquals(listOf(beta, alpha), manager.pinned)
			val persisted = activity.getSharedPreferences(Preferences.PINNED_APPS, 0)
			assertEquals(beta.packageAndActivityName, persisted.getString("0", null))
			assertEquals(alpha.packageAndActivityName, persisted.getString("1", null))
			// The drop also leaves drag mode
			assertEquals(View.GONE, activity.findViewById<View>(R.id.lalTrash).visibility)
		}
	}

	@Test fun enterAndExitAnimationsDoNotThrow() {
		scenario.onActivity { activity ->
			val manager = activity.appManager
			val alpha = manager.findAppsByPackageName("com.example.alpha").first()
			manager.pin(alpha, false, false, true)
			val launcher = activity
				.findViewById<LinearLayout>(R.id.llLauncherPinnedApps)
				.findViewWithTag<AppLauncher>(alpha)
			val listener = AppLauncherDragListener(manager)

			assertTrue(listener.onDrag(launcher,
				DragEvents.obtain(DragEvent.ACTION_DRAG_ENTERED)))
			assertTrue(listener.onDrag(launcher,
				DragEvents.obtain(DragEvent.ACTION_DRAG_EXITED)))
		}
	}
}
