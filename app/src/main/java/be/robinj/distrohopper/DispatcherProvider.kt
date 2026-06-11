package be.robinj.distrohopper

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Indirection over the coroutine dispatchers so tests can substitute
 * deterministic ones (see ActivityTestSupport).
 */
interface DispatcherProvider {
	val main: CoroutineDispatcher
	val io: CoroutineDispatcher
	val default: CoroutineDispatcher
}

class DefaultDispatcherProvider : DispatcherProvider {
	override val main: CoroutineDispatcher = Dispatchers.Main
	override val io: CoroutineDispatcher = Dispatchers.IO
	override val default: CoroutineDispatcher = Dispatchers.Default
}
