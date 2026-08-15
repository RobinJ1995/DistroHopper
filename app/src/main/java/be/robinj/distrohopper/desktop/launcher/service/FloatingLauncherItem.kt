package be.robinj.distrohopper.desktop.launcher.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.os.UserHandle
import be.robinj.distrohopper.App
import be.robinj.distrohopper.AppUsageStats
import be.robinj.distrohopper.Profiles
import be.robinj.distrohopper.cache.AppIconCache
import be.robinj.distrohopper.dev.Log
import be.robinj.distrohopper.icons.IconConfig
import be.robinj.distrohopper.icons.IconRenderer

/**
 * One pinned app as the floating launcher knows it: just enough to draw and
 * launch it, in a form that survives the trip to the service through an Intent.
 *
 * [App] itself is Parcelable, but only by name — an unparcelled App has neither
 * the Context nor the AppManager it needs, so asking one for its icon or asking
 * it to launch throws. (That is what the old launcher service did.) This carries
 * the same identity, resolves its icon against the launcher's own icon cache,
 * and launches through a plain intent (or LauncherApps for another profile),
 * with nothing borrowed from the home screen's object graph.
 */
class FloatingLauncherItem(
	val packageName: String,
	val activityName: String,
	val label: String?,
	/** null = the personal profile; set = the (work) profile the app lives in. */
	val user: UserHandle?,
	/** The profile's stable serial number; -1 for the personal profile. */
	val userSerial: Long,
) : Parcelable {
	/**
	 * The key this app is cached and counted under — identical to
	 * [App.getProfileScopedKey], so the icons the home screen has already
	 * rendered are the ones the floating launcher shows.
	 */
	val key: String
		get() = if (this.user == null) "${this.packageName}\n${this.activityName}"
			else "${this.packageName}\n${this.activityName}\n${this.userSerial}"

	private val component: ComponentName
		get() = ComponentName(this.packageName, this.activityName)

	/**
	 * The app's icon: the home screen's cached one (already shaped, tinted and
	 * icon-packed to the user's settings) where there is one, else the system
	 * icon put through the same [IconRenderer] so a cache miss doesn't stand out.
	 * Null when the app can no longer be resolved at all.
	 */
	fun icon(context: Context): Drawable? {
		try {
			AppIconCache(context.applicationContext).get(this.key)?.let { return it }
		} catch (ex: Exception) {
			Log.getInstance().w(TAG, "Cached icon lookup failed for ${this.key}: ${ex.message}")
		}

		return try {
			val raw = if (this.user != null) {
				context.getSystemService(LauncherApps::class.java)
					?.getActivityList(this.packageName, this.user)
					?.firstOrNull { it.componentName.className == this.activityName }
					?.getIcon(0)
			} else {
				context.packageManager.getActivityIcon(this.component)
			} ?: return null

			val rendered = IconRenderer(context, IconConfig.fromPrefs(context)).render(raw)

			if (this.user == null) rendered else Profiles.badgedIcon(context, rendered, this.user)
		} catch (ex: Exception) {
			Log.getInstance().w(TAG, "Failed to load icon for ${this.key}: ${ex.message}")

			null
		}
	}

	/**
	 * Starts the app, the same way [App.launch] does — including recording the
	 * launch for the usage-based dash sort orders, so opening something from the
	 * floating launcher counts just as much as opening it from the dock.
	 */
	fun launch(context: Context) {
		try {
			if (this.user != null) {
				// Apps in another profile cannot be started with a regular intent //
				context.getSystemService(LauncherApps::class.java)
					?.startMainActivity(this.component, this.user, null, null)
			} else {
				context.startActivity(Intent(Intent.ACTION_MAIN)
					.addCategory(Intent.CATEGORY_LAUNCHER)
					.setComponent(this.component)
					.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
						or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
			}

			AppUsageStats(context).recordLaunch(this.key)
		} catch (ex: Exception) {
			Log.getInstance().e(TAG,
				"Failed to launch ${this.packageName}/${this.activityName}: ${ex.message}")
		}
	}

	override fun describeContents(): Int = 0

	override fun writeToParcel(dest: Parcel, flags: Int) {
		dest.writeString(this.packageName)
		dest.writeString(this.activityName)
		dest.writeString(this.label)
		dest.writeParcelable(this.user, flags)
		dest.writeLong(this.userSerial)
	}

	companion object {
		private const val TAG = "FloatingLauncherItem"

		/** The floating launcher's view of an [App] from the home screen's model. */
		@JvmStatic
		fun of(context: Context, app: App): FloatingLauncherItem {
			val user = app.user

			return FloatingLauncherItem(
				app.packageName,
				app.activityName,
				app.label,
				user,
				if (user == null) -1L else Profiles.serialOf(context, user))
		}

		@JvmField
		val CREATOR = object : Parcelable.Creator<FloatingLauncherItem> {
			override fun createFromParcel(parcel: Parcel): FloatingLauncherItem =
				FloatingLauncherItem(
					parcel.readString() ?: "",
					parcel.readString() ?: "",
					parcel.readString(),
					parcel.readParcelable(UserHandle::class.java.classLoader),
					parcel.readLong())

			override fun newArray(size: Int): Array<FloatingLauncherItem?> = arrayOfNulls(size)
		}
	}
}
