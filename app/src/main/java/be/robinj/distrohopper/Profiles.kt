package be.robinj.distrohopper

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
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

	/**
	 * A display label for [user]'s profile. Android exposes the profile *type*
	 * to launchers but not the user-assigned name, so — like AOSP's launcher —
	 * we map the system type to our own (translatable) label rather than invent
	 * a name. [LauncherApps.getLauncherUserInfo] is API 34+; before that the only
	 * non-personal profile a launcher can see is a managed (work) profile.
	 */
	@JvmStatic
	fun label(context: Context, user: UserHandle?): String {
		if (user == null) {
			return context.getString(R.string.profile_personal)
		}

		val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			context.getSystemService(LauncherApps::class.java)?.getLauncherUserInfo(user)?.userType
		} else {
			null
		}

		return context.getString(this.labelRes(type))
	}

	/**
	 * Our label string for a [LauncherApps.getLauncherUserInfo] profile type
	 * (a `UserManager.USER_TYPE_PROFILE_*`). Null = unknown/pre-34, where the
	 * only non-personal profile a launcher sees is a managed (work) one. Split
	 * out so the type→label mapping is unit-testable without LauncherApps.
	 */
	@JvmStatic
	fun labelRes(type: String?): Int = when (type) {
		UserManager.USER_TYPE_PROFILE_MANAGED, null -> R.string.profile_work
		UserManager.USER_TYPE_PROFILE_PRIVATE -> R.string.profile_private
		UserManager.USER_TYPE_PROFILE_CLONE -> R.string.profile_clone
		else -> R.string.profile_other
	}

	/** Stable-across-reboots identifier used to persist a profile (e.g. for pinned apps). */
	@JvmStatic
	fun serialOf(context: Context, user: UserHandle): Long =
		(context.getSystemService(Context.USER_SERVICE) as UserManager)
			.getSerialNumberForUser(user)

	/**
	 * [drawable] stamped with [user]'s system profile badge (work briefcase,
	 * private-space lock, …) in the bottom-right corner. The badge is composited
	 * ourselves rather than via [getUserBadgedDrawableForDensity], whose badge
	 * rect the platform ignores for some drawables (leaving a tiny default badge
	 * that is barely visible on the big dash icons).
	 */
	@JvmStatic
	fun badgedIcon(context: Context, drawable: Drawable, user: UserHandle): Drawable {
		val intrinsic = Math.max(drawable.intrinsicWidth, drawable.intrinsicHeight)
		val size = if (intrinsic > 0) intrinsic
			else (96 * context.resources.displayMetrics.density).toInt()

		val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
		val canvas = Canvas(bitmap)
		drawable.setBounds(0, 0, size, size)
		drawable.draw(canvas)

		val badgeSize = (size * BADGE_FRACTION).toInt()
		val badge = this.profileGlyph(context, user, badgeSize)
		badge.setBounds(size - badgeSize, size - badgeSize, size, size)
		badge.draw(canvas)

		return BitmapDrawable(context.resources, bitmap)
	}

	/**
	 * Just [user]'s system profile badge as a standalone [sizePx]-square glyph —
	 * the whole icon is the badge, so it is automatically correct for the profile
	 * type (work briefcase, private-space lock, clone, …) with nothing of ours to
	 * keep in sync. Used as the profile's tab glyph in indicators. [desaturate]
	 * greyscales it (keeping its shape, unlike a flat tint) to sit better beside a
	 * monochrome glyph set.
	 */
	@JvmStatic
	@JvmOverloads
	fun profileGlyph(
		context: Context, user: UserHandle, sizePx: Int, desaturate: Boolean = false): Drawable {
		// A transparent square base, with the badge filling its whole bounds.
		val blank = BitmapDrawable(context.resources,
			Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888))
		val badge = context.packageManager
			.getUserBadgedDrawableForDensity(blank, user, Rect(0, 0, sizePx, sizePx), 0)

		if (desaturate) {
			badge.mutate()
			badge.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0F) })
		}

		return badge
	}

	/** Badge covers this fraction of the icon's width/height in the corner. */
	private const val BADGE_FRACTION = 0.42F
}
