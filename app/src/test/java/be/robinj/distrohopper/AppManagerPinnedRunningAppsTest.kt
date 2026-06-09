package be.robinj.distrohopper

import android.app.ActivityManager
import android.content.Context
import android.graphics.Color
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.desktop.launcher.AppLauncher
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class AppManagerPinnedRunningAppsTest {
    private lateinit var scenario: ActivityScenario<HomeActivity>

    @Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
    @After fun tearDown() { scenario.close() }

    private fun setRunningProcess(activity: HomeActivity, packageName: String) {
        val activityManager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val process = ActivityManager.RunningAppProcessInfo(packageName, 1234, arrayOf(packageName))
            .apply { importance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND }
        Shadows.shadowOf(activityManager).setProcesses(listOf(process))
    }

    @Test fun addRunningAppsToleratesPinnedAppWithoutLauncherView() {
        scenario.onActivity { activity ->
            val manager = activity.appManager
            val app = manager.findAppsByPackageName("com.example.alpha").first()
            manager.pin(app, false, false, false) // pinned, but no view in llLauncherPinnedApps
            setRunningProcess(activity, "com.example.alpha")

            manager.addRunningApps(Color.BLACK)
        }
    }

    @Test fun addRunningAppsMarksPinnedAppViewAsRunning() {
        scenario.onActivity { activity ->
            val manager = activity.appManager
            val app = manager.findAppsByPackageName("com.example.alpha").first()
            manager.pin(app, false, false, true)
            setRunningProcess(activity, "com.example.alpha")

            manager.addRunningApps(Color.BLACK)

            val pinnedContainer = activity.findViewById<LinearLayout>(R.id.llLauncherPinnedApps)
            val launcher = pinnedContainer.findViewWithTag<AppLauncher>(app)
            assertNotNull(launcher)
            assertTrue(launcher.isRunning)
        }
    }

    @Test fun addRunningAppsAddsLauncherForUnpinnedRunningApp() {
        scenario.onActivity { activity ->
            val manager = activity.appManager
            setRunningProcess(activity, "com.example.beta")

            manager.addRunningApps(Color.BLACK)

            val runningContainer = activity.findViewById<LinearLayout>(R.id.llLauncherRunningApps)
            assertEquals(1, runningContainer.childCount)
        }
    }
}
