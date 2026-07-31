package be.robinj.distrohopper.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.desktop.launcher.LauncherIconGrid
import be.robinj.distrohopper.preferences.Preference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds HomeActivity's UI state, surviving recreate(). The state flows are
 * the source of record; view mutation happens in the home controllers, which
 * HomeStateBinder connects to these flows.
 */
class HomeViewModel(
	container: DependencyContainer,
) : ViewModel() {
	private val _dashOpen = MutableStateFlow(false)
	val dashOpen: StateFlow<Boolean> = this._dashOpen.asStateFlow()

	/*
	 * Preferences that apply live, without recreating the activity. Theme and
	 * edge changes still recreate: they do wholesale view-tree surgery.
	 */
	// Emits whenever the pinned-icon size preset changes; the actual pixel size is recomputed
	// from the screen and theme by LauncherIconGrid at apply time, so the emitted value is only
	// a change trigger.
	val launcherIconPreset: Flow<Int> = container.prefs
		.valueFlow(Preference.LAUNCHER_ICON_PRESET) {
			it.getInt(Preference.LAUNCHER_ICON_PRESET.getName(), LauncherIconGrid.DEFAULT_PRESET)
		}
	// Emits whenever the dash grid column preference changes; the actual count
	// is recomputed from the screen by DashGrid at apply time, so the emitted
	// value (-1 when unset) is only a change trigger.
	val dashGridColumns: Flow<Int> = container.prefs
		.valueFlow(Preference.DASH_GRID_COLUMNS) {
			it.getInt(Preference.DASH_GRID_COLUMNS.getName(), -1)
		}
	val panelOpacity: Flow<Int> = container.prefs
		.valueFlow(Preference.PANEL_OPACITY) {
			it.getInt(Preference.PANEL_OPACITY.getName(), 100)
		}
	val showRunningApps: Flow<Boolean> = container.prefs
		.valueFlow(Preference.LAUNCHER_SHOW_RUNNING_APPS) {
			it.getBoolean(Preference.LAUNCHER_SHOW_RUNNING_APPS.getName(), false)
		}

	fun openDash() {
		this._dashOpen.value = true
	}

	fun closeDash() {
		this._dashOpen.value = false
	}

	class Factory(private val container: DependencyContainer) : ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T =
			HomeViewModel(this.container) as T
	}
}
