package be.robinj.distrohopper.home

import android.view.View
import android.widget.GridView
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.preferences.Preference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

/**
 * The finger-tracked swipe drives each theme's own open animation, scrubbed by
 * an openness fraction: 1 must land on the dash's resting (open) state, 0 on
 * the theme's collapsed (closed) state, and the settle must end cleanly. Runs
 * under the PAUSED looper so the settle animators advance in drainTasks().
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class DashSwipeAnimationTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@After fun tearDown() { this.scenario.close() }

	private fun launch(theme: String) {
		this.scenario = ActivityTestSupport.launchHome(
			configurePrefs = { it.putString(Preference.THEME.getName(), theme) })
	}

	private fun controller(activity: HomeActivity): DashController {
		val container = DependencyContainer.of(activity)
		return DashController(activity, activity.viewFinder,
			container.themeManager.current, container.prefs)
	}

	/* Geometry for a fresh open is captured in a pre-draw listener on the dash. */
	private fun beginOpenSwipe(activity: HomeActivity, dash: DashController) {
		assertTrue(dash.swipeOpenBegin())
		activity.findViewById<GridView>(R.id.gvDashHomeApps).viewTreeObserver.dispatchOnPreDraw()
	}

	private fun llDash(activity: HomeActivity) = activity.findViewById<LinearLayout>(R.id.llDash)

	private fun assertAtOpenRest(activity: HomeActivity) {
		val llDash = this.llDash(activity)
		assertEquals(1F, llDash.alpha, 0.01F)
		assertEquals(1F, llDash.scaleX, 0.01F)
		assertEquals(1F, llDash.scaleY, 0.01F)
		assertEquals(0F, llDash.translationX, 0.01F)
		assertEquals(0F, llDash.translationY, 0.01F)

		val grid = activity.findViewById<GridView>(R.id.gvDashHomeApps)
		for (i in 0 until grid.childCount) {
			val child = grid.getChildAt(i)
			assertEquals(1F, child.scaleX, 0.01F)
			assertEquals(0F, child.translationX, 0.01F)
			assertEquals(0F, child.translationY, 0.01F)
		}
	}

	/* Openness 1 lands on the resting open state for every theme. */
	private fun assertSwipeOpensToRest(theme: String) {
		this.launch(theme)
		this.scenario.onActivity { activity ->
			val dash = this.controller(activity)
			this.beginOpenSwipe(activity, dash)

			dash.swipeUpdate(1F)

			assertEquals(View.VISIBLE, this.llDash(activity).visibility)
			this.assertAtOpenRest(activity)
		}
	}

	@Test fun unityOpensToRest() = this.assertSwipeOpensToRest("default")
	@Test fun gnomeOpensToRest() = this.assertSwipeOpensToRest("gnome")
	@Test fun elementaryOpensToRest() = this.assertSwipeOpensToRest("elementary")
	@Test fun cinnamonOpensToRest() = this.assertSwipeOpensToRest("cinnamon")
	@Test fun mateOpensToRest() = this.assertSwipeOpensToRest("mate")
	@Test fun cosmicOpensToRest() = this.assertSwipeOpensToRest("cosmic")

	@Test fun unityTracksAlphaWithOpenness() {
		this.launch("default")
		this.scenario.onActivity { activity ->
			val dash = this.controller(activity)
			this.beginOpenSwipe(activity, dash)

			dash.swipeUpdate(0F)
			assertEquals(0F, this.llDash(activity).alpha, 0.01F)

			dash.swipeUpdate(0.5F)
			assertEquals(0.5F, this.llDash(activity).alpha, 0.01F)

			dash.swipeUpdate(1F)
			assertEquals(1F, this.llDash(activity).alpha, 0.01F)
		}
	}

	@Test fun elementaryZoomsFromTheStartScaleWhenClosed() {
		this.launch("elementary")
		this.scenario.onActivity { activity ->
			val dash = this.controller(activity)
			this.beginOpenSwipe(activity, dash)

			dash.swipeUpdate(0F)

			val llDash = this.llDash(activity)
			assertEquals(0F, llDash.alpha, 0.01F)
			assertTrue("expected a zoomed-down dash, was ${llDash.scaleX}", llDash.scaleX < 0.5F)
		}
	}

	@Test fun cosmicZoomsFromTheStartScaleWhenClosed() {
		this.launch("cosmic")
		this.scenario.onActivity { activity ->
			val dash = this.controller(activity)
			this.beginOpenSwipe(activity, dash)

			dash.swipeUpdate(0F)

			val llDash = this.llDash(activity)
			assertEquals(0F, llDash.alpha, 0.01F)
			// Cosmic only zooms slightly (0.9), so it sits between elementary and 1 //
			assertTrue(llDash.scaleX in 0.85F..0.95F)
		}
	}

	@Test fun mateCollapsesTowardsTheButtonWhenClosed() {
		this.launch("mate")
		this.scenario.onActivity { activity ->
			val dash = this.controller(activity)
			this.beginOpenSwipe(activity, dash)

			dash.swipeUpdate(0F)

			val llDash = this.llDash(activity)
			assertEquals(0F, llDash.alpha, 0.01F)
			assertTrue(llDash.scaleX < 1F)
			assertTrue(llDash.scaleY < 1F)
		}
	}

	/*
	 * The dash itself fades; its icons genie out of the BFB. Robolectric's
	 * GridView does not materialise child views, so the per-icon collapse is
	 * only asserted when any happen to exist (the dash-level fade always is).
	 */
	@Test fun gnomeCollapsesWhenClosed() {
		this.launch("gnome")
		this.scenario.onActivity { activity ->
			val dash = this.controller(activity)
			this.beginOpenSwipe(activity, dash)

			dash.swipeUpdate(0F)

			assertEquals(0F, this.llDash(activity).alpha, 0.01F)
			val grid = activity.findViewById<GridView>(R.id.gvDashHomeApps)
			for (i in 0 until grid.childCount) {
				assertTrue("icon $i should be shrunk when closed, was ${grid.getChildAt(i).scaleX}",
					grid.getChildAt(i).scaleX < 1F)
			}
		}
	}

	@Test fun cinnamonSlidesOffscreenWhenClosed() {
		this.launch("cinnamon")
		this.scenario.onActivity { activity ->
			val dash = this.controller(activity)
			this.beginOpenSwipe(activity, dash)

			dash.swipeUpdate(0F)

			val llDash = this.llDash(activity)
			// Slid off its launcher edge (or, with no measurable edge, faded) //
			assertTrue(llDash.translationX != 0F || llDash.translationY != 0F || llDash.alpha == 0F)
		}
	}

	@Test fun committingAnOpenSwipeSettlesOpen() {
		this.launch("gnome")
		this.scenario.onActivity { activity ->
			val dash = this.controller(activity)
			this.beginOpenSwipe(activity, dash)
			dash.swipeUpdate(0.6F)

			dash.swipeOpenEnd(commit = true)
			ActivityTestSupport.drainTasks()

			assertTrue(dash.isOpen)
			assertEquals(View.VISIBLE, this.llDash(activity).visibility)
			this.assertAtOpenRest(activity)
		}
	}

	@Test fun cancellingAnOpenSwipeSettlesClosedAndResetsTransforms() {
		this.launch("gnome")
		this.scenario.onActivity { activity ->
			val dash = this.controller(activity)
			this.beginOpenSwipe(activity, dash)
			dash.swipeUpdate(0.6F)

			dash.swipeOpenEnd(commit = false)
			ActivityTestSupport.drainTasks()

			assertFalse(dash.isOpen)
			assertEquals(View.GONE, this.llDash(activity).visibility)
			// Recycled icon views must not keep the genie transform //
			val grid = activity.findViewById<GridView>(R.id.gvDashHomeApps)
			for (i in 0 until grid.childCount) {
				assertEquals(1F, grid.getChildAt(i).scaleX, 0.01F)
				assertEquals(0F, grid.getChildAt(i).translationX, 0.01F)
			}
		}
	}

	@Test fun swipingAnOpenDashClosedSettlesGoneWithCleanTransforms() {
		this.launch("gnome")
		this.scenario.onActivity { activity ->
			val dash = this.controller(activity)
			dash.open()
			ActivityTestSupport.drainTasks()
			assertTrue(dash.isOpen)

			assertTrue(dash.swipeCloseBegin())
			dash.swipeUpdate(0.3F)
			dash.swipeCloseEnd(commit = true)
			ActivityTestSupport.drainTasks()

			assertFalse(dash.isOpen)
			val llDash = this.llDash(activity)
			assertEquals(View.GONE, llDash.visibility)
			assertEquals(1F, llDash.alpha, 0.01F) // Reset so a later fade-in is correct //
			val grid = activity.findViewById<GridView>(R.id.gvDashHomeApps)
			for (i in 0 until grid.childCount) {
				assertEquals(1F, grid.getChildAt(i).scaleX, 0.01F)
			}
		}
	}
}
