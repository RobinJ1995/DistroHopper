package be.robinj.distrohopper.desktop.dash.lens

import android.app.Application
import android.content.Context
import android.widget.LinearLayout
import android.widget.ListView
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.Permission
import be.robinj.distrohopper.R
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class LensManagerTest {
    private lateinit var context: Context

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Preferences.getSharedPreferences(context, Preferences.PREFERENCES).edit().clear().commit()
        Preferences.getSharedPreferences(context, Preferences.LENSES).edit().clear().commit()
        // The historical defaults assume storage access; the permissionless
        // default behaviour is covered by LensManagerDefaultsTest //
        Shadows.shadowOf(context as Application).grantPermissions(*Permission.storagePermissions())
    }

    private fun manager() = LensManager(context, null, null, null, null)

    @Test fun exposesAllBuiltInLenses() {
        assertEquals(
            setOf("DuckDuckGo", "FDroid", "GitHub", "GooglePlayStore", "InstalledApps", "LocalFiles"),
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

    @Test fun constructionToleratesMissingLensesContainer() {
        // The apps container being present must not make the constructor dereference the
        // (absent) lenses container.
        LensManager(context, LinearLayout(context), null, null, null)
    }

    @Test fun activateFirstResultForwardsUrlAndObjToTheLens() {
        val manager = manager()
        val lens = RecordingLens(context)
        val obj = Any()
        val result = LensSearchResult(context, "name", "https://example.com", null, obj)
        setResults(manager, mutableListOf(LensSearchResultCollection(lens, mutableListOf(result))))

        assertTrue(manager.activateFirstResult())
        assertEquals("https://example.com", lens.clickedUrl)
        assertSame(obj, lens.clickedObj)
    }

    @Test fun activateFirstResultReturnsFalseWhenThereAreNoResults() {
        assertFalse(manager().activateFirstResult())
    }

    @Test fun activateFirstResultSkipsErrorAndEmptySectionsForTheFirstRealResult() {
        val manager = manager()
        val errorLens = RecordingLens(context)
        val emptyLens = RecordingLens(context)
        val hitLens = RecordingLens(context)
        val result = LensSearchResult(context, "name", "https://hit.example", null, null)
        setResults(manager, mutableListOf(
            LensSearchResultCollection(errorLens, RuntimeException("boom")),
            LensSearchResultCollection(emptyLens, mutableListOf()),
            LensSearchResultCollection(hitLens, mutableListOf(result)),
        ))

        assertTrue(manager.activateFirstResult())
        assertNull(errorLens.clickedUrl)
        assertNull(emptyLens.clickedUrl)
        assertEquals("https://hit.example", hitLens.clickedUrl)
    }

    private fun setResults(manager: LensManager, results: MutableList<LensSearchResultCollection>) {
        val field = LensManager::class.java.getDeclaredField("results")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val backing = field.get(manager) as MutableList<LensSearchResultCollection>
        backing.clear()
        backing.addAll(results)
    }

    private class RecordingLens(context: Context) : Lens(context) {
        var clickedUrl: String? = null
        var clickedObj: Any? = null

        override val type = LensType.LOCAL
        override fun getName() = "Recording"
        override fun getDescription() = "Records onClick for tests"
        override suspend fun search(query: String, maxResults: Int, emitter: LensResultEmitter) {}

        override fun onClick(url: String, obj: Any?) {
            this.clickedUrl = url
            this.clickedObj = obj
        }
    }

    @Test fun lensResultsViewIsResolvedFromTheLensesContainer() {
        val lensesContainer = LinearLayout(context)
        val lensResults = ListView(context).apply { id = R.id.lvDashHomeLensResults }
        lensesContainer.addView(lensResults)

        val manager = LensManager(context, null, lensesContainer, null, null)

        val field = LensManager::class.java.getDeclaredField("lvDashHomeLensResults")
        field.isAccessible = true
        assertSame(lensResults, field.get(manager))
    }
}
