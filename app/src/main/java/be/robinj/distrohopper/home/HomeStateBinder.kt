package be.robinj.distrohopper.home

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

/**
 * Connects HomeViewModel's state flows to the home controllers. Lives apart
 * from the ViewModel so the ViewModel never touches views, and apart from
 * HomeActivity because collecting flows from Java is unwieldy.
 *
 * Note: HomeActivity also calls the controllers directly in its event
 * handlers (the controllers are idempotent), keeping UI reactions
 * synchronous; this collection is the state-of-record path that restores
 * the UI after recreate().
 */
object HomeStateBinder {
	@JvmStatic
	fun bind(activity: AppCompatActivity, viewModel: HomeViewModel, dash: DashController) {
		activity.lifecycleScope.launch {
			activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.dashOpen.collect { open ->
					if (open) dash.open() else dash.close()
				}
			}
		}
	}
}
