package be.robinj.distrohopper;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import be.robinj.distrohopper.preferences.PreferencesActivity;

/**
 * Tests navigation to/from PreferencesActivity.
 *
 * Note: the cog button (ibPanelCog) is always visible in the panel regardless of whether
 * app loading has finished, so no BfbVisible IdlingResource is needed here.
 *
 * Note: HomeActivity.onActivityResult() always restarts itself after returning from
 * PreferencesActivity, so post-navigation checks target the fresh HomeActivity instance.
 */
@RunWith(AndroidJUnit4.class)
public class PreferencesNavigationTest {

    @Rule
    public ActivityScenarioRule<HomeActivity> activityRule =
            new ActivityScenarioRule<>(HomeActivity.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void clickingCogButtonLaunchesPreferencesActivity() {
        onView(withId(R.id.ibPanelCog)).perform(click());
        intended(hasComponent(PreferencesActivity.class.getName()));
    }

    @Test
    public void navigatingBackFromPreferencesReturnsToHomeScreen() {
        onView(withId(R.id.ibPanelCog)).perform(click());
        pressBack();
        // HomeActivity restarts itself on onActivityResult — verify the new instance is ready
        onView(withId(R.id.llLauncherAndDashContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void panelRemainsVisibleAfterReturningFromPreferences() {
        onView(withId(R.id.ibPanelCog)).perform(click());
        pressBack();
        onView(withId(R.id.llPanel)).check(matches(isDisplayed()));
    }

    @Test
    public void dashRemainsClosedAfterReturningFromPreferences() {
        onView(withId(R.id.ibPanelCog)).perform(click());
        pressBack();
        // Dash should not spontaneously open on return
        onView(withId(R.id.llDash))
                .check(matches(androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility(
                        androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE)));
    }
}
