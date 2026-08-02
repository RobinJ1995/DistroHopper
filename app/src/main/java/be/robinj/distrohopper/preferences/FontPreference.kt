package be.robinj.distrohopper.preferences

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.TypefaceSpan
import android.util.TypedValue
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

	/**
	 * OpenDyslexic ships very wide glyph advances and tall line metrics "by
	 * design". In a UI whose containers are sized for normal fonts that means
	 * text no longer fits, so we claw it back to something usable.
	 *
	 * [FontStyle.textSizeFactor] does the heavy lifting: OpenDyslexic's glyphs are
	 * optically larger than the system font at the same nominal sp, so labels
	 * overflow (or wrap) no matter how tight the tracking — the glyphs themselves,
	 * not the gaps, are what does not fit. Scaling the size down brings the run
	 * width back into the range the layouts were designed for. The negative
	 * [FontStyle.letterSpacingDelta] and sub-1 [FontStyle.lineSpacingFactor] then
	 * recover the font's extra tracking and leading on top of that.
	 *
	 * All three are applied *relative* to each view's own metrics (see
	 * [FontStyle.applyTo]), so intentional sizing and tracking set in
	 * layouts/styles is scaled, not discarded. These only touch OpenDyslexic;
	 * every other font keeps the neutral defaults below, which are exact no-ops.
	 */
	private const val OPENDYSLEXIC_LETTER_SPACING_DELTA = -0.05f
	private const val OPENDYSLEXIC_LINE_SPACING_FACTOR = 0.8f
	private const val OPENDYSLEXIC_TEXT_SIZE_FACTOR = 0.85f

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

	/**
	 * The picker's [entries], each span-styled in the font it names, so the user
	 * previews a font while choosing it ("Ubuntu" drawn in Ubuntu, "Oxygen" in
	 * Oxygen, …). [values] are the matching entry values; the System entry (and
	 * any value without a bundled font) is returned unchanged, keeping the device
	 * font.
	 *
	 * A span is used rather than a custom dialog because it travels with the text:
	 * the stock list-preference dialog renders it, and so does the summary on the
	 * settings screen. Spans also win over the view-wide typeface that
	 * [FontInflaterFactory] applies, so the preview survives the app-wide font.
	 */
	fun styledEntries(
		context: Context,
		entries: Array<out CharSequence>,
		values: Array<out CharSequence>,
	): Array<CharSequence> = Array(entries.size) { i ->
		val typeface = if (i < values.size) {
			this.typefaceFor(context, values[i].toString())
		} else {
			null
		}

		if (typeface == null) {
			entries[i]
		} else {
			SpannableString(entries[i]).apply {
				this.setSpan(
					TypefaceSpan(typeface),
					0,
					this.length,
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
				)
			}
		}
	}

	/**
	 * The selected font together with any per-font metric tweaks, or null when
	 * the system font should be used. This is what actually gets forced onto
	 * TextViews (see [FontStyle.applyTo]); prefer it over [typeface] so the
	 * spacing corrections travel with the typeface.
	 */
	fun fontStyle(context: Context): FontStyle? {
		val value = this.current(context)
		val typeface = this.typeface(context) ?: return null
		return if (value == "opendyslexic") {
			FontStyle(
				typeface,
				OPENDYSLEXIC_LETTER_SPACING_DELTA,
				OPENDYSLEXIC_LINE_SPACING_FACTOR,
				OPENDYSLEXIC_TEXT_SIZE_FACTOR,
			)
		} else {
			FontStyle(typeface)
		}
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
		val fontStyle = this.fontStyle(activity) ?: return

		// We run before AppCompat installs its own factory, so the inflater
		// should be untouched; guard like AppCompat does to never clobber an
		// existing factory (and to avoid setFactory2's "already set" crash).
		val inflater = activity.layoutInflater
		if (inflater.factory2 != null) return

		LayoutInflaterCompat.setFactory2(
			inflater,
			FontInflaterFactory(activity.delegate, fontStyle),
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
		val root = dialog.window?.decorView ?: return

		this.applyTo(root)
	}

	/**
	 * Applies the chosen font to [view] and every TextView beneath it — for text
	 * the [FontInflaterFactory] never sees, because it is not inflated at all:
	 * views built in code (see `IconStripPreference`) and the framework's own
	 * chrome (an ActionBar creates its title TextView internally rather than from
	 * a layout, so it escapes the factory the way dialog chrome does).
	 *
	 * Safe to call on a tree that is already styled: every correction is anchored
	 * to the view's captured baseline, so re-applying lands on the same metrics
	 * instead of compounding. No-op for System.
	 */
	fun applyTo(view: View) {
		val fontStyle = this.fontStyle(view.context) ?: return

		this.applyFontStyle(view, fontStyle)
	}

	private fun applyFontStyle(view: View, fontStyle: FontStyle) {
		when (view) {
			is ViewGroup -> for (i in 0 until view.childCount) {
				this.applyFontStyle(view.getChildAt(i), fontStyle)
			}
			is TextView -> fontStyle.applyTo(view)
		}
	}
}

