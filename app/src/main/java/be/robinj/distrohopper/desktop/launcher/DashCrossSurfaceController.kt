package be.robinj.distrohopper.desktop.launcher

import android.os.Handler
import android.os.Looper
import be.robinj.distrohopper.HomeActivity

/**
 * Resolves the cross-surface drag's dash open/close intent. During a drag the
 * launcher and panel want the dash CLOSED (so the desktop can be reached), while
 * either BFB (the launcher's, or the panel's "Applications"/"Activities" label)
 * wants it OPEN (so something can be dragged into the dash).
 *
 * Because a BFB sits inside the launcher/panel, hovering it makes both the BFB
 * (open) and its container (close) register at once; tracking the hovered
 * surfaces as two sets and giving **open precedence** resolves that cleanly:
 * over a BFB the dash opens, and stepping off it back onto the bar closes it
 * again. The visibility change is posted (never run during event dispatch) and
 * debounced, and is a no-op when the dash is already in the target state, so the
 * dash never flickers as the drag crosses a parent/child boundary.
 */
class DashCrossSurfaceController(private val activity: HomeActivity) {
	private val openTargets = HashSet<Int>()
	private val closeTargets = HashSet<Int>()
	private val handler = Handler(Looper.getMainLooper())

	private val apply = Runnable {
		when {
			this.openTargets.isNotEmpty() -> if (!this.activity.dashIsOpen()) this.activity.openDash()
			this.closeTargets.isNotEmpty() -> if (this.activity.dashIsOpen()) this.activity.closeDash()
		}
	}

	fun entered(viewId: Int, open: Boolean) {
		this.targets(open).add(viewId)
		this.schedule()
	}

	fun exited(viewId: Int, open: Boolean) {
		this.targets(open).remove(viewId)
		this.schedule()
	}

	/** Clears all tracking (e.g. when the drag ends). */
	fun reset() {
		this.openTargets.clear()
		this.closeTargets.clear()
		this.handler.removeCallbacks(this.apply)
	}

	private fun targets(open: Boolean) = if (open) this.openTargets else this.closeTargets

	private fun schedule() {
		this.handler.removeCallbacks(this.apply)
		this.handler.postDelayed(this.apply, DEBOUNCE_MS)
	}

	companion object {
		private const val DEBOUNCE_MS = 80L
	}
}
