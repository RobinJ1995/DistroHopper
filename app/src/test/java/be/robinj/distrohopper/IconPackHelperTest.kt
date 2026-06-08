package be.robinj.distrohopper

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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

    @Test fun fallbackWrapsOriginalDrawable() {
        val drawable = ColorDrawable(Color.RED)
        assertSame(drawable, helper.getFallbackIcon(drawable).drawable)
    }
}
