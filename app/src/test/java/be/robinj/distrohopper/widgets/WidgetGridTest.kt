package be.robinj.distrohopper.widgets

import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WidgetGridTest {
    // Reset to historic defaults so pure-math tests are not affected by any
    // Robolectric test that called WidgetGrid.init() on a different screen config.
    @Before fun resetGrid() {
        WidgetGrid.COLS = 8
        WidgetGrid.ROWS = 8
    }

    // ---- calculate() -------------------------------------------------------

    @Test fun calculateGivesRoughlySquareCellsForTypicalPhone() {
        val (cols, rows) = WidgetGrid.calculate(360, 800)
        // Cells should be roughly square: aspect = 800/360 ≈ 2.2
        assertEquals(8, cols)
        assertEquals(17, rows)
    }

    @Test fun calculateScalesProportionallyWithAspectRatio() {
        val (cols, rows) = WidgetGrid.calculate(480, 960)
        // 480/48=10 cols, 960/48=20 rows
        assertEquals(10, cols)
        assertEquals(20, rows)
    }

    @Test fun calculateRowsNeverBelowCols() {
        // A hypothetical square screen should give ROWS >= COLS
        val (cols, rows) = WidgetGrid.calculate(360, 360)
        assertTrue(rows >= cols)
    }

    @Test fun calculateClampsToMinCols() {
        // Very narrow screen — should not go below 2 columns
        val (cols, _) = WidgetGrid.calculate(64, 200)
        assertTrue(cols >= 2)
    }

    // ---- user-chosen column count ------------------------------------------

    @Test fun columnRangeAndDefaultForTypicalPhone() {
        assertEquals(5, WidgetGrid.minColumns(360))  // ceil(360/72)
        assertEquals(10, WidgetGrid.maxColumns(360)) // 360/36
        assertEquals(8, WidgetGrid.defaultColumns(360))
    }

    /**
     * At the adaptive default the rows keep the EXACT legacy formula, so
     * existing desktops keep the grid their stored coordinates were written
     * against across the update that introduced the column setting.
     */
    @Test fun defaultColumnsKeepLegacyRows() {
        assertEquals(WidgetGrid.calculate(360, 800),
            WidgetGrid.calculate(360, 800, WidgetGrid.defaultColumns(360)))
        assertEquals(8 to 17, WidgetGrid.calculate(360, 800, 8))
    }

    @Test fun chosenColumnsDeriveRowsFromAspectRatio() {
        // 10 cols on 360×800: rows = round(800 * 10 / 360) = 22
        assertEquals(10 to 22, WidgetGrid.calculate(360, 800, 10))
        // 5 cols: rows = round(800 * 5 / 360) = 11
        assertEquals(5 to 11, WidgetGrid.calculate(360, 800, 5))
    }

    @Test fun chosenColumnsAreClampedToRange() {
        assertEquals(WidgetGrid.maxColumns(360), WidgetGrid.calculate(360, 800, 99).first)
        assertEquals(WidgetGrid.minColumns(360), WidgetGrid.calculate(360, 800, 1).first)
    }

    @Test fun chosenColumnsRowsNeverBelowCols() {
        for (cols in WidgetGrid.minColumns(360)..WidgetGrid.maxColumns(360)) {
            val (c, r) = WidgetGrid.calculate(360, 360, cols)
            assertTrue(r >= c)
        }
    }

    // ---- stable-density anchor ---------------------------------------------

    /**
     * The system "Display size" setting rescales dp while pixels stay fixed;
     * correcting the live dp back to the stable density must recover the same
     * edge regardless of the chosen display size, so the grid (and the
     * absolute coordinates stored against it) never changes underneath.
     */
    @Test fun stableEdgeDpIsInvariantAcrossDisplaySizes() {
        // A 411 dp at 420 dpi device: px = 411 * 420/160.
        // Larger display size (480dpi): live dp = round(px / 3.0) = 360.
        assertEquals(411, WidgetGrid.stableEdgeDp(360, 480, 420))
        // Smaller display size (356dpi): live dp ≈ 485.
        assertEquals(411, WidgetGrid.stableEdgeDp(485, 356, 420))
        // Default display size: identity.
        assertEquals(411, WidgetGrid.stableEdgeDp(411, 420, 420))
    }

    @Test fun stableEdgeDpFallsBackWhenStableDensityIsUnreliable() {
        assertEquals(360, WidgetGrid.stableEdgeDp(360, 480, 0))    // unset
        assertEquals(360, WidgetGrid.stableEdgeDp(360, 0, 420))    // bogus current
        assertEquals(360, WidgetGrid.stableEdgeDp(360, 480, 160))  // 3× — outside any real display-size range
        assertEquals(360, WidgetGrid.stableEdgeDp(360, 160, 480))  // ⅓× likewise
    }

    // ---- rotation transforms -----------------------------------------------

    private fun portraitToDisplay(col: Int, row: Int, colSpan: Int, rowSpan: Int, rotation: Int) =
        WidgetGrid.portraitToDisplay(col, row, colSpan, rowSpan, rotation)

    private fun displayToPortrait(dc: Int, dr: Int, dcs: Int, drs: Int, rotation: Int) =
        WidgetGrid.displayToPortrait(dc, dr, dcs, drs, rotation)

    @Test fun portraitToDisplayIsIdentityForPortrait() {
        val d = portraitToDisplay(2, 3, 4, 5, Surface.ROTATION_0)
        assertEquals(2, d.col); assertEquals(3, d.row)
        assertEquals(4, d.colSpan); assertEquals(5, d.rowSpan)
    }

    @Test fun portraitToDisplayCcwMovesTopRowToLeftColumn() {
        // Full top row → full left column (CCW: top becomes left)
        val d = portraitToDisplay(0, 0, WidgetGrid.COLS, 1, Surface.ROTATION_90)
        assertEquals(0, d.col)
        assertEquals(0, d.row)
        assertEquals(1, d.colSpan)
        assertEquals(WidgetGrid.COLS, d.rowSpan)
    }

    @Test fun portraitToDisplayCcwMovesBottomRowToRightColumn() {
        // Full bottom row (portrait) → full right column in CCW landscape
        val d = portraitToDisplay(0, WidgetGrid.ROWS - 1, WidgetGrid.COLS, 1, Surface.ROTATION_90)
        assertEquals(WidgetGrid.ROWS - 1, d.col)
        assertEquals(0, d.row)
        assertEquals(1, d.colSpan)
        assertEquals(WidgetGrid.COLS, d.rowSpan)
    }

    @Test fun portraitToDisplayCwMovesTopRowToRightColumn() {
        // Full top row → full right column (CW: top becomes right)
        val d = portraitToDisplay(0, 0, WidgetGrid.COLS, 1, Surface.ROTATION_270)
        assertEquals(WidgetGrid.ROWS - 1, d.col)
        assertEquals(0, d.row)
        assertEquals(1, d.colSpan)
        assertEquals(WidgetGrid.COLS, d.rowSpan)
    }

    @Test fun ccwRoundTrip() {
        val rotation = Surface.ROTATION_90
        for (col in 0 until WidgetGrid.COLS) {
            for (row in 0 until WidgetGrid.ROWS) {
                val d = portraitToDisplay(col, row, 1, 1, rotation)
                val p = displayToPortrait(d.col, d.row, d.colSpan, d.rowSpan, rotation)
                assertEquals("round-trip col at ($col,$row)", col, p.col)
                assertEquals("round-trip row at ($col,$row)", row, p.row)
                assertEquals(1, p.colSpan); assertEquals(1, p.rowSpan)
            }
        }
    }

    @Test fun cwRoundTrip() {
        val rotation = Surface.ROTATION_270
        for (col in 0 until WidgetGrid.COLS) {
            for (row in 0 until WidgetGrid.ROWS) {
                val d = portraitToDisplay(col, row, 1, 1, rotation)
                val p = displayToPortrait(d.col, d.row, d.colSpan, d.rowSpan, rotation)
                assertEquals("round-trip col at ($col,$row)", col, p.col)
                assertEquals("round-trip row at ($col,$row)", row, p.row)
                assertEquals(1, p.colSpan); assertEquals(1, p.rowSpan)
            }
        }
    }

    @Test fun isLandscapeDetectsRotations() {
        assertFalse(WidgetGrid.isLandscape(Surface.ROTATION_0))
        assertTrue(WidgetGrid.isLandscape(Surface.ROTATION_90))
        assertFalse(WidgetGrid.isLandscape(Surface.ROTATION_180))
        assertTrue(WidgetGrid.isLandscape(Surface.ROTATION_270))
    }

    @Test fun displayColsAndRowsSwapInLandscape() {
        // In portrait: displayCols=COLS, displayRows=ROWS
        assertEquals(WidgetGrid.COLS, WidgetGrid.displayCols(Surface.ROTATION_0))
        assertEquals(WidgetGrid.ROWS, WidgetGrid.displayRows(Surface.ROTATION_0))
        // In landscape: displayCols=ROWS (transposed), displayRows=COLS
        assertEquals(WidgetGrid.ROWS, WidgetGrid.displayCols(Surface.ROTATION_90))
        assertEquals(WidgetGrid.COLS, WidgetGrid.displayRows(Surface.ROTATION_90))
    }

    // ---- existing tests (unchanged) ----------------------------------------


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
