package be.robinj.distrohopper.icons

import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class IconConfigTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun config(shape: IconShape, themed: Boolean) =
        IconConfig(shape, themed, 108, Color.BLUE, Color.DKGRAY)

    @Test fun signatureDiffersAcrossShapes() {
        assertNotEquals(
            this.config(IconShape.CIRCLE, false).signature(),
            this.config(IconShape.SQUARE, false).signature())
    }

    @Test fun signatureDiffersAcrossTheThemedFlag() {
        assertNotEquals(
            this.config(IconShape.CIRCLE, false).signature(),
            this.config(IconShape.CIRCLE, true).signature())
    }

    @Test fun fromPrefsDefaultsToSystemShapeAndThemedOff() {
        val config = IconConfig.fromPrefs(this.context)
        assertEquals(IconShape.SYSTEM, config.getShape())
        assertFalse(config.isThemedIcons())
        assertTrue(config.getSizePx() > 0)
    }

    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    @Test fun themedRenderingSupportedWhenEnabledOnApi33() {
        assertTrue(this.config(IconShape.SYSTEM, true).themedRenderingSupported())
    }

    @Config(sdk = [Build.VERSION_CODES.S_V2])
    @Test fun themedRenderingUnsupportedBelowApi33EvenWhenEnabled() {
        assertFalse(this.config(IconShape.SYSTEM, true).themedRenderingSupported())
    }
}
