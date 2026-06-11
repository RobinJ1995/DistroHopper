package be.robinj.distrohopper

import android.content.Context
import androidx.annotation.VisibleForTesting
import be.robinj.distrohopper.preferences.PreferencesRepository
import be.robinj.distrohopper.theme.ThemeManager
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Hand-wired dependency container for objects shared across the app.
 * Deliberately a plain class — this project does not use DI frameworks;
 * everything is constructed explicitly, here or in constructors.
 *
 * The instance is owned by [Application] (so Robolectric tests get a fresh
 * one per test); [of] is the lookup used by code that only has a [Context].
 */
class DependencyContainer(context: Context) {
	@set:VisibleForTesting // Tests substitute deterministic dispatchers //
	var dispatchers: DispatcherProvider = DefaultDispatcherProvider()
	val prefs: PreferencesRepository = PreferencesRepository(context)
	val themeManager: ThemeManager = ThemeManager(this.prefs)

	/**
	 * Whether the home screen is in customise mode (icon size sliders, edge
	 * spinners). App-wide rather than on HomeViewModel because App.launch()
	 * checks it with only a Context at hand.
	 */
	val customiseMode: MutableStateFlow<Boolean> = MutableStateFlow(false)

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
