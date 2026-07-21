package be.robinj.distrohopper.desktop.dash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The edge-scroll velocity geometry: where in the grid a drag triggers an
 * auto-scroll, and how fast. The ticking that actually drives the scroll is thin
 * glue over a Handler; the interesting, testable part is [DashEdgeScroller.velocityFor],
 * a pure mapping from pointer position to a per-frame scroll step.
 *
 * With EDGE_FRACTION = 0.15, a 1000px grid has a 150px zone at each end: y < 150
 * scrolls up, y > 850 scrolls down, and everything between rests still.
 */
class DashEdgeScrollerTest {
	private companion object {
		const val H = 1000
	}

	@Test fun theMiddleOfTheGridDoesNotScroll() {
		assertEquals(0, DashEdgeScroller.velocityFor(500f, H))
		assertEquals(0, DashEdgeScroller.velocityFor(H * 0.5f, H))
	}

	@Test fun theZoneBoundariesAreInclusiveOfTheStillArea() {
		// y == zone (150) and y == height - zone (850) are the first still pixels. //
		assertEquals(0, DashEdgeScroller.velocityFor(150f, H))
		assertEquals(0, DashEdgeScroller.velocityFor(850f, H))
	}

	@Test fun theTopZoneScrollsUp() {
		assertTrue("near the top edge scrolls up (negative)",
			DashEdgeScroller.velocityFor(10f, H) < 0)
	}

	@Test fun theBottomZoneScrollsDown() {
		assertTrue("near the bottom edge scrolls down (positive)",
			DashEdgeScroller.velocityFor(H - 10f, H) > 0)
	}

	@Test fun theVeryEdgesScrollAtTheMaximumStep() {
		// At the exact edges the depth is 1, so the step is MAX_STEP_PX (30). //
		assertEquals(-30, DashEdgeScroller.velocityFor(0f, H))
		assertEquals(30, DashEdgeScroller.velocityFor(H.toFloat(), H))
	}

	@Test fun scrollAcceleratesTowardsTheEdge() {
		// Deeper into the top zone (smaller y) must scroll faster. //
		val shallow = DashEdgeScroller.velocityFor(140f, H)
		val deep = DashEdgeScroller.velocityFor(20f, H)
		assertTrue("both scroll up", shallow < 0 && deep < 0)
		assertTrue("deeper is faster", kotlin.math.abs(deep) > kotlin.math.abs(shallow))

		// Symmetrically at the bottom. //
		val shallowBottom = DashEdgeScroller.velocityFor(860f, H)
		val deepBottom = DashEdgeScroller.velocityFor(980f, H)
		assertTrue("both scroll down", shallowBottom > 0 && deepBottom > 0)
		assertTrue("deeper is faster", deepBottom > shallowBottom)
	}

	@Test fun aZeroHeightGridNeverScrolls() {
		// Before layout the grid has no height; there is nothing to scroll. //
		assertEquals(0, DashEdgeScroller.velocityFor(0f, 0))
		assertEquals(0, DashEdgeScroller.velocityFor(500f, 0))
	}
}
