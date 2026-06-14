package be.robinj.distrohopper.preferences

import android.app.Activity
import android.content.Context
import androidx.annotation.StyleRes
import be.robinj.distrohopper.R

/**
 * Maps the user's "font" preference to a theme overlay and applies it.
 *
 * "System" applies no overlay, so the app stays visually identical to its
 * default. The other values map to the FontOverlay.* styles in styles.xml,
 * which override the dhFontBody / dhFontMedium theme attributes with a bundled
 * font. [applyTo] is called for every activity before it inflates its layout
 * (see Application's ActivityLifecycleCallbacks).
 */
object FontPreference {

	const val SYSTEM = "system"

	/** Overlay style for [value], or null when no overlay is needed (System). */
	@StyleRes
	fun overlayFor(value: String?): Int? = when (value) {
		"opendyslexic" -> R.style.FontOverlay_OpenDyslexic
		"ubuntu" -> R.style.FontOverlay_Ubuntu
		"oxygen" -> R.style.FontOverlay_Oxygen
		else -> null // SYSTEM and any unknown value fall back to the system font
	}

	private fun current(context: Context): String =
		Preferences.getSharedPreferences(context)
			.getString(Preference.FONT.getName(), SYSTEM) ?: SYSTEM

	/**
	 * Applies the selected font overlay to [activity]'s theme. Must run before
	 * the activity inflates its content so the font reaches every view.
	 */
	fun applyTo(activity: Activity) {
		val overlay = overlayFor(current(activity)) ?: return
		activity.theme.applyStyle(overlay, true)
	}
}
