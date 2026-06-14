package be.robinj.distrohopper.folder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The folder contents grid maths: the spec's 1..9 apps-only column mapping and
 * the 3x3 packing used for desktop folders (apps + widgets).
 */
class FolderGridTest {
	@Test fun columnsFollowTheSpecMapping() {
		// 1..3 apps in a single row, 4 apps as a 2x2, 5..9 as three columns.
		assertEquals(1, FolderGrid.columns(1))
		assertEquals(2, FolderGrid.columns(2))
		assertEquals(3, FolderGrid.columns(3))
		assertEquals(2, FolderGrid.columns(4))
		assertEquals(3, FolderGrid.columns(5))
		assertEquals(3, FolderGrid.columns(6))
		assertEquals(3, FolderGrid.columns(7))
		assertEquals(3, FolderGrid.columns(8))
		assertEquals(3, FolderGrid.columns(9))
	}

	@Test fun rowsMatchTheColumnLayout() {
		assertEquals(1, FolderGrid.rows(3))
		assertEquals(2, FolderGrid.rows(4))
		assertEquals(2, FolderGrid.rows(6))
		assertEquals(3, FolderGrid.rows(7))
		assertEquals(3, FolderGrid.rows(9))
	}

	@Test fun fitsRejectsOutOfBoundsAndOverlap() {
		val occupied = listOf(FolderGrid.Rect(0, 0, 1, 1))
		assertTrue(FolderGrid.fits(occupied, FolderGrid.Rect(1, 0, 2, 2)))
		assertFalse(FolderGrid.fits(occupied, FolderGrid.Rect(0, 0, 1, 1))) // overlap
		assertFalse(FolderGrid.fits(emptyList(), FolderGrid.Rect(2, 2, 2, 2))) // out of 3x3
	}

	@Test fun packingRespectsTheOccupiedCells() {
		// Four 1x1 apps packed into the top-left 2x2 block leave only an L-shape,
		// so no 2x2 rectangle fits — but a 1x3 column and a 1x1 still do.
		val block = listOf(
			FolderGrid.Rect(0, 0), FolderGrid.Rect(1, 0),
			FolderGrid.Rect(0, 1), FolderGrid.Rect(1, 1),
		)
		assertNull(FolderGrid.findFreeRect(block, 2, 2))
		assertEquals(FolderGrid.Rect(2, 0, 1, 3), FolderGrid.findFreeRect(block, 1, 3))
		assertEquals(FolderGrid.Rect(2, 0, 1, 1), FolderGrid.findFreeRect(block, 1, 1))

		// The same four apps arranged as a top row + one below DO leave a 2x2 hole,
		// showing the fit depends on arrangement (the host repacks to make room).
		val spread = listOf(
			FolderGrid.Rect(0, 0), FolderGrid.Rect(1, 0), FolderGrid.Rect(2, 0),
			FolderGrid.Rect(0, 1),
		)
		assertEquals(FolderGrid.Rect(1, 1, 2, 2), FolderGrid.findFreeRect(spread, 2, 2))
	}

	@Test fun oneAppLeavesRoomForUpToA3x2Widget() {
		val app = listOf(FolderGrid.Rect(0, 0))
		// 2x3, 3x2, 2x2 all fit alongside a single corner app; 3x3 cannot.
		assertEquals(FolderGrid.Rect(1, 0, 2, 3), FolderGrid.findFreeRect(app, 2, 3))
		assertEquals(FolderGrid.Rect(0, 1, 3, 2), FolderGrid.findFreeRect(app, 3, 2))
		assertNull(FolderGrid.findFreeRect(app, 3, 3))
	}
}
