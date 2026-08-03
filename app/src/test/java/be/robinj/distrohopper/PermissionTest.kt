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
    // Permission is a generic wrapper; CAMERA is just a stand-in dangerous
    // permission. The app itself declares none. //
    private lateinit var application: Application

    @Before fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        Shadows.shadowOf(application).denyPermissions(Manifest.permission.CAMERA)
    }

    @Test fun checkReturnsFalseWhenPermissionIsDenied() {
        assertFalse(Permission(application, Manifest.permission.CAMERA).check())
    }

    @Test fun checkReturnsTrueWhenPermissionIsGranted() {
        Shadows.shadowOf(application).grantPermissions(Manifest.permission.CAMERA)
        assertTrue(Permission(application, Manifest.permission.CAMERA).check())
    }
}
