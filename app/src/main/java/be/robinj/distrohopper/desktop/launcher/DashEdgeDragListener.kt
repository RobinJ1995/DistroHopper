package be.robinj.distrohopper.desktop.launcher

import android.view.DragEvent
import android.view.View
import be.robinj.distrohopper.HomeActivity

/**
 * Reports a chrome element's hover state to the [DashCrossSurfaceController] so
 * the cross-surface drag can open/close the dash: attach with [open] = false to
 * the launcher and panel (hovering them closes the dash to reveal the desktop)
 * and [open] = true to each BFB (hovering opens the dash so an app can be
 * dragged into it). The controller resolves the overlap (a BFB sits inside its
 * bar) with open precedence.
 */
class DashEdgeDragListener(
	private val activity: HomeActivity,
	private val open: Boolean,
) : View.OnDragListener {
	override fun onDrag(view: View, event: DragEvent): Boolean {
		val controller = this.activity.dashCrossSurface

		when (event.action) {
			// Claim the drag so ENTERED/EXITED are delivered for this view.
			DragEvent.ACTION_DRAG_STARTED -> return true

			DragEvent.ACTION_DRAG_ENTERED -> controller.entered(view.id, this.open)

			DragEvent.ACTION_DRAG_EXITED -> controller.exited(view.id, this.open)

			DragEvent.ACTION_DRAG_ENDED -> controller.exited(view.id, this.open)
		}

		return true
	}
}
