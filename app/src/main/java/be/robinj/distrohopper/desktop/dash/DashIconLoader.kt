package be.robinj.distrohopper.desktop.dash

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import be.robinj.distrohopper.App
import be.robinj.distrohopper.DispatcherProvider
import be.robinj.distrohopper.ExceptionHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Loads the icons a grid asks for off the main thread, so binding a cell never
 * opens another app's APK there (an ANR on a screenful of uncached icons).
 * [onLoaded] runs on the main thread after each batch; the grid rebinds from it.
 *
 * Main-thread confined. Loads run one batch at a time, in request order.
 */
class DashIconLoader(
	private val owner: LifecycleOwner,
	private val dispatchers: DispatcherProvider,
	private val onLoaded: Runnable,
) {
	private val queue = ArrayDeque<App>()
	// Never cleared: an app whose load failed must not be retried on every bind //
	private val requested = HashSet<App>()
	private var job: Job? = null

	/** Queues [app]'s icon for loading; a no-op if it was already requested. */
	fun request(app: App) {
		if (!this.requested.add(app)) {
			return
		}

		this.queue.addLast(app)

		if (this.job?.isActive != true) {
			// Dispatched, never run inline: requests come from getView(), mid-layout,
			// where the rebind in onLoaded must not happen //
			this.job = this.owner.lifecycleScope.launch(this.dispatchers.main) {
				this@DashIconLoader.drain()
			}
		}
	}

	private suspend fun drain() {
		while (this.queue.isNotEmpty()) {
			val batch = List(minOf(BATCH_SIZE, this.queue.size)) { this.queue.removeFirst() }

			withContext(this.dispatchers.io) {
				for (app in batch) {
					try {
						app.icon
					} catch (ex: CancellationException) {
						throw ex
					} catch (ex: Exception) {
						ExceptionHandler(ex).logAndTrack()
					}
				}
			}

			this.onLoaded.run()
		}
	}

	companion object {
		/** Icons per rebind: small enough that a row fills in at a time. */
		private const val BATCH_SIZE = 4
	}
}
