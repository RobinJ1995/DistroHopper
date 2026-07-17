package be.robinj.distrohopper.preferences

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate

/**
 * A [LayoutInflater.Factory2] that forces a [FontStyle] onto every [TextView] as
 * it is inflated, so the user's chosen font (and its spacing corrections) reach
 * the whole app.
 *
 * Setting a font at the theme level does not cascade to TextViews (the
 * framework's default text appearances pin fontFamily to sans-serif, which
 * wins), so instead we set the real typeface programmatically here — after the
 * view is created — which overrides any style or textAppearance.
 *
 * Creation is delegated to AppCompat's [AppCompatDelegate.createView] (the same
 * call AppCompat's own factory makes) so view substitution and tinting are
 * preserved. This is installed per-activity before layout inflation; adapters
 * and dialogs reuse the activity's inflater (directly or via cloneInContext),
 * so their text is covered too. When "System" is selected no factory is
 * installed at all, leaving the app visually identical to before.
 */
class FontInflaterFactory(
	private val delegate: AppCompatDelegate,
	private val fontStyle: FontStyle,
) : LayoutInflater.Factory2 {

	override fun onCreateView(
		parent: View?,
		name: String,
		context: Context,
		attrs: AttributeSet,
	): View? {
		val view = this.delegate.createView(parent, name, context, attrs)
		if (view is TextView) {
			this.fontStyle.applyTo(view)
		}

		return view
	}

	override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? =
		this.onCreateView(null, name, context, attrs)
}
