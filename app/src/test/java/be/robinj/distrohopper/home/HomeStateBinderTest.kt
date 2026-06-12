package be.robinj.distrohopper.home

import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class HomeStateBinderTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
	@After fun tearDown() { scenario.close() }

	/**
	 * The preference flows re-emit their current value every time the activity
	 * returns to STARTED; collectors must skip values that are already applied
	 * instead of re-running their (expensive) view work on every home-button
	 * press. Observable here through the running-apps row: with the preference
	 * unchanged (false), a lifecycle restart used to re-clear the row.
	 */
	@Test fun aLifecycleRestartDoesNotReapplyUnchangedPreferences() {
		scenario.onActivity { activity ->
			activity.findViewById<LinearLayout>(R.id.llLauncherRunningApps)
				.addView(View(activity))
		}

		scenario.moveToState(Lifecycle.State.CREATED)
		scenario.moveToState(Lifecycle.State.RESUMED)
		ActivityTestSupport.drainTasks()

		scenario.onActivity { activity ->
			assertEquals(1, activity
				.findViewById<LinearLayout>(R.id.llLauncherRunningApps).childCount)
		}
	}
}
