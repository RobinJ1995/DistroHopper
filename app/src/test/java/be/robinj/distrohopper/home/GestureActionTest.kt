package be.robinj.distrohopper.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The gesture-action model and, crucially, the no-lockout reconciliation rule
 * that keeps at least one of swipe-up/down opening the dash.
 */
class GestureActionTest {
	@Test fun fromValueMapsEveryKnownValue() {
		assertEquals(GestureAction.NONE, GestureAction.fromValue("none"))
		assertEquals(GestureAction.OPEN_DASH, GestureAction.fromValue("open_dash"))
		assertEquals(GestureAction.OPEN_DASH_SEARCH, GestureAction.fromValue("open_dash_search"))
		assertEquals(GestureAction.NOTIFICATIONS, GestureAction.fromValue("notifications_tray"))
	}

	@Test fun fromValueDefaultsToNoneForNullOrUnknown() {
		assertEquals(GestureAction.NONE, GestureAction.fromValue(null))
		assertEquals(GestureAction.NONE, GestureAction.fromValue(""))
		assertEquals(GestureAction.NONE, GestureAction.fromValue("nonsense"))
	}

	@Test fun valueRoundTrips() {
		for (action in GestureAction.entries) {
			assertEquals(action, GestureAction.fromValue(action.value))
		}
	}

	@Test fun onlyDashActionsOpenTheDash() {
		assertTrue(GestureAction.OPEN_DASH.opensDash)
		assertTrue(GestureAction.OPEN_DASH_SEARCH.opensDash)
		assertFalse(GestureAction.NONE.opensDash)
		assertFalse(GestureAction.NOTIFICATIONS.opensDash)
	}

	/**
	 * The full 4×4 matrix: reconcileOther forces the sibling to OPEN_DASH exactly
	 * when both gestures would otherwise be non-dash (NONE/NOTIFICATIONS), and
	 * leaves any pairing alone where one already opens the dash.
	 */
	@Test fun reconcileOtherForcesOpenDashOnlyWhenBothAreNonDash() {
		for (changed in GestureAction.entries) {
			for (other in GestureAction.entries) {
				val result = GestureAction.reconcileOther(changed, other)
				if (! changed.opensDash && ! other.opensDash) {
					assertEquals("$changed + $other should reconcile to OPEN_DASH",
						GestureAction.OPEN_DASH, result)
				} else {
					assertNull("$changed + $other should need no change", result)
				}
			}
		}
	}

	@Test fun reconcileOtherMatchesTheSpecExample() {
		// Set swipe-up to notifications while swipe-down is the default (none):
		// swipe-down must flip to open dash. //
		assertEquals(GestureAction.OPEN_DASH,
			GestureAction.reconcileOther(GestureAction.NOTIFICATIONS, GestureAction.NONE))
	}

	@Test fun reconcileOtherPreservesADeliberateValidPairing() {
		// up = open dash + search, down = notifications: dash is still reachable,
		// so neither gets forced. //
		assertNull(GestureAction.reconcileOther(
			GestureAction.NOTIFICATIONS, GestureAction.OPEN_DASH_SEARCH))
		assertNull(GestureAction.reconcileOther(
			GestureAction.OPEN_DASH_SEARCH, GestureAction.NOTIFICATIONS))
	}
}
