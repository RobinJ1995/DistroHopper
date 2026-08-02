package be.robinj.distrohopper.desktop.dash

import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.view.DragEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.AdapterView
import android.widget.GridView
import android.widget.Toast
import be.robinj.distrohopper.App
import be.robinj.distrohopper.AppManager
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.desktop.launcher.LauncherDragPayload
import be.robinj.distrohopper.home.LauncherBarBinder

/**
 * Handles dragging within the dash grid: reordering (custom order only), folder
 * creation / adding, and extracting a folder member.
 *
 * A loose dash app and a whole folder are dragged with a *live* preview that
 * mirrors the launcher bar: the dragged cell becomes an empty placeholder (the
 * gap the drop will land in) and the other icons flow around it as the finger
 * moves, so the user can see exactly where the app will drop. The distinction
 * between reordering and foldering is spatial, like the launcher: over a cell's
 * centre rings it for a folder create/add (the gap freezes where it is); over a
 * cell's edge (or the space between cells) opens a gap there to reorder into.
 *
 * A loose dash app's drag carries its [App] as local state (so dropping on the
 * launcher still pins it); folders and folder members carry a [DashDragPayload].
 * Folder-member extraction keeps its simpler pause-to-fold gesture. The listener
 * claims the drag at [DragEvent.ACTION_DRAG_STARTED] only for these dash kinds,
 * leaving widget/other drags to their own listeners.
 */
