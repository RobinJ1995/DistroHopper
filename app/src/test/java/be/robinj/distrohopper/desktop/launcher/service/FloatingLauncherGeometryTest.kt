package be.robinj.distrohopper.desktop.launcher.service

import be.robinj.distrohopper.theme.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The feel of the floating launcher's pull-out gesture: which way "out" is on
 * each edge, how far the bar has come, and what letting go does.
 */
class FloatingLauncherGeometryTest {
	@Test fun onlyTheSideEdgesAreVertical() {
		assertTrue(FloatingLauncherGeometry.isVertical(Location.LEFT))
		assertTrue(FloatingLauncherGeometry.isVertical(Location.RIGHT))
		assertFalse(FloatingLauncherGeometry.isVertical(Location.TOP))
		assertFalse(FloatingLauncherGeometry.isVertical(Location.BOTTOM))
	}

	@Test fun anEdgelessLauncherFallsBackToTheLeft() {
		assertEquals(Location.LEFT, FloatingLauncherGeometry.edgeOrDefault(Location.NONE))
		assertEquals(Location.BOTTOM, FloatingLauncherGeometry.edgeOrDefault(Location.BOTTOM))
	}

	/** Pulling "out" is inwards from the docked edge, whichever edge that is. */
	@Test fun pullIsMeasuredAwayFromTheEdge() {
		assertEquals(60F, FloatingLauncherGeometry.pulled(Location.LEFT, 10F, 500F, 70F, 500F), 0F)
		assertEquals(60F, FloatingLauncherGeometry.pulled(Location.RIGHT, 990F, 500F, 930F, 500F), 0F)
		assertEquals(60F, FloatingLauncherGeometry.pulled(Location.TOP, 500F, 10F, 500F, 70F), 0F)
		assertEquals(60F, FloatingLauncherGeometry.pulled(Location.BOTTOM, 500F, 990F, 500F, 930F), 0F)
	}

	@Test fun pushingBackTowardsTheEdgeIsNegative() {
		assertEquals(-20F, FloatingLauncherGeometry.pulled(Location.LEFT, 30F, 0F, 10F, 0F), 0F)
	}

	@Test fun progressIsThePullOverTheBarLengthAndCannotOvershoot() {
		assertEquals(0F, FloatingLauncherGeometry.progress(0F, 200), 0F)
		assertEquals(0.5F, FloatingLauncherGeometry.progress(100F, 200), 0F)
		assertEquals(1F, FloatingLauncherGeometry.progress(200F, 200), 0F)
		assertEquals(1F, FloatingLauncherGeometry.progress(999F, 200), 0F)
		assertEquals(0F, FloatingLauncherGeometry.progress(-50F, 200), 0F)
	}

	@Test fun progressIsZeroUntilTheBarHasBeenMeasured() {
		assertEquals(0F, FloatingLauncherGeometry.progress(100F, 0), 0F)
	}

	@Test fun aPullThatReachesTheSensitivityDistanceSettlesOpen() {
		assertTrue(FloatingLauncherGeometry.settleOpen(56F, 400, 56))
		assertFalse(FloatingLauncherGeometry.settleOpen(55F, 400, 56))
	}

	/** A bar shorter than the committing distance still opens when dragged right out. */
	@Test fun aShortBarDraggedMostOfTheWayOutSettlesOpen() {
		assertTrue(FloatingLauncherGeometry.settleOpen(30F, 40, 96))
		assertFalse(FloatingLauncherGeometry.settleOpen(15F, 40, 96))
	}

	@Test fun aHiddenBarSitsItsOwnLengthOutsideTheEdgeItIsDockedOn() {
		assertEquals(-300F, FloatingLauncherGeometry.translation(Location.LEFT, 300, 0F), 0F)
		assertEquals(300F, FloatingLauncherGeometry.translation(Location.RIGHT, 300, 0F), 0F)
		assertEquals(-300F, FloatingLauncherGeometry.translation(Location.TOP, 300, 0F), 0F)
		assertEquals(300F, FloatingLauncherGeometry.translation(Location.BOTTOM, 300, 0F), 0F)
	}

	@Test fun anOpenBarSitsAtTheEdgeAndAHalfPullHalfway() {
		assertEquals(0F, FloatingLauncherGeometry.translation(Location.LEFT, 300, 1F), 0F)
		assertEquals(-150F, FloatingLauncherGeometry.translation(Location.LEFT, 300, 0.5F), 0F)
	}

	@Test fun settlingIsProportionalToWhatIsLeftToTravel() {
		assertEquals(200L, FloatingLauncherGeometry.settleDurationMs(0F, 1F, 200L))
		assertEquals(100L, FloatingLauncherGeometry.settleDurationMs(0.5F, 1F, 200L))
		assertEquals(100L, FloatingLauncherGeometry.settleDurationMs(0.5F, 0F, 200L))
	}

	@Test fun anAlmostSettledBarStillAnimates() {
		assertEquals(FloatingLauncherGeometry.MIN_SETTLE_MS,
			FloatingLauncherGeometry.settleDurationMs(0.99F, 1F, 200L))
	}
}
