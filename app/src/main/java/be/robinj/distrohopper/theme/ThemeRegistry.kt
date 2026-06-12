package be.robinj.distrohopper.theme

/**
 * The one place that knows which themes exist. Replaces the reflective
 * Class.forName()-style instantiation that used to live in HomeActivity.
 * Iteration order is the order themes are listed in the theme picker.
 */
object ThemeRegistry {
	const val DEFAULT = "default"

	val themes: Map<String, () -> Theme> = linkedMapOf(
		DEFAULT to ::Default,
		"gnome" to ::Gnome,
		"elementary" to ::Elementary,
		"cinnamon" to ::Cinnamon,
		"plasma" to ::Plasma,
		"mate" to ::Mate,
		"cosmic" to ::Cosmic,
	)

	/** Falls back to the default theme for unknown or null names. */
	@JvmStatic
	fun create(name: String?): Theme =
		(this.themes[name] ?: this.themes.getValue(DEFAULT)).invoke()
}
