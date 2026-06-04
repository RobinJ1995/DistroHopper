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

import androidx.test.core.app.ActivityScenario;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class DashSearchUiTest {

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

    // ── Initial state ──────────────────────────────────────────────────────────

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
        pressBack();
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
        onView(withId(R.id.llDashHomeAppsContainer)).check(matches(isDisplayed()));
    }
}
