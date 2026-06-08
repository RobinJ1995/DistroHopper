package be.robinj.distrohopper

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class PermissionTest {
    private lateinit var application: Application

    @Before fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        Shadows.shadowOf(application).denyPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    @Test fun checkReturnsFalseWhenPermissionIsDenied() {
        assertFalse(Permission(application, Manifest.permission.READ_EXTERNAL_STORAGE).check())
    }

    @Test fun checkReturnsTrueWhenPermissionIsGranted() {
        Shadows.shadowOf(application).grantPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
        assertTrue(Permission(application, Manifest.permission.READ_EXTERNAL_STORAGE).check())
    }
}
