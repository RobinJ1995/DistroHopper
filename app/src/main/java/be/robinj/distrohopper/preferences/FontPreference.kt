package be.robinj.distrohopper.preferences

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import androidx.annotation.FontRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.LayoutInflaterCompat
import be.robinj.distrohopper.R

/**
 * Maps the user's "font" preference to a bundled font and applies it app-wide.
 *
 * "System" uses no custom font, so the app stays visually identical to its
 * default. The other values map to the bundled font families in res/font. The
 * selected font is applied by installing a [FontInflaterFactory] on the
 * activity's LayoutInflater (see [applyTo]), which forces the typeface onto
 * every inflated TextView. [applyTo] is called for every activity before it
 * inflates its layout (see Application's ActivityLifecycleCallbacks).
 */
object FontPreference {

	const val SYSTEM = "system"

	/** Bundled font resource for [value], or null for System / unknown values. */
	@FontRes
	fun fontResFor(value: String?): Int? = when (value) {
		"opendyslexic" -> R.font.opendyslexic
		"ubuntu" -> R.font.ubuntu
		"oxygen" -> R.font.oxygen
		else -> null // SYSTEM and any unknown value fall back to the system font
	}

	/** The selected [Typeface], or null when the system font should be used. */
	fun typeface(context: Context): Typeface? {
		val fontRes = this.fontResFor(this.current(context)) ?: return null
		return ResourcesCompat.getFont(context, fontRes)
	}

	private fun current(context: Context): String =
		Preferences.getSharedPreferences(context)
			.getString(Preference.FONT.getName(), SYSTEM) ?: SYSTEM

	/**
	 * Installs the chosen font on [activity] by setting a [FontInflaterFactory]
	 * on its LayoutInflater. Must run before the activity inflates its content
	 * so the font reaches every view. No-op for System (or non-AppCompat
	 * activities), leaving the system font untouched.
	 */
	fun applyTo(activity: Activity) {
		if (activity !is AppCompatActivity) return
		val typeface = this.typeface(activity) ?: return

		// We run before AppCompat installs its own factory, so the inflater
		// should be untouched; guard like AppCompat does to never clobber an
		// existing factory (and to avoid setFactory2's "already set" crash).
		val inflater = activity.layoutInflater
		if (inflater.factory2 != null) return

		LayoutInflaterCompat.setFactory2(
			inflater,
			FontInflaterFactory(activity.delegate, typeface),
		)
	}
}
