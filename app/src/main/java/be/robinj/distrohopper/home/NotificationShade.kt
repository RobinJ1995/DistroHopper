package be.robinj.distrohopper.home

import android.annotation.SuppressLint
import android.content.Context
import be.robinj.distrohopper.dev.Log

/**
 * Pulls down the system notification shade. There is no public API for this:
 * StatusBarManager#expandNotificationsPanel is hidden (on the unsupported
 * greylist), so reflection is the only way for a launcher to offer the usual
 * swipe-down-for-notifications gesture. Failure (OEM removals, future
 * blocklisting) is non-fatal — the gesture just does nothing.
 */
object NotificationShade {
	private const val TAG = "NotificationShade"

	@SuppressLint("WrongConstant")
	fun expand(context: Context): Boolean =
		try {
			val statusBar = context.getSystemService("statusbar")
			Class.forName("android.app.StatusBarManager")
				.getMethod("expandNotificationsPanel")
				.invoke(statusBar)

			true
		} catch (ex: Exception) {
			Log.getInstance().w(TAG, "Failed to expand the notification shade: " + ex)

			false
		}
}
