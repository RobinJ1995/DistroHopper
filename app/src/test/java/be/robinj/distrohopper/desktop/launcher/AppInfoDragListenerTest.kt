package be.robinj.distrohopper.desktop.launcher

import android.graphics.Color
import android.provider.Settings
import android.view.DragEvent
import android.view.View
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.DragEvents
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class AppInfoDragListenerTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
	@After fun tearDown() { scenario.close() }

	@Test fun theAppInfoTargetGetsItsThemedIcon() {
		// The icon is applied by ThemeApplier from the theme's launcher_appinfo_image
		// (a layout-level custom:icon attribute is dead — see AppLauncher's styleable);
		// regression test for the target rendering as a blank tile.
		scenario.onActivity { activity ->
			val lalAppInfo = activity.findViewById<AppLauncher>(R.id.lalAppInfo)

			assertNotNull(lalAppInfo.icon)
			assertNotNull(lalAppInfo
				.findViewById<android.widget.ImageView>(R.id.imgIcon).drawable)
		}
	}

	@Test fun enteringTheAppInfoTargetHighlightsItAndExitingRestoresIt() {
		scenario.onActivity { activity ->
			val lalAppInfo = activity.findViewById<AppLauncher>(R.id.lalAppInfo)
			val originalColour = lalAppInfo.colour
			val listener = AppInfoDragListener(activity)

			listener.onDrag(lalAppInfo, DragEvents.obtain(DragEvent.ACTION_DRAG_ENTERED))
			assertEquals(Color.rgb(40, 120, 255), lalAppInfo.colour)

			listener.onDrag(lalAppInfo, DragEvents.obtain(DragEvent.ACTION_DRAG_EXITED))
			assertEquals(originalColour, lalAppInfo.colour)
		}
	}

	@Test fun droppingADashAppOpensItsAppInfoScreenAndLeavesDragMode() {
		scenario.onActivity { activity ->
			val app = activity.appManager.findAppsByPackageName("com.example.alpha").first()
			val lalAppInfo = activity.findViewById<AppLauncher>(R.id.lalAppInfo)
			val listener = AppInfoDragListener(activity)

			listener.onDrag(lalAppInfo,
				DragEvents.obtain(DragEvent.ACTION_DROP, localState = app))

			val started = Shadows.shadowOf(activity).nextStartedActivity
			assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, started.action)
			assertEquals("package:com.example.alpha", started.dataString)
			assertEquals(View.GONE, lalAppInfo.visibility)
		}
	}

	@Test fun droppingAPinnedDashAppOpensItsAppInfoScreen() {
		scenario.onActivity { activity ->
			val app = activity.appManager.findAppsByPackageName("com.example.beta").first()
			val listener = AppInfoDragListener(activity)

			listener.onDrag(activity.findViewById<AppLauncher>(R.id.lalAppInfo),
				DragEvents.obtain(DragEvent.ACTION_DROP,
					localState = LauncherDragPayload.PinnedAppDrag(app)))

			val started = Shadows.shadowOf(activity).nextStartedActivity
			assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, started.action)
			assertEquals("package:com.example.beta", started.dataString)
		}
	}

	@Test fun droppingSomethingElseOpensNothing() {
		scenario.onActivity { activity ->
			val listener = AppInfoDragListener(activity)

			listener.onDrag(activity.findViewById<AppLauncher>(R.id.lalAppInfo),
				DragEvents.obtain(DragEvent.ACTION_DROP, localState = Any()))

			assertNull(Shadows.shadowOf(activity).nextStartedActivity)
		}
	}
}