/**
 * A chosen [typeface] plus the per-font metric corrections that must travel with
 * it, expressed *relative* to each view's designed text metrics:
 * [textSizeFactor] (scales the text size — < 1 to shrink), [letterSpacingDelta]
 * (em units, added — negative to tighten) and [lineSpacingFactor] (multiplies the
 * line-spacing multiplier — < 1 to pull lines closer). Applying relatively means
 * a view's intentional sizing and tracking (e.g. a style's `letterSpacing="0.04"`
 * or a 20sp heading) is preserved and shifted, not wiped.
 *
 * [textSizeFactor] is the lever that actually makes text fit: a font whose glyphs
 * are optically larger than the system font at the same nominal sp overflows
 * containers no amount of tracking can rescue, because the glyphs themselves —
 * not the gaps — are too wide. Tracking and leading then fine-tune the result.
 *
 * "Designed metrics" are the view's values the first time it is seen, captured
 * and remembered in a tag. Corrections are always recomputed from that baseline,
 * never from the current value, so [applyTo] is idempotent: a view reached by
 * both the [FontInflaterFactory] and the dialog decor sweep — or swept again on
 * every [Dialog] show — lands on the same metrics instead of compounding.
 *
 * The defaults (all identity) touch nothing: for fonts that need no correction,
 * [applyTo] swaps only the typeface and leaves every metric untouched.
 */
class FontStyle(
	val typeface: Typeface,
	val letterSpacingDelta: Float = 0f,
	val lineSpacingFactor: Float = 1f,
	val textSizeFactor: Float = 1f,
) {

	/** True when this style leaves text metrics exactly as the view declared them. */
	private val metricsAreNeutral: Boolean
		get() = this.letterSpacingDelta == 0f &&
			this.lineSpacingFactor == 1f &&
			this.textSizeFactor == 1f

	/** Forces this font (and its metrics) onto [view], keeping its bold/italic. */
	fun applyTo(view: TextView) {
		// Keep each view's own style (bold/italic) while swapping the family.
		view.setTypeface(this.typeface, view.typeface?.style ?: Typeface.NORMAL)

		// Never touch metrics when there is no correction to make, so fonts that
		// need none leave every view exactly as its layout/style declared it.
		if (this.metricsAreNeutral) return

		val baseline = this.baselineOf(view)
		// The baseline is in px, so set px back: the SP unit the setter defaults
		// to would re-apply font scaling that getTextSize() has already resolved.
		view.setTextSize(TypedValue.COMPLEX_UNIT_PX, baseline.textSize * this.textSizeFactor)
		view.letterSpacing = baseline.letterSpacing + this.letterSpacingDelta
		view.setLineSpacing(
			baseline.lineSpacingExtra,
			baseline.lineSpacingMultiplier * this.lineSpacingFactor,
		)
	}

	/** The view's designed metrics, captured on first sight and reused after. */
	private fun baselineOf(view: TextView): MetricsBaseline =
		view.getTag(R.id.font_spacing_baseline) as? MetricsBaseline
			?: MetricsBaseline(
				view.textSize,
				view.letterSpacing,
				view.lineSpacingExtra,
				view.lineSpacingMultiplier,
			).also { view.setTag(R.id.font_spacing_baseline, it) }

	/** A view's text metrics as its layout/style declared them, before correction. */
	private class MetricsBaseline(
		val textSize: Float,
		val letterSpacing: Float,
		val lineSpacingExtra: Float,
		val lineSpacingMultiplier: Float,
	)
}
