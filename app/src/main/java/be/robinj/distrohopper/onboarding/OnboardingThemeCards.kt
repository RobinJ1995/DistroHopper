package be.robinj.distrohopper.onboarding

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import be.robinj.distrohopper.R
import be.robinj.distrohopper.theme.Theme

/**
 * Populates the wizard's theme page with one selectable card per theme.
 * Selection is persisted immediately through [onSelected]; [selectedName]
 * supplies the persisted choice so rebinds restore the highlight.
 */
class OnboardingThemeCards(
	private val themes: List<Theme>,
	private val selectedName: () -> String,
	private val onSelected: (Theme) -> Unit,
) {
	fun bind(container: LinearLayout) {
		container.removeAllViews()
		val inflater = LayoutInflater.from(container.context)

		for (theme in this.themes) {
			val card = inflater.inflate(R.layout.widget_onboarding_theme_card, container, false)
			card.findViewById<ImageView>(R.id.ivOnboardingThemeLogo)
				.setImageResource(theme.launcher_bfb_image)
			card.findViewById<TextView>(R.id.tvOnboardingThemeName).text = theme.name
			card.findViewById<TextView>(R.id.tvOnboardingThemeDescription).text = theme.description
			card.tag = theme

			this.applySelection(card, theme, theme.getName() == this.selectedName())

			card.setOnClickListener {
				for (i in 0 until container.childCount) {
					val sibling = container.getChildAt(i)
					this.applySelection(sibling, sibling.tag as Theme, sibling === card)
				}

				this.onSelected(theme)
			}

			container.addView(card)
		}
	}

	private fun applySelection(card: View, theme: Theme, selected: Boolean) {
		card.isSelected = selected

		val context = card.context
		val background = card.background.mutate() as GradientDrawable
		val strokeWidth = (2f * context.resources.displayMetrics.density).toInt()

		if (selected) {
			background.setStroke(strokeWidth, context.getColor(theme.brand_colour))
			background.setColor(context.getColor(R.color.transparent80))
		} else {
			background.setStroke(strokeWidth, context.getColor(R.color.transparent))
			background.setColor(context.getColor(R.color.transparentblack60))
		}
	}
}
