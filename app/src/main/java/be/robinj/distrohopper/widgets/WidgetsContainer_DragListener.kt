package be.robinj.distrohopper.widgets

import android.view.DragEvent
import android.view.View
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.home.LauncherBarBinder

/**
 * Handles widget drops over the topmost desktop layer while the system drag shadow
 * follows the finger. The underlying widget grid draws the snapped landing target.
 */
internal class WidgetsContainer_DragListener(
	private val parent: HomeActivity,
	private val vgWidgets: WidgetsContainer,
) : View.OnDragListener {
	override fun onDrag(view: View, event: DragEvent): Boolean {
		val container = event.localState as? WidgetContainer ?: return false

		when (event.action) {
			DragEvent.ACTION_DRAG_LOCATION -> {
				val (col, row) = this.snap(view, container, event)
				val lp = container.layoutParams as WidgetsContainer.LayoutParams
				val candidate = WidgetLayout(
					container.appWidgetId, col, row, lp.colSpan, lp.rowSpan)
				val fits = WidgetGrid.fits(this.vgWidgets.collectLayouts(container), candidate)

				this.vgWidgets.showMoveTarget(col, row, lp.colSpan, lp.rowSpan, fits)
			}
			DragEvent.ACTION_DROP -> {
				val (col, row) = this.snap(view, container, event)

				container.commitMove(col, row)
				this.vgWidgets.hideMoveTarget()
			}
			DragEvent.ACTION_DRAG_EXITED -> this.vgWidgets.hideMoveTarget()
			DragEvent.ACTION_DRAG_ENDED -> {
				this.vgWidgets.hideMoveTarget()

				// Not via appManager: widgets are draggable before app loading finishes.
				// Posted: mutating views (even just visibility) during ENDED
				// dispatch throws a ConcurrentModificationException //
				view.post { LauncherBarBinder.stoppedDragging(this.parent) }
			}
		}

		return true
	}

	private fun snap(
		receiver: View,
		container: WidgetContainer,
		event: DragEvent,
	): Pair<Int, Int> {
		val lp = container.layoutParams as WidgetsContainer.LayoutParams
		val receiverLocation = IntArray(2)
		val gridLocation = IntArray(2)
		receiver.getLocationOnScreen(receiverLocation)
		this.vgWidgets.getLocationOnScreen(gridLocation)

		val gridX = event.x.toInt() + receiverLocation[0] - gridLocation[0]
		val gridY = event.y.toInt() + receiverLocation[1] - gridLocation[1]
		val left = gridX - container.dragGrabOffsetX - this.vgWidgets.paddingLeft
		val top = gridY - container.dragGrabOffsetY - this.vgWidgets.paddingTop
		val col = WidgetGrid.snapToCell(
			left, this.vgWidgets.cellWidth, WidgetGrid.COLS - lp.colSpan)
		val row = WidgetGrid.snapToCell(
			top, this.vgWidgets.cellHeight, WidgetGrid.ROWS - lp.rowSpan)

		return col to row
	}
}
