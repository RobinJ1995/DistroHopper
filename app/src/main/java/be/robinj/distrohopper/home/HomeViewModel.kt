package be.robinj.distrohopper.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import be.robinj.distrohopper.DependencyContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds HomeActivity's UI state, surviving recreate(). The state flows are
 * the source of record; view mutation happens in the home controllers, which
 * HomeStateBinder connects to these flows.
 */
class HomeViewModel(
	@Suppress("unused") // Used from Phase 5 onwards (app loading on dispatchers) //
	private val container: DependencyContainer,
) : ViewModel() {
	private val _dashOpen = MutableStateFlow(false)
	val dashOpen: StateFlow<Boolean> = this._dashOpen.asStateFlow()

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
