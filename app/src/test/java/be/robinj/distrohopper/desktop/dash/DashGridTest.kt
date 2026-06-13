package be.robinj.distrohopper.desktop.dash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-maths tests for [DashGrid] (no Robolectric needed). The screen-width
 * cases below match the table in the plan: a default column count that scales
 * with the screen, clamped to a sensible cell-size range.
 */
class DashGridTest {
	@Test fun defaultColumnsScaleWithScreenWidth() {
		assertEquals(3, DashGrid.defaultColumns(320))
		assertEquals(4, DashGrid.defaultColumns(360))
		assertEquals(4, DashGrid.defaultColumns(411))
		assertEquals(6, DashGrid.defaultColumns(600))
		assertEquals(8, DashGrid.defaultColumns(800))
		assertEquals(13, DashGrid.defaultColumns(1280))
	}

	@Test fun columnRangeWidensWithScreenWidth() {
		assertEquals(3, DashGrid.minColumns(320))
		assertEquals(5, DashGrid.maxColumns(320))
		assertEquals(3, DashGrid.minColumns(411))
		assertEquals(6, DashGrid.maxColumns(411))
		assertEquals(9, DashGrid.minColumns(1280))
		assertEquals(20, DashGrid.maxColumns(1280))
	}

	@Test fun largePhonesStillOfferAThreeWideGrid() {
		// A ~427dp phone (Pixel-class) keeps 3 as its lowest option (bigger
		// icons), not 4: cells may grow up to 150dp.
		assertEquals(3, DashGrid.minColumns(427))
		assertEquals(6, DashGrid.maxColumns(427))
		assertEquals(4, DashGrid.defaultColumns(427))
	}

	@Test fun defaultAlwaysWithinRange() {
		for (sw in intArrayOf(320, 360, 411, 600, 800, 1280)) {
			val default = DashGrid.defaultColumns(sw)
			assertTrue(default >= DashGrid.minColumns(sw))
			assertTrue(default <= DashGrid.maxColumns(sw))
		}
	}

	@Test fun portraitShowsExactlyTheChosenColumns() {
		// short edge horizontal -> N columns //
		assertEquals(4, DashGrid.dashColumns(1080, 2160, true, 4))
	}

	@Test fun landscapeShowsProportionallyMoreColumns() {
		// short edge vertical -> the long (horizontal) edge fits more columns,
		// so the visible rows invert to roughly N //
		assertEquals(8, DashGrid.dashColumns(1080, 2160, false, 4))
		assertTrue(DashGrid.dashColumns(1080, 2160, false, 4) > 4)
	}

	@Test fun landscapeColumnsAreCappedAtTwiceTheShortEdge() {
		// 3:1 ultra-wide would be 12 columns for N=4, but the long edge never
		// shows more than 2x the short edge's icons //
		assertEquals(8, DashGrid.dashColumns(1000, 3000, false, 4))
	}

	@Test fun cellSizeIsTheShortEdgeDividedByColumns() {
		assertEquals(270, DashGrid.cellSizePx(1080, 4))
		assertEquals(0, DashGrid.cellSizePx(1080, 0))
	}
}
