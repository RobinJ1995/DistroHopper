package be.robinj.distrohopper.desktop.dash

import android.content.Context
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.desktop.dash.lens.LensManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowLooper

/**
 * The dash search box's text watcher: each keystroke forwards the current text
 * to the lens manager, and any failure is caught and surfaced through
 * [be.robinj.distrohopper.ExceptionHandler] rather than crashing typing.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class SearchTextWatcherTest {
    private lateinit var scenario: ActivityScenario<HomeActivity>

    @Before fun setUp() {
        ShadowAlertDialog.reset()
        scenario = ActivityTestSupport.launchHome()
    }

    @After fun tearDown() { scenario.close() }

    private class RecordingLensManager(context: Context) : LensManager(context, null, null, null, null) {
        var lastQuery: String? = null
        var calls = 0
        override fun startSearch(pattern: String?) { this.calls++; this.lastQuery = pattern }
    }

    private class ThrowingLensManager(context: Context) : LensManager(context, null, null, null, null) {
        override fun startSearch(pattern: String?) { throw RuntimeException("boom") }
    }

    @Test fun onTextChangedForwardsTypedTextToStartSearch() {
        scenario.onActivity { activity ->
            val lenses = RecordingLensManager(activity)
            val watcher = SearchTextWatcher(activity.appManager, lenses)

            watcher.onTextChanged("fire", 0, 0, 4)

            assertEquals("fire", lenses.lastQuery)
            assertEquals(1, lenses.calls)
        }
    }

    @Test fun emptyTextIsStillForwarded() {
        scenario.onActivity { activity ->
            val lenses = RecordingLensManager(activity)
            val watcher = SearchTextWatcher(activity.appManager, lenses)

            // The watcher does not pre-filter; clearing the box must reach the
            // lens manager so it can drop the previous results.
            watcher.onTextChanged("", 0, 4, 0)

            assertEquals("", lenses.lastQuery)
            assertEquals(1, lenses.calls)
        }
    }

    @Test fun beforeAndAfterTextChangedDoNotSearch() {
        scenario.onActivity { activity ->
            val lenses = RecordingLensManager(activity)
            val watcher = SearchTextWatcher(activity.appManager, lenses)

            watcher.beforeTextChanged("fire", 0, 0, 4)
            watcher.afterTextChanged(android.text.SpannableStringBuilder("fire"))

            assertEquals(0, lenses.calls)
        }
    }

    @Test fun exceptionInStartSearchIsCaughtAndSurfaced() {
        scenario.onActivity { activity ->
            val watcher = SearchTextWatcher(activity.appManager, ThrowingLensManager(activity))

            // Must not propagate — typing keeps working even if a lens blows up.
            watcher.onTextChanged("x", 0, 0, 1)

            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            assertNotNull(
                "the failure should be surfaced to the user via ExceptionHandler",
                ShadowAlertDialog.getLatestAlertDialog(),
            )
        }
    }
}
