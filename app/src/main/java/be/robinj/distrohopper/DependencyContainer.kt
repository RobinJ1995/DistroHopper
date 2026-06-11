package be.robinj.distrohopper

import android.content.Context
import androidx.annotation.VisibleForTesting
import be.robinj.distrohopper.preferences.PreferencesRepository
import be.robinj.distrohopper.theme.ThemeManager

/**
 * Hand-wired dependency container for objects shared across the app.
 * Deliberately a plain class — this project does not use DI frameworks;
 * everything is constructed explicitly, here or in constructors.
 *
 * The instance is owned by [Application] (so Robolectric tests get a fresh
 * one per test); [of] is the lookup used by code that only has a [Context].
 */
class DependencyContainer(context: Context) {
	val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
	val prefs: PreferencesRepository = PreferencesRepository(context)
	val themeManager: ThemeManager = ThemeManager(this.prefs)

	companion object {
		@JvmStatic
		@VisibleForTesting
		var override: DependencyContainer? = null

		@JvmStatic
		fun of(context: Context): DependencyContainer =
			override
				?: (context.applicationContext as Application).dependencyContainer
	}
}
