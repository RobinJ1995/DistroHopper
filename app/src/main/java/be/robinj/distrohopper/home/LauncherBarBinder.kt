package be.robinj.distrohopper.home

import android.view.View
import android.widget.ArrayAdapter
import android.widget.GridView
import android.widget.LinearLayout
import be.robinj.distrohopper.App
import be.robinj.distrohopper.AppManager
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.R
import be.robinj.distrohopper.preferences.Preferences
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

	fun notifyDashAdapterChanged() {
		(this.gvDashHomeApps.adapter as? ArrayAdapter<*>)?.notifyDataSetChanged()
	}

	fun invalidateDashViews() {
		this.gvDashHomeApps.invalidateViews()
	}

	fun startedDraggingPinnedApp() {
		val lalBfb = this.activity.viewFinder.get<AppLauncher>(this.llLauncher, R.id.lalBfb)
		val lalPreferences = this.activity.viewFinder.get<AppLauncher>(this.llLauncher, R.id.lalPreferences)
		val lalTrash = this.activity.viewFinder.get<AppLauncher>(this.llLauncher, R.id.lalTrash)

		val theme = DependencyContainer.of(this.activity).themeManager.current
		if (this.activity.resources.getBoolean(theme.launcher_bfb_hide_while_dragging)) {
			lalBfb.visibility = View.GONE
		}
		lalPreferences.visibility = View.GONE
		lalTrash.visibility = View.VISIBLE
		this.activity.closeDash()

		this.llLauncherPinnedApps.alpha = 0.9F
	}

	fun stoppedDraggingPinnedApp() {
		val lalBfb = this.activity.viewFinder.get<AppLauncher>(this.llLauncher, R.id.lalBfb)
		val lalPreferences = this.activity.viewFinder.get<AppLauncher>(this.llLauncher, R.id.lalPreferences)
		val lalTrash = this.activity.viewFinder.get<AppLauncher>(this.llLauncher, R.id.lalTrash)

		val theme = DependencyContainer.of(this.activity).themeManager.current
		val lalPreferences_location = theme.lalPreferences_getLocation(
			this.activity.resources, Preferences.getSharedPreferences(this.activity))
		lalBfb.visibility = View.VISIBLE
		lalPreferences.visibility =
			if (lalPreferences_location == Location.NONE) View.GONE else View.VISIBLE
		lalTrash.visibility = View.GONE

		this.llLauncherPinnedApps.alpha = 1.0F
	}

	private fun pinnedAppLauncher(app: App): AppLauncher {
		val appLauncher = AppLauncher(this.activity, app)
		appLauncher.setOnClickListener(AppLauncherClickListener(this.activity))
		appLauncher.setOnLongClickListener(AppLauncherLongClickListener(this.activity))
		appLauncher.setOnDragListener(AppLauncherDragListener(this.appManager))

		return appLauncher
	}
}
