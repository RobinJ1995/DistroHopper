package be.robinj.distrohopper.home

import android.app.ActivityManager
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.App
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.desktop.launcher.AppLauncher
import be.robinj.distrohopper.desktop.launcher.RunningAppLauncher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class LauncherBarBinderTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
	@After fun tearDown() { scenario.close() }

	private fun app(activity: HomeActivity, packageName: String): App =
		activity.appManager.findAppsByPackageName(packageName).first()

	private fun settingsShortcut(activity: HomeActivity): App =
		ActivityTestSupport.settingsShortcut(activity)

	private fun pinnedContainer(activity: HomeActivity): LinearLayout =
		activity.findViewById(R.id.llLauncherPinnedApps)

	private fun setRunningProcess(activity: HomeActivity, packageName: String) {
		val activityManager =
			activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
		val process = ActivityManager
			.RunningAppProcessInfo(packageName, 1234, arrayOf(packageName))
			.apply { importance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND }
		Shadows.shadowOf(activityManager).setProcesses(listOf(process))
	}

	@Test fun addAndRemovePinnedAppViewRoundTripThroughTheViewTag() {
		scenario.onActivity { activity ->
			val binder = LauncherBarBinder(activity.appManager)
			val app = app(activity, "com.example.alpha")
			val container = pinnedContainer(activity)

			binder.addPinnedAppView(app)
			assertEquals(1, container.childCount)
			assertNotNull(container.findViewWithTag<AppLauncher>(app))

			binder.removePinnedAppView(app)
			assertEquals(0, container.childCount)
			assertNull(container.findViewWithTag<AppLauncher>(app))
		}
	}

	@Test fun addAndRemoveSettingsShortcutViewRoundTripThroughTheViewTag() {
		scenario.onActivity { activity ->
			val binder = LauncherBarBinder(activity.appManager)
			val settings = settingsShortcut(activity)
			val container = pinnedContainer(activity)

			binder.addPinnedAppView(settings)
			assertEquals(1, container.childCount)
			assertNotNull(container.findViewWithTag<AppLauncher>(settings))

			binder.removePinnedAppView(settings)
			assertEquals(0, container.childCount)
			assertNull(container.findViewWithTag<AppLauncher>(settings))
		}
	}

	@Test fun refreshPinnedViewRebuildsTheBarFromTheModel() {
		scenario.onActivity { activity ->
			val binder = LauncherBarBinder(activity.appManager)
			val manager = activity.appManager
			manager.pin(app(activity, "com.example.alpha"), false, false, false)
			manager.pin(app(activity, "com.example.beta"), false, false, false)

			binder.refreshPinnedView()

			val container = pinnedContainer(activity)
			assertEquals(2, container.childCount)
			assertNotNull(container.findViewWithTag<AppLauncher>(app(activity, "com.example.alpha")))
			assertNotNull(container.findViewWithTag<AppLauncher>(app(activity, "com.example.beta")))
		}
	}

	@Test fun addRunningAppsAddsAColouredLauncherForUnpinnedApps() {
		scenario.onActivity { activity ->
			val binder = LauncherBarBinder(activity.appManager)
			setRunningProcess(activity, "com.example.beta")

			binder.addRunningApps(Color.RED)

			val running = activity.findViewById<LinearLayout>(R.id.llLauncherRunningApps)
			assertEquals(1, running.childCount)
			val launcher = running.getChildAt(0) as RunningAppLauncher
			// The default theme uses dynamic colours, so the passed colour sticks.
			assertEquals(Color.RED, launcher.colour)
		}
	}

	@Test fun addRunningAppsMarksThePinnedLauncherInsteadOfAddingOne() {
		scenario.onActivity { activity ->
			val binder = LauncherBarBinder(activity.appManager)
			val app = app(activity, "com.example.alpha")
			activity.appManager.pin(app, false, false, false)
			binder.addPinnedAppView(app)
			setRunningProcess(activity, "com.example.alpha")

			binder.addRunningApps(Color.RED)

			assertTrue(pinnedContainer(activity).findViewWithTag<AppLauncher>(app).isRunning)
			assertEquals(0,
				activity.findViewById<LinearLayout>(R.id.llLauncherRunningApps).childCount)
		}
	}

	@Test fun draggingAPinnedAppSwapsPreferencesForTheTrash() {
		scenario.onActivity { activity ->
			val binder = LauncherBarBinder(activity.appManager)

			binder.startedDraggingPinnedApp()

			assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.lalTrash).visibility)
			assertEquals(View.GONE, activity.findViewById<View>(R.id.lalPreferences).visibility)
			// The default theme keeps the BFB visible while dragging.
			assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.lalBfb).visibility)
			assertEquals(0.9F, pinnedContainer(activity).alpha, 0.001F)
		}
	}

	@Test fun stoppingADragRestoresTheLauncherBar() {
		scenario.onActivity { activity ->
			val binder = LauncherBarBinder(activity.appManager)
			binder.startedDraggingPinnedApp()

			binder.stoppedDraggingPinnedApp()

			assertEquals(View.GONE, activity.findViewById<View>(R.id.lalTrash).visibility)
			// The default theme places the preferences launcher at the bottom.
			assertEquals(View.VISIBLE,
				activity.findViewById<View>(R.id.lalPreferences).visibility)
			assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.lalBfb).visibility)
			assertEquals(1.0F, pinnedContainer(activity).alpha, 0.001F)
		}
	}
}
