package be.robinj.distrohopper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageTest {
    private val resources get() = ApplicationProvider.getApplicationContext<android.content.Context>().resources

    private fun solidBitmap(color: Int): Bitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply {
        eraseColor(color)
    }

    @Test fun bitmapDrawableReturnsOriginalBitmap() {
        val bitmap = solidBitmap(Color.RED)
        assertSame(bitmap, Image(BitmapDrawable(resources, bitmap)).toBitmap())
    }

    @Test fun drawableWithDimensionsIsRenderedToBitmap() {
        val drawable = SizedDrawable(Color.BLUE, 12, 8)
        val bitmap = Image(drawable).toBitmap()
        assertEquals(12, bitmap.width); assertEquals(8, bitmap.height)
    }

    @Test fun drawableWithoutDimensionsCannotBecomeBitmap() {
        assertNull(Image(ColorDrawable(Color.RED)).toBitmap())
    }

    @Test fun averageRgbPreservesSolidColourAndRequestedAlpha() {
        val result = Image(BitmapDrawable(resources, solidBitmap(Color.rgb(10, 20, 30)))).getAverageColour(false, 123)
        assertEquals(Color.argb(123, 10, 20, 30), result)
    }

    @Test fun transparentBitmapUsesDarkFallback() {
        val result = Image(BitmapDrawable(resources, solidBitmap(Color.TRANSPARENT))).getAverageColour(false, 200)
        assertEquals(Color.argb(200, 40, 40, 40), result)
    }

    @Test fun grayscaleBitmapUsesLightFallback() {
        val result = Image(BitmapDrawable(resources, solidBitmap(Color.WHITE))).getAverageColour(true, 180)
        assertEquals(Color.argb(180, 240, 240, 240), result)
    }

    @Test fun nonBitmapAverageIsTransparent() {
        assertEquals(Color.TRANSPARENT, Image(ColorDrawable(Color.RED)).getAverageColour(255))
    }

    @Test fun imagesWithSameDrawableAreEqualAndHaveSameHashCode() {
        val drawable = ColorDrawable(Color.RED)
        val first = Image(drawable); val second = Image(drawable)
        assertEquals(first, second); assertEquals(first.hashCode(), second.hashCode())
    }

    @Test fun imageDoesNotEqualDrawableOrNull() {
        val drawable = ColorDrawable(Color.RED); val image = Image(drawable)
        assertNotEquals(image, drawable); assertNotEquals(image, null)
    }

    private class SizedDrawable(
        private val color: Int,
        private val width: Int,
        private val height: Int,
    ) : Drawable() {
        override fun draw(canvas: Canvas) = canvas.drawColor(color)
        override fun setAlpha(alpha: Int) = Unit
        override fun setColorFilter(colorFilter: ColorFilter?) = Unit
        @Deprecated("Deprecated in Android") override fun getOpacity() = PixelFormat.OPAQUE
        override fun getIntrinsicWidth() = width
        override fun getIntrinsicHeight() = height
    }
}
