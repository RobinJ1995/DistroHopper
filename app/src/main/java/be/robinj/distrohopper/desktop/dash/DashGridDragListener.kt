package be.robinj.distrohopper.desktop.dash

import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.view.DragEvent
import android.view.View
import android.widget.AdapterView
import android.widget.GridView
import android.widget.Toast
import be.robinj.distrohopper.App
import be.robinj.distrohopper.AppManager
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.home.LauncherBarBinder

/**
 * Handles dragging within the dash grid: folder creation (pause an app over
 * another app), adding to a folder (drop on it), reordering (custom order only)
 * and extracting a folder member (drop a member on the dash to pull it out).
 *
 * A loose dash app's drag carries its [App] as local state (so dropping on the
 * launcher still pins it); folders and folder members carry a [DashDragPayload].
 * The listener claims the drag at [DragEvent.ACTION_DRAG_STARTED] only for these
 * dash kinds, leaving widget/other drags to their own listeners.
 */
class DashGridDragListener(
	private val activity: HomeActivity,
	private val appManager: AppManager,
	private val profile: UserHandle?,
) : View.OnDragListener {
	private val handler = Handler(Looper.getMainLooper())
	private var hoverPosition = AdapterView.INVALID_POSITION
	private var armedPosition = AdapterView.INVALID_POSITION
	private var highlighted: View? = null

	override fun onDrag(view: View, event: DragEvent): Boolean {
		val grid = view as? GridView ?: return false

		when (event.action) {
			DragEvent.ACTION_DRAG_STARTED -> return this.draggedApp(event) != null ||
				event.localState is DashDragPayload

			DragEvent.ACTION_DRAG_LOCATION -> this.onLocation(grid, event)

			DragEvent.ACTION_DROP -> this.onDrop(grid, event)

			DragEvent.ACTION_DRAG_EXITED -> this.clearHover()

			DragEvent.ACTION_DRAG_ENDED -> {
				this.clearHover()
				// Must not mutate views during ENDED dispatch (see AGENTS.md). //
				grid.post { LauncherBarBinder.stoppedDragging(this.activity) }
			}
		}

		return true
	}

	private fun onLocation(grid: GridView, event: DragEvent) {
		val position = grid.pointToPosition(event.x.toInt(), event.y.toInt())
		if (position == this.hoverPosition) {
			return
		}

		this.hoverPosition = position
		this.handler.removeCallbacksAndMessages(null)
		this.clearHighlight()
		this.armedPosition = AdapterView.INVALID_POSITION

		val dragged = this.draggedApp(event) ?: return
		val target = this.itemAt(grid, position) ?: return

		// Pausing an app over a different app (or any app over a folder) arms a
		// folder create/add; a quick pass instead reorders (custom order only).
		val canFold = when (target) {
			is DashItem.FolderItem -> true
			is DashItem.AppItem -> target.app != dragged
		}
		if (canFold) {
			this.handler.postDelayed({
				this.armedPosition = position
				this.highlight(grid, position)
			}, FOLDER_DWELL_MS)
		}
	}

	private fun onDrop(grid: GridView, event: DragEvent) {
		val position = grid.pointToPosition(event.x.toInt(), event.y.toInt())
		val armed = this.armedPosition == position && position != AdapterView.INVALID_POSITION
		val target = this.itemAt(grid, position)
		val layout = this.appManager.dashLayout

		when (val state = event.localState) {
			is App -> this.dropApp(state, target, armed, position)

			is DashDragPayload.FolderMemberDrag -> {
				// Pull the member out, then fold/add if it landed on another item.
				layout.removeFromFolder(state.folderId, state.app.profileScopedKey)
				if (armed) {
					this.dropApp(state.app, target, true, position)
				}
				this.appManager.dashLayoutChanged()
			}

			is DashDragPayload.FolderDrag -> {
				// Folders only reposition (custom order); they never enter folders.
				if (FolderPopup.customOrderingEnabled(this.activity) &&
					position != AdapterView.INVALID_POSITION) {
					this.reorderTo(grid, state.folderId, position)
				}
			}
		}
	}

	private fun dropApp(app: App, target: DashItem?, armed: Boolean, position: Int) {
		val layout = this.appManager.dashLayout

		if (armed && target is DashItem.AppItem && target.app != app) {
			layout.createFolder(app, target.app)
		} else if (armed && target is DashItem.FolderItem) {
			if (!layout.addToFolder(target.folder.id, app)) {
				Toast.makeText(this.activity, R.string.folder_full, Toast.LENGTH_SHORT).show()
			}
		} else if (FolderPopup.customOrderingEnabled(this.activity) &&
			position != AdapterView.INVALID_POSITION) {
			this.moveAppTo(app, position)
		}

		this.appManager.dashLayoutChanged()
	}

	/** Reorders a loose app to the dropped grid position (custom order). */
	private fun moveAppTo(app: App, toPosition: Int) {
		val items = this.appManager.dashLayout.dashItems(this.profile)
		val from = items.indexOfFirst { it is DashItem.AppItem && it.app == app }
		if (from >= 0) {
			this.appManager.dashLayout.moveItem(this.profile, from, toPosition)
		}
	}

	private fun reorderTo(grid: GridView, folderId: String, toPosition: Int) {
		val items = this.appManager.dashLayout.dashItems(this.profile)
		val from = items.indexOfFirst { it is DashItem.FolderItem && it.folder.id == folderId }
		if (from >= 0) {
			this.appManager.dashLayout.moveItem(this.profile, from, toPosition)
			this.appManager.dashLayoutChanged()
		}
	}

	/** The dragged loose/extracted app, or null when the drag is a whole folder. */
	private fun draggedApp(event: DragEvent): App? = when (val state = event.localState) {
		is App -> state
		is DashDragPayload.FolderMemberDrag -> state.app
		else -> null
	}

	private fun itemAt(grid: GridView, position: Int): DashItem? {
		if (position == AdapterView.INVALID_POSITION) {
			return null
		}

		return grid.adapter?.getItem(position) as? DashItem
	}

	private fun highlight(grid: GridView, position: Int) {
		val child = grid.getChildAt(position - grid.firstVisiblePosition) ?: return
		child.setBackgroundResource(R.drawable.dash_folder_drop_indicator)
		this.highlighted = child
	}

	private fun clearHighlight() {
		this.highlighted?.background = null
		this.highlighted = null
	}

	private fun clearHover() {
		this.handler.removeCallbacksAndMessages(null)
		this.clearHighlight()
		this.hoverPosition = AdapterView.INVALID_POSITION
		this.armedPosition = AdapterView.INVALID_POSITION
	}

	companion object {
		private const val FOLDER_DWELL_MS = 550L
	}
}
