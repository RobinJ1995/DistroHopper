package be.robinj.distrohopper;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowLooper;

import be.robinj.distrohopper.preferences.PreferencesActivity;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class PreferencesNavigationTest {

    private ActivityScenario<HomeActivity> scenario;

    @Before
    public void setUp() {
        HomeActivityTest.seedPackageManager();
        scenario = ActivityScenario.launch(HomeActivity.class);
        Robolectric.flushBackgroundThreadScheduler();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
    }

    @After
    public void tearDown() {
        scenario.close();
    }

    @Test
    public void clickingCogButtonLaunchesPreferencesActivity() {
        onView(withId(R.id.ibPanelCog)).perform(click());
        scenario.onActivity(activity -> {
            ShadowActivity.IntentForResult intentForResult =
                    Shadows.shadowOf(activity).getNextStartedActivityForResult();
            assertNotNull("Cog click must start PreferencesActivity", intentForResult);
            assertEquals(PreferencesActivity.class.getName(),
                    intentForResult.intent.getComponent().getClassName());
        });
    }

    @Test
    public void homeContainerIsDisplayed() {
        onView(withId(R.id.llLauncherAndDashContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void panelIsVisibleOnHomeScreen() {
        onView(withId(R.id.llPanel)).check(matches(isDisplayed()));
    }

    @Test
    public void dashIsClosedInitially() {
        onView(withId(R.id.llDash))
                .check(matches(androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility(
                        androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE)));
    }
}
