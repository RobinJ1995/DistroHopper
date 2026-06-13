package be.robinj.distrohopper.home

import android.graphics.drawable.Drawable
import android.view.View
import androidx.lifecycle.lifecycleScope
import be.robinj.distrohopper.DispatcherProvider
import be.robinj.distrohopper.ExceptionHandler
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.cache.ICache
import be.robinj.distrohopper.desktop.Wallpaper
import be.robinj.distrohopper.desktop.launcher.AppLauncher
import be.robinj.distrohopper.desktop.launcher.SpinnerAppLauncher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Orchestrates the startup loading that used to be a chain of AsyncTasks on
 * the SERIAL_EXECUTOR: wallpaper init, then the app list, then label and
 * icon caching — strictly in that order, as both the wallpaper and app
 * loading paths touch the BFB. Runs in the activity's lifecycleScope so it
 * is cancelled on destroy (the activity is recreated for theme/edge changes,
 * which restarts loading from scratch, as before).
 *
 * Scoped to the activity rather than HomeViewModel on purpose: the loaded
 * AppManager still binds views (see AGENTS.md), so it must not outlive the
 * activity instance. Once AppRepository exists this can move to the
 * ViewModel.
 */
class StartupLoader(
	private val activity: HomeActivity,
	private val dispatchers: DispatcherProvider,
) {
	private var job: Job? = null

	fun start(
		wpWallpaper: Wallpaper,
		lalSpinner: SpinnerAppLauncher,
		lalBfb: AppLauncher,
		appLabelCache: ICache<String>,
		appIconCache: ICache<Drawable>,
		displayDensity: Float,
		dashIconWidth: Int,
	) {
		this.job = this.activity.lifecycleScope.launch {
			try {
				// Wallpaper first // In its own try so a wallpaper failure cannot
				// prevent the apps from loading (the AsyncTasks were independent) //
				try {
					withContext(this@StartupLoader.dispatchers.io) {
						wpWallpaper.init()
					}
					wpWallpaper.set()
					this@StartupLoader.activity.asyncInitWallpaperDone(wpWallpaper)
				} catch (ex: CancellationException) {
					throw ex
				} catch (ex: Exception) {
					ExceptionHandler(ex).show(this@StartupLoader.activity)
				}

				// Then the apps //
				val appManager = withContext(this@StartupLoader.dispatchers.io) {
					AppsLoader.loadApps(this@StartupLoader.activity,
						this@StartupLoader.activity.applicationContext,
						appLabelCache, appIconCache) { step, steps ->
						this@StartupLoader.activity.runOnUiThread {
							lalSpinner.progressWheel.setProgress(
								Math.round((step.toFloat() / steps.toFloat()) * 360F))
						}
					}
				}

				lalSpinner.visibility = View.GONE
				lalBfb.visibility = View.VISIBLE

				appManager.refreshPinnedView()

				// Single grid, or one labelled section per workspace (work profile) //
				appManager.bindDashApps(displayDensity, dashIconWidth)

				this@StartupLoader.activity.asyncLoadInstalledAppsDone(appManager)

				// Finally the label and icon caches //
				withContext(this@StartupLoader.dispatchers.io) {
					AppsLoader.loadLabels(appManager, appLabelCache)
				}
				appManager.asyncLoadAppLabelsDone()

				withContext(this@StartupLoader.dispatchers.io) {
					AppsLoader.loadIcons(appManager, appIconCache)
				}
				appManager.asyncLoadAppIconsDone()
			} catch (ex: CancellationException) {
				throw ex
			} catch (ex: Exception) {
				ExceptionHandler(ex).show(this@StartupLoader.activity)
			}
		}
	}

	fun cancel() {
		this.job?.cancel()
	}
}
