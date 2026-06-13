package be.robinj.distrohopper.home

import android.os.UserHandle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.GridView
import android.widget.LinearLayout
import androidx.viewpager2.widget.ViewPager2
import be.robinj.distrohopper.App
import be.robinj.distrohopper.AppManager
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.preferences.Preferences
import be.robinj.distrohopper.desktop.dash.GridAdapter
import be.robinj.distrohopper.desktop.dash.WorkspacePagerAdapter
import be.robinj.distrohopper.desktop.dash.workspace.GnomeWorkspacePillIndicator
import be.robinj.distrohopper.desktop.dash.workspace.UnityRibbonIndicator
import be.robinj.distrohopper.desktop.dash.workspace.WorkspaceIndicator
import be.robinj.distrohopper.desktop.dash.AppLauncherClickListener as DashAppLauncherClickListener
import be.robinj.distrohopper.desktop.dash.AppLauncherLongClickListener as DashAppLauncherLongClickListener
import be.robinj.distrohopper.desktop.launcher.AppLauncher
import be.robinj.distrohopper.desktop.launcher.AppLauncherClickListener
import be.robinj.distrohopper.desktop.launcher.AppLauncherDragListener
import be.robinj.distrohopper.desktop.launcher.AppLauncherLongClickListener
import be.robinj.distrohopper.desktop.launcher.RunningAppLauncher
import be.robinj.distrohopper.theme.Location
import be.robinj.distrohopper.theme.WorkspaceIndicatorStyle

/**
 * The view half of app management: keeps the launcher bar's pinned and
 * running app icons and the dash grid in sync with the model. Split out of
 * AppManager, which remains the facade gluing this to AppRepository. Views
 * are resolved lazily so AppManager can still be constructed on a
 * background thread during startup loading.
 */
class LauncherBarBinder(private val appManager: AppManager) {
	private val activity = this.appManager.parent

	/** While a pinned icon is dragged, its own view stays in the bar as an
	 *  invisible placeholder: the empty slot previewing where it would drop. */
	private var draggedPinnedApp: AppLauncher? = null
	private var draggedPinnedAppOldIndex = -1
	private var draggedPinnedAppDropped = false

	private val llLauncher: LinearLayout by lazy {
		this.activity.viewFinder.get(R.id.llLauncher)
	}
	private val llLauncherPinnedApps: LinearLayout by lazy {
		this.activity.viewFinder.get(this.llLauncher, R.id.llLauncherPinnedApps)
	}
	private val llLauncherRunningApps: LinearLayout by lazy {
		this.activity.viewFinder.get(this.llLauncher, R.id.llLauncherRunningApps)
	}
	private val gvDashHomeApps: GridView by lazy {
		this.activity.viewFinder.get(R.id.gvDashHomeApps)
	}
	private val llDashHomeAppsGridWrapper: LinearLayout by lazy {
		this.activity.viewFinder.get(R.id.llDashHomeAppsGridWrapper)
	}
	private val vpDashWorkspaces: ViewPager2 by lazy {
		this.activity.viewFinder.get(R.id.vpDashWorkspaces)
	}

	private var dashBound = false
	private var dashDisplayDensity = 0F
	private var dashIconWidth = 0
	/** The workspaces the dash was last bound for (null = the personal profile). */
	private var boundWorkspaces: List<UserHandle?> = emptyList()
	/** The current workspace tab; preserved across rebinds (app install/remove). */
	private var currentWorkspaceIndex = 0
	private var pagerAdapter: WorkspacePagerAdapter? = null
	private var indicator: WorkspaceIndicator? = null
	private var pageCallbackRegistered = false
	private var dashOpen = false

	fun addPinnedAppView(app: App) {
		this.llLauncherPinnedApps.addView(this.pinnedAppLauncher(app))
	}

	fun refreshPinnedView() {
		this.llLauncherPinnedApps.removeAllViews()

		for (app in this.appManager.pinned) {
			this.llLauncherPinnedApps.addView(this.pinnedAppLauncher(app))
		}
	}

	fun removePinnedAppView(app: App) {
		val appLauncher = this.llLauncherPinnedApps.findViewWithTag<AppLauncher>(app)
		this.llLauncherPinnedApps.removeView(appLauncher)
	}

