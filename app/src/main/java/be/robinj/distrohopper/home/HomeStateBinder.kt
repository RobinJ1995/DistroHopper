package be.robinj.distrohopper.home

import android.widget.LinearLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.desktop.launcher.AppLauncher
import be.robinj.distrohopper.desktop.launcher.LauncherIconGrid
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
						// Back must close the dash (and not flash the home) whenever
						// it's open, regardless of how it was opened.
						activity.updateBackCallback()
						// Gates the dash profile indicator (the GNOME pill only
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
					viewModel.dashGridColumns.collect {
						// Once apps load, the binder owns the dash grid and the
						// per-profile pager pages; before that, the ThemeApplier
						// sizes the (only) grid. DashGrid recomputes the count.
						val apps = activity.appManager
						if (apps != null) apps.applyDashColumns()
						else themeApplier.applyDashColumns()
					}
				}

				this.launch {
					viewModel.launcherIconPreset.collect {
						applyLauncherIconSize(activity)
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
	 * icon for the current pinned-icon size preset (the pixel size is recomputed
	 * by [LauncherIconGrid]). Used to live inside the customise seekbar's
	 * listener; now any writer of the preference gets it applied.
	 */
	private fun applyLauncherIconSize(activity: HomeActivity) {
		val viewFinder = activity.viewFinder

		val ibDashClose_layoutParams = LinearLayout.LayoutParams(
			LauncherIconGrid.iconSizePx(activity), LinearLayout.LayoutParams.MATCH_PARENT)
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
		viewFinder.get<AppLauncher>(R.id.lalAppInfo).init()
		viewFinder.get<AppLauncher>(R.id.lalPreferences).init()

		// The slot size changed, so the whole-slot scroll clip must re-measure.
		viewFinder.get<LinearLayout>(R.id.llLauncher).requestLayout()
	}
}
