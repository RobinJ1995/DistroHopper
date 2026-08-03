package be.robinj.distrohopper.home

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.DispatcherProvider
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.desktop.dash.lens.CollectionGridAdapter
import be.robinj.distrohopper.desktop.dash.lens.Lens
import be.robinj.distrohopper.desktop.dash.lens.LensResultEmitter
import be.robinj.distrohopper.desktop.dash.lens.LensSearchResult
import be.robinj.distrohopper.desktop.dash.lens.LensSearchResultCollection
import be.robinj.distrohopper.desktop.dash.lens.LensType
import be.robinj.distrohopper.thirdparty.ProgressWheel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper
import java.time.Duration

/**
 * SearchLoader replaces the old AsyncSearch. Under launchHome's test
 * dispatchers the IO dispatcher is Unconfined, so synchronous lenses run inline
 * during start(); coroutine delays (the 240 ms wheel reveal, the 150 ms
 * debounce) are posted to the main looper and advanced with idleFor().
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class SearchLoaderTest {
    private lateinit var scenario: ActivityScenario<HomeActivity>

    private val testDispatchers = object : DispatcherProvider {
        override val main = Dispatchers.Main
        override val io = Dispatchers.Unconfined
        override val default = Dispatchers.Unconfined
    }

    @Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
    @After fun tearDown() { scenario.close() }

    private fun idle(ms: Long) =
        ShadowLooper.shadowMainLooper().idleFor(Duration.ofMillis(ms))

    private fun start(activity: HomeActivity, lenses: List<Lens>): Fixture {
        val results = mutableListOf<LensSearchResultCollection>()
        val adapter = CollectionGridAdapter(activity, results)
        val wheel = ProgressWheel(activity, Robolectric.buildAttributeSet().build())
        wheel.visibility = View.GONE
        val loader = SearchLoader(activity, testDispatchers)
        loader.start("query", lenses, 10, adapter, results, wheel)
        return Fixture(loader, results, wheel)
    }

    private class Fixture(
        val loader: SearchLoader,
        val results: MutableList<LensSearchResultCollection>,
        val wheel: ProgressWheel,
    )

    @Test fun localLensResultsAppearWithoutWaitingForTheNetworkLens() {
        lateinit var fixture: Fixture
        lateinit var network: SuspendingNetworkLens
        scenario.onActivity { activity ->
            network = SuspendingNetworkLens(activity)
            fixture = start(activity, listOf(LocalLens(activity), network))
        }

        // No idling: the LOCAL lens ran inline during start(); the NETWORK lens
        // is still parked behind the debounce delay.
        assertEquals(1, fixture.results.size)
        assertEquals("Local", fixture.results[0].name)
        assertFalse("network lens must not run before the debounce", network.started)
    }

    @Test fun networkLensRunsOnlyAfterTheDebounce() {
        lateinit var network: FlagNetworkLens
        scenario.onActivity { activity ->
            network = FlagNetworkLens(activity)
            start(activity, listOf(network))
        }

        assertFalse(network.searched)
        idle(160)
        assertTrue("network lens must run once the debounce elapses", network.searched)
    }

    @Test fun localOnlySearchHidesTheWheelOnCompletion() {
        lateinit var fixture: Fixture
        scenario.onActivity { activity -> fixture = start(activity, listOf(LocalLens(activity))) }

        // Local-only completes inline, no debounced phase, wheel never flashed.
        assertEquals(View.GONE, fixture.wheel.visibility)
    }

    @Test fun cancellingKeepsAnAlreadyVisibleWheel() {
        lateinit var fixture: Fixture
        scenario.onActivity { activity ->
            fixture = start(activity, listOf(SuspendingNetworkLens(activity)))
            fixture.wheel.visibility = View.VISIBLE // the wheel already appeared
            fixture.loader.cancel()
        }
        assertEquals(View.VISIBLE, fixture.wheel.visibility)
    }

    @Test fun wheelAppearsAfterTheDelayWhileTheSearchIsStillRunning() {
        lateinit var fixture: Fixture
        lateinit var network: SuspendingNetworkLens
        scenario.onActivity { activity ->
            network = SuspendingNetworkLens(activity)
            fixture = start(activity, listOf(network))
        }

        idle(160) // past the debounce: the network lens started and is hanging
        assertTrue(network.started)
        assertEquals("wheel must not show before the 240ms delay", View.GONE, fixture.wheel.visibility)

        idle(120) // past the 240ms reveal while the search is still running
        assertEquals(View.VISIBLE, fixture.wheel.visibility)
    }

    @Test fun wheelDoesNotAppearAfterCancellation() {
        lateinit var fixture: Fixture
        scenario.onActivity { activity ->
            fixture = start(activity, listOf(SuspendingNetworkLens(activity)))
            fixture.loader.cancel()
        }

        idle(400)
        assertNotEquals(
            "the cancelled reveal must not make the wheel visible",
            View.VISIBLE, fixture.wheel.visibility)
    }

    /** Instant, in-memory LOCAL lens emitting a single result. */
    private class LocalLens(context: Context) : Lens(context) {
        override val key = "LocalLens"
        override fun getName() = "Local"
        override fun getDescription() = "local"
        override val type = LensType.LOCAL
        override suspend fun search(query: String, maxResults: Int, emitter: LensResultEmitter) {
            emitter.emit(LensSearchResult(context, "app", "app://x", ColorDrawable(0)))
        }
    }

    /** NETWORK lens that records when it was searched. */
    private class FlagNetworkLens(context: Context) : Lens(context) {
        override val key = "FlagNetworkLens"
        var searched = false
        override fun getName() = "Flag"
        override fun getDescription() = "flag"
        override val type = LensType.NETWORK
        override suspend fun search(query: String, maxResults: Int, emitter: LensResultEmitter) {
            searched = true
            emitter.emit(LensSearchResult(context, "n", "http://n", ColorDrawable(0)))
        }
    }

    /** NETWORK lens that starts then hangs, keeping the search running. */
    private class SuspendingNetworkLens(context: Context) : Lens(context) {
        override val key = "SuspendingNetworkLens"
        @Volatile var started = false
        override fun getName() = "Net"
        override fun getDescription() = "net"
        override val type = LensType.NETWORK
        override suspend fun search(query: String, maxResults: Int, emitter: LensResultEmitter) {
            started = true
            awaitCancellation()
        }
    }
}
