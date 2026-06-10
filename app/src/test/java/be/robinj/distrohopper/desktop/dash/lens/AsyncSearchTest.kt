package be.robinj.distrohopper.desktop.dash.lens

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.ListView
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.Preferences
import be.robinj.distrohopper.thirdparty.ProgressWheel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class AsyncSearchTest {
    /** AsyncSearch.PROGRESS_WHEEL_DELAY is 240 ms; wait comfortably longer than that. */
    private val afterDelayMs = 600L

    private lateinit var context: Context
    private lateinit var progressWheel: ProgressWheel
    private lateinit var search: TestableAsyncSearch

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Preferences.getSharedPreferences(context, Preferences.PREFERENCES).edit().clear().commit()
        Preferences.getSharedPreferences(context, Preferences.LENSES).edit().clear().commit()

        progressWheel = ProgressWheel(context, Robolectric.buildAttributeSet().build())
        progressWheel.visibility = View.GONE
        val lensManager = LensManager(context, LinearLayout(context), LinearLayout(context), progressWheel, null)
        search = TestableAsyncSearch(lensManager, progressWheel, ListView(context))
    }

    private fun waitForDelayedThread() {
        Thread.sleep(afterDelayMs)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }

    @Test fun cancellingSearchHidesTheProgressWheel() {
        search.startSearchUi()
        progressWheel.visibility = View.VISIBLE // the wheel already appeared

        search.cancelSearchUi()

        assertEquals(View.GONE, progressWheel.visibility)
    }

    @Test fun progressWheelDoesNotAppearAfterCancellation() {
        search.startSearchUi()
        search.cancelSearchUi()

        waitForDelayedThread()

        assertNotEquals(
            "the delayed progress wheel thread must not outlive a cancelled search",
            View.VISIBLE, progressWheel.visibility,
        )
    }

    @Test fun progressWheelAppearsAfterDelayWhileSearchIsRunning() {
        search.startSearchUi()

        waitForDelayedThread()

        assertEquals(View.VISIBLE, progressWheel.visibility)
    }

    @Test fun progressWheelDoesNotAppearAfterSearchHasFinished() {
        search.startSearchUi()
        search.finishSearchUi()

        waitForDelayedThread()

        assertEquals(View.GONE, progressWheel.visibility)
    }

    private class TestableAsyncSearch(
        lensManager: LensManager,
        progressWheel: ProgressWheel,
        listView: ListView,
    ) : AsyncSearch(lensManager, progressWheel, listView, 1f, 80) {
        fun startSearchUi() = onPreExecute()
        fun finishSearchUi() = onPostExecute(emptyList())
        fun cancelSearchUi() = onCancelled()
    }
}
