package be.robinj.distrohopper.widgets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetGridTest {
    @Test fun snapToCellRoundsToNearestBoundary() {
        assertEquals(0, WidgetGrid.snapToCell(0, 100, 7))
        assertEquals(0, WidgetGrid.snapToCell(49, 100, 7))
        assertEquals(1, WidgetGrid.snapToCell(51, 100, 7))
        assertEquals(3, WidgetGrid.snapToCell(290, 100, 7))
    }

    @Test fun snapToCellClampsToBounds() {
        assertEquals(7, WidgetGrid.snapToCell(5000, 100, 7))
        assertEquals(0, WidgetGrid.snapToCell(-50, 100, 7))
    }

    @Test fun snapToCellHandlesZeroCellSize() {
        assertEquals(0, WidgetGrid.snapToCell(123, 0, 7))
    }

    @Test fun spanForSizeRoundsUp() {
        assertEquals(1, WidgetGrid.spanForSize(1, 100, 8))
        assertEquals(1, WidgetGrid.spanForSize(100, 100, 8))
        assertEquals(2, WidgetGrid.spanForSize(101, 100, 8))
        assertEquals(3, WidgetGrid.spanForSize(250, 100, 8))
    }

    @Test fun spanForSizeClampsToBounds() {
        assertEquals(8, WidgetGrid.spanForSize(5000, 100, 8))
        assertEquals(1, WidgetGrid.spanForSize(0, 100, 8))
        assertEquals(1, WidgetGrid.spanForSize(100, 0, 8))
    }

    @Test fun overlapsDetectsIntersection() {
        val a = WidgetLayout(1, 0, 0, 2, 2)

        assertTrue(WidgetGrid.overlaps(a, WidgetLayout(2, 1, 1, 2, 2)))
        assertFalse(WidgetGrid.overlaps(a, WidgetLayout(2, 2, 0, 2, 2)))
        assertFalse(WidgetGrid.overlaps(a, WidgetLayout(2, 0, 2, 2, 2)))
    }

    @Test fun fitsRejectsOutOfBounds() {
        val none = emptyList<WidgetLayout>()

        assertTrue(WidgetGrid.fits(none, WidgetLayout(1, 0, 0, 8, 8)))
        assertFalse(WidgetGrid.fits(none, WidgetLayout(1, -1, 0, 1, 1)))
        assertFalse(WidgetGrid.fits(none, WidgetLayout(1, 7, 0, 2, 1)))
        assertFalse(WidgetGrid.fits(none, WidgetLayout(1, 0, 7, 1, 2)))
        assertFalse(WidgetGrid.fits(none, WidgetLayout(1, 0, 0, 0, 1)))
    }

    @Test fun fitsRejectsOverlap() {
        val occupied = listOf(WidgetLayout(1, 0, 0, 4, 4))

        assertFalse(WidgetGrid.fits(occupied, WidgetLayout(2, 3, 3, 2, 2)))
        assertTrue(WidgetGrid.fits(occupied, WidgetLayout(2, 4, 0, 2, 2)))
    }

    @Test fun findFreeRectReturnsFirstFitRowMajor() {
        val occupied = listOf(WidgetLayout(1, 0, 0, 4, 2))

        val rect = WidgetGrid.findFreeRect(occupied, 2, 2)

        assertEquals(4, rect!!.col)
        assertEquals(0, rect.row)
    }

    @Test fun findFreeRectReturnsNullWhenFull() {
        val occupied = listOf(WidgetLayout(1, 0, 0, 8, 8))

        assertNull(WidgetGrid.findFreeRect(occupied, 1, 1))
    }

    @Test fun findFreeRectClampsOversizedSpans() {
        val rect = WidgetGrid.findFreeRect(emptyList(), 20, 20)

        assertEquals(8, rect!!.colSpan)
        assertEquals(8, rect.rowSpan)
    }
}
