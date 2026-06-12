package be.robinj.distrohopper

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Default-launcher (HOME role) helpers, shared by the first-run wizard and
 * the preferences screen.
 */
object HomeRole {
	@JvmStatic
	fun isHeld(context: Context): Boolean =
		context.getSystemService(RoleManager::class.java)
			?.isRoleHeld(RoleManager.ROLE_HOME) == true

	/**
	 * An intent prompting the user to make this app the default home app: the
	 * system's role dialog, or the home settings screen where the role is
	 * unavailable.
	 */
	@JvmStatic
	fun requestIntent(context: Context): Intent {
		val roleManager = context.getSystemService(RoleManager::class.java)

		return if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
			roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
		} else {
			Intent(Settings.ACTION_HOME_SETTINGS)
		}
	}
}
