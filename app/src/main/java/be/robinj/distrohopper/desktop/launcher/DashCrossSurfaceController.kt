package be.robinj.distrohopper.desktop.launcher

import android.os.Handler
import android.os.Looper
import be.robinj.distrohopper.HomeActivity

/**
 * Resolves the cross-surface drag's dash open/close intent, **based on the
 * dash's current state**:
 *
 *  - while the dash is OPEN, hovering the launcher or panel (but NOT a BFB)
 *    closes it so the desktop can be reached;
 *  - while the dash is CLOSED, hovering a BFB (the launcher's, or the panel's
 *    "Applications"/"Activities" label) re-opens it so something can be dragged
 *    into the dash; a plain launcher/panel hover does nothing (already closed).
 *
 * A BFB therefore only ever *opens* the dash — it never closes it (**open
 * precedence**). Because a BFB sits inside its bar, both an open- and a
 * close-target register when one is hovered; treating that as a close would make
 * the BFB a toggle and the dash would flicker as the drag crosses the BFB↔bar
 * boundary. The two are tracked as separate sets; while open, the close only
 * fires when a close-target is hovered AND no BFB is. Opening also drops the
 * close-target that rode in with the BFB hover, so leaving the BFB doesn't slam
 * the dash shut (a genuine later launcher/panel hover — a fresh enter — closes
 * it). Drag enter/exit are edge-triggered (the listeners don't act on every
 * LOCATION), so a single hover resolves once and the dash doesn't oscillate; the
 * visibility change is posted (never during event dispatch) and debounced.
 */
class DashCrossSurfaceController(private val activity: HomeActivity) {
	private val openTargets = HashSet<Int>()
	private val closeTargets = HashSet<Int>()
	private val handler = Handler(Looper.getMainLooper())

	private val apply = Runnable {
		if (this.activity.dashIsOpen()) {
			// Open: a launcher/panel hover closes it — but NOT while a BFB is also
			// hovered (open precedence). A BFB only ever opens; it never closes. It
			// sits inside its bar, so treating a BFB hover as a close would make it
			// a toggle and the dash would flicker as the drag crosses the boundary.
			if (this.closeTargets.isNotEmpty() && this.openTargets.isEmpty()) {
				this.activity.closeDash()
			}
		} else {
			// Closed: a BFB hover opens it.
			if (this.openTargets.isNotEmpty()) {
				this.activity.openDash()
				// Drop the close-target that rode in with the BFB hover — a BFB sits
				// inside its bar, so the bar registered a close too. Without this,
				// the moment the drag leaves the BFB that lingering close-target
				// would slam the dash shut again, making it unreachable. A genuine
				// later launcher/panel hover (a fresh enter) still closes it //
				this.closeTargets.clear()
			}
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
