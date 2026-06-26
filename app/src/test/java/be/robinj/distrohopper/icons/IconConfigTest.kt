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

    private fun config(shape: IconShape, tinted: Boolean) =
        IconConfig(shape, tinted, 108, Color.BLUE, Color.DKGRAY, Color.WHITE)

    @Test fun signatureDiffersAcrossShapes() {
        assertNotEquals(
            this.config(IconShape.CIRCLE, false).signature(),
            this.config(IconShape.SQUARE, false).signature())
    }

    @Test fun signatureDiffersAcrossTheTintedFlag() {
        assertNotEquals(
            this.config(IconShape.CIRCLE, false).signature(),
            this.config(IconShape.CIRCLE, true).signature())
    }

    @Test fun signatureDiffersAcrossTheResolvedTintColour() {
        assertNotEquals(
            IconConfig(IconShape.CIRCLE, true, 108, Color.BLUE, Color.DKGRAY, Color.WHITE).signature(),
            IconConfig(IconShape.CIRCLE, true, 108, Color.RED, Color.DKGRAY, Color.WHITE).signature())
    }

    @Test fun signatureIgnoresTintColourWhenTintingIsOff() {
        // With tinting off the resolved colour is irrelevant, so a theme/wallpaper
        // change must not invalidate the cache.
        assertEquals(
            IconConfig(IconShape.CIRCLE, false, 108, Color.BLUE, Color.DKGRAY, Color.WHITE).signature(),
            IconConfig(IconShape.CIRCLE, false, 108, Color.RED, Color.GREEN, Color.YELLOW).signature())
    }

    @Test fun fromPrefsDefaultsToSystemShapeAndTintOff() {
        val config = IconConfig.fromPrefs(this.context)
        assertEquals(IconShape.SYSTEM, config.getShape())
        assertFalse(config.isTintedIcons())
        assertTrue(config.getSizePx() > 0)
    }

    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    @Test fun tintedRenderingSupportedWhenEnabledOnApi33() {
        assertTrue(this.config(IconShape.SYSTEM, true).tintedRenderingSupported())
    }

    @Config(sdk = [Build.VERSION_CODES.S_V2])
    @Test fun tintedRenderingUnsupportedBelowApi33EvenWhenEnabled() {
        assertFalse(this.config(IconShape.SYSTEM, true).tintedRenderingSupported())
    }
}
