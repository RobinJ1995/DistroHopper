package be.robinj.distrohopper.desktop

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FrostedFallbackDrawableTest {
	private val opaqueTint = Color.argb(255, 40, 40, 40)

	private fun render(drawable: FrostedFallbackDrawable, size: Int = 100): Bitmap {
		val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
		drawable.setBounds(0, 0, size, size)
		drawable.draw(Canvas(bitmap))

		return bitmap
	}

	@Test fun opacityIsTranslucent() {
		assertEquals(PixelFormat.TRANSLUCENT, FrostedFallbackDrawable(this.opaqueTint).opacity)
	}

	@Test fun setAlphaDrivesTheFraction() {
		val drawable = FrostedFallbackDrawable(this.opaqueTint)

		drawable.alpha = 255
		assertEquals(1F, drawable.fraction, 0.001F)

		drawable.alpha = 0
		assertEquals(0F, drawable.fraction, 0.001F)
	}

	@Test fun nothingIsPaintedAtZeroFraction() {
		val drawable = FrostedFallbackDrawable(this.opaqueTint).apply { fraction = 0F }

		assertEquals(0, Color.alpha(this.render(drawable).getPixel(50, 50)))
	}

	@Test fun fullBleedFillsRightUpToTheCorner() {
		val drawable = FrostedFallbackDrawable(this.opaqueTint).apply { fraction = 1F }

		val bitmap = this.render(drawable)
		assertTrue(Color.alpha(bitmap.getPixel(0, 0)) > 0)
		assertTrue(Color.alpha(bitmap.getPixel(50, 50)) > 0)
	}

	@Test fun roundedVariantLeavesTheCornerTransparentButFillsTheCentre() {
		val drawable = FrostedFallbackDrawable(this.opaqueTint, cornerRadius = 40F)
			.apply { fraction = 1F }

		val bitmap = this.render(drawable)
		// The extreme corner falls outside the rounded card, so it stays clear. //
		assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
		assertTrue(Color.alpha(bitmap.getPixel(50, 50)) > 0)
	}
}
