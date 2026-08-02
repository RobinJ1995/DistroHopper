package be.robinj.distrohopper

import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spanned
import android.text.style.TypefaceSpan
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.LayoutInflaterCompat
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

	/** OpenDyslexic's baked-in spacing is clawed back: a negative letter-spacing
	 *  delta and a sub-1 line-spacing factor so text fits its containers. */
	@Test fun openDyslexicTightensSpacing() {
		this.setFont("opendyslexic")
		val style = FontPreference.fontStyle(this.context)

		assertNotNull(style)
		assertTrue("letter spacing delta should be negative", style!!.letterSpacingDelta < 0f)
		assertTrue("line spacing factor should be < 1", style.lineSpacingFactor < 1f)

		val tv = TextView(this.context).apply {
			this.letterSpacing = 0.04f // explicit tracking, e.g. from a style
			this.setLineSpacing(0f, 1f)
		}
		style.applyTo(tv)
		// Correction is relative: existing 0.04 tracking is tightened, not wiped.
		assertEquals(0.04f + style.letterSpacingDelta, tv.letterSpacing, 1e-6f)
		assertEquals(style.lineSpacingFactor, tv.lineSpacingMultiplier, 1e-6f)
	}

	/** Other bundled fonts get an identity transform: existing letter spacing set
	 *  by a style/layout is preserved, not clobbered to 0. */
	@Test fun otherFontsPreserveExistingSpacing() {
		this.setFont("ubuntu")
		val style = FontPreference.fontStyle(this.context)

		assertNotNull(style)
		assertEquals(0f, style!!.letterSpacingDelta, 0f)
		assertEquals(1f, style.lineSpacingFactor, 0f)

		val tv = TextView(this.context).apply {
			this.letterSpacing = 0.04f
			this.setLineSpacing(8f, 1.1f)
		}
		style.applyTo(tv)
		assertEquals("neutral font must not touch letter spacing", 0.04f, tv.letterSpacing, 0f)
		assertEquals("neutral font must not touch line spacing", 8f, tv.lineSpacingExtra, 0f)
		assertEquals("neutral font must not touch line spacing", 1.1f, tv.lineSpacingMultiplier, 0f)
	}

	/** A view can be reached by both the inflater factory and the dialog decor
	 *  sweep (and swept again on every show), so corrections must be computed
	 *  from the view's designed spacing and never compound. */
	@Test fun spacingCorrectionIsIdempotent() {
		this.setFont("opendyslexic")
		val style = FontPreference.fontStyle(this.context)!!

		val tv = TextView(this.context).apply {
			this.letterSpacing = 0.04f
			this.setLineSpacing(0f, 1f)
		}

		style.applyTo(tv)
		val letterSpacingAfterOnce = tv.letterSpacing
		val lineSpacingAfterOnce = tv.lineSpacingMultiplier

		style.applyTo(tv)
		style.applyTo(tv)

		assertEquals(letterSpacingAfterOnce, tv.letterSpacing, 1e-6f)
		assertEquals(lineSpacingAfterOnce, tv.lineSpacingMultiplier, 1e-6f)
		// Explicitly: the third application must not have drifted to 0.04 - 0.15.
		assertEquals(0.04f + style.letterSpacingDelta, tv.letterSpacing, 1e-6f)
	}

	/** The real compounding path: WidgetPickerDialog's adapter inflates rows with
	 *  an inflater that carries the font factory, and those same rows are then
	 *  caught by the dialog decor sweep. They must be corrected exactly once. */
	@Test fun factoryThenDialogSweepDoesNotCompound() {
		this.setFont("opendyslexic")
		val style = FontPreference.fontStyle(this.context)!!
		val activity = Robolectric.buildActivity(FontTestActivity::class.java).create().get()

		val inflater = LayoutInflater.from(activity).cloneInContext(activity)
		LayoutInflaterCompat.setFactory2(inflater, FontInflaterFactory(activity.delegate, style))
		val row = inflater.inflate(R.layout.widget_picker_header, null)
		val tvName = row.findViewById<TextView>(R.id.tvName)

		// The layout declares letterSpacing="0.04"; tighten it exactly once.
		val expected = 0.04f + style.letterSpacingDelta
		assertEquals(expected, tvName.letterSpacing, 1e-6f)

		// Now the dialog decor sweep reaches the very same row.
		val dialog = Dialog(activity)
		dialog.setContentView(row)
		FontPreference.applyTo(dialog)

		assertEquals(expected, tvName.letterSpacing, 1e-6f)
	}

	/** Line spacing is scaled from the view's own multiplier, and its extra
	 *  leading (set in dp by several layouts) is carried through untouched. */
	@Test fun openDyslexicScalesLineSpacingFromTheViewsOwnValues() {
		this.setFont("opendyslexic")
		val style = FontPreference.fontStyle(this.context)!!

		val tv = TextView(this.context).apply { this.setLineSpacing(4f, 1.1f) }
		style.applyTo(tv)

		assertEquals(1.1f * style.lineSpacingFactor, tv.lineSpacingMultiplier, 1e-6f)
		assertEquals("extra leading must be preserved", 4f, tv.lineSpacingExtra, 1e-6f)
	}

	/** Swapping the family must keep each view's own bold/italic styling. Uses a
	 *  built-in family so this pins our style pass-through rather than how
	 *  faithfully the test runtime derives weights from a bundled OTF. */
	@Test fun applyToPreservesBoldStyle() {
		val style = FontStyle(Typeface.MONOSPACE, letterSpacingDelta = -0.05f)

		val tv = TextView(this.context).apply { this.setTypeface(null, Typeface.BOLD) }
		style.applyTo(tv)

		assertEquals("bold must survive the family swap", Typeface.BOLD, tv.typeface.style)
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
