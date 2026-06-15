package be.robinj.distrohopper

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.R
import be.robinj.distrohopper.cache.AppIconCache
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.PreferencesActivity
import be.robinj.distrohopper.preferences.Preferences
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class AppManagerTest {
    private lateinit var scenario: ActivityScenario<HomeActivity>

    @Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
    @After fun tearDown() { scenario.close() }

    private fun withManager(block: (AppManager) -> Unit) {
        scenario.onActivity { block(it.appManager) }
    }
    private fun AppManager.unpinned() = firstOrNull { !isPinned(it) }
    private fun AppManager.settingsShortcut() = ActivityTestSupport.settingsShortcut(this.context)

    private fun cacheAnIcon(key: String): AppIconCache {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val cache = AppIconCache(ctx)
        cache.put(key, BitmapDrawable(ctx.resources,
            Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)))
        return cache
    }

    private fun cacheContains(key: String): Boolean =
        AppIconCache(ApplicationProvider.getApplicationContext()).containsKey(key)

    @Test fun changingTheIconConfigPurgesTheIconCache() = withManager { manager ->
        val key = "purge-me"
        assertTrue(cacheAnIcon(key).containsKey(key))

        // A new shape makes the icon config signature no longer match the cache. //
        Preferences.getSharedPreferences(manager.context).edit()
            .putString(Preference.ICON_SHAPE.getName(), "circle").commit()
        manager.iconRenderer // rebuilds the renderer, which reconciles the cache

        assertFalse(cacheContains(key))
    }

    @Test fun unchangedIconConfigKeepsTheIconCache() = withManager { manager ->
        val key = "keep-me"
        assertTrue(cacheAnIcon(key).containsKey(key))

        manager.iconRenderer // same config as at launch: nothing to reconcile

        assertTrue(cacheContains(key))
    }

    @Test fun everyAppHasNonEmptyPackageName() = withManager { manager ->
        manager.forEach { assertTrue(it.packageName.isNotEmpty()) }
    }

    @Test fun everyAppHasNonEmptyLabel() = withManager { manager ->
        manager.forEach { assertTrue(it.label.isNotEmpty()) }
    }

    @Test fun getPackageAndActivityNameContainsBothParts() = withManager {
        val app = it[0]
        assertEquals("${app.packageName}\n${app.activityName}", app.packageAndActivityName)
    }

    @Test fun appEqualsItself() = withManager { assertEquals(it[0], it[0]) }

    @Test fun appsWithDifferentIdentitiesAreNotEqual() = withManager { assertNotEquals(it[0], it[1]) }

    @Test fun appDoesNotEqualNonAppObject() = withManager {
        assertNotEquals(it[0], "a string")
        assertNotEquals(it[0], null)
    }

    @Test fun equalAppsHaveEqualHashCodes() = withManager { manager ->
        val app = manager[0]
        val sameIdentity = App(
            manager.context,
            manager,
            ActivityTestSupport.resolveInfo(app.packageName, app.activityName, app.label),
        )

        assertNotSame(app, sameIdentity)
        assertEquals(app, sameIdentity)
        assertEquals(app.hashCode(), sameIdentity.hashCode())
    }

    @Test fun installedAppsAreSortedAlphabetically() = withManager {
        assertEquals(it.installedApps.sortedBy { app -> app.label.lowercase() }, it.installedApps)
    }

    @Test fun searchEmptyPatternReturnsAllApps() = withManager { assertEquals(it.size(), it.search("").size) }

    @Test fun searchWithNoMatchReturnsEmpty() = withManager { assertTrue(it.search("xyzqvnomatch123").isEmpty()) }

    @Test fun searchIsCaseInsensitive() = withManager {
        assertEquals(it.search("alpha"), it.search("ALPHA"))
    }

    @Test fun searchMatchesByLabelPrefix() = withManager { assertTrue(it.search("Alp").contains(it[0])) }

    @Test fun searchResultsAreASubsetOfAllApps() = withManager { manager ->
        assertTrue(manager.installedApps.containsAll(manager.search("a")))
    }

    @Test fun searchWithMaxResultsLimitsOutput() = withManager { assertTrue(it.search("a", 2).size <= 2) }

    @Test fun searchFindsSettingsShortcutByPrefix() = withManager { manager ->
        assertEquals(listOf(manager.settingsShortcut()), manager.search("DistroHopper"))
    }

    @Test fun fullSearchFindsInfixMatches() = withManager { manager ->
        val settingsLabel = manager.context.getString(R.string.shortcut_label_distrohopper_settings)
        assertEquals(listOf(settingsLabel, "Settings"), manager.search("ting").map(App::getLabel))
    }

    @Test fun distroHopperSettingsShortcutReplacesOwnLauncherApp() = withManager { manager ->
        val packageName = ApplicationProvider.getApplicationContext<android.app.Application>().packageName

        assertNull(manager.findAppByPackageAndActivityName(packageName, HomeActivity::class.java.name))
        assertNotNull(manager.findAppByPackageAndActivityName(packageName, PreferencesActivity::class.java.name))
        assertTrue(manager.installedApps.any {
            it.label == manager.context.getString(R.string.shortcut_label_distrohopper_settings)
        })
    }

    @Test fun prefixOnlySearchRejectsInfixMatches() {
        scenario.onActivity { activity ->
            Preferences.getSharedPreferences(activity).edit()
                .putBoolean(Preference.DASH_SEARCH_FULL.getName(), false).commit()
            assertTrue(activity.appManager.search("ting").isEmpty())
        }
    }

    @Test fun pinningAnAppMarksItAsPinned() = withManager {
        val app = requireNotNull(it.unpinned()); assertTrue(it.pin(app, false, false)); assertTrue(it.isPinned(app))
    }

    @Test fun pinningIncreasesThePinnedCount() = withManager {
        val before = it.pinned.size; it.pin(requireNotNull(it.unpinned()), false, false); assertEquals(before + 1, it.pinned.size)
    }

    @Test fun pinningAlreadyPinnedAppIsIdempotent() = withManager {
        val app = requireNotNull(it.unpinned()); it.pin(app, false, false); assertFalse(it.pin(app, false, false))
    }

    @Test fun movePinnedAppChangesOrder() = withManager {
        val first = it[0]; val second = it[1]
        it.pin(first, false, false); it.pin(second, false, false); it.movePinnedApp(0, 1)
        assertEquals(listOf(second, first), it.pinned)
    }

    @Test fun settingsShortcutCanBePinnedMovedAndPersisted() = withManager { manager ->
        val alpha = manager.findAppsByPackageName("com.example.alpha").first()
        val settings = manager.settingsShortcut()

        manager.pin(alpha, false, false)
        manager.pin(settings, false, false)
        manager.movePinnedApp(1, 0)
        manager.savePinnedApps()

        assertEquals(listOf(settings, alpha), manager.pinned)
        val pinnedPrefs = Preferences.getSharedPreferences(manager.context, Preferences.PINNED_APPS)
        val desktops = PinnedAppsStorage.read(pinnedPrefs)
        assertEquals(
            listOf(settings.packageAndActivityName, alpha.packageAndActivityName), desktops[0])
    }

    @Test fun findAppByPackageAndActivityNameReturnsCorrectApp() = withManager {
        val expected = it[0]; assertEquals(expected, it.findAppByPackageAndActivityName(expected.packageName, expected.activityName))
    }

    @Test fun findAppByPackageAndActivityNameReturnsNullForUnknown() = withManager {
        assertNull(it.findAppByPackageAndActivityName("no.such.package", "NoSuchActivity"))
    }

    @Test fun findAppsByPackageNameReturnsOnlyMatchingApps() = withManager {
        val expected = it[0]; assertEquals(listOf(expected), it.findAppsByPackageName(expected.packageName))
    }

    @Test fun installedAppsMapUsesCombinedIdentity() = withManager {
        assertEquals(it.size(), it.installedAppsMap.size); assertSame(it[0], it.installedAppsMap[it[0].packageAndActivityName])
    }

    @Test fun installedAppsMapIncludesSettingsShortcutIdentity() = withManager { manager ->
        val settings = manager.settingsShortcut()
        assertSame(settings, manager.installedAppsMap[settings.packageAndActivityName])
    }

    @Test fun packageQueryCanBeFiltered() = withManager {
        assertEquals(1, it.queryInstalledApps("com.example.alpha").size)
    }

    @Test fun refreshDashSortOrderReordersForUsageBasedOrder() {
        scenario.onActivity { activity ->
            val manager = activity.appManager
            Preferences.getSharedPreferences(activity, Preferences.APP_USAGE).edit().clear().commit()
            // Make the last app in the current (alphabetical) order the most used //
            val last = manager.installedApps.last()
            AppUsageStats(activity).apply {
                recordLaunch(last.profileScopedKey)
                recordLaunch(last.profileScopedKey)
            }
            Preferences.getSharedPreferences(activity).edit()
                .putString(Preference.APP_SORT_ORDER.getName(), "most_used").commit()

            manager.refreshDashSortOrder()

            assertEquals(last, manager.installedApps.first())
        }
    }

    @Test fun refreshDashSortOrderIsANoOpForAlphabeticalOrder() {
        scenario.onActivity { activity ->
            val manager = activity.appManager
            Preferences.getSharedPreferences(activity, Preferences.APP_USAGE).edit().clear().commit()
            AppUsageStats(activity).recordLaunch(manager.installedApps.last().profileScopedKey)
            Preferences.getSharedPreferences(activity).edit()
                .putString(Preference.APP_SORT_ORDER.getName(), "alphabetical").commit()
            val before = manager.installedApps.toList()

            manager.refreshDashSortOrder()

            assertEquals(before, manager.installedApps)
            assertEquals(manager.installedApps.sortedBy { it.label.lowercase() }, manager.installedApps)
        }
    }
}
