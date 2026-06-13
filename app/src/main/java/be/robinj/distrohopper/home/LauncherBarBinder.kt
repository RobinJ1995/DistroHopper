package be.robinj.distrohopper.home

import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import be.robinj.distrohopper.App
import be.robinj.distrohopper.AppManager
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.Workspaces
import be.robinj.distrohopper.preferences.Preferences
import be.robinj.distrohopper.desktop.dash.GridAdapter
import be.robinj.distrohopper.desktop.dash.AppLauncherClickListener as DashAppLauncherClickListener
import be.robinj.distrohopper.desktop.dash.AppLauncherLongClickListener as DashAppLauncherLongClickListener
import be.robinj.distrohopper.desktop.launcher.AppLauncher
import be.robinj.distrohopper.desktop.launcher.AppLauncherClickListener
import be.robinj.distrohopper.desktop.launcher.AppLauncherDragListener
import be.robinj.distrohopper.desktop.launcher.AppLauncherLongClickListener
import be.robinj.distrohopper.desktop.launcher.RunningAppLauncher
import be.robinj.distrohopper.theme.Location

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
	private val svDashHomeWorkspaces: ScrollView by lazy {
		this.activity.viewFinder.get(R.id.svDashHomeWorkspaces)
	}
	private val llDashHomeWorkspaces: LinearLayout by lazy {
		this.activity.viewFinder.get(R.id.llDashHomeWorkspaces)
	}

	private var dashBound = false
	private var dashDisplayDensity = 0F
	private var dashIconWidth = 0
	/** The workspaces the dash was last bound for (null = the personal profile). */
	private var boundWorkspaces: List<UserHandle?> = emptyList()
	/** Per-workspace section adapters; empty in single-workspace mode. */
	private val workspaceAdapters = LinkedHashMap<UserHandle?, GridAdapter>()

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
	 * Binds the dash app grid(s): the single live-list-backed grid when only
	 * the personal workspace exists, or one labelled section per workspace
	 * (personal, work profile) stacked in the scrollable workspace container.
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
		this.workspaceAdapters.clear()
		this.llDashHomeWorkspaces.removeAllViews()

		if (workspaces.size <= 1) {
			this.svDashHomeWorkspaces.visibility = View.GONE
			this.gvDashHomeApps.visibility = View.VISIBLE

			if (this.gvDashHomeApps.adapter == null) {
				// Backed by the live installed list, as before //
				this.gvDashHomeApps.adapter = GridAdapter(this.activity.applicationContext,
					this.appManager.installedApps, this.dashDisplayDensity, this.dashIconWidth)
				this.gvDashHomeApps.onItemClickListener =
					DashAppLauncherClickListener(this.activity)
				this.gvDashHomeApps.onItemLongClickListener =
					DashAppLauncherLongClickListener(this.activity)
			}

			return
		}

		this.gvDashHomeApps.visibility = View.GONE
		this.svDashHomeWorkspaces.visibility = View.VISIBLE

		val theme = DependencyContainer.of(this.activity).themeManager.current
		val res = this.activity.resources
		for (workspace in workspaces) {
			val section = LayoutInflater.from(this.activity)
				.inflate(R.layout.widget_dash_workspace, this.llDashHomeWorkspaces, false)

			val tvLabel = section.findViewById<TextView>(R.id.tvWorkspaceLabel)
			tvLabel.text = Workspaces.label(this.activity, workspace)
			tvLabel.setTextColor(res.getColor(theme.dash_applauncher_text_colour))
			tvLabel.setShadowLayer(5F, 2F, 2F,
				res.getColor(theme.dash_applauncher_text_shadow_colour))

			// Sections refresh from the repository on change (notifyDashAdapterChanged),
			// so each gets its own copied list rather than the shared live list //
			val adapter = GridAdapter(this.activity.applicationContext,
				ArrayList(this.appManager.repository.appsForWorkspace(workspace)),
				this.dashDisplayDensity, this.dashIconWidth)

			val grid = section.findViewById<GridView>(R.id.gvWorkspaceApps)
			grid.setColumnWidth(Math.round((80 + this.dashIconWidth) * this.dashDisplayDensity))
			grid.adapter = adapter
			grid.onItemClickListener = DashAppLauncherClickListener(this.activity)
			grid.onItemLongClickListener = DashAppLauncherLongClickListener(this.activity)

			this.workspaceAdapters[workspace] = adapter
			this.llDashHomeWorkspaces.addView(section)
		}
	}

	fun notifyDashAdapterChanged() {
		if (this.dashBound && this.appManager.repository.workspaces() != this.boundWorkspaces) {
			// A workspace appeared or vanished (e.g. the first work-profile app
			// was installed): rebuild the dash sections wholesale //
			this.rebindDashApps()

			return
		}

		(this.gvDashHomeApps.adapter as? ArrayAdapter<*>)?.notifyDataSetChanged()

		for ((workspace, adapter) in this.workspaceAdapters) {
			adapter.setNotifyOnChange(false)
			adapter.clear()
			adapter.addAll(this.appManager.repository.appsForWorkspace(workspace))
			adapter.notifyDataSetChanged()
		}
	}

	fun invalidateDashViews() {
		this.gvDashHomeApps.invalidateViews()

		for (i in 0 until this.llDashHomeWorkspaces.childCount) {
			this.llDashHomeWorkspaces.getChildAt(i)
				.findViewById<GridView>(R.id.gvWorkspaceApps)?.invalidateViews()
		}
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
