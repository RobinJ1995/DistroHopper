package be.robinj.distrohopper.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IconRendererTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val size = 48
    private val accent = Color.BLUE
    private val themedBg = Color.DKGRAY

    private fun config(shape: IconShape, themed: Boolean = false) =
        IconConfig(shape, themed, this.size, this.accent, this.themedBg)

    private fun renderer(shape: IconShape, themed: Boolean = false) =
        IconRenderer(this.context, this.config(shape, themed))

    private fun adaptive(background: Int, foreground: Int): AdaptiveIconDrawable =
        AdaptiveIconDrawable(ColorDrawable(background), ColorDrawable(foreground))

    private fun bitmapOf(drawable: Drawable): Bitmap = (drawable as BitmapDrawable).bitmap

    private fun centre(drawable: Drawable): Int = this.bitmapOf(drawable).let { it.getPixel(it.width / 2, it.height / 2) }

    private fun corner(drawable: Drawable): Int = this.bitmapOf(drawable).getPixel(0, 0)

    @Test fun adaptiveIconRendersToASquareBitmapOfTheConfiguredSize() {
        val out = this.renderer(IconShape.SYSTEM).render(this.adaptive(Color.RED, Color.GREEN))

        val bitmap = this.bitmapOf(out)
        assertEquals(this.size, bitmap.width)
        assertEquals(this.size, bitmap.height)
    }

    @Test fun foregroundIsCompositedOverBackground() {
        val out = this.renderer(IconShape.SQUARE).render(this.adaptive(Color.RED, Color.GREEN))
        assertEquals(Color.GREEN, this.centre(out))
    }

    @Test fun squareKeepsItsCorners() {
        val out = this.renderer(IconShape.SQUARE).render(this.adaptive(Color.RED, Color.GREEN))
        assertEquals(Color.GREEN, this.corner(out))
    }

    @Test fun roundedShapesClipTheCornersButKeepTheCentre() {
        for (shape in listOf(IconShape.CIRCLE, IconShape.SQUIRCLE, IconShape.ROUNDED_SQUARE)) {
            val out = this.renderer(shape).render(this.adaptive(Color.RED, Color.GREEN))
            assertEquals("$shape corner should be transparent", 0, Color.alpha(this.corner(out)))
            assertEquals("$shape centre should be opaque foreground", Color.GREEN, this.centre(out))
        }
    }

    @Test fun legacyDrawableIsReturnedUntouchedEvenWithAShapeSelected() {
        val legacy = ColorDrawable(Color.MAGENTA)
        assertSame(legacy, this.renderer(IconShape.CIRCLE).render(legacy))

        val bitmap = BitmapDrawable(this.context.resources,
            Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888))
        assertSame(bitmap, this.renderer(IconShape.CIRCLE).render(bitmap))
    }

    @Test fun renderingIsIdempotentOnItsOwnOutput() {
        val renderer = this.renderer(IconShape.CIRCLE)
        val once = renderer.render(this.adaptive(Color.RED, Color.GREEN))
        // The output is a plain BitmapDrawable, so a second pass leaves it alone.
        assertSame(once, renderer.render(once))
    }

    @Test fun themedOffRendersTheForeground() {
        val out = this.renderer(IconShape.SQUARE, themed = false).render(this.adaptive(Color.RED, Color.GREEN))
        assertEquals(Color.GREEN, this.centre(out))
    }

    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    @Test fun themedTintsTheMonochromeLayerWithTheAccent() {
        val adaptive = AdaptiveIconDrawable(
            ColorDrawable(Color.RED), ColorDrawable(Color.GREEN), ColorDrawable(Color.WHITE))
        val out = this.renderer(IconShape.SQUARE, themed = true).render(adaptive)
        assertEquals(this.accent, this.centre(out))
    }

    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    @Test fun themedFallsBackToStandardCompositeWhenThereIsNoMonochromeLayer() {
        val out = this.renderer(IconShape.SQUARE, themed = true).render(this.adaptive(Color.RED, Color.GREEN))
        assertEquals(Color.GREEN, this.centre(out))
    }

    @Config(sdk = [Build.VERSION_CODES.S_V2]) // API 32, below themed-icon support
    @Test fun themedIsIgnoredBelowApi33() {
        val adaptive = this.adaptive(Color.RED, Color.GREEN)
        val out = this.renderer(IconShape.SQUARE, themed = true).render(adaptive)
        assertEquals(Color.GREEN, this.centre(out))
        assertTrue(out is BitmapDrawable)
    }
}
