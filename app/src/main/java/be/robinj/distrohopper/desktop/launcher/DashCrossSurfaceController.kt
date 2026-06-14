package be.robinj.distrohopper.desktop.launcher

import android.os.Handler
import android.os.Looper
import be.robinj.distrohopper.HomeActivity

/**
 * Resolves the cross-surface drag's dash open/close intent, **based on the
 * dash's current state**:
 *
 *  - while the dash is OPEN, hovering anything on the launcher or panel closes
 *    it so the desktop can be reached — and a BFB counts as part of the bar it
 *    sits in, so hovering a BFB closes the dash too;
 *  - while the dash is CLOSED, hovering a BFB (the launcher's, or the panel's
 *    "Applications"/"Activities" label) re-opens it so something can be dragged
 *    into the dash; a plain launcher/panel hover does nothing (already closed).
 *
 * Hovered surfaces are tracked as two sets (a BFB also lives inside its bar, so
 * both register at once); the state-based rule needs no precedence between them.
 * Drag enter/exit are edge-triggered (the listeners don't act on every
 * LOCATION), so a single hover resolves once and the dash doesn't oscillate; the
 * visibility change is posted (never during event dispatch) and debounced.
 */
class DashCrossSurfaceController(private val activity: HomeActivity) {
	private val openTargets = HashSet<Int>()
	private val closeTargets = HashSet<Int>()
	private val handler = Handler(Looper.getMainLooper())

	private val apply = Runnable {
		if (this.activity.dashIsOpen()) {
			// Open: any launcher/panel hover — BFB included — closes the dash.
			if (this.openTargets.isNotEmpty() || this.closeTargets.isNotEmpty()) {
				this.activity.closeDash()
			}
		} else {
			// Closed: only a BFB hover re-opens it.
			if (this.openTargets.isNotEmpty()) {
				this.activity.openDash()
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
