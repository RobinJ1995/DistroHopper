package be.robinj.distrohopper.desktop.dash.lens

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.Permission
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class LensManagerDefaultsTest {
    private lateinit var application: Application

    @Before fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        application.getSharedPreferences(Preferences.LENSES, 0).edit().clear().commit()
    }

    @Test fun localFilesIsNotADefaultLensWithoutStoragePermission() {
        val lensManager = LensManager(application, null, null, null, null)

        assertFalse(lensManager.isLensEnabled("LocalFiles"))
        assertTrue(lensManager.isLensEnabled("InstalledApps"))
    }

    @Test fun localFilesIsADefaultLensOnceStoragePermissionIsGranted() {
        Shadows.shadowOf(application).grantPermissions(*Permission.storagePermissions())

        val lensManager = LensManager(application, null, null, null, null)

        assertTrue(lensManager.isLensEnabled("LocalFiles"))
        assertTrue(lensManager.isLensEnabled("InstalledApps"))
    }
}
