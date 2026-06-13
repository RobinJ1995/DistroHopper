package be.robinj.distrohopper.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * The only public route left for a non-system launcher to open the notification
 * shade is an accessibility service's performGlobalAction(): the
 * StatusBarManager API is blocklisted. This service does nothing but expose that
 * one action — it reads no screen content (see its config, typeNone) — and the
 * home screen reaches the running instance through the [instance] singleton (a
 * different process component can't bind to it directly for this).
 *
 * Experimental and opt-in: gated behind the GESTURE_NOTIFICATION_TRAY developer
 * setting and the user manually granting the service in system settings.
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
	}
}
