package be.robinj.distrohopper.widgets

import android.view.DragEvent
import android.view.View
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.home.LauncherBarBinder

/**
 * Drives the move of a widget that is being dragged via the system drag-and-drop
 * framework (started in WidgetContainer). The widget itself follows the finger as a
 * cell-snapped preview; dropping commits the move, dropping anywhere else (including
 * the launcher's trash icon, which removes the widget) reverts it.
 */
internal class WidgetsContainer_DragListener(
	private val parent: HomeActivity,
) : View.OnDragListener {
	override fun onDrag(view: View, event: DragEvent): Boolean {
		val container = event.localState as? WidgetContainer ?: return false
		val vgWidgets = view as? WidgetsContainer ?: return false

		when (event.action) {
			DragEvent.ACTION_DRAG_LOCATION -> {
				val lp = container.layoutParams as WidgetsContainer.LayoutParams
				val (col, row) = this.snap(vgWidgets, container, event)

				lp.previewLeftPx = vgWidgets.paddingLeft + col * vgWidgets.cellWidth
				lp.previewTopPx = vgWidgets.paddingTop + row * vgWidgets.cellHeight

				vgWidgets.requestLayout()
			}
			DragEvent.ACTION_DROP -> {
				val (col, row) = this.snap(vgWidgets, container, event)

				container.commitMove(col, row)
			}
			DragEvent.ACTION_DRAG_ENDED -> {
				// Also fires when the drag is cancelled, dropped outside the grid, or
				// dropped on the trash (in which case the container is already gone) //
				if (container.parent != null) {
					(container.layoutParams as WidgetsContainer.LayoutParams).clearPreview()
					vgWidgets.requestLayout()
				}

				// Not via appManager: widgets are draggable before app loading finishes //
				LauncherBarBinder.stoppedDragging(this.parent)
			}
		}

		return true
	}

	private fun snap(
		vgWidgets: WidgetsContainer,
		container: WidgetContainer,
		event: DragEvent,
	): Pair<Int, Int> {
		val lp = container.layoutParams as WidgetsContainer.LayoutParams

		val left = event.x.toInt() - container.dragGrabOffsetX - vgWidgets.paddingLeft
		val top = event.y.toInt() - container.dragGrabOffsetY - vgWidgets.paddingTop

		val col = WidgetGrid.snapToCell(left, vgWidgets.cellWidth, WidgetGrid.COLS - lp.colSpan)
		val row = WidgetGrid.snapToCell(top, vgWidgets.cellHeight, WidgetGrid.ROWS - lp.rowSpan)

		return col to row
	}
}
