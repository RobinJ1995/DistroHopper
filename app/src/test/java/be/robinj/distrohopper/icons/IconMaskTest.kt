package be.robinj.distrohopper.icons

import android.graphics.Path
import android.graphics.RectF
import android.graphics.Region
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IconMaskTest {
    private val size = 100

    private fun region(shape: IconShape): Region {
        val path = IconMask.pathFor(shape, this.size)
        return Region().apply { setPath(path, Region(0, 0, size, size)) }
    }

    private fun contains(shape: IconShape, x: Int, y: Int): Boolean = this.region(shape).contains(x, y)

    @Test fun everyShapeProducesANonEmptyBoundedPath() {
        for (shape in IconShape.values()) {
            val path = IconMask.pathFor(shape, this.size)
            assertFalse("$shape path is empty", path.isEmpty)

            val bounds = RectF()
            path.computeBounds(bounds, true)
            assertTrue("$shape escapes the icon box", bounds.left >= -0.5f && bounds.top >= -0.5f &&
                bounds.right <= this.size + 0.5f && bounds.bottom <= this.size + 0.5f)
        }
    }

    @Test fun squareFillsTheWholeBoxIncludingCorners() {
        assertTrue(this.contains(IconShape.SQUARE, 0, 0))
        assertTrue(this.contains(IconShape.SQUARE, this.size - 1, this.size - 1))
        assertTrue(this.contains(IconShape.SQUARE, this.size / 2, this.size / 2))
    }

    @Test fun roundedShapesCutTheCornersButKeepTheCentre() {
        for (shape in listOf(IconShape.CIRCLE, IconShape.SQUIRCLE, IconShape.ROUNDED_SQUARE)) {
            assertFalse("$shape should clip its top-left corner", this.contains(shape, 0, 0))
            assertTrue("$shape should keep its centre", this.contains(shape, this.size / 2, this.size / 2))
        }
    }

    @Test fun systemShapeProducesAFilledCentre() {
        // The device mask under Robolectric may be a default; either way it must be a real, filled shape.
        val path: Path = IconMask.pathFor(IconShape.SYSTEM, this.size)
        assertFalse(path.isEmpty)
        assertTrue(this.contains(IconShape.SYSTEM, this.size / 2, this.size / 2))
    }
}
