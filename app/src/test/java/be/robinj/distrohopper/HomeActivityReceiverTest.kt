package be.robinj.distrohopper

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.broadcast.PackageManagerBroadcastReceiver
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class HomeActivityReceiverTest {
    private lateinit var scenario: ActivityScenario<HomeActivity>

    @Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
    @After fun tearDown() { scenario.close() }

    private fun packageManagerReceiverRegistrations(): List<Any> {
        val application = ApplicationProvider.getApplicationContext<Application>()
        return Shadows.shadowOf(application).registeredReceivers
            .filter { it.broadcastReceiver is PackageManagerBroadcastReceiver }
    }

    @Test fun packageManagerReceiverIsRegisteredWhileActive() {
        assertTrue(packageManagerReceiverRegistrations().isNotEmpty())
    }

    @Test fun packageManagerReceiverIsUnregisteredWhenDestroyed() {
        scenario.moveToState(Lifecycle.State.DESTROYED)
        ActivityTestSupport.drainTasks()
        assertTrue(
            "PackageManagerBroadcastReceiver must not stay registered after the activity is destroyed",
            packageManagerReceiverRegistrations().isEmpty(),
        )
    }
}