	fun addRunningApps(colour: Int) {
		var colour = colour
		this.llLauncherRunningApps.removeAllViews()

		for (i in 0 until this.llLauncherPinnedApps.childCount)
			(this.llLauncherPinnedApps.getChildAt(i) as AppLauncher).setRunning(false)

		for (app in this.appManager.runningApps) {
			if (this.appManager.isPinned(app)) {
				val appLauncher = this.llLauncherPinnedApps.findViewWithTag<AppLauncher>(app)
				appLauncher?.setRunning(true)
			} else {
				val theme = DependencyContainer.of(this.activity).themeManager.current
				if (! this.activity.resources.getBoolean(theme.launcher_applauncher_backgroundcolour_dynamic))
					colour = this.activity.resources.getColor(theme.launcher_applauncher_backgroundcolour)

				val appLauncher = RunningAppLauncher(this.activity, app)
				appLauncher.setOnClickListener(AppLauncherClickListener(this.activity))
				appLauncher.colour = colour

				this.llLauncherRunningApps.addView(appLauncher)
			}
		}
	}

	/**
	 * Binds the dash app grid: the single live-list-backed grid when only the
	 * personal workspace exists (the standard dash), or a swipeable per-
	 * workspace pager with a theme-specific tab indicator when a work profile
	 * is present too.
	 */
	fun bindDashApps(displayDensity: Float, dashIconWidth: Int) {
		this.dashDisplayDensity = displayDensity
		this.dashIconWidth = dashIconWidth
		this.dashBound = true

		this.rebindDashApps()
	}

	private fun rebindDashApps() {
		val workspaces = this.appManager.repository.workspaces()
		this.boundWorkspaces = workspaces

		if (workspaces.size <= 1) {
			// Standard dash: the single grid backed by the live installed list //
			this.indicator?.clear()
			this.indicator = null
			this.vpDashWorkspaces.adapter = null
			this.pagerAdapter = null
			this.vpDashWorkspaces.visibility = View.GONE
			this.llDashHomeAppsGridWrapper.visibility = View.VISIBLE

			if (this.gvDashHomeApps.adapter == null) {
				this.gvDashHomeApps.adapter = GridAdapter(this.activity.applicationContext,
					this.appManager.installedApps, this.dashDisplayDensity, this.dashIconWidth)
				this.gvDashHomeApps.onItemClickListener =
					DashAppLauncherClickListener(this.activity)
				this.gvDashHomeApps.onItemLongClickListener =
					DashAppLauncherLongClickListener(this.activity)
			} else {
				// Reverting from tabs (last work-profile app removed): the live-
				// list-backed adapter needs a refresh to drop the gone apps //
				(this.gvDashHomeApps.adapter as? ArrayAdapter<*>)?.notifyDataSetChanged()
			}

			return
		}

		// Multiple workspaces: swipeable tabs, one app grid per profile //
		this.llDashHomeAppsGridWrapper.visibility = View.GONE
		this.vpDashWorkspaces.visibility = View.VISIBLE

		val selected = this.currentWorkspaceIndex.coerceIn(0, workspaces.size - 1)
		this.currentWorkspaceIndex = selected

		val adapter = WorkspacePagerAdapter(this.activity, this.appManager, workspaces,
			this.dashDisplayDensity, this.dashIconWidth)
		this.pagerAdapter = adapter
		this.vpDashWorkspaces.adapter = adapter
		this.vpDashWorkspaces.setCurrentItem(selected, false)
		this.registerPageCallback()

		this.indicator?.clear()
		this.indicator = this.createIndicator()?.also {
			it.bind(workspaces, selected)
			it.onDashOpenChanged(this.dashOpen)
		}
	}

	private fun registerPageCallback() {
		if (this.pageCallbackRegistered) {
			return
		}
		this.pageCallbackRegistered = true

		this.vpDashWorkspaces.registerOnPageChangeCallback(
			object : ViewPager2.OnPageChangeCallback() {
				override fun onPageScrolled(
					position: Int, positionOffset: Float, positionOffsetPixels: Int) {
					this@LauncherBarBinder.indicator?.onPageScrolled(position, positionOffset)
				}

				override fun onPageSelected(position: Int) {
					this@LauncherBarBinder.currentWorkspaceIndex = position
					this@LauncherBarBinder.indicator?.onPageSelected(position)
				}
			})
	}

