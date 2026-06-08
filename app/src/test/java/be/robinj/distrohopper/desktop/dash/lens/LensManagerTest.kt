package be.robinj.distrohopper.desktop.dash.lens

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LensManagerTest {
    private lateinit var context: Context

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Preferences.getSharedPreferences(context, Preferences.PREFERENCES).edit().clear().commit()
        Preferences.getSharedPreferences(context, Preferences.LENSES).edit().clear().commit()
    }

    private fun manager() = LensManager(context, null, null, null, null)

    @Test fun exposesAllBuiltInLenses() {
        assertEquals(
            setOf("AskUbuntu", "DuckDuckGo", "GitHub", "InstalledApps", "LocalFiles", "Reddit", "ServerFault", "StackOverflow", "SuperUser"),
            manager().availableLenses.keys,
        )
    }

    @Test fun defaultsToInstalledAppsAndLocalFiles() {
        assertEquals(listOf("InstalledApps", "LocalFiles"), manager().enabledLenses.map { it.javaClass.simpleName })
    }

    @Test fun defaultMaxResultsIsTen() = assertEquals(10, manager().maxResultsPerLens)

    @Test fun configuredMaxResultsIsLoaded() {
        Preferences.getSharedPreferences(context).edit()
            .putString(Preference.DASH_SEARCH_LENSES_MAX_RESULTS.getName(), "25").commit()
        assertEquals(25, manager().maxResultsPerLens)
    }

    @Test fun enablingLensAddsItOnce() {
        val manager = manager(); manager.enableLens("GitHub"); manager.enableLens("GitHub")
        assertEquals(1, manager.enabledLenses.count { it.javaClass.simpleName == "GitHub" })
    }

    @Test fun enablingUnknownLensIsIgnored() {
        val manager = manager(); val before = manager.enabledLenses.toList(); manager.enableLens("Missing")
        assertEquals(before, manager.enabledLenses)
    }

    @Test fun disablingLensRemovesIt() {
        val manager = manager(); manager.disableLens("InstalledApps")
        assertFalse(manager.isLensEnabled("InstalledApps"))
    }

    @Test fun enabledLensesPersistAcrossManagers() {
        manager().apply { disableLens("LocalFiles"); enableLens("GitHub") }
        assertEquals(listOf("InstalledApps", "GitHub"), manager().enabledLenses.map { it.javaClass.simpleName })
    }

    @Test fun sortEnabledLensesUsesReferenceOrderAndDropsMissingItems() {
        val manager = manager(); manager.enableLens("GitHub")
        val reference = listOf(manager.availableLenses["GitHub"]!!, manager.availableLenses["InstalledApps"]!!)
        manager.sortEnabledLenses(reference)
        assertEquals(listOf("GitHub", "InstalledApps"), manager.enabledLenses.map { it.javaClass.simpleName })
    }

    @Test fun saveEnabledLensesWritesContiguousIndexes() {
        val manager = manager(); manager.enableLens("GitHub"); manager.saveEnabledLenses()
        val prefs = Preferences.getSharedPreferences(context, Preferences.LENSES)
        assertEquals("InstalledApps", prefs.getString("0", null))
        assertEquals("LocalFiles", prefs.getString("1", null))
        assertEquals("GitHub", prefs.getString("2", null))
        assertNull(prefs.getString("3", null))
    }
}
