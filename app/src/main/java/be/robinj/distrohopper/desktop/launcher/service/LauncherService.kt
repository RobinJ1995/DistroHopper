package be.robinj.distrohopper.desktop.launcher.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.IBinder
import android.provider.Settings
import be.robinj.distrohopper.ExceptionHandler
import be.robinj.distrohopper.dev.Log
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences

/**
 * Hosts the floating launcher: the dock, pullable from the screen edge, while
 * another app is in front. The window and the gesture live in
 * [FloatingLauncherWindow]; this is the lifecycle shell around it.
 *
 * **It is only ever on screen off the home screen.** DistroHopper's own home
 * screen already shows the dock, so a floating copy of it there would be a
 * second dock over the first: [HomeActivity][be.robinj.distrohopper.HomeActivity]
 * reports itself in front on resume and gone on pause, and the overlay is added
 * and removed with that. (Where DistroHopper is *not* the default home, its home
 * screen is just another app and someone else's home screen is not — so the
 * floating launcher is available there, which is rather the point.)
 *
 * The service is started by the home screen, which is also what feeds it the
 * pinned apps ([FloatingLauncherItem]s — see there for why the model's own [App]
 * objects cannot make the trip). It holds no window while home is in front, so
 * it costs nothing until it is needed, and it is stopped outright as soon as the
 * setting is switched off or the overlay permission is withdrawn.
 */
class LauncherService : Service() {
	private var window: FloatingLauncherWindow? = null
	private var items: List<FloatingLauncherItem> = emptyList()
	private var homeForeground = true

	override fun onBind(intent: Intent?): IBinder? = null

	override fun onCreate() {
		super.onCreate()

		instance = this
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		if (intent != null) {
			intent.getParcelableArrayListExtra<FloatingLauncherItem>(EXTRA_ITEMS)?.let {
				this.items = it
			}
			this.homeForeground = intent.getBooleanExtra(EXTRA_HOME_FOREGROUND, this.homeForeground)
		}

		this.apply()

		// Nothing worth restoring without the home screen's app list; it starts
		// the service again (with fresh pins) the next time it is opened. //
		return START_NOT_STICKY
	}

	/**
	 * Called by the home screen as it comes to the front and as it leaves,
	 * carrying the pins of the desktop it is showing.
	 */
	fun update(items: List<FloatingLauncherItem>, homeForeground: Boolean) {
		this.items = items
		this.homeForeground = homeForeground

		this.apply()
	}

	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)

		// A rotation moves the edge the strip is pinned to and changes how long
		// the zone is; a theme/font change re-inflates the bar. //
		try {
			this.window?.refresh()
		} catch (ex: Exception) {
			ExceptionHandler(ex).logAndTrack()
		}
	}

	override fun onDestroy() {
		try {
			this.window?.hide()
		} catch (ex: Exception) {
			ExceptionHandler(ex).logAndTrack()
		}

		this.window = null
		instance = null

		super.onDestroy()
	}

	private fun apply() {
		try {
			if (! isAvailable(this)) {
				this.window?.hide()
				this.stopSelf()

				return
			}

			if (this.homeForeground) {
				this.window?.hide()

				return
			}

			val window = this.window ?: FloatingLauncherWindow(this).also { this.window = it }
			window.show(this.items)
		} catch (ex: Exception) {
			// A window we cannot put up (permission withdrawn mid-flight, an OEM
			// refusing the overlay type) must not take the home screen down with it //
			Log.getInstance().e(TAG, "Floating launcher failed: " + ex.message)
			ExceptionHandler(ex).logAndTrack()
			this.window = null
			this.stopSelf()
		}
	}

	companion object {
		private const val TAG = "LauncherService"

		const val EXTRA_ITEMS = "items"
		const val EXTRA_HOME_FOREGROUND = "homeForeground"

		/** The running service, so the home screen can nudge it without an Intent round trip. */
		@Volatile
		@JvmStatic
		var instance: LauncherService? = null
			private set

		/** Whether the user has switched the floating launcher on. */
		@JvmStatic
		fun isEnabled(context: Context): Boolean =
			Preferences.getSharedPreferences(context).getBoolean(
				Preference.LAUNCHER_SERVICE_ENABLED.getName(),
				Preference.LAUNCHER_SERVICE_ENABLED.getDefault())

		/**
		 * Whether it can actually run: switched on *and* still allowed to draw over
		 * other apps (the permission can be withdrawn in system settings at any time).
		 */
		@JvmStatic
		fun isAvailable(context: Context): Boolean =
			isEnabled(context) && Settings.canDrawOverlays(context)

		/**
		 * Brings the service in line with the current settings, the pins of the
		 * desktop currently shown, and whether the home screen is in front: starts
		 * it, updates it, or stops it when it may not run.
		 *
		 * A running instance is spoken to directly rather than through
		 * startService(): this is called from onPause too, and leaving the home
		 * screen is exactly the moment a background service start would be refused.
		 */
		@JvmStatic
		fun sync(context: Context, items: ArrayList<FloatingLauncherItem>,
				 homeForeground: Boolean) {
			if (! isAvailable(context)) {
				context.stopService(Intent(context, LauncherService::class.java))

				return
			}

			val running = instance
			if (running != null) {
				running.update(items, homeForeground)

				return
			}

			try {
				context.startService(Intent(context, LauncherService::class.java)
					.putParcelableArrayListExtra(EXTRA_ITEMS, items)
					.putExtra(EXTRA_HOME_FOREGROUND, homeForeground))
			} catch (ex: Exception) {
				// Nothing to do but wait for the next time home is opened: a start
				// refused while backgrounding is the one case this cannot cover //
				Log.getInstance().w(TAG, "Could not start the floating launcher: " + ex.message)
			}
		}
	}
}
