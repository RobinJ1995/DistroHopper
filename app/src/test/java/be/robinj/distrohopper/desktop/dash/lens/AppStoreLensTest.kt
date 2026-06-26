package be.robinj.distrohopper.desktop.dash.lens

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

/**
 * Covers the [AppStoreLens.isInstalled] helper shared by every app-store lens
 * (Google Play, F-Droid, …); the concrete lenses use it to hide apps the user
 * already has, but the helper itself was only exercised indirectly.
 */
@RunWith(RobolectricTestRunner::class)
class AppStoreLensTest {
    private lateinit var application: Application

    @Before fun setUp() {
        application = ApplicationProvider.getApplicationContext()
    }

    /** Minimal concrete store lens exposing the protected install check. */
    private class ProbeStore(context: Context) : AppStoreLens(context) {
        fun probe(packageName: String) = isInstalled(packageName)
        override fun getName() = "ProbeStore"
        override fun getDescription() = "Test store lens"
        override suspend fun search(query: String, maxResults: Int, emitter: LensResultEmitter) {}
    }

    @Test fun reportsInstalledPackageAsInstalled() {
        val packageInfo = PackageInfo().apply { packageName = "com.example.installed" }
        Shadows.shadowOf(application.packageManager).installPackage(packageInfo)

        assertTrue(ProbeStore(application).probe("com.example.installed"))
    }

    @Test fun reportsAbsentPackageAsNotInstalled() {
        assertFalse(ProbeStore(application).probe("com.example.absent"))
    }
}
