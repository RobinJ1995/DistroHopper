package be.robinj.distrohopper.dev

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class DevLogsActivityTest {
	private lateinit var log: Log

	@Before fun setUp() {
		val instance = Log::class.java.getDeclaredField("instance")
		instance.isAccessible = true
		instance.set(null, null)

		log = Log.getInstance()
		log.setEnabled(true)
	}

	@Test fun seededEntriesAreRenderedAsRows() {
		log.i("AppsLoader", "Loaded 214 apps")
		log.w("Image", "Falling back to the default icon")

		ActivityScenario.launch(DevLogsActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				val list = activity.findViewById<RecyclerView>(R.id.rvLogs)

				assertEquals(2, list.adapter?.itemCount)
				assertEquals(View.VISIBLE, list.visibility)
				assertEquals(View.GONE, activity.findViewById<TextView>(R.id.tvLogsEmpty).visibility)
			}
		}
	}

	@Test fun theEmptyStateShowsWhenNothingHasBeenLogged() {
		ActivityScenario.launch(DevLogsActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				assertEquals(
					View.VISIBLE,
					activity.findViewById<TextView>(R.id.tvLogsEmpty).visibility)
				assertEquals(View.GONE, activity.findViewById<RecyclerView>(R.id.rvLogs).visibility)
			}
		}
	}

	@Test fun entriesLoggedWhileTheScreenIsOpenAreRendered() {
		ActivityScenario.launch(DevLogsActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				log.e("WidgetHost", "Widget 7 has no provider")

				// nudge() coalesces onto the main looper with a delay; drain it. //
				ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

				assertEquals(1, activity.findViewById<RecyclerView>(R.id.rvLogs).adapter?.itemCount)
			}
		}
	}
}
