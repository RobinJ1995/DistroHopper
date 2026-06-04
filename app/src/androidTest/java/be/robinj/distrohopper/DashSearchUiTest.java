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

import android.view.View;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the Dash search UI: field state, container switching, and
 * the synchronous empty-search reset path in LensManager.
 */
@RunWith(AndroidJUnit4.class)
public class DashSearchUiTest {

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

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    public void searchFieldIsEmptyWhenDashFirstOpens() {
        onView(withId(R.id.lalBfb)).perform(click());
        onView(withId(R.id.etDashSearch)).check(matches(withText("")));
    }

    @Test
    public void appGridIsVisibleWhenDashFirstOpens() {
        onView(withId(R.id.lalBfb)).perform(click());
        onView(withId(R.id.llDashHomeAppsContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void lensResultsContainerIsHiddenWhenDashFirstOpens() {
        onView(withId(R.id.lalBfb)).perform(click());
        onView(withId(R.id.llDashHomeLensesContainer)).check(matches(not(isDisplayed())));
    }

    // ── Typing and clearing ────────────────────────────────────────────────────

    @Test
    public void searchFieldAcceptsAndDisplaysTypedText() {
        onView(withId(R.id.lalBfb)).perform(click());
        onView(withId(R.id.etDashSearch)).perform(replaceText("hello"));
        onView(withId(R.id.etDashSearch)).check(matches(withText("hello")));
    }

    @Test
    public void clearingSearchRestoresAppGridImmediately() {
        // LensManager.startSearch("") is synchronous — no async wait needed.
        onView(withId(R.id.lalBfb)).perform(click());
        onView(withId(R.id.etDashSearch)).perform(replaceText("settings"));
        onView(withId(R.id.etDashSearch)).perform(replaceText(""));
        onView(withId(R.id.llDashHomeAppsContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void clearingSearchHidesLensResultsContainer() {
        onView(withId(R.id.lalBfb)).perform(click());
        onView(withId(R.id.etDashSearch)).perform(replaceText("test"));
        onView(withId(R.id.etDashSearch)).perform(replaceText(""));
        onView(withId(R.id.llDashHomeLensesContainer)).check(matches(not(isDisplayed())));
    }

    // ── Persistence across dash close/open ────────────────────────────────────

    @Test
    public void searchFieldClearsWhenDashIsClosedWithBackButton() {
        onView(withId(R.id.lalBfb)).perform(click());
        onView(withId(R.id.etDashSearch)).perform(replaceText("something"));
        pressBack(); // closeDash() calls etDashSearch.setText("")
        onView(withId(R.id.lalBfb)).perform(click());
        onView(withId(R.id.etDashSearch)).check(matches(withText("")));
    }

    @Test
    public void searchFieldClearsWhenDashIsClosedWithCloseButton() {
        onView(withId(R.id.lalBfb)).perform(click());
        onView(withId(R.id.etDashSearch)).perform(replaceText("query"));
        onView(withId(R.id.ibPanelDashClose)).perform(click());
        onView(withId(R.id.lalBfb)).perform(click());
        onView(withId(R.id.etDashSearch)).check(matches(withText("")));
    }

    @Test
    public void appGridRestoredAfterDashClosedAndReopened() {
        onView(withId(R.id.lalBfb)).perform(click());
        onView(withId(R.id.etDashSearch)).perform(replaceText("filter"));
        pressBack();
        onView(withId(R.id.lalBfb)).perform(click());
        // After reopening, LensManager shows apps container (empty search on open)
        onView(withId(R.id.llDashHomeAppsContainer)).check(matches(isDisplayed()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static class BfbVisibleIdlingResource implements IdlingResource {
        private final View bfbView;
        private ResourceCallback callback;

        BfbVisibleIdlingResource(View bfbView) { this.bfbView = bfbView; }

        @Override public String getName() { return "BfbVisible"; }

        @Override
        public boolean isIdleNow() {
            boolean idle = bfbView != null && bfbView.getVisibility() == View.VISIBLE;
            if (idle && callback != null) callback.onTransitionToIdle();
            return idle;
        }

        @Override
        public void registerIdleTransitionCallback(ResourceCallback cb) { this.callback = cb; }
    }
}
