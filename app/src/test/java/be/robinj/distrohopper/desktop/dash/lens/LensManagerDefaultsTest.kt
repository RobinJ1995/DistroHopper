package be.robinj.distrohopper.desktop.dash.lens

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LensManagerDefaultsTest {
    private lateinit var application: Application

    @Before fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        application.getSharedPreferences(Preferences.LENSES, 0).edit().clear().commit()
    }

    private fun manager() = LensManager(application, null, null, null, null)

    @Test fun installedAppsIsOnByDefault() {
        assertTrue(manager().isLensEnabled("InstalledApps"))
    }

    /** It finds nothing until folders are granted, so it must not arrive switched on. */
    @Test fun localFilesIsNotOnByDefault() {
        assertFalse(manager().isLensEnabled("LocalFiles_v2"))
    }

    @Test fun networkLensesAreNotOnByDefault() {
        val manager = manager()

        assertFalse(manager.isLensEnabled("DuckDuckGo"))
        assertFalse(manager.isLensEnabled("FDroid"))
        assertFalse(manager.isLensEnabled("GitHub"))
        assertFalse(manager.isLensEnabled("GooglePlayStore"))
    }
}