class DashGridDragListener(
	private val activity: HomeActivity,
	private val appManager: AppManager,
	private val profile: UserHandle?,
) : View.OnDragListener {
	private val handler = Handler(Looper.getMainLooper())
	private val reflowInterpolator = DecelerateInterpolator()

	/** Auto-scrolls the grid when a drag lingers near its top/bottom edge. */
	private var edgeScroller: DashEdgeScroller? = null
	/** The last drag position, so an auto-scroll can re-resolve the target there. */
	private var lastX = 0f
	private var lastY = 0f

	// --- Folder-member extraction (pause-to-fold, no reorder preview) ---
	private var hoverPosition = AdapterView.INVALID_POSITION
	private var armedPosition = AdapterView.INVALID_POSITION
	/** The extracted member being dragged, so an auto-scroll can re-resolve its hover. */
	private var memberApp: App? = null

	// --- Loose app / folder reorder preview ---
	/** The dragged item's [stableKey], or null when this drag isn't previewed. */
	private var draggedKey: String? = null
	/** The dragged item itself, re-inserted into the preview at [previewIndex]. */
	private var draggedItem: DashItem? = null
	/** The display order with the dragged item removed (the model never changes mid-drag). */
	private var baseItems: List<DashItem> = emptyList()
	/** Where the dragged placeholder currently sits within [baseItems]. */
	private var previewIndex = 0
	/** The item ringed for a fold, or null when a reorder gap is showing instead. */
	private var foldTargetKey: String? = null

	private var highlighted: View? = null

	override fun onDrag(view: View, event: DragEvent): Boolean {
		val grid = view as? GridView ?: return false

		when (event.action) {
			DragEvent.ACTION_DRAG_STARTED -> {
				val claim = this.draggedApp(event) != null || event.localState is DashDragPayload
				if (claim) {
					this.resetPreview() // never carry state over from a previous drag //
					this.memberApp = (event.localState as? DashDragPayload.FolderMemberDrag)?.app
					this.setupPreview(grid, event)
				}
				return claim
			}

			DragEvent.ACTION_DRAG_LOCATION -> {
				this.lastX = event.x
				this.lastY = event.y
				this.edgeScrollerFor(grid).onDrag(event.y)
				if (this.isMemberDrag(event)) {
					this.onMemberLocation(grid, event.x, event.y)
				} else {
					this.resolveAndPreview(grid, event.x, event.y)
				}
			}

			DragEvent.ACTION_DROP -> {
				this.edgeScroller?.stop()
				if (this.isMemberDrag(event)) {
					this.onMemberDrop(grid, event)
				} else {
					this.onPreviewDrop(event)
				}
			}

			DragEvent.ACTION_DRAG_EXITED -> {
				this.edgeScroller?.stop()
				if (this.isMemberDrag(event)) {
					this.clearHover()
				} else {
					this.setFold(grid, null) // keep the gap; only drop the fold ring //
				}
			}

			DragEvent.ACTION_DRAG_ENDED -> {
				this.edgeScroller?.stop()
				this.clearHover()
				val hadPreview = this.draggedKey != null
				val adapter = grid.adapter as? GridAdapter
				// Must not mutate views during ENDED dispatch (see AGENTS.md). //
				grid.post {
					if (hadPreview) {
						// Restore the app and, for a cancelled (uncommitted) drag, the
						// model's order — the adapter may still hold the preview. //
						adapter?.setHiddenKey(null)
						this.appManager.dashLayoutChanged()
					}
					this.resetPreview()
					LauncherBarBinder.stoppedDragging(this.activity)
				}
			}
		}

		return true
	}

	// --- Loose app / folder reorder preview -------------------------------------

	private fun isMemberDrag(event: DragEvent): Boolean =
		event.localState is DashDragPayload.FolderMemberDrag

	/** The lazily-created edge scroller for this page's grid. */
	private fun edgeScrollerFor(grid: GridView): DashEdgeScroller =
		this.edgeScroller ?: DashEdgeScroller(grid) {
			// A row scrolled under a still finger: re-resolve at the last pointer
			// position so a release without further movement acts on the cell now
			// under the finger, not the pre-scroll one. Both paths need this — the
			// loose/folder preview caches previewIndex/foldTargetKey, and the member
			// gesture caches its armed hover/highlight (onMemberDrop recomputes the
			// target cell, but the fold only fires when armedPosition still matches).
			if (this.draggedKey != null) {
				this.resolveAndPreview(grid, this.lastX, this.lastY)
			} else if (this.memberApp != null) {
				this.onMemberLocation(grid, this.lastX, this.lastY)
			}
		}.also { this.edgeScroller = it }

	/**
	 * Sets up the reorder preview for a loose app or a whole folder: the dragged
	 * cell becomes an empty placeholder and the rest of the grid is captured as
	 * the base order the placeholder slides through. A no-op for member drags and
	 * for a grid whose page doesn't hold the dragged item (another profile).
	 */
	private fun setupPreview(grid: GridView, event: DragEvent) {
		val key = this.draggedKeyOf(event) ?: return
		val items = this.appManager.dashLayout.dashItems(this.profile)
		val index = items.indexOfFirst { it.stableKey == key }
		if (index < 0) {
			return // this page doesn't hold the dragged item //
		}

		this.draggedItem = items[index]
		this.draggedKey = key
		this.baseItems = items.filterIndexed { i, _ -> i != index }
		this.previewIndex = index
		this.foldTargetKey = null
		(grid.adapter as? GridAdapter)?.setHiddenKey(key)
	}

	/** The [stableKey] of the item a preview-capable drag is carrying, else null. */
	private fun draggedKeyOf(event: DragEvent): String? = when (val state = event.localState) {
		is App -> DashItem.AppItem(state).stableKey
		is LauncherDragPayload.PinnedAppDrag -> DashItem.AppItem(state.app).stableKey
		is DashDragPayload.FolderDrag -> "folder:" + state.folderId
		else -> null
	}

	/**
	 * Resolves the drag over the grid into a fold (over a cell's centre) or a
	 * reorder gap (over an edge / between cells) and previews it.
	 */
	private fun resolveAndPreview(grid: GridView, x: Float, y: Float) {
		if (this.draggedKey == null) {
			return
		}
		val custom = FolderPopup.customOrderingEnabled(this.activity)
		val px = x.toInt()
		val py = y.toInt()
		val position = grid.pointToPosition(px, py)

		if (position == AdapterView.INVALID_POSITION) {
			// No cell under the pointer. Only the empty area *below* the last
			// laid-out cell is a genuine append; other invalid spots — the profile
			// title padding reserved above the first row, or the gaps beside cells —
			// must NOT move the gap, or a hover/drop there would jump the item to
			// the end and corrupt the manual order.
			this.setFold(grid, null)
			if (custom && this.isBelowLastCell(grid, py)) {
				this.previewInsertAt(grid, this.baseItems.size)
			}
			return
		}

		val over = this.itemAt(grid, position)
		val frac = this.horizontalFraction(grid, position, px)
		val draggedIsApp = this.draggedItem is DashItem.AppItem
		val foldable = draggedIsApp && over != null && over.stableKey != this.draggedKey &&
			(over is DashItem.FolderItem || over is DashItem.AppItem)

		if (foldable && frac in FOLD_ZONE_LO..FOLD_ZONE_HI) {
			// Over a cell's centre: ring it and freeze the gap (drop = fold). //
			this.setFold(grid, over!!.stableKey)
			return
		}

		this.setFold(grid, null)
		if (!custom || (over != null && over.stableKey == this.draggedKey)) {
			return // reorder is custom-only; and never past our own gap //
		}

		val baseIndex = if (over == null) {
			this.baseItems.size
		} else {
			this.baseItems.indexOfFirst { it.stableKey == over.stableKey }
		}
		if (baseIndex < 0) {
			return
		}
		val insertIndex = (baseIndex + if (frac >= 0.5f) 1 else 0).coerceIn(0, this.baseItems.size)
		this.previewInsertAt(grid, insertIndex)
	}

	/** Moves the placeholder to [index] within the base order, flowing the icons around it. */
	private fun previewInsertAt(grid: GridView, index: Int) {
		if (index == this.previewIndex) {
			return
		}
		val item = this.draggedItem ?: return
		val adapter = grid.adapter as? GridAdapter ?: return

		val before = this.captureChildBounds(grid)
		this.previewIndex = index
		val preview = ArrayList(this.baseItems)
		preview.add(index, item)
		adapter.setItems(preview)
		this.animateReflow(grid, before)
	}

	private fun onPreviewDrop(event: DragEvent) {
		val fold = this.foldTargetKey
		val custom = FolderPopup.customOrderingEnabled(this.activity)

		when (val state = event.localState) {
			is App -> this.dropApp(state, fold, custom)

			// A launcher-pinned app picked up from the dash rearranges the dash like
			// any other icon; its launcher pin is untouched either way. //
			is LauncherDragPayload.PinnedAppDrag -> this.dropApp(state.app, fold, custom)

			is DashDragPayload.FolderDrag ->
				// Folders only reposition (custom order); they never enter folders. //
				if (custom) {
					this.commitMove("folder:" + state.folderId)
				}
		}

		this.appManager.dashLayoutChanged()
	}

	/** Commits an app drop: fold it onto the ringed target, else reorder it there. */
	private fun dropApp(app: App, foldTargetKey: String?, custom: Boolean) {
		if (foldTargetKey != null) {
			this.foldOnto(app, foldTargetKey)
		} else if (custom) {
			this.commitMove(DashItem.AppItem(app).stableKey)
		}
	}

	/** Commits the previewed reorder: move the item to where its placeholder rests. */
	private fun commitMove(key: String) {
		val items = this.appManager.dashLayout.dashItems(this.profile)
		val from = items.indexOfFirst { it.stableKey == key }
		if (from < 0) {
			return
		}
		this.appManager.dashLayout.moveItem(
			this.profile, from, this.previewIndex.coerceIn(0, items.size - 1))
	}

	/** Folds the dragged app onto the target: create a folder, or add to one. */
	private fun foldOnto(app: App, targetKey: String) {
		val target = this.appManager.dashLayout.dashItems(this.profile)
			.firstOrNull { it.stableKey == targetKey }
		val layout = this.appManager.dashLayout

		if (target is DashItem.AppItem && target.app != app) {
			layout.createFolder(app, target.app)
		} else if (target is DashItem.FolderItem) {
			if (!layout.addToFolder(target.folder.id, app)) {
				Toast.makeText(this.activity, R.string.folder_full, Toast.LENGTH_SHORT).show()
			}
		}
	}

	private fun setFold(grid: GridView, key: String?) {
		if (this.foldTargetKey == key) {
			return
		}
		this.foldTargetKey = key
		this.clearHighlight()
		if (key != null) {
			this.highlightKey(grid, key)
		}
	}

	private fun resetPreview() {
		this.draggedKey = null
		this.draggedItem = null
		this.baseItems = emptyList()
		this.previewIndex = 0
		this.foldTargetKey = null
		this.memberApp = null
	}

	// --- Reflow animation -------------------------------------------------------

	/** Records each visible cell's position (keyed by item) before a reorder. */
	private fun captureChildBounds(grid: GridView): HashMap<String, IntArray> {
		val bounds = HashMap<String, IntArray>()
		for (i in 0 until grid.childCount) {
			val child = grid.getChildAt(i)
			val key = (child.tag as? DashItem)?.stableKey ?: continue
			bounds[key] = intArrayOf(child.left, child.top)
		}
		return bounds
	}

	/**
	 * After the reorder relays out the grid, translates each cell from its old
	 * position to its new one and animates the offset away — so the icons appear
	 * to slide into place rather than jumping (the same "make space" feel the
	 * launcher bar gets for free from its LayoutTransition).
	 */
	private fun animateReflow(grid: GridView, before: Map<String, IntArray>) {
		grid.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
			override fun onPreDraw(): Boolean {
				grid.viewTreeObserver.removeOnPreDrawListener(this)
				for (i in 0 until grid.childCount) {
					val child = grid.getChildAt(i)
					val key = (child.tag as? DashItem)?.stableKey ?: continue
					if (key == this@DashGridDragListener.draggedKey) {
						continue // the invisible placeholder //
					}
					val old = before[key] ?: continue
					val dx = old[0] - child.left
					val dy = old[1] - child.top
					if (dx == 0 && dy == 0) {
						continue
					}
					child.translationX = dx.toFloat()
					child.translationY = dy.toFloat()
					child.animate().translationX(0F).translationY(0F)
						.setDuration(REFLOW_MS)
						.setInterpolator(this@DashGridDragListener.reflowInterpolator)
						.start()
				}
				return true
			}
		})
	}

	// --- Folder-member extraction (unchanged pause-to-fold gesture) -------------

	private fun onMemberLocation(grid: GridView, x: Float, y: Float) {
		val position = grid.pointToPosition(x.toInt(), y.toInt())
		if (position == this.hoverPosition) {
			return
		}

		this.hoverPosition = position
		this.handler.removeCallbacksAndMessages(null)
		this.clearHighlight()
		this.armedPosition = AdapterView.INVALID_POSITION

		val dragged = this.memberApp ?: return
		val target = this.itemAt(grid, position) ?: return

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

	private fun onMemberDrop(grid: GridView, event: DragEvent) {
		val position = grid.pointToPosition(event.x.toInt(), event.y.toInt())
		val armed = this.armedPosition == position && position != AdapterView.INVALID_POSITION
		val target = this.itemAt(grid, position)
		val layout = this.appManager.dashLayout
		val state = event.localState as? DashDragPayload.FolderMemberDrag ?: return

		// Pull the member out, then fold/add if it landed on another item. //
		layout.removeFromFolder(state.folderId, state.app.profileScopedKey)
		if (armed && target is DashItem.AppItem && target.app != state.app) {
			layout.createFolder(state.app, target.app)
		} else if (armed && target is DashItem.FolderItem) {
			if (!layout.addToFolder(target.folder.id, state.app)) {
				Toast.makeText(this.activity, R.string.folder_full, Toast.LENGTH_SHORT).show()
			}
		}
		this.appManager.dashLayoutChanged()
	}

	/**
	 * The dragged loose/extracted app, or null when the drag is a whole folder.
	 * A [LauncherDragPayload.PinnedAppDrag] counts: it is a launcher-pinned app
	 * picked up *from the dash* (only the dash's long-click listener creates one),
	 * so within the dash it rearranges exactly like any other app icon.
	 */
	private fun draggedApp(event: DragEvent): App? = when (val state = event.localState) {
		is App -> state
		is LauncherDragPayload.PinnedAppDrag -> state.app
		is DashDragPayload.FolderMemberDrag -> state.app
		else -> null
	}

	// --- Shared helpers ---------------------------------------------------------

	private fun itemAt(grid: GridView, position: Int): DashItem? {
		if (position == AdapterView.INVALID_POSITION) {
			return null
		}

		return grid.adapter?.getItem(position) as? DashItem
	}

	/** Whether [y] (grid-relative) is past the bottom of the last laid-out cell. */
	private fun isBelowLastCell(grid: GridView, y: Int): Boolean {
		val last = grid.getChildAt(grid.childCount - 1) ?: return false
		return y > last.bottom
	}

	/** The drag's horizontal position within the cell at [position] (0..1); 0.5 if unknown. */
	private fun horizontalFraction(grid: GridView, position: Int, x: Int): Float {
		val child = grid.getChildAt(position - grid.firstVisiblePosition) ?: return 0.5f
		if (child.width <= 0) {
			return 0.5f
		}

		return ((x - child.left).toFloat() / child.width).coerceIn(0f, 1f)
	}

	private fun highlight(grid: GridView, position: Int) {
		val child = grid.getChildAt(position - grid.firstVisiblePosition) ?: return
		child.setBackgroundResource(R.drawable.dash_folder_drop_indicator)
		this.highlighted = child
	}

	private fun highlightKey(grid: GridView, key: String) {
		for (i in 0 until grid.childCount) {
			val child = grid.getChildAt(i)
			if ((child.tag as? DashItem)?.stableKey == key) {
				child.setBackgroundResource(R.drawable.dash_folder_drop_indicator)
				this.highlighted = child
				return
			}
		}
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

		/** How long the icons take to slide into place after a reorder gap moves. */
		private const val REFLOW_MS = 180L

		/** Fraction of a cell's width, centred, that reads as "fold onto this cell". */
		private const val FOLD_ZONE_LO = 0.3f
		private const val FOLD_ZONE_HI = 0.7f
	}
}
