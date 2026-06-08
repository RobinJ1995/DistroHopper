package be.robinj.distrohopper

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import android.widget.GridView

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class HomeActivityTest {
    private lateinit var scenario: ActivityScenario<HomeActivity>

    @Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
    @After fun tearDown() { scenario.close() }

    @Test fun activityLaunchesSuccessfully() {
        onView(withId(R.id.llLauncherAndDashContainer)).check(matches(isDisplayed()))
    }

    @Test fun launcherPanelIsDisplayed() {
        onView(withId(R.id.llPanel)).check(matches(isDisplayed()))
    }

    @Test fun bfbButtonIsDisplayedAfterAppsLoad() {
        onView(withId(R.id.lalBfb)).check(matches(isDisplayed()))
    }

    @Test fun dashIsHiddenInitially() {
        onView(withId(R.id.llDash)).check(matches(not(isDisplayed())))
    }

    @Test fun openingDashWithBfbButton() {
        onView(withId(R.id.lalBfb)).perform(click())
        onView(withId(R.id.llDash)).check(matches(isDisplayed()))
    }

    @Test fun closingDashWithBackButton() {
        onView(withId(R.id.lalBfb)).perform(click())
        pressBack()
        onView(withId(R.id.llDash)).check(matches(not(isDisplayed())))
    }

    @Test fun closingDashWithCloseButton() {
        onView(withId(R.id.lalBfb)).perform(click())
        onView(withId(R.id.ibPanelDashClose)).perform(click())
        onView(withId(R.id.llDash)).check(matches(not(isDisplayed())))
    }

    @Test fun appsDashContainsInstalledApps() {
        onView(withId(R.id.lalBfb)).perform(click())
        onView(withId(R.id.gvDashHomeApps)).check { view, error ->
            if (error != null) throw error
            val grid = view as GridView
            assertNotNull(grid.adapter)
            assertTrue(grid.adapter.count > 0)
        }
    }

    @Test fun searchFieldAcceptsTextInput() {
        onView(withId(R.id.lalBfb)).perform(click())
        onView(withId(R.id.etDashSearch)).perform(replaceText("alpha"))
        onView(withId(R.id.etDashSearch)).check(matches(withText("alpha")))
    }

    @Test fun searchFiltersAppResults() {
        onView(withId(R.id.lalBfb)).perform(click())
        onView(withId(R.id.etDashSearch)).perform(replaceText("zzznomatch"))
        ActivityTestSupport.drainTasks()
        onView(withId(R.id.llDashHomeAppsContainer)).check(matches(not(isDisplayed())))
    }

    @Test fun appManagerIsInitialisedAfterLoad() {
        scenario.onActivity { assertNotNull(it.appManager) }
    }
}
