package be.robinj.distrohopper.desktop.dash.lens

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Lens keys are written to disk, so changing one silently drops that lens from
 * every user's enabled list. These tests are here to make such a change
 * deliberate rather than a side effect of a rename or a refactor.
 */
@RunWith(RobolectricTestRunner::class)
class LensKeyTest {
    private lateinit var application: Application

    @Before fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        application.getSharedPreferences(Preferences.LENSES, 0).edit().clear().commit()
    }

    private fun manager() = LensManager(application, null, null, null, null)

    @Test fun keysAreExactlyTheseAndMustNotChange() {
        assertEquals(
            setOf(
                "DuckDuckGo",
                "FDroid",
                "GitHub",
                "GooglePlayStore",
                "InstalledApps",
                "LocalFiles_v2",
            ),
            manager().availableLenses.keys,
        )
    }

    /** The map is built from each lens's own key, so the two can never disagree. */
    @Test fun everyLensIsRegisteredUnderItsOwnKey() {
        manager().availableLenses.forEach { (key, lens) -> assertEquals(key, lens.key) }
    }

    /** A display name is free to change; the key it persists under is not. */
    @Test fun keysAreIndependentOfDisplayNames() {
        val fdroid = manager().availableLenses["FDroid"]!!

        assertEquals("F-Droid", fdroid.getName())
        assertEquals("FDroid", fdroid.key)
    }

    @Test fun keysSurviveASaveLoadRoundTrip() {
        manager().apply {
            enableLens("GitHub")
            enableLens("LocalFiles_v2")
            saveEnabledLenses()
        }

        assertEquals(
            listOf("InstalledApps", "GitHub", "LocalFiles_v2"),
            manager().enabledLenses.map { it.key },
        )
    }

    /**
     * The MediaStore-backed lens retired under its old key; an install carrying
     * it loses that lens and keeps everything else.
     */
    @Test fun theRetiredLocalFilesKeyIsDroppedWithoutDisturbingOtherLenses() {
        Preferences.getSharedPreferences(application, Preferences.LENSES).edit()
            .putString("0", "InstalledApps")
            .putString("1", "LocalFiles")
            .putString("2", "GitHub")
            .commit()

        val manager = manager()

        assertEquals(listOf("InstalledApps", "GitHub"), manager.enabledLenses.map { it.key })
        assertFalse(manager.isLensEnabled("LocalFiles_v2"))
        assertNull(manager.availableLenses["LocalFiles"])
    }

    @Test fun theNewLocalFilesKeyIsAvailableToEnable() {
        val manager = manager()

        assertTrue(manager.availableLenses.containsKey("LocalFiles_v2"))

        manager.enableLens("LocalFiles_v2")

        assertTrue(manager.isLensEnabled("LocalFiles_v2"))
    }
}
