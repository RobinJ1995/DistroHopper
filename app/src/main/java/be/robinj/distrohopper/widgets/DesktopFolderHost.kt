package be.robinj.distrohopper.widgets

import android.graphics.drawable.Drawable
import be.robinj.distrohopper.App
import be.robinj.distrohopper.AppRepository
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.dev.Log
import be.robinj.distrohopper.folder.FolderMember

/**
 * Owns the folders pinned to the desktops — the folder-world counterpart of
 * [DesktopAppHost]. Each folder is a [DesktopFolderView] (a 2x2 grid child)
 * holding apps.
 *
 * Uniqueness mirrors the desktop's single-copy invariant: an app is loose on the
 * desktop **or** in one folder, never both. Creating/adding removes the loose
 * view; deleting a folder removes its apps; extracting returns an app to the grid.
 */
class DesktopFolderHost(
	private val parent: HomeActivity,
	private val vgWidgets: WidgetsPager,
	private val repository: AppRepository,
	private val desktopAppHost: DesktopAppHost,
) {
	private val persistence = DesktopFolderPersistence(parent.applicationContext)

	// --- Restore / persist -------------------------------------------------

	/** Recreates folder views from persistence; run AFTER widgets and desktop apps restore. */
	fun restore() {
		val appMap = this.repository.installedAppsMap()
		var changed = false

		for (saved in this.persistence.load()) {
			val keptCells = saved.cells.filter { cell ->
				when (val member = cell.member) {
					// Enforce the desktop's one-copy invariant: drop an app that is also
					// loose on the desktop (loose wins — restore runs after the desktop
					// apps are placed, so viewForKey sees them) //
					is FolderMember.AppMember -> appMap.containsKey(member.key) &&
						this.desktopAppHost.viewForKey(member.key) == null
				}
			}
			val layout = saved.copy(cells = keptCells)

			if (layout.appCount < 1 || layout.cells.size < 2) {
				changed = true // Dissolved: too few members left; return them loose //
				this.returnMembersLoose(layout, appMap)
				continue
			}
			if (keptCells.size != saved.cells.size) {
				changed = true
			}

			val page = layout.page.coerceIn(0, WidgetsPager.MAX_PAGES - 1)
			val container = this.vgWidgets.pageAt(page)
			val placed = if (WidgetGrid.fits(container.collectOccupied(null), layout.toGridRect())) {
				layout.copy(page = page)
			} else {
				val free = WidgetGrid.findFreeRect(container.collectOccupied(null),
					DesktopFolderLayout.SPAN, DesktopFolderLayout.SPAN)
				changed = true
				if (free == null) {
					Log.getInstance().w(this.javaClass.simpleName, "No room for desktop folder on $page")
					this.returnMembersLoose(layout, appMap)
					continue
				}
				layout.copy(col = free.col, row = free.row, page = page)
			}

			this.addFolderView(placed, appMap)
		}

		this.vgWidgets.pagesChanged()
		if (changed) {
			this.persist()
		}
	}

	fun persist() {
		val layouts = ArrayList<DesktopFolderLayout>()
		for (page in 0 until this.vgWidgets.childCount) {
			val container = this.vgWidgets.pageAt(page)
			for (view in this.foldersOf(container)) {
				val lp = view.layoutParams as WidgetsContainer.LayoutParams
				layouts.add(view.layout.copy(col = lp.col, row = lp.row, page = page))
			}
		}

		this.persistence.save(layouts)
	}

	// --- Mutations ---------------------------------------------------------

	/**
	 * Creates a folder from two loose desktop apps. [a] is the dragged app and [b]
	 * the one it was dropped onto; the folder takes [b]'s cell, since b is the
	 * stationary target the user aimed at (a is absorbed into it).
	 */
	fun createFolder(a: DesktopAppView, b: DesktopAppView) {
		val container = b.parent as? WidgetsContainer ?: return
		val page = this.pageOf(container)
		val lp = b.layoutParams as WidgetsContainer.LayoutParams
		val col = lp.col
		val row = lp.row

		this.desktopAppHost.remove(a)
		this.desktopAppHost.remove(b)

		var layout = DesktopFolderLayout(java.util.UUID.randomUUID().toString(), col, row, page)
		layout = layout.withApp(a.app.profileScopedKey)!!.withApp(b.app.profileScopedKey)!!

		this.addFolderView(layout, this.repository.installedAppsMap())
		this.persist()
		this.vgWidgets.pagesChanged()
	}

	/** Adds a loose desktop app [view] into folder [folderId]; toast + no-op if full. */
	fun addApp(folderId: String, view: DesktopAppView) {
		val folderView = this.folderViewFor(folderId) ?: return
		val newLayout = folderView.layout.withApp(view.app.profileScopedKey)
		if (newLayout == null) {
			android.widget.Toast.makeText(this.parent,
				be.robinj.distrohopper.R.string.folder_full, android.widget.Toast.LENGTH_SHORT).show()
			return
		}

		this.desktopAppHost.remove(view)
		this.replaceFolderView(folderView, newLayout)
		this.persist()
	}

	/**
	 * Adds an [app] coming from another surface (a dash/search result, or a pin
	 * dragged off the launcher bar) into folder [folderId]. Unlike the [DesktopAppView]
	 * overload there is no loose desktop view to remove — the caller clears the app's
	 * origin surface. Shows the full toast and returns false (no-op) if the folder is
	 * full or gone; true once the app is in it.
	 */
	fun addApp(folderId: String, app: App): Boolean {
		val folderView = this.folderViewFor(folderId) ?: return false
		val newLayout = folderView.layout.withApp(app.profileScopedKey)
		if (newLayout == null) {
			android.widget.Toast.makeText(this.parent,
				be.robinj.distrohopper.R.string.folder_full, android.widget.Toast.LENGTH_SHORT).show()
			return false
		}

		this.replaceFolderView(folderView, newLayout)
		this.persist()
		return true
	}

	/** Extracts a member back onto the desktop near [col],[row] of [page]; dissolves at <2. */
	fun removeMember(folderId: String, member: FolderMember, col: Int, row: Int, page: Int) {
		val folderView = this.folderViewFor(folderId) ?: return
		val appMap = this.repository.installedAppsMap()
		val folderLp = folderView.layoutParams as WidgetsContainer.LayoutParams
		val folderCol = folderLp.col
		val folderRow = folderLp.row

		// Clear/shrink the folder BEFORE placing the extracted member: its 2x2 still
		// occupies the grid, so pinning into the drop cell first would let the cell
		// (when it overlaps the folder) bump the extracted member elsewhere — and a
		// dissolved remaining member's free-cell search could then land on the
		// just-vacated drop cell, so the *wrong* icon ends up where you released //
		val remaining = folderView.layout.without(member)
		val dissolving = remaining.appCount < 1 || remaining.cells.size < 2
		if (dissolving) {
			(folderView.parent as? WidgetsContainer)?.removeView(folderView)
		} else {
			this.replaceFolderView(folderView, remaining)
		}

		when (member) {
			is FolderMember.AppMember -> appMap[member.key]?.let {
				this.desktopAppHost.pinAt(it, col, row, page)
			}
		}

		// Return any members of a dissolved folder loose at the folder's old spot,
		// now that the extracted member holds the drop cell (so they avoid it) //
		if (dissolving) {
			this.returnMembersLoose(remaining, appMap, folderCol, folderRow)
		}
		this.persist()
		this.vgWidgets.pagesChanged()
	}

	/**
	 * Removes an app from the folder and deletes it (dropped on the trash): it
	 * simply leaves. Dissolves the folder at <2, the remaining members returning
	 * loose.
	 */
	fun deleteMember(folderId: String, member: FolderMember) {
		val folderView = this.folderViewFor(folderId) ?: return
		val appMap = this.repository.installedAppsMap()

		val remaining = folderView.layout.without(member)
		if (remaining.appCount < 1 || remaining.cells.size < 2) {
			this.dissolve(folderView, remaining, appMap, exclude = member)
		} else {
			this.replaceFolderView(folderView, remaining)
		}
		this.persist()
		this.vgWidgets.pagesChanged()
	}
	fun moveTo(view: DesktopFolderView, col: Int, row: Int) {
		val container = view.parent as? WidgetsContainer ?: return
		val lp = view.layoutParams as WidgetsContainer.LayoutParams
		if (! WidgetGrid.fits(container.collectOccupied(view),
				DesktopFolderLayout(view.folderId, col, row).toGridRect())) {
			return
		}

		lp.col = col
		lp.row = row
		container.requestLayout()
		this.persist()
	}

	/** Deletes the folder and its apps. */
	fun deleteFolder(folderId: String) {
		val folderView = this.folderViewFor(folderId) ?: return
		(folderView.parent as? WidgetsContainer)?.removeView(folderView)

		this.persist()
		this.vgWidgets.pagesChanged()
	}

	/** Drops [app] from every desktop folder it is in (e.g. when uninstalled), dissolving at <2. */
	fun unpinFromAllDesktops(app: App) {
		val key = app.profileScopedKey
		val appMap = this.repository.installedAppsMap()
		var changed = false

		for (page in 0 until this.vgWidgets.childCount) {
			for (view in this.foldersOf(this.vgWidgets.pageAt(page))) {
				if (! view.layout.appKeys.contains(key)) {
					continue
				}
				changed = true
				val remaining = view.layout.without(FolderMember.AppMember(key))
				if (remaining.appCount < 1 || remaining.cells.size < 2) {
					this.dissolve(view, remaining, appMap, FolderMember.AppMember(key))
				} else {
					this.replaceFolderView(view, remaining)
				}
			}
		}

		if (changed) {
			this.persist()
			this.vgWidgets.pagesChanged()
		}
	}

	/**
	 * Ensures [app] is in no desktop folder — its single desktop copy is now loose
	 * on the grid (it was just dropped there). Keeps the one-copy-per-app invariant
	 * so the same app can't sit both loose and in a folder; a no-op if it is in none.
	 */
	fun dropFromFolders(app: App) = this.unpinFromAllDesktops(app)

	fun removeDesktopPage(page: Int) {
		if (page in 0 until this.vgWidgets.childCount) {
			val container = this.vgWidgets.pageAt(page)
			for (view in this.foldersOf(container)) {
				container.removeView(view)
			}
			for (higher in (page + 1) until this.vgWidgets.childCount) {
				val from = this.vgWidgets.pageAt(higher)
				val to = this.vgWidgets.pageAt(higher - 1)
				for (view in this.foldersOf(from)) {
					val layoutParams = view.layoutParams
					from.removeView(view)
					to.addView(view, layoutParams)
				}
			}
		}

		this.persist()
		this.vgWidgets.pagesChanged()
	}

	fun highestDesktop(): Int {
		for (i in this.vgWidgets.childCount - 1 downTo 0) {
			if (this.foldersOf(this.vgWidgets.pageAt(i)).isNotEmpty()) {
				return i
			}
		}

		return -1
	}

	// --- Helpers -----------------------------------------------------------

	private fun addFolderView(layout: DesktopFolderLayout, appMap: Map<String, App>) {
		val apps = layout.appKeys.mapNotNull { appMap[it] }
		if (apps.isEmpty()) {
			return
		}

		val icons = ArrayList<Drawable>()
		// App icons in cell order so the mini-grid mirrors the contents layout.
		for (cell in layout.cells) {
			when (val member = cell.member) {
				is FolderMember.AppMember -> appMap[member.key]?.let { icons.add(it.icon.drawable) }
			}
		}

		val view = DesktopFolderView(this.parent, apps.first(), layout, icons) { this.open(it) }
		this.vgWidgets.pageAt(layout.page).addView(view, WidgetsContainer.LayoutParams(
			layout.col, layout.row, DesktopFolderLayout.SPAN, DesktopFolderLayout.SPAN))
	}

	private fun replaceFolderView(view: DesktopFolderView, newLayout: DesktopFolderLayout) {
		val container = view.parent as? WidgetsContainer ?: return
		val lp = view.layoutParams as WidgetsContainer.LayoutParams
		container.removeView(view)
		this.addFolderView(newLayout.copy(col = lp.col, row = lp.row, page = this.pageOf(container)),
			this.repository.installedAppsMap())
	}

	private fun dissolve(
		view: DesktopFolderView,
		remaining: DesktopFolderLayout,
		appMap: Map<String, App>,
		exclude: FolderMember,
	) {
		(view.parent as? WidgetsContainer)?.removeView(view)
		this.returnMembersLoose(remaining, appMap)
	}

	/** Returns a layout's apps to the desktop loose, preferring the [col],[row]
	 *  cell (each falls back to a free cell if that is taken). */
	private fun returnMembersLoose(
		layout: DesktopFolderLayout, appMap: Map<String, App>, col: Int = 0, row: Int = 0,
	) {
		val page = layout.page.coerceIn(0, WidgetsPager.MAX_PAGES - 1)
		for (key in layout.appKeys) {
			appMap[key]?.let { this.desktopAppHost.pinAt(it, col, row, page) }
		}
	}

	private fun open(view: DesktopFolderView) {
		DesktopFolderOverlay(this.parent, view.layout,
			this.repository.installedAppsMap()).show(view)
	}

	private fun folderViewFor(folderId: String): DesktopFolderView? {
		for (page in 0 until this.vgWidgets.childCount) {
			this.foldersOf(this.vgWidgets.pageAt(page)).firstOrNull { it.folderId == folderId }
				?.let { return it }
		}

		return null
	}

	private fun foldersOf(container: WidgetsContainer): List<DesktopFolderView> =
		(0 until container.childCount).mapNotNull { container.getChildAt(it) as? DesktopFolderView }

	private fun pageOf(container: WidgetsContainer): Int {
		for (i in 0 until this.vgWidgets.childCount) {
			if (this.vgWidgets.pageAt(i) === container) {
				return i
			}
		}

		return 0
	}
}
