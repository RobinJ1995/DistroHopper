package be.robinj.distrohopper.widgets

import android.view.DragEvent
import android.view.View
import be.robinj.distrohopper.App
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.home.LauncherBarBinder

/**
 * Handles drops over the topmost desktop layer while the system drag shadow
 * follows the finger. The underlying widget grid draws the snapped landing
 * target. All coordinates target the pager's current desktop: the pages cannot
 * change mid-drag (the drag owns the touch stream), so that is also the desktop
 * a moved widget or desktop app came from.
 *
 * Four kinds of drag land here, told apart by the drag's local state:
 *  - a [WidgetContainer] — a widget being moved on its grid;
 *  - a [DesktopAppView] — a desktop app being moved on its grid;
 *  - an [App] — a not-yet-pinned app dragged from the dash, pinned to the desktop;
 *  - otherwise the launcher-bar reorder clip (label = pinned index) — an existing
 *    launcher icon, **moved** onto the desktop (pinned here, unpinned from the bar).
 */
internal class WidgetsContainer_DragListener(
	private val parent: HomeActivity,
	private val pager: WidgetsPager,
) : View.OnDragListener {
	private val vgWidgets: WidgetsContainer
		get() = this.pager.currentPageContainer

	override fun onDrag(view: View, event: DragEvent): Boolean {
		val kind = this.kindOf(event) ?: return false

		when (event.action) {
			// Must claim the drag here to receive LOCATION/DROP for it //
			DragEvent.ACTION_DRAG_STARTED -> return true
			DragEvent.ACTION_DRAG_LOCATION -> this.showTarget(view, event, kind)
			DragEvent.ACTION_DROP -> this.commit(view, event, kind)
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

	private fun showTarget(receiver: View, event: DragEvent, kind: Drag) {
		val (col, row) = this.snap(receiver, event, kind)
		val candidate = WidgetLayout(kind.widgetId, col, row, kind.colSpan, kind.rowSpan)
		val fits = WidgetGrid.fits(this.vgWidgets.collectOccupied(kind.exclude), candidate)

		this.vgWidgets.showMoveTarget(col, row, kind.colSpan, kind.rowSpan, fits)
	}

	private fun commit(receiver: View, event: DragEvent, kind: Drag) {
		val (col, row) = this.snap(receiver, event, kind)
		this.vgWidgets.hideMoveTarget()

		when (kind) {
			is Drag.Widget -> kind.container.commitMove(col, row)
			is Drag.DesktopApp -> kind.host.moveTo(kind.view, col, row)
			is Drag.DashApp -> kind.host.pinAt(kind.app, col, row, this.pager.currentPage)
			is Drag.LauncherPin -> {
				// Move: pin onto the desktop, then unpin from the bar //
				if (kind.host.pinAt(kind.app, col, row, this.pager.currentPage)) {
					this.parent.appManager?.unpin(kind.app, false)
				}
			}
		}
	}

	/**
	 * Snaps the drag to a grid cell, centring the non-view (dash/launcher) drags.
	 * Returns portrait canonical (col, row) — the display-space snap is inverse-
	 * transformed so the result can be stored directly in LayoutParams.
	 */
	private fun snap(receiver: View, event: DragEvent, kind: Drag): Pair<Int, Int> {
		val receiverLocation = IntArray(2)
		val gridLocation = IntArray(2)
		receiver.getLocationOnScreen(receiverLocation)
		this.vgWidgets.getLocationOnScreen(gridLocation)

		val gridX = event.x.toInt() + receiverLocation[0] - gridLocation[0]
		val gridY = event.y.toInt() + receiverLocation[1] - gridLocation[1]
		val left = gridX - kind.grabOffsetX(this.vgWidgets) - this.vgWidgets.paddingLeft
		val top = gridY - kind.grabOffsetY(this.vgWidgets) - this.vgWidgets.paddingTop

		val rotation = this.vgWidgets.displayRotation
		// Compute the display-space span so the snap boundary is correct in landscape.
		val displaySpan = WidgetGrid.portraitToDisplay(
			0, 0, kind.colSpan, kind.rowSpan, rotation)
		val dColSpan = displaySpan.colSpan
		val dRowSpan = displaySpan.rowSpan

		val displayCol = WidgetGrid.snapToCell(
			left, this.vgWidgets.cellWidth, this.vgWidgets.displayCols() - dColSpan)
		val displayRow = WidgetGrid.snapToCell(
			top, this.vgWidgets.cellHeight, this.vgWidgets.displayRows() - dRowSpan)

		// Inverse-transform display col/row back to portrait canonical coords.
		val portrait = WidgetGrid.displayToPortrait(displayCol, displayRow, dColSpan, dRowSpan, rotation)
		return portrait.col to portrait.row
	}

	private fun kindOf(event: DragEvent): Drag? {
		when (val localState = event.localState) {
			is WidgetContainer -> return Drag.Widget(localState)
			is DesktopAppView -> {
				val host = this.parent.desktopAppHost ?: return null

				return Drag.DesktopApp(localState, host)
			}
			is App -> {
				val host = this.parent.desktopAppHost ?: return null

				return Drag.DashApp(localState, host)
			}
		}

		// Otherwise it is a launcher-bar pin reorder: the clip's label is the
		// dragged icon's index into the current desktop's pinned list //
		val host = this.parent.desktopAppHost ?: return null
		val appManager = this.parent.appManager ?: return null
		val index = event.clipDescription?.label?.toString()?.toIntOrNull() ?: return null
		val app = appManager.pinned.getOrNull(index) ?: return null

		return Drag.LauncherPin(app, host)
	}

	/**
	 * The drag's kind, carrying its grid footprint and how to find its grab
	 * offset (the distance from the dragged thing's top-left to the finger).
	 */
	private sealed class Drag {
		abstract val widgetId: Int
		abstract val colSpan: Int
		abstract val rowSpan: Int
		/** The view to ignore in collision checks (the dragged one), or null. */
		abstract val exclude: View?

		abstract fun grabOffsetX(grid: WidgetsContainer): Int
		abstract fun grabOffsetY(grid: WidgetsContainer): Int

		class Widget(val container: WidgetContainer) : Drag() {
			private val lp get() = this.container.layoutParams as WidgetsContainer.LayoutParams
			override val widgetId get() = this.container.appWidgetId
			override val colSpan get() = this.lp.colSpan
			override val rowSpan get() = this.lp.rowSpan
			override val exclude get() = this.container
			override fun grabOffsetX(grid: WidgetsContainer) = this.container.dragGrabOffsetX
			override fun grabOffsetY(grid: WidgetsContainer) = this.container.dragGrabOffsetY
		}

		class DesktopApp(val view: DesktopAppView, val host: DesktopAppHost) : Drag() {
			override val widgetId = DesktopAppLayout.NO_WIDGET_ID
			override val colSpan = DesktopAppLayout.SPAN
			override val rowSpan = DesktopAppLayout.SPAN
			override val exclude get() = this.view
			override fun grabOffsetX(grid: WidgetsContainer) = this.view.dragGrabOffsetX
			override fun grabOffsetY(grid: WidgetsContainer) = this.view.dragGrabOffsetY
		}

		/** A non-view drag (from the dash or the bar): centre the block under the finger. */
		sealed class IncomingApp : Drag() {
			override val widgetId = DesktopAppLayout.NO_WIDGET_ID
			override val colSpan = DesktopAppLayout.SPAN
			override val rowSpan = DesktopAppLayout.SPAN
			override val exclude: View? = null
			override fun grabOffsetX(grid: WidgetsContainer) = this.colSpan * grid.cellWidth / 2
			override fun grabOffsetY(grid: WidgetsContainer) = this.rowSpan * grid.cellHeight / 2
		}

		class DashApp(val app: App, val host: DesktopAppHost) : IncomingApp()

		class LauncherPin(val app: App, val host: DesktopAppHost) : IncomingApp()
	}
}
