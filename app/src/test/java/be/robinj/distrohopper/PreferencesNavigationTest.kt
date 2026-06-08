package be.robinj.distrohopper

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import be.robinj.distrohopper.preferences.PreferencesActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class PreferencesNavigationTest {
    private lateinit var scenario: ActivityScenario<HomeActivity>

    @Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
    @After fun tearDown() { scenario.close() }

    @Test fun clickingCogButtonLaunchesPreferencesActivity() {
        onView(withId(R.id.ibPanelCog)).perform(click())
        scenario.onActivity {
            val started = Shadows.shadowOf(it).nextStartedActivityForResult
            assertNotNull(started)
            assertEquals(PreferencesActivity::class.java.name, started.intent.component?.className)
        }
    }

    @Test fun homeContainerIsDisplayed() {
        onView(withId(R.id.llLauncherAndDashContainer)).check(matches(isDisplayed()))
    }

    @Test fun panelIsVisibleOnHomeScreen() {
        onView(withId(R.id.llPanel)).check(matches(isDisplayed()))
    }

    @Test fun dashIsClosedInitially() {
        onView(withId(R.id.llDash)).check(matches(withEffectiveVisibility(GONE)))
    }
}
