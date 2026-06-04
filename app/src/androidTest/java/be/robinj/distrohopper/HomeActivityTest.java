package be.robinj.distrohopper;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.GridView;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class HomeActivityTest {

    @Rule
    public ActivityScenarioRule<HomeActivity> activityRule =
            new ActivityScenarioRule<>(HomeActivity.class);

    private IdlingResource appsLoadedIdlingResource;

    @Before
    public void setUp() {
        activityRule.getScenario().onActivity(activity ->
                appsLoadedIdlingResource = new BfbVisibleIdlingResource(
                        activity.findViewById(R.id.lalBfb)));
        IdlingRegistry.getInstance().register(appsLoadedIdlingResource);
    }

    @After
    public void tearDown() {
        IdlingRegistry.getInstance().unregister(appsLoadedIdlingResource);
    }

    @Test
    public void activityLaunchesSuccessfully() {
        onView(withId(R.id.llLauncherAndDashContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void launcherPanelIsDisplayed() {
        onView(withId(R.id.llPanel)).check(matches(isDisplayed()));
    }

    @Test
    public void bfbButtonIsDisplayedAfterAppsLoad() {
        onView(withId(R.id.lalBfb)).check(matches(isDisplayed()));
    }

    @Test
    public void dashIsHiddenInitially() {
        onView(withId(R.id.llDash)).check(matches(not(isDisplayed())));
    }

    @Test
    public void openingDashWithBfbButton() {
        onView(withId(R.id.lalBfb)).perform(click());
        onView(withId(R.id.llDash)).check(matches(isDisplayed()));
    }

    @Test
    public void closingDashWithBackButton() {
        onView(withId(R.id.lalBfb)).perform(click());
        onView(withId(R.id.llDash)).check(matches(isDisplayed()));
        pressBack();
        onView(withId(R.id.llDash)).check(matches(not(isDisplayed())));
    }

    @Test
    public void closingDashWithCloseButton() {
        onView(withId(R.id.lalBfb)).perform(click());
        onView(withId(R.id.ibPanelDashClose)).perform(click());
        onView(withId(R.id.llDash)).check(matches(not(isDisplayed())));
    }

    @Test
    public void appsDashContainsInstalledApps() {
        onView(withId(R.id.lalBfb)).perform(click());
        onView(withId(R.id.gvDashHomeApps)).check((view, noViewFoundException) -> {
            if (noViewFoundException != null) throw noViewFoundException;
            GridView gridView = (GridView) view;
            assertNotNull("Dash app grid should have an adapter", gridView.getAdapter());
            assertTrue("Dash should contain at least one installed app",
                    gridView.getAdapter().getCount() > 0);
        });
    }

    @Test
    public void searchFieldAcceptsTextInput() {
        onView(withId(R.id.lalBfb)).perform(click());
        onView(withId(R.id.etDashSearch)).perform(replaceText("settings"));
        onView(withId(R.id.etDashSearch)).check(matches(withText("settings")));
    }

    @Test
    public void searchFiltersAppResults() {
        onView(withId(R.id.lalBfb)).perform(click());

        // Capture full app count with empty search
        final int[] fullCount = {0};
        onView(withId(R.id.gvDashHomeApps)).check((view, noViewFoundException) -> {
            if (noViewFoundException != null) throw noViewFoundException;
            fullCount[0] = ((GridView) view).getAdapter().getCount();
            assertTrue("Expected apps to be loaded before testing search",
                    fullCount[0] > 0);
        });

        // A query with no matches should produce fewer results than the full list
        onView(withId(R.id.etDashSearch)).perform(replaceText("zzznomatchzzz"));
        onView(withId(R.id.gvDashHomeApps)).check((view, noViewFoundException) -> {
            if (noViewFoundException != null) throw noViewFoundException;
            int filteredCount = ((GridView) view).getAdapter().getCount();
            assertTrue("Search should filter app list to fewer results than the full list",
                    filteredCount < fullCount[0]);
        });
    }

    @Test
    public void appManagerIsInitialisedAfterLoad() {
        activityRule.getScenario().onActivity(activity ->
                assertNotNull("AppManager should be initialised after apps load",
                        activity.getAppManager()));
    }

    /**
     * Blocks Espresso until the BFB launcher button transitions from GONE to VISIBLE,
     * which signals that AsyncLoadApps has finished populating the app list.
     */
    private static class BfbVisibleIdlingResource implements IdlingResource {
        private final View bfbView;
        private ResourceCallback callback;

        BfbVisibleIdlingResource(View bfbView) {
            this.bfbView = bfbView;
        }

        @Override
        public String getName() {
            return "BfbVisible";
        }

        @Override
        public boolean isIdleNow() {
            boolean idle = bfbView != null && bfbView.getVisibility() == View.VISIBLE;
            if (idle && callback != null) {
                callback.onTransitionToIdle();
            }
            return idle;
        }

        @Override
        public void registerIdleTransitionCallback(ResourceCallback callback) {
            this.callback = callback;
        }
    }
}
