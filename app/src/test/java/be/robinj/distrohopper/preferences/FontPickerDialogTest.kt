package be.robinj.distrohopper.preferences

import android.app.Application
import android.text.Spanned
import android.text.style.TypefaceSpan
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.ListPreference
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

/**
 * The font picker must draw every option in the font it names. Asserting on
 * [FontPreference.styledEntries] alone would not catch the dialog dropping the
 * styling on its way to the screen, so this drives the real picker dialog and
 * reads the typeface off the row views the user actually sees.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class FontPickerDialogTest {

	private lateinit var application: Application

	@Before fun setUp() {
		this.application = ApplicationProvider.getApplicationContext()
		Preferences.getSharedPreferences(this.application).edit().clear().commit()
	}

	/** Opens the font picker and returns the text of each row, as rendered. */
	private fun pickerRowText(): List<CharSequence> {
		val rows = mutableListOf<CharSequence>()

		ActivityScenario.launch(PreferencesActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				val fragment = activity.supportFragmentManager
					.findFragmentById(R.id.preferences_container)
					as PreferencesActivity.PreferencesFragment
				val fontPref = fragment.findPreference<ListPreference>(
					Preference.FONT.getName())!!

				fragment.onDisplayPreferenceDialog(fontPref)
				activity.supportFragmentManager.executePendingTransactions()

				val dialogFragment = activity.supportFragmentManager.findFragmentByTag(
					"androidx.preference.PreferenceFragment.DIALOG") as DialogFragment
				val dialog = dialogFragment.dialog as AlertDialog
				val listView = dialog.listView
				assertNotNull("picker has no list", listView)

				val adapter = listView.adapter
				for (i in 0 until adapter.count) {
					val row = adapter.getView(i, null, listView)
					val label = row as? TextView
						?: row.findViewById(android.R.id.text1)
					rows.add(label.text)
				}
			}
		}

		return rows
	}

	@Test fun everyOptionIsDrawnInTheFontItNames() {
		val entries = this.application.resources.getTextArray(R.array.font_entries)
		val values = this.application.resources.getTextArray(R.array.font_values)

		val rows = this.pickerRowText()

		assertEquals(entries.size, rows.size)
		for (i in rows.indices) {
			assertEquals(entries[i].toString(), rows[i].toString())

			val spans = (rows[i] as? Spanned)
				?.getSpans(0, rows[i].length, TypefaceSpan::class.java)
				?: emptyArray()

			if (values[i] == FontPreference.SYSTEM) {
				assertTrue("System must keep the device font", spans.isEmpty())
			} else {
				assertEquals("${values[i]} row is not styled", 1, spans.size)
				assertEquals(
					"${values[i]} row uses the wrong typeface",
					FontPreference.typefaceFor(this.application, values[i].toString()),
					spans[0].typeface,
				)
			}
		}
	}

	/** The preview must not depend on which font is currently selected. */
	@Test fun optionsKeepTheirOwnFontsWhileACustomFontIsActive() {
		Preferences.getSharedPreferences(this.application).edit()
			.putString(Preference.FONT.getName(), "ubuntu").commit()
		val values = this.application.resources.getTextArray(R.array.font_values)

		val rows = this.pickerRowText()

		val styled = rows.indices.count { i ->
			values[i] != FontPreference.SYSTEM &&
				(rows[i] as? Spanned)
					?.getSpans(0, rows[i].length, TypefaceSpan::class.java)
					?.size == 1
		}
		assertEquals(values.count { it != FontPreference.SYSTEM }, styled)
	}
}
