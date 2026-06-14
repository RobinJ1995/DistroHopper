package be.robinj.distrohopper.broadcast

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class PackageManagerBroadcastReceiverTest {
    private lateinit var scenario: ActivityScenario<HomeActivity>

    @Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
    @After fun tearDown() { scenario.close() }

    @Test fun packageRemovedDeletesMatchingApp() {
        scenario.onActivity { activity ->
            val manager = activity.appManager
            val receiver = PackageManagerBroadcastReceiver(activity)
            val before = manager.size()
            receiver.onReceive(activity, Intent(Intent.ACTION_PACKAGE_REMOVED, Uri.parse("package:com.example.alpha")))
            assertEquals(before - 1, manager.size())
            assertEquals(0, manager.findAppsByPackageName("com.example.alpha").size)
        }
    }

    @Test fun replacingPackageDoesNotModifyApps() {
        scenario.onActivity { activity ->
            val before = activity.appManager.size()
            val intent = Intent(Intent.ACTION_PACKAGE_REMOVED, Uri.parse("package:com.example.alpha"))
                .putExtra(Intent.EXTRA_REPLACING, true)
            PackageManagerBroadcastReceiver(activity).onReceive(activity, intent)
            assertEquals(before, activity.appManager.size())
        }
    }

    private fun installPackage(packageName: String, label: String) {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val resolveInfo = ActivityTestSupport.resolveInfo(packageName, "${label}Activity", label)
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        Shadows.shadowOf(application.packageManager).apply {
            addResolveInfoForIntent(launcherIntent, resolveInfo)
            addResolveInfoForIntent(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER).setPackage(packageName),
                resolveInfo,
            )
        }
    }

    @Test fun packageAddedInsertsTheNewApp() {
        scenario.onActivity { activity ->
            val manager = activity.appManager
            val before = manager.size()
            installPackage("be.samsonengert.modest", "Modest")

            PackageManagerBroadcastReceiver(activity).onReceive(activity,
                Intent(Intent.ACTION_PACKAGE_ADDED, Uri.parse("package:be.samsonengert.modest")))

            assertEquals(before + 1, manager.size())
            assertEquals(1, manager.findAppsByPackageName("be.samsonengert.modest").size)
        }
    }

    @Test fun packageAddedWhileReplacingIsIgnored() {
        scenario.onActivity { activity ->
            val before = activity.appManager.size()
            installPackage("be.samsonengert.modest", "Modest")

            PackageManagerBroadcastReceiver(activity).onReceive(activity,
                Intent(Intent.ACTION_PACKAGE_ADDED, Uri.parse("package:be.samsonengert.modest"))
                    .putExtra(Intent.EXTRA_REPLACING, true))

            assertEquals(before, activity.appManager.size())
        }
    }

    @Test fun intentWithoutPackageDataShowsTheErrorDialogInsteadOfCrashing() {
        ShadowAlertDialog.reset()
        scenario.onActivity { activity ->
            PackageManagerBroadcastReceiver(activity)
                .onReceive(activity, Intent(Intent.ACTION_PACKAGE_ADDED))
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertNotNull(ShadowAlertDialog.getLatestAlertDialog())

        scenario.onActivity { activity ->
            assertNotNull("the activity must survive a malformed broadcast",
                activity.appManager)
        }
    }
}
