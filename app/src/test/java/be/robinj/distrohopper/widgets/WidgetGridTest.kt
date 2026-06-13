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

    @Test fun initialSpanPrefersTargetCells() {
        // Provider's preferred cell count is used directly when within limits //
        assertEquals(3, WidgetGrid.initialSpan(3, 0, 0, 100, 8))
    }

    @Test fun initialSpanFallsBackToMinResize() {
        // No target -> nearest span to the minimum resize size (260/100 -> 3) //
        assertEquals(3, WidgetGrid.initialSpan(0, 260, 0, 100, 8))
    }

    @Test fun initialSpanDefaultsToOneWithoutHints() {
        assertEquals(1, WidgetGrid.initialSpan(0, 0, 0, 100, 8))
    }

    @Test fun initialSpanDefersWhenCellSizeUnknown() {
        // 0 signals "grid not measured yet" so the caller can defer rather than
        // squashing the widget to 1x1 //
        assertEquals(0, WidgetGrid.initialSpan(3, 200, 0, 0, 8))
    }

    @Test fun clampSpanEnforcesMinimumSize() {
        // 250px minimum on 100px cells -> nearest is 3 cells //
        assertEquals(3, WidgetGrid.clampSpan(1, 250, 0, 100, 8))
        assertEquals(4, WidgetGrid.clampSpan(4, 250, 0, 100, 8))
    }

    @Test fun clampSpanEnforcesMaximumSize() {
        // 240px maximum on 100px cells -> nearest is 2 cells //
        assertEquals(2, WidgetGrid.clampSpan(5, 0, 240, 100, 8))
        assertEquals(1, WidgetGrid.clampSpan(1, 0, 240, 100, 8))
    }

    @Test fun clampSpanTreatsZeroAsUnspecified() {
        assertEquals(1, WidgetGrid.clampSpan(1, 0, 0, 100, 8))
        assertEquals(8, WidgetGrid.clampSpan(8, 0, 0, 100, 8))
    }

    @Test fun clampSpanKeepsMaxAboveMinWhenLimitsConflict() {
        // Max below min -> min wins //
        assertEquals(3, WidgetGrid.clampSpan(1, 250, 150, 100, 8))
    }

    @Test fun clampSpanKeepsAResizeRangeForTightLimits() {
        // min=150, max=260 on 100px cells: nearest rounding keeps a real range
        // [2, 3] instead of collapsing to a single span (the old ceil/floor bug
        // made such widgets unresizable) //
        val min = WidgetGrid.clampSpan(1, 150, 260, 100, 8)
        val max = WidgetGrid.clampSpan(8, 150, 260, 100, 8)
        assertEquals(2, min)
        assertEquals(3, max)
        assertTrue(max > min)
    }

    @Test fun clampSpanClampsToGrid() {
        assertEquals(8, WidgetGrid.clampSpan(1, 5000, 0, 100, 8))
        assertEquals(1, WidgetGrid.clampSpan(0, 0, 0, 100, 8))
        assertEquals(1, WidgetGrid.clampSpan(0, 0, 0, 0, 8))
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
