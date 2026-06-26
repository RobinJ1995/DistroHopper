package be.robinj.distrohopper

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.pressBack
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

/*
 * Back is handled through the OnBackPressedDispatcher (not the deprecated
 * onBackPressed override) so that, under predictive back, the system knows the
 * launcher consumes Back and does not play its own "back to home" animation —
 * which on the default launcher flashed the home and snapped the dash shut
 * without its close animation. These tests pin the callback's enabled state
 * (which is what suppresses that flash) and the no-op-on-home behaviour.
 *
 * Runs under the PAUSED looper so the dash close animation can finish before
 * its end-state is asserted.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class HomeBackButtonTest {
    private lateinit var scenario: ActivityScenario<HomeActivity>

    @Before fun setUp() { this.scenario = ActivityTestSupport.launchHome() }
    @After fun tearDown() { this.scenario.close() }

    /** Marks DistroHopper as the device's preferred HOME activity. */
    private fun makeDefaultLauncher(activity: HomeActivity) {
        val filter = IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val component = ComponentName(activity, HomeActivity::class.java)
        activity.packageManager.addPreferredActivity(filter, 0, arrayOf(component), component)
    }

    private fun llDash(activity: HomeActivity) = activity.findViewById<View>(R.id.llDash)

    /*
     * Default launcher, nothing open: Back must be consumed (so the system does
     * not flash/restart the home) yet leave the launcher exactly where it is.
     */
    @Test fun backOnDefaultLauncherHomeScreenIsConsumedAndDoesNotFinish() {
        this.scenario.onActivity { activity ->
            this.makeDefaultLauncher(activity)
            activity.updateBackCallback()

            assertTrue(activity.onBackPressedDispatcher.hasEnabledCallbacks())
        }

        pressBack() // consumed by the launcher's callback; no activity change //

        this.scenario.onActivity { activity ->
            assertFalse(activity.isFinishing)
            assertEquals(View.GONE, this.llDash(activity).visibility)
        }
    }

    /*
     * Running as an ordinary app (not the default launcher) with nothing open,
     * the launcher must NOT swallow Back, so the system can handle it (exit).
     * Becoming the default launcher flips the callback on.
     */
    @Test fun backCallbackTracksDefaultLauncherStatus() {
        this.scenario.onActivity { activity ->
            // launchHome() does not register a preferred HOME activity //
            activity.updateBackCallback()
            assertFalse(activity.onBackPressedDispatcher.hasEnabledCallbacks())

            this.makeDefaultLauncher(activity)
            activity.updateBackCallback()
            assertTrue(activity.onBackPressedDispatcher.hasEnabledCallbacks())
        }
    }

    /*
     * Even when not the default launcher, an open dash must capture Back so it
     * closes the dash instead of leaving the app.
     */
    @Test fun backCallbackEnablesWhileDashOpenEvenWhenNotDefaultLauncher() {
        this.scenario.onActivity { activity ->
            activity.updateBackCallback()
            assertFalse(activity.onBackPressedDispatcher.hasEnabledCallbacks())

            activity.openDash()
            assertTrue(activity.onBackPressedDispatcher.hasEnabledCallbacks())
        }
    }

    /* Back closes an open dash (and the callback relaxes again afterwards). */
    @Test fun backClosesTheOpenDash() {
        this.scenario.onActivity { activity ->
            activity.openDash()
            assertEquals(View.VISIBLE, this.llDash(activity).visibility)
        }

        pressBack()
        ActivityTestSupport.drainTasks() // let the close animation finish //

        this.scenario.onActivity { activity ->
            assertEquals(View.GONE, this.llDash(activity).visibility)
            // Not the default launcher and nothing open: stop consuming Back //
            assertFalse(activity.onBackPressedDispatcher.hasEnabledCallbacks())
        }
    }
}
