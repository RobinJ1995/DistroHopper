package be.robinj.distrohopper.widgets

import be.robinj.distrohopper.App
import be.robinj.distrohopper.AppRepository
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.dev.Log

/**
 * Owns the apps pinned to the desktops — the app-world counterpart of
 * [WidgetHost]. Desktop apps live as 1x1 [DesktopAppView] children inside the
 * same [WidgetsContainer] grid as the widgets, one page per desktop, and are
 * persisted independently of both the widgets and the launcher-bar pins.
 *
 * The single source of truth for a desktop app's position is its view's
 * [WidgetsContainer.LayoutParams] and the index of the page it sits in (so a
 * view moved between pages re-pages itself on [persist], exactly like widgets).
 *
 * Invariant: **at most one desktop pin per app** ([App.getProfileScopedKey]),
 * across all desktops. A launcher-bar pin for the same app is allowed and
 * independent (a separate store). Enforced in one place — [pinAt].
 */
class DesktopAppHost(
	private val parent: HomeActivity,
	private val vgWidgets: WidgetsPager,
	private val repository: AppRepository,
) {
	private val persistence = DesktopAppPersistence(parent.applicationContext)

	/**
	 * Recreates the views for every persisted desktop app, resolving each against
	 * the installed apps: prunes the ones whose app is gone, drops duplicate keys
	 * (keeping the first), and re-packs any whose saved cell now collides with a
	 * widget or another desktop app. Must run once the app model has loaded (the
	 * keys only resolve through [AppRepository.installedAppsMap]).
	 */
	fun restore() {
		val appMap = this.repository.installedAppsMap()
		val seen = HashSet<String>()
		var changed = false

		for (layout in this.persistence.load()) {
			val app = appMap[layout.key]
			if (app == null) {
				changed = true // Pruned: the app is no longer installed //

				Log.getInstance().w(this.javaClass.simpleName, "Pruned stale desktop app: ${layout.key}")

				continue
			}

			if (! seen.add(layout.key)) {
				changed = true // De-duplicated: this key is already placed //

				continue
			}

			val page = layout.page.coerceIn(0, WidgetsPager.MAX_PAGES - 1)
			val container = this.vgWidgets.pageAt(page)
			val occupied = container.collectOccupied(null)

			val placed = if (WidgetGrid.fits(occupied,
					DesktopAppLayout(layout.key, layout.col, layout.row, page).toGridRect())) {
				DesktopAppLayout(layout.key, layout.col, layout.row, page)
			} else {
				// The saved cell is taken (e.g. by a widget restored before us):
				// re-pack into the first free block, or drop it if the page is full //
				val free = WidgetGrid.findFreeRect(occupied, DesktopAppLayout.SPAN, DesktopAppLayout.SPAN)
				changed = true

				if (free == null) {
					Log.getInstance().w(this.javaClass.simpleName,
						"No room for desktop app on page $page: ${layout.key}")

					continue
				}

				DesktopAppLayout(layout.key, free.col, free.row, page)
			}

			this.addAppView(app, placed)
		}

		this.vgWidgets.pagesChanged()

		if (changed) {
			this.persist()
		}
	}

	/**
	 * Pins [app] onto desktop [page] at (or near) ([col], [row]). Enforces the
	 * single-copy invariant: any existing desktop pin for the same app — on any
	 * desktop — is removed first, so this both *adds* a new pin and *relocates* an
	 * existing one (never duplicates). Falls back to the first free cell if the
	 * requested one is taken; a no-op returning false if the desktop is full.
	 */
	fun pinAt(app: App, col: Int, row: Int, page: Int): Boolean {
		// Cross-desktop single copy: drop any existing pin for this app first, so
		// the dropped cell (which may be the one it is vacating) is free again //
		this.removeViewsForKey(app.profileScopedKey)

		val targetPage = page.coerceIn(0, WidgetsPager.MAX_PAGES - 1)
		val container = this.vgWidgets.pageAt(targetPage)
		val occupied = container.collectOccupied(null)

		val candidate = DesktopAppLayout(app.profileScopedKey, col, row, targetPage).toGridRect()
		val target = if (WidgetGrid.fits(occupied, candidate)) {
			candidate
		} else {
			WidgetGrid.findFreeRect(occupied, DesktopAppLayout.SPAN, DesktopAppLayout.SPAN)
		} ?: return false

		this.addAppView(app, DesktopAppLayout(app.profileScopedKey, target.col, target.row, targetPage))

		this.persist()
		this.vgWidgets.pagesChanged()

		return true
	}

	/**
	 * Moves [view] to ([col], [row]) on its current desktop, keeping it put if the
	 * cell is taken by a widget or another desktop app.
	 */
	fun moveTo(view: DesktopAppView, col: Int, row: Int) {
		val container = view.parent as? WidgetsContainer ?: return
		val lp = view.layoutParams as WidgetsContainer.LayoutParams

		val candidate = DesktopAppLayout(view.key, col, row, 0).toGridRect()
		if (! WidgetGrid.fits(container.collectOccupied(view), candidate)) {
			return // Revert to the previous position //
		}

		lp.col = col
		lp.row = row

		container.requestLayout()
		this.persist()
	}

	/** Removes [view] from its desktop (e.g. dropped on the trash). */
	fun remove(view: DesktopAppView) {
		(view.parent as? WidgetsContainer)?.removeView(view)

		this.persist()
		this.vgWidgets.pagesChanged()
	}

	/**
	 * Removes every desktop app on [page] and shifts the apps on higher desktops
	 * down by one, for desktop deletion — mirrors [WidgetHost.removeWidgetPage] so
	 * widgets and desktop apps shift together. Coordinated by
	 * [be.robinj.distrohopper.home.Desktops].
	 */
	fun removeDesktopPage(page: Int) {
		if (page in 0 until this.vgWidgets.childCount) {
			val container = this.vgWidgets.pageAt(page)
			for (view in this.appsOf(container)) {
				container.removeView(view)
			}

			for (higher in (page + 1) until this.vgWidgets.childCount) {
				val from = this.vgWidgets.pageAt(higher)
				val to = this.vgWidgets.pageAt(higher - 1)
				for (view in this.appsOf(from)) {
					val layoutParams = view.layoutParams
					from.removeView(view)
					to.addView(view, layoutParams)
				}
			}
		}

		this.persist()
		this.vgWidgets.pagesChanged()
	}

	/** Removes [app]'s desktop pin from every desktop (e.g. when it is uninstalled). */
	fun unpinFromAllDesktops(app: App) {
		if (this.removeViewsForKey(app.profileScopedKey)) {
			this.persist()
			this.vgWidgets.pagesChanged()
		}
	}

	fun isPinnedOnDesktop(app: App): Boolean = this.viewForKey(app.profileScopedKey) != null

	/** Highest desktop holding a pinned app (or -1), for the `home/Desktops` coordinator. */
	fun highestDesktop(): Int {
		for (i in this.vgWidgets.childCount - 1 downTo 0) {
			val container = this.vgWidgets.pageAt(i)
			for (j in 0 until container.childCount) {
				if (container.getChildAt(j) is DesktopAppView) {
					return i
				}
			}
		}

		return -1
	}

	/**
	 * Saves the current placements, reading each app's position from its view's
	 * layout params and the index of the page it sits in — so views moved between
	 * pages (page-shift on delete) re-page themselves automatically.
	 */
	fun persist() {
		val layouts = mutableListOf<DesktopAppLayout>()

		for (page in 0 until this.vgWidgets.childCount) {
			val container = this.vgWidgets.pageAt(page)
			for (view in this.appsOf(container)) {
				val lp = view.layoutParams as WidgetsContainer.LayoutParams
				layouts.add(DesktopAppLayout(view.key, lp.col, lp.row, page))
			}
		}

		this.persistence.save(layouts)
	}

	private fun addAppView(app: App, layout: DesktopAppLayout) {
		val view = DesktopAppView(this.parent, app, layout)
		this.vgWidgets.pageAt(layout.page).addView(view, WidgetsContainer.LayoutParams(
			layout.col, layout.row, DesktopAppLayout.SPAN, DesktopAppLayout.SPAN))
	}

	/** @return whether any view was removed. */
	private fun removeViewsForKey(key: String): Boolean {
		var removed = false

		for (page in 0 until this.vgWidgets.childCount) {
			val container = this.vgWidgets.pageAt(page)
			for (i in container.childCount - 1 downTo 0) {
				val child = container.getChildAt(i) as? DesktopAppView ?: continue
				if (child.key == key) {
					container.removeView(child)
					removed = true
				}
			}
		}

		return removed
	}

	fun viewForKey(key: String): DesktopAppView? {
		for (page in 0 until this.vgWidgets.childCount) {
			val container = this.vgWidgets.pageAt(page)
			for (view in this.appsOf(container)) {
				if (view.key == key) {
					return view
				}
			}
		}

		return null
	}

	private fun appsOf(container: WidgetsContainer): List<DesktopAppView> =
		(0 until container.childCount).mapNotNull { container.getChildAt(it) as? DesktopAppView }
}
