package be.robinj.distrohopper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ActivityScenario;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class AppManagerTest {

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

    // ── App model ─────────────────────────────────────────────────────────────

    @Test
    public void everyAppHasNonEmptyPackageName() {
        scenario.onActivity(activity -> {
            for (App app : activity.getAppManager()) {
                assertFalse("Package name must not be empty for " + app.getLabel(),
                        app.getPackageName().isEmpty());
            }
        });
    }

    @Test
    public void everyAppHasNonEmptyLabel() {
        scenario.onActivity(activity -> {
            for (App app : activity.getAppManager()) {
                assertFalse("Label must not be empty for " + app.getPackageName(),
                        app.getLabel().isEmpty());
            }
        });
    }

    @Test
    public void getPackageAndActivityNameContainsBothParts() {
        scenario.onActivity(activity -> {
            App app = activity.getAppManager().get(0);
            String combined = app.getPackageAndActivityName();
            assertTrue(combined.contains(app.getPackageName()));
            assertTrue(combined.contains(app.getActivityName()));
            assertTrue("Must be newline-separated", combined.contains("\n"));
        });
    }

    @Test
    public void appEqualsItself() {
        scenario.onActivity(activity -> {
            App app = activity.getAppManager().get(0);
            assertEquals(app, app);
        });
    }

    @Test
    public void appsWithDifferentIdentitiesAreNotEqual() {
        scenario.onActivity(activity -> {
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
        scenario.onActivity(activity -> {
            App app = activity.getAppManager().get(0);
            assertNotEquals(app, "a string");
            assertNotEquals(app, null);
        });
    }

    // ── Sorting ───────────────────────────────────────────────────────────────

    @Test
    public void installedAppsAreSortedAlphabetically() {
        scenario.onActivity(activity -> {
            List<App> apps = activity.getAppManager().getInstalledApps();
            assertTrue("Need at least two apps", apps.size() >= 2);
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
        scenario.onActivity(activity -> {
            AppManager mgr = activity.getAppManager();
            assertEquals(mgr.size(), mgr.search("").size());
        });
    }

    @Test
    public void searchWithNoMatchReturnsEmpty() {
        scenario.onActivity(activity ->
                assertEquals(0, activity.getAppManager().search("xyzqvnomatch123").size()));
    }

    @Test
    public void searchIsCaseInsensitive() {
        scenario.onActivity(activity -> {
            AppManager mgr = activity.getAppManager();
            String label = mgr.get(0).getLabel();
            assertEquals(mgr.search(label.toLowerCase()).size(),
                         mgr.search(label.toUpperCase()).size());
        });
    }

    @Test
    public void searchMatchesByLabelPrefix() {
        scenario.onActivity(activity -> {
            AppManager mgr = activity.getAppManager();
            App first = mgr.get(0);
            String prefix = first.getLabel().substring(0, Math.min(3, first.getLabel().length()));
            assertTrue(mgr.search(prefix).contains(first));
        });
    }

    @Test
    public void searchResultsAreASubsetOfAllApps() {
        scenario.onActivity(activity -> {
            AppManager mgr = activity.getAppManager();
            List<App> all = mgr.getInstalledApps();
            for (App result : mgr.search("a")) {
                assertTrue("Search result must be in the full app list", all.contains(result));
            }
        });
    }

    @Test
    public void searchWithMaxResultsLimitsOutput() {
        scenario.onActivity(activity -> {
            AppManager mgr = activity.getAppManager();
            assertTrue(mgr.search("a", 2).size() <= 2);
        });
    }

    // ── Pinning ───────────────────────────────────────────────────────────────

    @Test
    public void pinningAnAppMarksItAsPinned() {
        scenario.onActivity(activity -> {
            AppManager mgr = activity.getAppManager();
            App app = findUnpinnedApp(mgr);
            assertNotNull("Need at least one unpinned app", app);
            assertFalse(mgr.isPinned(app));
            mgr.pin(app, false, false, false);
            assertTrue(mgr.isPinned(app));
        });
    }

    @Test
    public void pinningIncreasesThePinnedCount() {
        scenario.onActivity(activity -> {
            AppManager mgr = activity.getAppManager();
            App app = findUnpinnedApp(mgr);
            assertNotNull(app);
            int before = mgr.getPinned().size();
            mgr.pin(app, false, false, false);
            assertEquals(before + 1, mgr.getPinned().size());
        });
    }

    @Test
    public void pinningAlreadyPinnedAppIsIdempotent() {
        scenario.onActivity(activity -> {
            AppManager mgr = activity.getAppManager();
            App app = findUnpinnedApp(mgr);
            assertNotNull(app);
            mgr.pin(app, false, false, false);
            int countAfterFirst = mgr.getPinned().size();
            mgr.pin(app, false, false, false);
            assertEquals(countAfterFirst, mgr.getPinned().size());
        });
    }

    @Test
    public void findAppByPackageAndActivityNameReturnsCorrectApp() {
        scenario.onActivity(activity -> {
            AppManager mgr = activity.getAppManager();
            App expected = mgr.get(0);
            assertEquals(expected,
                    mgr.findAppByPackageAndActivityName(
                            expected.getPackageName(), expected.getActivityName()));
        });
    }

    @Test
    public void findAppByPackageAndActivityNameReturnsNullForUnknown() {
        scenario.onActivity(activity ->
                assertNull(activity.getAppManager()
                        .findAppByPackageAndActivityName("no.such.package", "NoSuchActivity")));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static App findUnpinnedApp(AppManager mgr) {
        for (App app : mgr) {
            if (!mgr.isPinned(app)) return app;
        }
        return null;
    }
}
