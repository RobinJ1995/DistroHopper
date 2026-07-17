package be.robinj.distrohopper.preferences

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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

	/**
	 * The [Typeface] for a given font preference [value], or null when [value] is
	 * System / unknown (i.e. the system font should be used). Unlike [typeface]
	 * this ignores the stored preference and resolves whatever value is passed,
	 * so the picker can render each option in its own font.
	 */
	fun typefaceFor(context: Context, value: String?): Typeface? {
		val fontRes = this.fontResFor(value) ?: return null
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

	/**
	 * Applies the chosen font to an already-shown [dialog]. A dialog inflates its
	 * title (and chrome) through the dialog window's own LayoutInflater, which
	 * doesn't carry the activity's [FontInflaterFactory], so we sweep the decor
	 * view and force the typeface onto every TextView. No-op for System.
	 *
	 * Attach via [Dialog.setOnShowListener] so the views exist when this runs.
	 */
	fun applyTo(dialog: Dialog) {
		val typeface = this.typeface(dialog.context) ?: return
		val root = dialog.window?.decorView ?: return

		this.applyTypeface(root, typeface)
	}

	private fun applyTypeface(view: View, typeface: Typeface) {
		when (view) {
			is ViewGroup -> for (i in 0 until view.childCount) {
				this.applyTypeface(view.getChildAt(i), typeface)
			}
			// Keep each view's own style (bold/italic) while swapping the family.
			is TextView -> view.setTypeface(typeface, view.typeface?.style ?: Typeface.NORMAL)
		}
	}
}
