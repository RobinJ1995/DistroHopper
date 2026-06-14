package be.robinj.distrohopper.theme

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.R
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.widget.bfb.BfbWidgetProvider

/**
 * Populates a container with one selectable card per theme; used by the
 * first-run wizard's theme page and the theme preferences screen.
 * Selection is persisted immediately through [onSelected]; [selectedName]
 * supplies the persisted choice so rebinds restore the highlight.
 */
class ThemeCards(
	private val themes: List<Theme>,
	private val selectedName: () -> String,
	private val onSelected: (Theme) -> Unit,
) {
	fun bind(container: LinearLayout) {
		container.removeAllViews()
		val inflater = LayoutInflater.from(container.context)

		for (theme in this.themes) {
			val card = inflater.inflate(R.layout.widget_theme_card, container, false)
			card.findViewById<ImageView>(R.id.ivThemeCardLogo)
				.setImageResource(theme.card_logo)
			card.findViewById<TextView>(R.id.tvThemeCardName).text = theme.name
			card.findViewById<TextView>(R.id.tvThemeCardDescription).text = theme.description
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
			background.setStroke(strokeWidth, context.getColor(theme.card_colour))
			background.setColor(context.getColor(R.color.transparent80))
		} else {
			background.setStroke(strokeWidth, context.getColor(R.color.transparent))
			background.setColor(context.getColor(R.color.transparentblack60))
		}
	}

	companion object {
		/** Same three preferences the theme picker has always written. */
		@JvmStatic
		fun applyTheme(context: Context, theme: Theme) {
			val res = context.resources

			DependencyContainer.of(context).prefs.edit {
				this.putString(Preference.THEME.getName(), theme.getName())
				this.putInt(Preference.LAUNCHER_EDGE.getName(), res.getInteger(theme.launcher_location))
				this.putInt(Preference.PANEL_EDGE.getName(), res.getInteger(theme.panel_location))
				// Reset the menu-button (BFB) position to the new theme's default so
				// a choice carried over from the previous theme can't leave the
				// customise dropdown out of sync with what's actually shown.
				this.remove(Preference.LAUNCHER_BFB_LOCATION.getName())
			}

			// Repaint any placed BFB widget in the newly-selected theme.
			BfbWidgetProvider.requestUpdate(context)
		}
	}
}
