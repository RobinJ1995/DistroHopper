package be.robinj.distrohopper.widgets

import android.os.Handler
import android.os.Looper
import android.view.DragEvent
import android.view.View
import be.robinj.distrohopper.App
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.home.LauncherBarBinder

/**
 * Handles drops over the topmost desktop layer while the system drag shadow
 * follows the finger. The underlying widget grid draws the snapped landing
 * target. All coordinates target the pager's current desktop.
 *
 * Drag kinds, told apart by the drag's local state:
 *  - a [WidgetContainer] — a widget being moved on its grid;
 *  - a [DesktopAppView] — a desktop app being moved on its grid;
 *  - a [DesktopFolderView] — a desktop folder being moved on its grid;
 *  - an [App] — a not-yet-pinned app dragged from the dash, pinned to the desktop;
 *  - otherwise the launcher-bar reorder clip (label = pinned index) — moved here.
 *
 * Pausing a desktop app over another desktop app/folder, or a widget over a
 * folder, arms a **fold** (create / add to folder); a quick pass instead moves.
 */
internal class WidgetsContainer_DragListener(
	private val parent: HomeActivity,
	private val pager: WidgetsPager,
) : View.OnDragListener {
	private val vgWidgets: WidgetsContainer
		get() = this.pager.currentPageContainer

	private val handler = Handler(Looper.getMainLooper())
	private var hoverCol = -1
	private var hoverRow = -1
	private var armedFoldTarget: View? = null

	override fun onDrag(view: View, event: DragEvent): Boolean {
		val kind = this.kindOf(event) ?: return false

		when (event.action) {
			// Must claim the drag here to receive LOCATION/DROP for it //
			DragEvent.ACTION_DRAG_STARTED -> return true
			DragEvent.ACTION_DRAG_LOCATION -> this.onLocation(view, event, kind)
			DragEvent.ACTION_DROP -> this.commit(view, event, kind)
			DragEvent.ACTION_DRAG_EXITED -> {
				this.clearFoldArm()
				this.vgWidgets.hideMoveTarget()
			}
			DragEvent.ACTION_DRAG_ENDED -> {
				this.clearFoldArm()
				this.vgWidgets.hideMoveTarget()

				// Not via appManager: widgets are draggable before app loading finishes.
				// Posted: mutating views during ENDED dispatch throws a
				// ConcurrentModificationException //
				view.post { LauncherBarBinder.stoppedDragging(this.parent) }
			}
		}

		return true
	}

	private fun onLocation(receiver: View, event: DragEvent, kind: Drag) {
		// While the dash is open the drop goes INTO the dash (see dropIntoDash), not
		// the desktop, so don't paint a landing target / fold ring on the desktop
		// behind it (this listener still gets LOCATION because the dash grid never
		// claimed the drag) //
		if (this.parent.dashIsOpen()) {
			this.clearFoldArm()
			this.vgWidgets.hideMoveTarget()
			this.hoverCol = -1
			this.hoverRow = -1
			return
		}

		val (col, row) = this.snap(receiver, event, kind)

		if (col != this.hoverCol || row != this.hoverRow) {
			this.hoverCol = col
			this.hoverRow = row
			this.clearFoldArm()

			val target = this.foldTarget(col, row, kind)
			if (target != null) {
				this.handler.postDelayed({
					this.armedFoldTarget = target
					target.setBackgroundResource(R.drawable.dash_folder_drop_indicator)
					// Drop the "doesn't fit" move-target shown while dwelling: no further
					// LOCATION fires to hit the else-branch below if the pointer holds
					// still over the target, so both borders would otherwise show //
					this.vgWidgets.hideMoveTarget()
				}, FOLD_DWELL_MS)
			}
		}

		if (this.armedFoldTarget == null) {
			val candidate = WidgetLayout(kind.widgetId, col, row, kind.colSpan, kind.rowSpan)
			val fits = WidgetGrid.fits(this.vgWidgets.collectOccupied(kind.exclude), candidate)
			this.vgWidgets.showMoveTarget(col, row, kind.colSpan, kind.rowSpan, fits)
		} else {
			this.vgWidgets.hideMoveTarget()
		}
	}

	private fun commit(receiver: View, event: DragEvent, kind: Drag) {
		val (col, row) = this.snap(receiver, event, kind)
		this.vgWidgets.hideMoveTarget()
		val foldTarget = this.armedFoldTarget
		this.clearFoldArm()

		// Dropped while the dash is open = dropped INTO the dash (the desktop is
		// behind it; had the user wanted the desktop they'd have hovered the
		// launcher/panel to close the dash first). The app returns to just the app
		// drawer: remove it from whatever surface it came from, never land it on
		// the desktop behind. This listener gets the drop because the dash views
		// were GONE at drag start and so never claimed it (they can't, by then) //
		if (this.parent.dashIsOpen()) {
			this.dropIntoDash(kind)
			return
		}

		if (foldTarget != null && this.fold(kind, foldTarget)) {
			return
		}

		when (kind) {
			is Drag.Widget -> kind.container.commitMove(col, row)
			is Drag.DesktopApp -> kind.host.moveTo(kind.view, col, row)
			is Drag.DesktopFolder -> kind.host.moveTo(kind.view, col, row)
			is Drag.FolderMember ->
				kind.host.removeMember(kind.payload.folderId, kind.payload.member,
					col, row, this.pager.currentPage)
			is Drag.DashApp -> kind.host.pinAt(kind.app, col, row, this.pager.currentPage)
			is Drag.LauncherPin ->
				if (kind.host.pinAt(kind.app, col, row, this.pager.currentPage)) {
					this.parent.appManager?.unpin(kind.app, false)
				}
			is Drag.LauncherFolderMember ->
				if (kind.host.pinAt(kind.app, col, row, this.pager.currentPage)) {
					// Now on the desktop: ungroup it and drop the pin, so it leaves the
					// launcher entirely (the folder dissolves at one app via reconcile) //
					this.parent.appManager?.let {
						it.launcherLayout.removeFromFolder(kind.folderId, kind.app.profileScopedKey)
						it.unpin(kind.app, false)
						it.launcherLayoutChanged()
					}
				}
		}
	}

	/**
	 * Drop committed while the dash is open: the app returns to just the app drawer,
	 * so remove it from its source surface (a widget/folder, which can't live in the
	 * dash, simply stays put — the drag cancels). Never lands anything on the
	 * desktop behind the dash.
	 */
	private fun dropIntoDash(kind: Drag) {
		val appManager = this.parent.appManager
		when (kind) {
			is Drag.LauncherPin -> appManager?.unpin(kind.app, false)
			is Drag.LauncherFolderMember -> appManager?.let {
				it.launcherLayout.removeFromFolder(kind.folderId, kind.app.profileScopedKey)
				it.unpin(kind.app, false)
				it.launcherLayoutChanged()
			}
			is Drag.DesktopApp -> kind.host.remove(kind.view)
			is Drag.FolderMember -> kind.host.deleteMember(kind.payload.folderId, kind.payload.member)
			// A dash app is already in the drawer; a widget or whole folder can't go
			// into the dash — leave them where they were (the drag just cancels) //
			is Drag.DashApp, is Drag.DesktopFolder, is Drag.Widget -> Unit
		}
	}

	/** The fold target under ([col],[row]) for this drag, or null if folding doesn't apply. */
	private fun foldTarget(col: Int, row: Int, kind: Drag): View? {
		if (this.parent.desktopFolderHost == null) {
			return null
		}
		val target = this.vgWidgets.findViewAtCell(col, row) ?: return null
		if (target === kind.exclude) {
			return null
		}

		return when (kind) {
			// An on-desktop app folds onto another app (create) or a folder (add).
			is Drag.DesktopApp ->
				if (target is DesktopAppView || target is DesktopFolderView) target else null
			// A widget can only be added to an existing folder.
			is Drag.Widget -> if (target is DesktopFolderView) target else null
			else -> null
		}
	}

	private fun fold(kind: Drag, target: View): Boolean {
		val folderHost = this.parent.desktopFolderHost ?: return false

		return when (kind) {
			is Drag.DesktopApp -> {
				when (target) {
					is DesktopAppView -> { folderHost.createFolder(kind.view, target); true }
					is DesktopFolderView -> { folderHost.addApp(target.folderId, kind.view); true }
					else -> false
				}
			}
			is Drag.Widget -> {
				if (target is DesktopFolderView) {
					folderHost.addWidget(target.folderId, kind.container); true
				} else {
					false
				}
			}
			else -> false
		}
	}

	private fun clearFoldArm() {
		this.handler.removeCallbacksAndMessages(null)
		this.armedFoldTarget?.background = null
		this.armedFoldTarget = null
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
			is DesktopFolderView -> {
				val host = this.parent.desktopFolderHost ?: return null

				return Drag.DesktopFolder(localState, host)
			}
			is DesktopFolderMemberDrag -> {
				val host = this.parent.desktopFolderHost ?: return null

				return Drag.FolderMember(localState, host)
			}
			is be.robinj.distrohopper.desktop.launcher.LauncherDragPayload.FolderMemberDrag -> {
				val host = this.parent.desktopAppHost ?: return null

				return Drag.LauncherFolderMember(localState.app, localState.folderId, host)
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

		class DesktopFolder(val view: DesktopFolderView, val host: DesktopFolderHost) : Drag() {
			override val widgetId = DesktopAppLayout.NO_WIDGET_ID
			override val colSpan = DesktopFolderLayout.SPAN
			override val rowSpan = DesktopFolderLayout.SPAN
			override val exclude get() = this.view
			override fun grabOffsetX(grid: WidgetsContainer) = this.view.dragGrabOffsetX
			override fun grabOffsetY(grid: WidgetsContainer) = this.view.dragGrabOffsetY
		}

		/** A member pulled out of an open desktop folder; lands loose at the drop cell. */
		class FolderMember(val payload: DesktopFolderMemberDrag, val host: DesktopFolderHost) : Drag() {
			override val widgetId = DesktopAppLayout.NO_WIDGET_ID
			override val colSpan get() = this.payload.colSpan
			override val rowSpan get() = this.payload.rowSpan
			override val exclude: View? = null
			override fun grabOffsetX(grid: WidgetsContainer) = this.colSpan * grid.cellWidth / 2
			override fun grabOffsetY(grid: WidgetsContainer) = this.rowSpan * grid.cellHeight / 2
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

		/** An app pulled out of a *launcher* folder, dropped on the desktop: placed
		 *  like a dash app, then removed from its folder and unpinned off the bar,
		 *  so it leaves the launcher entirely — like dropping a dock pin here. */
		class LauncherFolderMember(val app: App, val folderId: String, val host: DesktopAppHost) : IncomingApp()
	}

	companion object {
		private const val FOLD_DWELL_MS = 550L
	}
}
