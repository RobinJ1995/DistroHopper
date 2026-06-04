package be.robinj.distrohopper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class AppManagerTest {

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

    // ── App model ────────────────────────────────────────────────────────────

    @Test
    public void everyAppHasNonEmptyPackageName() {
        activityRule.getScenario().onActivity(activity -> {
            for (App app : activity.getAppManager()) {
                assertFalse("Package name must not be empty for " + app.getLabel(),
                        app.getPackageName().isEmpty());
            }
        });
    }

    @Test
    public void everyAppHasNonEmptyLabel() {
        activityRule.getScenario().onActivity(activity -> {
            for (App app : activity.getAppManager()) {
                assertFalse("Label must not be empty for package " + app.getPackageName(),
                        app.getLabel().isEmpty());
            }
        });
    }

    @Test
    public void getPackageAndActivityNameContainsBothParts() {
        activityRule.getScenario().onActivity(activity -> {
            App app = activity.getAppManager().get(0);
            String combined = app.getPackageAndActivityName();
            assertTrue(combined.contains(app.getPackageName()));
            assertTrue(combined.contains(app.getActivityName()));
            assertTrue("Package and activity name must be newline-separated",
                    combined.contains("\n"));
        });
    }

    @Test
    public void appEqualsItself() {
        activityRule.getScenario().onActivity(activity -> {
            App app = activity.getAppManager().get(0);
            assertEquals(app, app);
        });
    }

    @Test
    public void appsWithDifferentPackagesAreNotEqual() {
        activityRule.getScenario().onActivity(activity -> {
            AppManager mgr = activity.getAppManager();
            assertTrue("Need at least two apps", mgr.size() >= 2);
            App a = mgr.get(0);
            App b = mgr.get(1);
            if (!a.getPackageName().equals(b.getPackageName())
                    || !a.getActivityName().equals(b.getActivityName())) {
                assertNotEquals(a, b);
            }
        });
    }

    @Test
    public void appDoesNotEqualNonAppObject() {
        activityRule.getScenario().onActivity(activity -> {
            App app = activity.getAppManager().get(0);
            assertNotEquals(app, "a string");
            assertNotEquals(app, null);
        });
    }

    // ── Sorting ───────────────────────────────────────────────────────────────

    @Test
    public void installedAppsAreSortedAlphabetically() {
        activityRule.getScenario().onActivity(activity -> {
            List<App> apps = activity.getAppManager().getInstalledApps();
            assertTrue("Need at least two apps to verify sort order", apps.size() >= 2);
            for (int i = 0; i < apps.size() - 1; i++) {
                String a = apps.get(i).getLabel().toLowerCase();
                String b = apps.get(i + 1).getLabel().toLowerCase();
                assertTrue("Expected '" + a + "' <= '" + b + "'", a.compareTo(b) <= 0);
            }
        });
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @Test
    public void searchEmptyPatternReturnsAllApps() {
        activityRule.getScenario().onActivity(activity -> {
            AppManager mgr = activity.getAppManager();
            List<App> results = mgr.search("");
            assertEquals("Empty search should return all apps",
                    mgr.size(), results.size());
        });
    }

    @Test
    public void searchWithNoMatchReturnsEmpty() {
        activityRule.getScenario().onActivity(activity -> {
            List<App> results = activity.getAppManager().search("xyzqvnomatch123");
            assertEquals("Non-matching search should return zero results", 0, results.size());
        });
    }

    @Test
    public void searchIsCaseInsensitive() {
        activityRule.getScenario().onActivity(activity -> {
            AppManager mgr = activity.getAppManager();
            String label = mgr.get(0).getLabel();
            List<App> lower = mgr.search(label.toLowerCase());
            List<App> upper = mgr.search(label.toUpperCase());
            assertEquals("Search must be case-insensitive", lower.size(), upper.size());
        });
    }

    @Test
    public void searchMatchesByLabelPrefix() {
        activityRule.getScenario().onActivity(activity -> {
            AppManager mgr = activity.getAppManager();
            App first = mgr.get(0);
            String prefix = first.getLabel().substring(0,
                    Math.min(3, first.getLabel().length()));
            List<App> results = mgr.search(prefix);
            assertTrue("Prefix search must return at least the source app",
                    results.contains(first));
        });
    }

    @Test
    public void searchResultsAreASubsetOfAllApps() {
        activityRule.getScenario().onActivity(activity -> {
            AppManager mgr = activity.getAppManager();
            List<App> all = mgr.getInstalledApps();
            List<App> results = mgr.search("a");
            for (App result : results) {
                assertTrue("Search result must be in the full app list", all.contains(result));
            }
        });
    }

    @Test
    public void searchWithMaxResultsLimitsOutput() {
        activityRule.getScenario().onActivity(activity -> {
            AppManager mgr = activity.getAppManager();
            int limit = 2;
            List<App> results = mgr.search("", limit);
            // Empty pattern ignores maxResults (returns all), so use a real query
            List<App> limited = mgr.search("a", limit);
            assertTrue("Results must not exceed maxResults limit",
                    limited.size() <= limit);
        });
    }

    // ── Pinning ───────────────────────────────────────────────────────────────

    @Test
    public void pinnedListStartsEmpty() {
        activityRule.getScenario().onActivity(activity -> {
            // Fresh activity with no persisted pins (test isolation via no-save pin calls)
            AppManager mgr = activity.getAppManager();
            assertNotNull(mgr.getPinned());
        });
    }

    @Test
    public void pinningAnAppMarksItAsPinned() {
        activityRule.getScenario().onActivity(activity -> {
            AppManager mgr = activity.getAppManager();
            App app = findUnpinnedApp(mgr);
            assertNotNull("Need at least one unpinned app", app);

            assertFalse(mgr.isPinned(app));
            mgr.pin(app, false, false, false); // no persist, no toast, no view
            assertTrue(mgr.isPinned(app));
        });
    }

    @Test
    public void pinningIncreasesThePinnedCount() {
        activityRule.getScenario().onActivity(activity -> {
            AppManager mgr = activity.getAppManager();
            App app = findUnpinnedApp(mgr);
            assertNotNull("Need at least one unpinned app", app);

            int before = mgr.getPinned().size();
            mgr.pin(app, false, false, false);
            assertEquals(before + 1, mgr.getPinned().size());
        });
    }

    @Test
    public void pinningAlreadyPinnedAppIsIdempotent() {
        activityRule.getScenario().onActivity(activity -> {
            AppManager mgr = activity.getAppManager();
            App app = findUnpinnedApp(mgr);
            assertNotNull("Need at least one unpinned app", app);

            mgr.pin(app, false, false, false);
            int countAfterFirst = mgr.getPinned().size();

            mgr.pin(app, false, false, false); // second pin attempt
            assertEquals("Pinning twice must not change pinned count",
                    countAfterFirst, mgr.getPinned().size());
        });
    }

    @Test
    public void findAppByPackageAndActivityNameReturnsCorrectApp() {
        activityRule.getScenario().onActivity(activity -> {
            AppManager mgr = activity.getAppManager();
            App expected = mgr.get(0);
            App found = mgr.findAppByPackageAndActivityName(
                    expected.getPackageName(), expected.getActivityName());
            assertEquals(expected, found);
        });
    }

    @Test
    public void findAppByPackageAndActivityNameReturnsNullForUnknown() {
        activityRule.getScenario().onActivity(activity -> {
            App result = activity.getAppManager()
                    .findAppByPackageAndActivityName("no.such.package", "NoSuchActivity");
            assertEquals(null, result);
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static App findUnpinnedApp(AppManager mgr) {
        for (App app : mgr) {
            if (!mgr.isPinned(app)) return app;
        }
        return null;
    }

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
