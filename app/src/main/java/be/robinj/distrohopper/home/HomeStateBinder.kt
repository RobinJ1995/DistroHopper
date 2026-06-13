package be.robinj.distrohopper.home

import android.widget.LinearLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.desktop.launcher.AppLauncher
import kotlinx.coroutines.launch

/**
 * Connects HomeViewModel's state flows to the home controllers. Lives apart
 * from the ViewModel so the ViewModel never touches views, and apart from
 * HomeActivity because collecting flows from Java is unwieldy.
 *
 * Note: HomeActivity also calls the controllers directly in its event
 * handlers (the controllers are idempotent), keeping UI reactions
 * synchronous; this collection is the state-of-record path that restores
 * the UI after recreate() and applies live preference changes.
 */
object HomeStateBinder {
	@JvmStatic
	fun bind(
		activity: HomeActivity,
		viewModel: HomeViewModel,
		dash: DashController,
		themeApplier: ThemeApplier,
	) {
		activity.lifecycleScope.launch {
			activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
				this.launch {
					viewModel.dashOpen.collect { open ->
						if (open) dash.open() else dash.close()
						// Gates the dash workspace indicator (the GNOME pill only
						// shows while the dash is open); no-op until apps load.
						activity.appManager?.setDashOpen(open)
					}
				}

				this.launch {
					viewModel.panelOpacity.collect { opacity ->
						if (! dash.isOpen) {
							activity.viewFinder.get<LinearLayout>(R.id.llPanel).alpha =
								opacity.toFloat() / 100F
						}
					}
				}

				this.launch {
					viewModel.dashIconWidth.collect { width ->
						// Once apps load, the binder owns the dash grid and the
						// per-workspace pager pages; before that, the ThemeApplier
						// sizes the (only) grid.
						val apps = activity.appManager
						if (apps != null) apps.applyDashIconWidth(width)
						else themeApplier.applyDashIconWidth(width)
					}
				}

				this.launch {
					viewModel.launcherIconWidth.collect { width ->
						applyLauncherIconWidth(activity, width)
					}
				}

				this.launch {
					viewModel.showRunningApps.collect { show ->
						val apps = activity.appManager

						if (show && apps != null) {
							apps.addRunningApps(dash.chameleonicBgColour)
						} else if (! show) {
							activity.viewFinder
								.get<LinearLayout>(R.id.llLauncherRunningApps).removeAllViews()
						}
					}
				}
			}
		}
	}

	/**
	 * Resizes the panel's dash-close button and re-initialises every launcher
	 * icon with the new width preference. Used to live inside the customise
	 * seekbar's listener; now any writer of the preference gets it applied.
	 */
	private fun applyLauncherIconWidth(activity: HomeActivity, width: Int) {
		val density = activity.resources.displayMetrics.density
		val viewFinder = activity.viewFinder

		val ibDashClose_layoutParams = LinearLayout.LayoutParams(
			((48 + width).toFloat() * density).toInt(), LinearLayout.LayoutParams.MATCH_PARENT)
		viewFinder.get<android.widget.ImageButton>(R.id.ibPanelDashClose).layoutParams =
			ibDashClose_layoutParams

		viewFinder.get<AppLauncher>(R.id.lalBfb).init()
		viewFinder.get<AppLauncher>(R.id.lalSpinner).init()

		val llLauncherPinnedApps = viewFinder.get<LinearLayout>(R.id.llLauncherPinnedApps)
		for (i in 0 until llLauncherPinnedApps.childCount)
			(llLauncherPinnedApps.getChildAt(i) as AppLauncher).init()

		val llLauncherRunningApps = viewFinder.get<LinearLayout>(R.id.llLauncherRunningApps)
		for (i in 0 until llLauncherRunningApps.childCount)
			(llLauncherRunningApps.getChildAt(i) as AppLauncher).init()

		viewFinder.get<AppLauncher>(R.id.lalTrash).init()
		viewFinder.get<AppLauncher>(R.id.lalPreferences).init()
	}
}
