package be.robinj.distrohopper

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.os.UserManager

/**
 * Helpers around Android user profiles, surfaced in the UI as "profiles":
 * the personal profile plus any other profiles (typically the work profile)
 * visible to the launcher through the LauncherApps API. Throughout the app
 * model a null UserHandle means the personal profile; only apps in other
 * profiles carry their profile's UserHandle.
 */
object Profiles {
	/** Profiles other than the personal one whose apps the launcher should show. */
	@JvmStatic
	fun otherProfiles(context: Context): List<UserHandle> {
		val launcherApps = context.getSystemService(LauncherApps::class.java)
			?: return emptyList()
		val personal = Process.myUserHandle()

		return launcherApps.profiles.filter { personal != it }
	}

	@JvmStatic
	fun label(context: Context, user: UserHandle?): String =
		context.getString(
			if (user == null) R.string.profile_personal else R.string.profile_work)

	/** Stable-across-reboots identifier used to persist a profile (e.g. for pinned apps). */
	@JvmStatic
	fun serialOf(context: Context, user: UserHandle): Long =
		(context.getSystemService(Context.USER_SERVICE) as UserManager)
			.getSerialNumberForUser(user)
}
