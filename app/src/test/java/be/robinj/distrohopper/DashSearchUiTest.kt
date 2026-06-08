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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class DashSearchUiTest {
    private lateinit var scenario: ActivityScenario<HomeActivity>

    @Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
    @After fun tearDown() { scenario.close() }

    private fun openDash() = onView(withId(R.id.lalBfb)).perform(click())

    @Test fun searchFieldIsEmptyWhenDashFirstOpens() {
        openDash(); onView(withId(R.id.etDashSearch)).check(matches(withText("")))
    }

    @Test fun appGridIsVisibleWhenDashFirstOpens() {
        openDash(); onView(withId(R.id.llDashHomeAppsContainer)).check(matches(isDisplayed()))
    }

    @Test fun lensResultsContainerIsHiddenWhenDashFirstOpens() {
        openDash(); onView(withId(R.id.llDashHomeLensesContainer)).check(matches(not(isDisplayed())))
    }

    @Test fun searchFieldAcceptsAndDisplaysTypedText() {
        openDash(); onView(withId(R.id.etDashSearch)).perform(replaceText("hello"))
        onView(withId(R.id.etDashSearch)).check(matches(withText("hello")))
    }

    @Test fun clearingSearchRestoresAppGridImmediately() {
        openDash(); onView(withId(R.id.etDashSearch)).perform(replaceText("settings"), replaceText(""))
        onView(withId(R.id.llDashHomeAppsContainer)).check(matches(isDisplayed()))
    }

    @Test fun clearingSearchHidesLensResultsContainer() {
        openDash(); onView(withId(R.id.etDashSearch)).perform(replaceText("test"), replaceText(""))
        onView(withId(R.id.llDashHomeLensesContainer)).check(matches(not(isDisplayed())))
    }

    @Test fun searchFieldClearsWhenDashIsClosedWithBackButton() {
        openDash(); onView(withId(R.id.etDashSearch)).perform(replaceText("something")); pressBack(); openDash()
        onView(withId(R.id.etDashSearch)).check(matches(withText("")))
    }

    @Test fun searchFieldClearsWhenDashIsClosedWithCloseButton() {
        openDash(); onView(withId(R.id.etDashSearch)).perform(replaceText("query"))
        onView(withId(R.id.ibPanelDashClose)).perform(click()); openDash()
        onView(withId(R.id.etDashSearch)).check(matches(withText("")))
    }

    @Test fun appGridRestoredAfterDashClosedAndReopened() {
        openDash(); onView(withId(R.id.etDashSearch)).perform(replaceText("filter")); pressBack(); openDash()
        onView(withId(R.id.llDashHomeAppsContainer)).check(matches(isDisplayed()))
    }
}
