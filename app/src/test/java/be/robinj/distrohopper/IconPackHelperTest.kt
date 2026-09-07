package be.robinj.distrohopper

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class IconPackHelperTest {
    private lateinit var context: Context
    private lateinit var helper: IconPackHelper

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        helper = IconPackHelper(context)
    }

    @Test fun startsWithoutLoadedIconPack() = assertFalse(helper.isIconPackLoaded)

    @Test fun loadingEmptyPackageKeepsIconPackUnloaded() {
        helper.loadIconPack("")
        assertFalse(helper.isIconPackLoaded)
    }

    @Test fun unloadedPackCannotResolveNamedIcon() = assertNull(helper.getIcon("missing"))

    /**
     * A pack can stop resolving at any time -- uninstalled, mid-update, on storage that
     * isn't mounted yet. That is an expected condition, not an error to throw or report.
     */
    @Test fun anUnresolvablePackLeavesTheIconPackUnloaded() {
        helper.loadIconPack("ddt.free.icon.packs")

        assertFalse(helper.isIconPackLoaded)
        assertNull(helper.getIcon("anything"))
    }

    /** Installed, but carrying nothing we can use: unloaded too, and equally not an error. */
    @Test fun anInstalledPackWithoutAnAppfilterStaysUnloaded() {
        helper.loadIconPack(installEmptyPack())

        assertFalse(helper.isIconPackLoaded)
        assertNull(helper.getIcon("anything"))
    }

    /** A failed load must not leave the previous pack's state resolving icons. */
    @Test fun anUnresolvablePackClearsWhatTheLastLoadLeftBehind() {
        helper.loadIconPack(installEmptyPack())
        helper.loadIconPack("ddt.free.icon.packs")

        assertFalse(helper.isIconPackLoaded)
        assertNull(helper.getIcon("anything"))
    }

    /** An icon pack package that resolves but holds no icon resources of its own. */
    private fun installEmptyPack(packageName: String = "fake.icon.pack"): String {
        Shadows.shadowOf(context.packageManager).installPackage(PackageInfo().apply {
            this.packageName = packageName
            applicationInfo = ApplicationInfo().apply { this.packageName = packageName }
        })

        return packageName
    }

    @Test fun fallbackWrapsOriginalDrawable() {
        val drawable = ColorDrawable(Color.RED)
        assertSame(drawable, helper.getFallbackIcon(drawable).drawable)
    }
}
