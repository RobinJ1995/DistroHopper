package be.robinj.distrohopper.broadcast

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

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
}
