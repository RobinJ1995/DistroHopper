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

import android.app.Application;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.widget.GridView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class HomeActivityTest {

    private ActivityScenario<HomeActivity> scenario;

    @Before
    public void setUp() {
        seedPackageManager();
        scenario = ActivityScenario.launch(HomeActivity.class);
        Robolectric.flushBackgroundThreadScheduler();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
    }

    @After
    public void tearDown() {
        scenario.close();
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
            GridView grid = (GridView) view;
            assertNotNull("Dash app grid must have an adapter", grid.getAdapter());
            assertTrue("Dash must show at least one installed app",
                    grid.getAdapter().getCount() > 0);
        });
    }

    @Test
    public void searchFieldAcceptsTextInput() {
        onView(withId(R.id.lalBfb)).perform(click());
        onView(withId(R.id.etDashSearch)).perform(replaceText("alpha"));
        onView(withId(R.id.etDashSearch)).check(matches(withText("alpha")));
    }

    @Test
    public void searchFiltersAppResults() {
        onView(withId(R.id.lalBfb)).perform(click());

        // Before searching, the apps container is visible
        onView(withId(R.id.llDashHomeAppsContainer)).check(matches(isDisplayed()));

        // Typing a non-matching query triggers AsyncSearch which hides the apps container
        // and shows the lens results container instead
        onView(withId(R.id.etDashSearch)).perform(replaceText("zzznomatch"));
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        onView(withId(R.id.llDashHomeAppsContainer)).check(matches(not(isDisplayed())));
    }

    @Test
    public void appManagerIsInitialisedAfterLoad() {
        scenario.onActivity(activity ->
                assertNotNull("AppManager must be non-null after async load",
                        activity.getAppManager()));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    static void seedPackageManager() {
        Application app = ApplicationProvider.getApplicationContext();
        ShadowPackageManager spm = Shadows.shadowOf(app.getPackageManager());
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER);

        String[][] entries = {
            {"com.example.alpha",    "AlphaActivity",    "Alpha"},
            {"com.example.beta",     "BetaActivity",     "Beta"},
            {"com.example.gamma",    "GammaActivity",    "Gamma"},
            {"com.example.settings", "SettingsActivity", "Settings"},
            {"com.example.zeta",     "ZetaActivity",     "Zeta"},
        };
        for (String[] e : entries) {
            ActivityInfo ai = new ActivityInfo();
            ai.packageName = e[0];
            ai.name       = e[1];
            ai.nonLocalizedLabel = e[2];
            ai.applicationInfo = new ApplicationInfo();
            ai.applicationInfo.packageName = e[0];
            ai.applicationInfo.enabled = true;

            ResolveInfo ri = new ResolveInfo();
            ri.activityInfo = ai;
            ri.nonLocalizedLabel = e[2];

            spm.addResolveInfoForIntent(launcherIntent, ri);
        }
    }
}
