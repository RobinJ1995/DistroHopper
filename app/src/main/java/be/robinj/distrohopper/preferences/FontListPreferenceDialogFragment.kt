package be.robinj.distrohopper.preferences

import android.content.DialogInterface
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference

/**
 * The Appearance font picker. Behaves like the framework's list-preference dialog
 * (single-choice, tap-to-select-and-dismiss) but renders each option in the font
 * it names — the "Ubuntu" row is drawn in Ubuntu, "Oxygen" in Oxygen, and so on —
 * so the user previews each font while choosing it. "System default" keeps the
 * device font.
 *
 * We can't just let the base class build the list because it wires up a private
 * clicked-index field we can't reach, so this replicates its select-and-persist
 * flow with a custom adapter (see [onPrepareDialogBuilder] / [onDialogClosed]).
 * Extends the frosted variant so the legibility fallback is preserved.
 */
class FontListPreferenceDialogFragment :
	PreferencesActivity.FrostedListPreferenceDialogFragment() {

	private var clickedIndex = -1

	override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
		val listPreference = this.preference as ListPreference
		val entries = listPreference.entries
		val entryValues = listPreference.entryValues
		this.clickedIndex = listPreference.findIndexOfValue(listPreference.value)

		val adapter = object : ArrayAdapter<CharSequence>(
			this.requireContext(),
			android.R.layout.select_dialog_singlechoice,
			android.R.id.text1,
			entries,
		) {
			override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
				val view = super.getView(position, convertView, parent)
				val textView = view.findViewById<TextView>(android.R.id.text1)
				textView.typeface =
					FontPreference.typefaceFor(this.context, entryValues[position]?.toString())
						?: Typeface.DEFAULT
				return view
			}
		}

		// Mirror ListPreferenceDialogFragmentCompat: tapping a row selects it and
		// immediately closes with a positive result (no separate OK button). //
		builder.setSingleChoiceItems(adapter, this.clickedIndex) { dialog, which ->
			this.clickedIndex = which
			this.onClick(dialog, DialogInterface.BUTTON_POSITIVE)
			dialog.dismiss()
		}
		builder.setPositiveButton(null, null)
	}

	override fun onDialogClosed(positiveResult: Boolean) {
		if (positiveResult && this.clickedIndex >= 0) {
			val listPreference = this.preference as ListPreference
			val value = listPreference.entryValues[this.clickedIndex].toString()
			if (listPreference.callChangeListener(value)) {
				listPreference.value = value
			}
		}
	}

	companion object {
		fun newInstance(key: String): FontListPreferenceDialogFragment {
			val fragment = FontListPreferenceDialogFragment()
			fragment.arguments = Bundle(1).apply { this.putString("key", key) }
			return fragment
		}
	}
}