	private fun createIndicator(): WorkspaceIndicator? {
		val theme = DependencyContainer.of(this.activity).themeManager.current
		val select: (Int) -> Unit = { this.vpDashWorkspaces.setCurrentItem(it, true) }

		return when (WorkspaceIndicatorStyle.of(
				this.activity.resources.getInteger(theme.workspace_indicator))) {
			WorkspaceIndicatorStyle.UNITY_RIBBON -> UnityRibbonIndicator(this.activity,
				this.activity.viewFinder.get(R.id.llDashRibbonWorkspaces), select)
			WorkspaceIndicatorStyle.GNOME_PANEL -> GnomeWorkspacePillIndicator(this.activity,
				this.activity.viewFinder.get(R.id.llPanelWorkspaceIndicator), select)
			WorkspaceIndicatorStyle.NONE -> null
		}
	}

	fun notifyDashAdapterChanged() {
		if (this.dashBound && this.appManager.repository.workspaces() != this.boundWorkspaces) {
			// A workspace appeared or vanished (e.g. the first work-profile app
			// was installed, or the last removed): switch between the single grid
			// and the tabbed pager wholesale //
			this.rebindDashApps()

			return
		}

		(this.gvDashHomeApps.adapter as? ArrayAdapter<*>)?.notifyDataSetChanged()
		this.pagerAdapter?.refresh()
	}

	fun invalidateDashViews() {
		this.gvDashHomeApps.invalidateViews()
		this.pagerAdapter?.invalidatePages(this.vpDashWorkspaces)
	}

	/** Applies the dash icon-width preference to the single grid and pager pages. */
	fun applyDashIconWidth(dashIconWidth: Int) {
		this.dashIconWidth = dashIconWidth

		val columnWidth = WorkspacePagerAdapter.columnWidthPx(this.dashDisplayDensity, dashIconWidth)
		this.gvDashHomeApps.setColumnWidth(columnWidth)
		this.pagerAdapter?.applyIconWidth(this.vpDashWorkspaces, dashIconWidth)
	}

	/** The dash opened or closed; indicators that only show while open react. */
	fun setDashOpen(open: Boolean) {
		this.dashOpen = open
		this.indicator?.onDashOpenChanged(open)
	}

	fun startedDraggingPinnedApp() = startedDragging(this.activity)

	fun startedDraggingPinnedApp(app: App) {
		val appLauncher = this.llLauncherPinnedApps.findViewWithTag<AppLauncher>(app)
		if (appLauncher != null) {
			this.draggedPinnedApp = appLauncher
			this.draggedPinnedAppOldIndex = this.llLauncherPinnedApps.indexOfChild(appLauncher)
			this.draggedPinnedAppDropped = false
			appLauncher.visibility = View.INVISIBLE
		}

		this.startedDraggingPinnedApp()
	}

	/**
	 * A not-yet-pinned app dragged from the dash: a placeholder slot opens
	 * at the end of the bar and follows the drag like a reorder; the app is
	 * only actually pinned if it is dropped on the bar.
	 */
	fun startedDraggingDashApp(app: App) {
		val appLauncher = this.pinnedAppLauncher(app)
		appLauncher.visibility = View.INVISIBLE
		this.llLauncherPinnedApps.addView(appLauncher)

		this.draggedPinnedApp = appLauncher
		this.draggedPinnedAppOldIndex = NOT_YET_PINNED
		this.draggedPinnedAppDropped = false

		this.startedDraggingPinnedApp()
	}

	/**
	 * The drag is hovering over another pinned icon: shift the placeholder
	 * into that icon's slot, so that the icons in between slide over
	 * (animated by the container's LayoutTransition) and the empty slot
	 * shows exactly where the dragged icon would land.
	 */
	fun draggedPinnedAppOver(target: App) {
		val dragged = this.draggedPinnedApp ?: return
		val targetView = this.llLauncherPinnedApps.findViewWithTag<AppLauncher>(target)
		if (targetView == null || targetView == dragged)
			return

		val targetIndex = this.llLauncherPinnedApps.indexOfChild(targetView)
		if (targetIndex < 0)
			return

		this.llLauncherPinnedApps.removeView(dragged)
		this.llLauncherPinnedApps.addView(dragged, targetIndex)
	}

