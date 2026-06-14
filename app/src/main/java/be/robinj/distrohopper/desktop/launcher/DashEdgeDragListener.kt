package be.robinj.distrohopper.desktop.launcher

import android.view.DragEvent
import android.view.View
import be.robinj.distrohopper.HomeActivity

/**
 * Opens or closes the dash mid-drag when the drag hovers a chrome element,
 * powering the cross-surface drag: hovering the BFB (launcher or panel) brings
 * the dash back ([open] = true) so an app can be dragged into it, while hovering
 * the panel closes it ([open] = false) to reveal the desktop as a drop target.
 * The launcher itself closes the dash from its own [LauncherDragListener].
 *
 * The dash visibility change is posted, never run during the drag-event dispatch
 * (and never during ACTION_DRAG_ENDED), and is a no-op when already in the target
 * state, so grazing the target doesn't flip the dash back and forth.
 */
class DashEdgeDragListener(
	private val activity: HomeActivity,
	private val open: Boolean,
) : View.OnDragListener {
	override fun onDrag(view: View, event: DragEvent): Boolean {
		when (event.action) {
			// Claim the drag so ENTERED/EXITED are delivered for this view.
			DragEvent.ACTION_DRAG_STARTED -> return true

			DragEvent.ACTION_DRAG_ENTERED -> view.post {
				if (this.open && !this.activity.dashIsOpen()) {
					this.activity.openDash()
				} else if (!this.open && this.activity.dashIsOpen()) {
					this.activity.closeDash()
				}
			}
		}

		return true
	}
}
