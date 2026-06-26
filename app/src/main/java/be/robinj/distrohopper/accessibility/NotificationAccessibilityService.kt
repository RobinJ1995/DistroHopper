package be.robinj.distrohopper.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager

/**
 * The only public route left for a non-system launcher to open the notification
 * shade is an accessibility service's performGlobalAction(): the
 * StatusBarManager API is blocklisted. This service does nothing but expose that
 * one action — it reads no screen content (see its config, typeNone) — and the
 * home screen reaches the running instance through the [instance] singleton (a
 * different process component can't bind to it directly for this).
 *
 * Opt-in: a swipe gesture only maps to it once the user has granted the service
 * in system settings (see [isEnabled]).
 */
class NotificationAccessibilityService : AccessibilityService() {
	override fun onServiceConnected() {
		super.onServiceConnected()
		instance = this
	}

	override fun onUnbind(intent: android.content.Intent?): Boolean {
		instance = null
		return super.onUnbind(intent)
	}

	override fun onDestroy() {
		instance = null
		super.onDestroy()
	}

	override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* No events observed. */ }

	override fun onInterrupt() { /* Nothing to interrupt. */ }

	fun openNotifications(): Boolean = this.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)

	companion object {
		@Volatile
		var instance: NotificationAccessibilityService? = null
			private set

		val isConnected: Boolean
			get() = instance != null

		private fun component(context: Context): ComponentName =
			ComponentName(context, NotificationAccessibilityService::class.java)

		/**
		 * Whether the user has granted this service in system accessibility
		 * settings — distinct from [isConnected], which only reflects a live bind.
		 */
		@JvmStatic
		fun isEnabled(context: Context): Boolean {
			val component = component(context)
			val manager =
				context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
			val running = manager?.getEnabledAccessibilityServiceList(
				AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
			if (running != null && running.any { it.id == component.flattenToString() }) {
				return true
			}

			// Fallback: parse the secure setting directly (the manager list can lag
			// a just-granted service). //
			val enabled = Settings.Secure.getString(
				context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
				?: return false
			val flat = component.flattenToString()
			return enabled.split(':').any { it.equals(flat, ignoreCase = true) }
		}

		/**
		 * Opens the system accessibility settings list (reliable across OEMs); the
		 * user enables DistroHopper's service from there.
		 */
		@JvmStatic
		fun accessibilitySettingsIntent(): Intent =
			Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
	}
}
