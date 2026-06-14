package be.robinj.distrohopper.accessibility

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import be.robinj.distrohopper.ExceptionHandler
import be.robinj.distrohopper.R

/**
 * A small, onboarding-styled wizard that explains why the notification-tray
 * gesture needs an accessibility service (and that the service does nothing
 * else), then guides the user to turn it on. Auto-finishes once the service is
 * enabled. Launched from the Gestures preferences when the service is off.
 */
class AccessibilityGestureSetupActivity : AppCompatActivity() {
	private lateinit var flipper: ViewFlipper

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		this.setContentView(R.layout.activity_accessibility_gesture_setup)
		this.flipper = this.findViewById(R.id.vfGestureSetup)

		this.findViewById<Button>(R.id.btnGestureSetupCancel).setOnClickListener { this.finish() }
		this.findViewById<Button>(R.id.btnGestureSetupProceed).setOnClickListener {
			this.flipper.displayedChild = STEP_GUIDE
			this.openAccessibilitySettings()
		}
		this.findViewById<Button>(R.id.btnGestureSetupOpenSettings).setOnClickListener {
			this.openAccessibilitySettings()
		}
	}

	override fun onResume() {
		super.onResume()

		// Returning with the service granted is the success exit. //
		if (NotificationAccessibilityService.isEnabled(this)) {
			Toast.makeText(this, R.string.toast_gesture_setup_enabled, Toast.LENGTH_SHORT).show()
			this.finish()
		}
	}

	private fun openAccessibilitySettings() {
		try {
			this.startActivity(NotificationAccessibilityService.accessibilitySettingsIntent())
		} catch (ex: Exception) {
			ExceptionHandler(ex).show(this)
		}
	}

	companion object {
		private const val STEP_GUIDE = 1
	}
}
