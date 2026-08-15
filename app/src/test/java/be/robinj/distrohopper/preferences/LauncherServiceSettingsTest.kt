package be.robinj.distrohopper.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The floating launcher's two settings: which stretch of the edge it can be
 * pulled from, and how readily the pull triggers. Both are pure mappings from a
 * persisted string, so they are worth pinning — a renamed value silently resets
 * everyone's choice to the default.
 */
class LauncherServiceZoneTest {
	@Test fun mapsEveryStoredValue() {
		assertEquals(LauncherServiceZone.FULL, LauncherServiceZone.of("full"))
		assertEquals(LauncherServiceZone.START, LauncherServiceZone.of("start"))
		assertEquals(LauncherServiceZone.CENTRE, LauncherServiceZone.of("centre"))
		assertEquals(LauncherServiceZone.END, LauncherServiceZone.of("end"))
	}

	@Test fun unknownAndUnsetValuesFallBackToTheWholeEdge() {
		assertEquals(LauncherServiceZone.FULL, LauncherServiceZone.of(null))
		assertEquals(LauncherServiceZone.FULL, LauncherServiceZone.of("nonsense"))
	}

	@Test fun theDefaultPreferenceValueIsAKnownZone() {
		assertEquals(LauncherServiceZone.FULL,
			LauncherServiceZone.of(Preference.LAUNCHER_SERVICE_ZONE.getDefault()))
	}

	@Test fun theWholeEdgeSpansIt() {
		assertEquals(0, LauncherServiceZone.FULL.offsetPx(1000))
		assertEquals(1000, LauncherServiceZone.FULL.lengthPx(1000))
	}

	@Test fun aZoneStartsWhereItSaysAndStaysOnTheEdge() {
		for (zone in LauncherServiceZone.entries) {
			val offset = zone.offsetPx(1000)
			val length = zone.lengthPx(1000)

			assertTrue("$zone starts off the edge", offset >= 0)
			assertTrue("$zone runs past the edge", offset + length <= 1000)
			assertTrue("$zone is ungrabbable", length > 0)
		}
	}

	@Test fun theThreePartialZonesRunStartToEndInOrder() {
		assertEquals(0, LauncherServiceZone.START.offsetPx(1000))
		assertTrue(LauncherServiceZone.START.offsetPx(1000)
			< LauncherServiceZone.CENTRE.offsetPx(1000))
		assertTrue(LauncherServiceZone.CENTRE.offsetPx(1000)
			< LauncherServiceZone.END.offsetPx(1000))
		assertEquals(1000,
			LauncherServiceZone.END.offsetPx(1000) + LauncherServiceZone.END.lengthPx(1000))
	}

	@Test fun everyZoneIsShorterThanTheWholeEdge() {
		for (zone in LauncherServiceZone.entries.filter { it != LauncherServiceZone.FULL }) {
			assertTrue("$zone is not a narrowing", zone.lengthPx(1000) < 1000)
		}
	}

	@Test fun anEmptyEdgeYieldsNothingRatherThanANegativeStrip() {
		assertEquals(0, LauncherServiceZone.CENTRE.offsetPx(0))
		assertEquals(0, LauncherServiceZone.CENTRE.lengthPx(0))
		assertEquals(0, LauncherServiceZone.CENTRE.lengthPx(-100))
	}
}

class LauncherServiceSensitivityTest {
	@Test fun mapsEveryStoredValue() {
		assertEquals(LauncherServiceSensitivity.LOW, LauncherServiceSensitivity.of("low"))
		assertEquals(LauncherServiceSensitivity.MEDIUM, LauncherServiceSensitivity.of("medium"))
		assertEquals(LauncherServiceSensitivity.HIGH, LauncherServiceSensitivity.of("high"))
	}

	@Test fun unknownAndUnsetValuesFallBackToTheMiddle() {
		assertEquals(LauncherServiceSensitivity.MEDIUM, LauncherServiceSensitivity.of(null))
		assertEquals(LauncherServiceSensitivity.MEDIUM, LauncherServiceSensitivity.of("nonsense"))
	}

	@Test fun theDefaultPreferenceValueIsAKnownSensitivity() {
		assertEquals(LauncherServiceSensitivity.MEDIUM,
			LauncherServiceSensitivity.of(Preference.LAUNCHER_SERVICE_SENSITIVITY.getDefault()))
	}

	/** More sensitive means both an easier target and a shorter pull to commit. */
	@Test fun sensitivityRunsOneWayOnBothKnobs() {
		val ascending = listOf(LauncherServiceSensitivity.LOW,
			LauncherServiceSensitivity.MEDIUM, LauncherServiceSensitivity.HIGH)

		for ((lower, higher) in ascending.zipWithNext()) {
			assertTrue("$higher grabs no wider than $lower",
				higher.hotZoneDp > lower.hotZoneDp)
			assertTrue("$higher does not commit sooner than $lower",
				higher.pullDp < lower.pullDp)
		}
	}

	@Test fun pixelSizesScaleWithDensityAndAreNeverZero() {
		assertEquals(32, LauncherServiceSensitivity.MEDIUM.hotZonePx(2F))
		assertEquals(112, LauncherServiceSensitivity.MEDIUM.pullPx(2F))
		assertTrue(LauncherServiceSensitivity.LOW.hotZonePx(0.01F) >= 1)
		assertTrue(LauncherServiceSensitivity.LOW.pullPx(0.01F) >= 1)
	}
}
