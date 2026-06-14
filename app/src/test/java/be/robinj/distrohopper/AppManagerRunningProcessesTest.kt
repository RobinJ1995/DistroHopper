package be.robinj.distrohopper

import android.app.ActivityManager
import android.content.Context
import androidx.test.core.app.ActivityScenario
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowActivityManager

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class AppManagerRunningProcessesTest {
    private lateinit var scenario: ActivityScenario<HomeActivity>

    @Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
    @After fun tearDown() { scenario.close() }

    private fun process(packageName: String, processImportance: Int) =
        ActivityManager.RunningAppProcessInfo(packageName, 1234, arrayOf(packageName))
            .apply { importance = processImportance }

    private fun setProcesses(activity: HomeActivity, processes: List<ActivityManager.RunningAppProcessInfo>) {
        val activityManager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        Shadows.shadowOf(activityManager).setProcesses(processes)
    }

    @Test
    @Config(shadows = [NullProcessListActivityManagerShadow::class])
    fun getRunningAppsReturnsEmptyListWhenProcessListIsUnavailable() {
        scenario.onActivity { activity ->
            // ActivityManager.getRunningAppProcesses() is documented to return null
            val running = activity.appManager.runningApps
            assertTrue(running.isEmpty())
        }
    }

    @Test fun getRunningAppsReturnsAppsWithImportantProcesses() {
        scenario.onActivity { activity ->
            setProcesses(activity, listOf(
                process("com.example.alpha", ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND),
                process("com.example.beta", ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE),
            ))
            val running = activity.appManager.runningApps
            assertEquals(
                listOf("com.example.alpha", "com.example.beta"),
                running.map { it.packageName },
            )
        }
    }

    @Test fun getRunningAppsIgnoresGoneProcesses() {
        scenario.onActivity { activity ->
            setProcesses(activity, listOf(
                process("com.example.alpha", ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE),
            ))
            assertTrue(activity.appManager.runningApps.isEmpty())
        }
    }

    @Test fun getRunningAppsIgnoresProcessesOfUnknownPackages() {
        scenario.onActivity { activity ->
            setProcesses(activity, listOf(
                process("de.craggy.ghost", ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND),
            ))
            assertTrue(activity.appManager.runningApps.isEmpty())
        }
    }

    @Implements(ActivityManager::class)
    class NullProcessListActivityManagerShadow : ShadowActivityManager() {
        @Implementation
        override fun getRunningAppProcesses(): MutableList<ActivityManager.RunningAppProcessInfo>? = null
    }
}
