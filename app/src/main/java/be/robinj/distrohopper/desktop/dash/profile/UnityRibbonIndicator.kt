package be.robinj.distrohopper.desktop.dash.profile

import android.content.Context
import android.os.UserHandle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import be.robinj.distrohopper.Profiles
import be.robinj.distrohopper.R

/**
 * Unity-style profile indicator: one monochrome glyph per profile in the
 * dash's always-visible bottom ribbon. Other profiles use the generic profile
 * glyph stamped with the system's own profile badge (so a work profile shows
 * the briefcase, a private space its lock, etc., correct for any profile
 * type), while the personal profile uses the theme's personal glyph
 * ([personalGlyphRes]; the house for Unity).
 *
 * The current profile is marked with the ribbon's existing "selected" look —
 * the same `transparent90` backing the dash's home button has — tracking the
 * swipe; tapping a glyph switches. The ribbon's own decorative home button is
 * hidden while these tabs are shown (the personal glyph replaces it).
 */
class UnityRibbonIndicator(
	private val context: Context,
	private val container: LinearLayout,
	private val personalGlyphRes: Int,
	private val onSelect: (Int) -> Unit,
) : ProfileIndicator {
	private val icons = mutableListOf<ImageView>()
	private var position = 0F

	private val selectedColour = ContextCompat.getColor(this.context, R.color.transparent90)

	/** The ribbon's standalone home button, hidden while the profile tabs show. */
	private val homeButton: View?
		get() = (this.container.parent as? ViewGroup)?.findViewById(R.id.ibDashLensHome)

	override fun bind(profiles: List<UserHandle?>, selected: Int) {
		val density = this.context.resources.displayMetrics.density
		val size = (36 * density).toInt()
		val margin = (1 * density).toInt()
		val padding = (5 * density).toInt()

		this.homeButton?.visibility = View.GONE // The personal glyph replaces it //
		this.container.removeAllViews()
		this.icons.clear()

		for ((i, profile) in profiles.withIndex()) {
			val icon = ImageView(this.context)
			icon.setImageDrawable(this.glyphFor(profile))
			icon.setPadding(padding, padding, padding, padding)
			icon.setBackgroundColor(this.selectedColour) // alpha is the highlight //
			val params = LinearLayout.LayoutParams(size, size)
			params.leftMargin = margin
			params.rightMargin = margin
			icon.layoutParams = params
			icon.setOnClickListener { this.onSelect(i) }

			this.container.addView(icon)
			this.icons.add(icon)
		}

		this.container.visibility = View.VISIBLE
		this.position = selected.toFloat()
		this.applyHighlight()
	}

	private fun glyphFor(profile: UserHandle?) =
		if (profile == null) {
			ContextCompat.getDrawable(this.context, this.personalGlyphRes)
		} else {
			// The system badge is the whole glyph, so it stays correct for the
			// profile type (work briefcase, private-space lock, …) by itself;
			// desaturated so it sits beside the monochrome personal glyph.
			val px = (36 * this.context.resources.displayMetrics.density).toInt()
			Profiles.profileGlyph(this.context, profile, px, desaturate = true)
		}

	override fun onPageScrolled(position: Int, positionOffset: Float) {
		this.position = position + positionOffset
		this.applyHighlight()
	}

	override fun onPageSelected(position: Int) {
		this.position = position.toFloat()
		this.applyHighlight()
	}

	private fun applyHighlight() {
		for ((i, icon) in this.icons.withIndex()) {
			val highlight = 1F - Math.min(Math.abs(this.position - i), 1F)
			// Selected backing fades in (like the home button's), and the glyph
			// itself dims a little when inactive for extra contrast.
			icon.background?.alpha = (highlight * 255F).toInt()
			icon.alpha = INACTIVE_ALPHA + (1F - INACTIVE_ALPHA) * highlight
		}
	}

	override fun clear() {
		this.container.removeAllViews()
		this.container.visibility = View.GONE
		this.icons.clear()
		this.homeButton?.visibility = View.VISIBLE
	}

	companion object {
		private const val INACTIVE_ALPHA = 0.55F
	}
}
