package be.robinj.distrohopper

import androidx.test.core.app.ActivityScenario
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class AppComparatorAlphabeticalTest {
    private lateinit var scenario: ActivityScenario<HomeActivity>
    private val comparator = AppComparatorAlphabetical()

    @Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
    @After fun tearDown() { scenario.close() }

    private fun app(activity: HomeActivity, label: String) = App(
        activity,
        activity.appManager,
        ActivityTestSupport.resolveInfo("com.example.${label.lowercase()}", "${label}Activity", label),
    )

    @Test
    fun comparesLabelsCaseInsensitively() {
        scenario.onActivity { activity ->
            assertTrue(comparator.compare(app(activity, "alpha"), app(activity, "Beta")) < 0)
            assertTrue(comparator.compare(app(activity, "Beta"), app(activity, "alpha")) > 0)
        }
    }

    @Test
    fun equalLabelsCompareAsEqualRegardlessOfCase() {
        scenario.onActivity { activity ->
            assertEquals(0, comparator.compare(app(activity, "Gamma"), app(activity, "gAMMA")))
        }
    }

    @Test
    fun sortsMixedCaseListAlphabetically() {
        scenario.onActivity { activity ->
            val apps = mutableListOf(
                app(activity, "zeta"),
                app(activity, "Alpha"),
                app(activity, "gamma"),
                app(activity, "Beta"),
            )
            apps.sortWith(comparator)
            assertEquals(listOf("Alpha", "Beta", "gamma", "zeta"), apps.map { it.label })
        }
    }
}
