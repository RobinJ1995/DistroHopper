package be.robinj.distrohopper

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import android.widget.GridView

/*
 * Runs under the PAUSED looper so that the dash close animation (the default
 * theme's UNITY fade) can complete before its end-state is asserted.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class HomeActivityTest {
    private lateinit var scenario: ActivityScenario<HomeActivity>

    @Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
    @After fun tearDown() {
        DependencyContainer.of(
            androidx.test.core.app.ApplicationProvider.getApplicationContext()).customiseMode.value = false
        scenario.close()
    }

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
        ActivityTestSupport.drainTasks() // Let the close animation finish //
        onView(withId(R.id.llDash)).check(matches(not(isDisplayed())))
    }

    @Test fun closingDashWithCloseButton() {
        onView(withId(R.id.lalBfb)).perform(click())
        onView(withId(R.id.ibPanelDashClose)).perform(click())
        ActivityTestSupport.drainTasks() // Let the close animation finish //
        onView(withId(R.id.llDash)).check(matches(not(isDisplayed())))
    }

    @Test fun appsDashContainsInstalledApps() {
        onView(withId(R.id.lalBfb)).perform(click())
        // The dash grid lives on the pager's current page, laid out lazily;
        // force it, then assert the page's adapter holds the installed apps.
        scenario.onActivity { activity ->
            ActivityTestSupport.layoutDashApps(activity)
            val grid = ActivityTestSupport.dashGrid(activity)
            assertNotNull(grid)
            assertNotNull(grid!!.adapter)
            assertTrue(grid.adapter.count > 0)
        }
    }

    @Test fun resumingWithDashOpenRefreshesUsageBasedOrder() {
        onView(withId(R.id.lalBfb)).perform(click()) // open the dash //
        ActivityTestSupport.drainTasks()

        lateinit var mostUsedKey: String
        scenario.onActivity { activity ->
            val manager = activity.appManager
            Preferences.getSharedPreferences(activity, Preferences.APP_USAGE)
                .edit().clear().commit()
            // Make the last app in the current (alphabetical) order the most used //
            val last = manager.installedApps.last()
            mostUsedKey = last.profileScopedKey
            AppUsageStats(activity).apply {
                recordLaunch(last.profileScopedKey)
                recordLaunch(last.profileScopedKey)
            }
            Preferences.getSharedPreferences(activity).edit()
                .putString(Preference.APP_SORT_ORDER.getName(), "most_used").commit()
            // Still stale: nothing has re-sorted the open dash yet //
            assertNotEquals(mostUsedKey, manager.installedApps.first().profileScopedKey)
        }

        // Returning to the launcher with the dash still open (no openDash extra) //
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        ActivityTestSupport.drainTasks()

        scenario.onActivity { activity ->
            assertEquals(mostUsedKey, activity.appManager.installedApps.first().profileScopedKey)
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

    /*
     * Entering customise mode must land with the customise controls showing
     * inside an open dash (customise mode lives inside the dash, swapping
     * llDashContent out for llDashCustomise). Guards against regressing to the
     * plain home screen with the dash closed.
     */
    @Test fun launchingInCustomiseModeShowsCustomiseUiInOpenDash() {
        scenario.close()
        scenario = ActivityTestSupport.launchHome(customise = true)
        onView(withId(R.id.llDash)).check(matches(isDisplayed()))
        onView(withId(R.id.llDashCustomise)).check(matches(isDisplayed()))
        onView(withId(R.id.llDashContent)).check(matches(not(isDisplayed())))
    }
}
