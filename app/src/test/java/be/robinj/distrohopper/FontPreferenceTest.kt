package be.robinj.distrohopper

import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spanned
import android.text.style.TypefaceSpan
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.FontInflaterFactory
import be.robinj.distrohopper.preferences.FontPreference
import be.robinj.distrohopper.preferences.FontStyle
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FontPreferenceTest {

	private val context: Context
		get() = ApplicationProvider.getApplicationContext()

	private fun setFont(value: String?) {
		Preferences.getSharedPreferences(this.context).edit()
			.putString(Preference.FONT.getName(), value).commit()
	}

	@Test fun systemAndUnknownValuesUseNoFont() {
		assertNull(FontPreference.fontResFor("system"))
		assertNull(FontPreference.fontResFor(null))
		assertNull(FontPreference.fontResFor("something-else"))
	}

	@Test fun bundledFontsMapToResources() {
		assertEquals(R.font.opendyslexic, FontPreference.fontResFor("opendyslexic"))
		assertEquals(R.font.ubuntu, FontPreference.fontResFor("ubuntu"))
		assertEquals(R.font.oxygen, FontPreference.fontResFor("oxygen"))
	}

	/** No custom typeface for System, so the app keeps the system font. */
	@Test fun typefaceIsNullForSystem() {
		this.setFont("system")
		assertNull(FontPreference.typeface(this.context))
	}

	@Test fun typefaceLoadsBundledFont() {
		this.setFont("ubuntu")
		assertNotNull(FontPreference.typeface(this.context))
	}

	/** The factory forces its typeface onto every TextView it inflates. */
	@Test fun factoryAppliesTypefaceToInflatedTextViews() {
		this.setFont("system")
		val activity = Robolectric.buildActivity(FontTestActivity::class.java).create().get()

		val factory = FontInflaterFactory(activity.delegate, FontStyle(Typeface.MONOSPACE))
		val attrs = Robolectric.buildAttributeSet().build()
		val view = factory.onCreateView(null, "TextView", activity, attrs)

		assertTrue(view is TextView)
		assertEquals(Typeface.MONOSPACE, (view as TextView).typeface)
	}

	/** OpenDyslexic's baked-in spacing is clawed back: negative letter spacing
	 *  and a sub-1 line-spacing multiplier so text fits its containers. */
	@Test fun openDyslexicTightensSpacing() {
		this.setFont("opendyslexic")
		val style = FontPreference.fontStyle(this.context)

		assertNotNull(style)
		assertTrue("letter spacing should be negative", style!!.letterSpacing < 0f)
		assertTrue("line spacing multiplier should be < 1", style.lineSpacingMultiplier < 1f)

		val tv = TextView(this.context)
		style.applyTo(tv)
		assertEquals(style.letterSpacing, tv.letterSpacing, 0f)
		assertEquals(style.lineSpacingMultiplier, tv.lineSpacingMultiplier, 0f)
	}

	/** Other bundled fonts get neutral (identity) spacing, so they're unchanged. */
	@Test fun otherFontsKeepNeutralSpacing() {
		this.setFont("ubuntu")
		val style = FontPreference.fontStyle(this.context)

		assertNotNull(style)
		assertEquals(0f, style!!.letterSpacing, 0f)
		assertEquals(1f, style.lineSpacingMultiplier, 0f)
	}

	/** applyTo is a harmless no-op when the system font is selected. */
	@Test fun applyToIsNoOpForSystem() {
		this.setFont("system")
		val activity = Robolectric.buildActivity(FontTestActivity::class.java).create().get()

		FontPreference.applyTo(activity) // must not throw
	}

	/** Each picker entry is drawn in the font it names, so the user previews it. */
	@Test fun styledEntriesApplyEachOptionsOwnFont() {
		val entries = this.context.resources.getTextArray(R.array.font_entries)
		val values = this.context.resources.getTextArray(R.array.font_values)

		val styled = FontPreference.styledEntries(this.context, entries, values)

		assertEquals(entries.size, styled.size)
		for (i in entries.indices) {
			// The label itself must survive the styling.
			assertEquals(entries[i].toString(), styled[i].toString())

			val spans = (styled[i] as? Spanned)
				?.getSpans(0, styled[i].length, TypefaceSpan::class.java)
				?: emptyArray()

			if (values[i] == FontPreference.SYSTEM) {
				assertTrue("System must stay unstyled", spans.isEmpty())
			} else {
				assertEquals("${values[i]} must be styled", 1, spans.size)
				assertEquals(
					FontPreference.typefaceFor(this.context, values[i].toString()),
					spans[0].typeface,
				)
			}
		}
	}

	/** Every bundled option resolves to a real font, so no option silently
	 *  falls back to the device font. (Robolectric does not differentiate the
	 *  loaded typefaces, so their distinctness cannot be asserted here.) */
	@Test fun everyBundledOptionResolvesToAFont() {
		val values = this.context.resources.getTextArray(R.array.font_values)
			.filter { it != FontPreference.SYSTEM }

		assertTrue("no bundled fonts to check", values.isNotEmpty())
		values.forEach {
			assertNotNull("$it did not resolve", FontPreference.typefaceFor(this.context, it.toString()))
		}
	}

	/** The Dialog overload sweeps the decor view, reaching nested TextViews
	 *  (e.g. the title) that the inflater factory never touches. */
	@Test fun applyToDialogAppliesFontToNestedTextViews() {
		this.setFont("ubuntu")
		val activity = Robolectric.buildActivity(FontTestActivity::class.java).create().get()

		val dialog = Dialog(activity)
		val tvLabel = TextView(activity)
		dialog.setContentView(LinearLayout(activity).apply { this.addView(tvLabel) })

		FontPreference.applyTo(dialog)

		assertEquals(FontPreference.typeface(activity), tvLabel.typeface)
	}

	/** The Dialog overload leaves text untouched when the system font is selected. */
	@Test fun applyToDialogIsNoOpForSystem() {
		this.setFont("system")
		val activity = Robolectric.buildActivity(FontTestActivity::class.java).create().get()

		val dialog = Dialog(activity)
		val tvLabel = TextView(activity)
		val before = tvLabel.typeface
		dialog.setContentView(tvLabel)

		FontPreference.applyTo(dialog)

		assertEquals(before, tvLabel.typeface)
	}
}

/** Minimal AppCompat host so we can obtain a real delegate in tests. */
class FontTestActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		this.setTheme(R.style.AppTheme)
		super.onCreate(savedInstanceState)
	}
}
