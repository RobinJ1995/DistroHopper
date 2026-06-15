package be.robinj.distrohopper.desktop.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

/**
 * The cross-surface drag's open/close-the-dash state machine. The tricky cases —
 * a BFB sitting inside its bar (so a hover registers both an open- and a
 * close-target), leaving the BFB after it opened the dash, and re-hovering a BFB
 * while open — are exactly the ones that used to flicker or strand the dash.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class DashCrossSurfaceControllerTest {
	private class FakeDash(private var open: Boolean) : DashCrossSurfaceController.Dash {
		override fun isOpen() = this.open
		override fun open() { this.open = true }
		override fun close() { this.open = false }
	}

	// A BFB is an open-target; the launcher/panel are close-targets. A BFB also
	// lives inside its bar, so hovering it registers the bar's close-target too.
	private val bfb = 1
	private val bar = 2
	private val panel = 3

	/** Runs the debounced apply (and any other pending main-looper tasks). */
	private fun settle() = ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

	@Test fun bfbHoverWhileClosedOpensTheDash() {
		val dash = FakeDash(open = false)
		val controller = DashCrossSurfaceController(dash)

		controller.entered(this.bar, false) // the bar the BFB sits in
		controller.entered(this.bfb, true)
		this.settle()

		assertTrue(dash.isOpen())
	}

	@Test fun launcherHoverWhileClosedDoesNothing() {
		val dash = FakeDash(open = false)
		val controller = DashCrossSurfaceController(dash)

		controller.entered(this.bar, false)
		this.settle()

		assertFalse(dash.isOpen())
	}

	@Test fun leavingTheBfbAfterItOpenedTheDashKeepsItOpen() {
		val dash = FakeDash(open = false)
		val controller = DashCrossSurfaceController(dash)

		controller.entered(this.bar, false)
		controller.entered(this.bfb, true)
		this.settle()
		assertTrue("the BFB hover opens it", dash.isOpen())

		controller.exited(this.bfb, true) // drag moves off the BFB into the dash
		this.settle()

		assertTrue("the bar close-target rode in with the BFB and was dropped on open", dash.isOpen())
	}

	@Test fun reHoveringTheBfbWhileOpenDoesNotCloseIt() {
		val dash = FakeDash(open = true)
		val controller = DashCrossSurfaceController(dash)

		controller.entered(this.bar, false)
		controller.entered(this.bfb, true)
		this.settle()

		assertTrue("open precedence: a BFB only ever opens, never closes", dash.isOpen())
	}

	@Test fun launcherHoverWhileOpenClosesIt() {
		val dash = FakeDash(open = true)
		val controller = DashCrossSurfaceController(dash)

		controller.entered(this.bar, false)
		this.settle()

		assertFalse(dash.isOpen())
	}

	@Test fun aFreshPanelHoverStillClosesADashThatABfbOpened() {
		val dash = FakeDash(open = false)
		val controller = DashCrossSurfaceController(dash)

		controller.entered(this.bfb, true)
		this.settle()
		assertTrue(dash.isOpen())

		controller.exited(this.bfb, true)
		controller.entered(this.panel, false) // a genuine, fresh close-target enter
		this.settle()

		assertFalse("a deliberate launcher/panel hover reaches the desktop", dash.isOpen())
	}

	@Test fun resetCancelsAPendingToggle() {
		val dash = FakeDash(open = false)
		val controller = DashCrossSurfaceController(dash)

		controller.entered(this.bfb, true)
		controller.reset()
		this.settle()

		assertFalse(dash.isOpen())
	}
}
