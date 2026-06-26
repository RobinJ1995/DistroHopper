package be.robinj.distrohopper.theme

import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Resolves the active [Theme] from preferences. Lives on the
 * DependencyContainer; [current] replaces the old HomeActivity.theme static.
 * Theme changes still recreate HomeActivity — [theme] exists for code that
 * wants to react without recreation.
 */
class ThemeManager(private val prefs: PreferencesRepository) {
	private var cached: Theme? = null
	private var cachedName: String? = null

	val theme: Flow<Theme> = this.prefs
		.valueFlow(Preference.THEME) { it.getString(Preference.THEME.getName(), ThemeRegistry.DEFAULT) }
		.map { ThemeRegistry.create(it) }

	val current: Theme
		@Synchronized get() {
			val name = this.prefs.getString(Preference.THEME, ThemeRegistry.DEFAULT)

			if (this.cached == null || name != this.cachedName) {
				this.cached = ThemeRegistry.create(name)
				this.cachedName = name
			}

			return this.cached!!
		}
}