	/** Commits the order previewed by the placeholder's position. */
	fun droppedPinnedApp() {
		val dragged = this.draggedPinnedApp ?: return
		val newIndex = this.llLauncherPinnedApps.indexOfChild(dragged)
		if (newIndex < 0)
			return
		this.draggedPinnedAppDropped = true

		if (this.draggedPinnedAppOldIndex == NOT_YET_PINNED) {
			// A dash app dropped onto the bar: pin appends it to the model,
			// then shift it to the previewed slot //
			if (!this.appManager.pin(dragged.tag as App, false, false, false)) {
				this.draggedPinnedAppDropped = false // pinned meanwhile: let ended rebuild //
				return
			}

			val appendedIndex = this.appManager.pinned.size - 1
			if (newIndex != appendedIndex)
				this.appManager.movePinnedApp(appendedIndex, newIndex)
			this.appManager.savePinnedApps()
		} else if (newIndex != this.draggedPinnedAppOldIndex) {
			this.appManager.movePinnedApp(this.draggedPinnedAppOldIndex, newIndex)
			this.appManager.savePinnedApps()
		}
	}

	fun endedDraggingPinnedApp() {
		val dragged = this.draggedPinnedApp ?: return
		this.draggedPinnedApp = null
		this.draggedPinnedAppOldIndex = -1

		dragged.visibility = View.VISIBLE
		if (!this.draggedPinnedAppDropped)
			this.refreshPinnedView() // Cancelled, or unpinned via the trash: snap back to the model's order //
	}

	fun stoppedDraggingPinnedApp() = stoppedDragging(this.activity)

	private fun pinnedAppLauncher(app: App): AppLauncher {
		val appLauncher = AppLauncher(this.activity, app)
		appLauncher.setOnClickListener(AppLauncherClickListener(this.activity))
		appLauncher.setOnLongClickListener(AppLauncherLongClickListener(this.activity))
		appLauncher.setOnDragListener(AppLauncherDragListener(this.appManager))

		return appLauncher
	}

	companion object {
		/** draggedPinnedAppOldIndex value for a dash app not yet in the pinned model. */
		private const val NOT_YET_PINNED = -1

		/*
		 * The drag decorations only touch views, not the app model, and widget
		 * drags can start before app loading has finished — so these are usable
		 * with just the activity.
		 */

		@JvmStatic
		fun startedDragging(activity: HomeActivity) {
			val viewFinder = activity.viewFinder
			val llLauncher = viewFinder.get<LinearLayout>(R.id.llLauncher)
			val lalBfb = viewFinder.get<AppLauncher>(llLauncher, R.id.lalBfb)
			val lalPreferences = viewFinder.get<AppLauncher>(llLauncher, R.id.lalPreferences)
			val lalTrash = viewFinder.get<AppLauncher>(llLauncher, R.id.lalTrash)

			val theme = DependencyContainer.of(activity).themeManager.current
			if (activity.resources.getBoolean(theme.launcher_bfb_hide_while_dragging)) {
				lalBfb.visibility = View.GONE
			}
			lalPreferences.visibility = View.GONE
			lalTrash.visibility = View.VISIBLE
			activity.closeDash()

			viewFinder.get<LinearLayout>(llLauncher, R.id.llLauncherPinnedApps).alpha = 0.9F
		}

		@JvmStatic
		fun stoppedDragging(activity: HomeActivity) {
			val viewFinder = activity.viewFinder
			val llLauncher = viewFinder.get<LinearLayout>(R.id.llLauncher)
			val lalBfb = viewFinder.get<AppLauncher>(llLauncher, R.id.lalBfb)
			val lalPreferences = viewFinder.get<AppLauncher>(llLauncher, R.id.lalPreferences)
			val lalTrash = viewFinder.get<AppLauncher>(llLauncher, R.id.lalTrash)

			val theme = DependencyContainer.of(activity).themeManager.current
			val lalPreferences_location = theme.lalPreferences_getLocation(
				activity.resources, Preferences.getSharedPreferences(activity))
			lalBfb.visibility = View.VISIBLE
			lalPreferences.visibility =
				if (lalPreferences_location == Location.NONE) View.GONE else View.VISIBLE
			lalTrash.visibility = View.GONE

			viewFinder.get<LinearLayout>(llLauncher, R.id.llLauncherPinnedApps).alpha = 1.0F
		}
	}
}
