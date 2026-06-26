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
	 * The system's HOME-role dialog asking the user to make this app the default
	 * home app, or `null` where the role is unavailable.
	 *
	 * The dialog is unreliable on some OEM builds (notably Samsung One UI), where
	 * it returns without ever showing a picker. Callers should observe its result
	 * and, when the role still isn't [held][isHeld], fall back to
	 * [homeSettingsIntent].
	 */
	@JvmStatic
	fun roleRequestIntent(context: Context): Intent? {
		val roleManager = context.getSystemService(RoleManager::class.java)

		return if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
			roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
		} else {
			null
		}
	}

	/**
	 * The system home-settings screen (the "default home app" picker): the
	 * fallback used where the role is unavailable or its dialog did nothing.
	 */
	@JvmStatic
	fun homeSettingsIntent(): Intent = Intent(Settings.ACTION_HOME_SETTINGS)
}
