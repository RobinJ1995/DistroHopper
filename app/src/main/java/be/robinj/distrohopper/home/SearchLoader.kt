package be.robinj.distrohopper.home

import android.view.View
import androidx.lifecycle.lifecycleScope
import be.robinj.distrohopper.DispatcherProvider
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.desktop.dash.lens.CollectionGridAdapter
import be.robinj.distrohopper.desktop.dash.lens.Lens
import be.robinj.distrohopper.desktop.dash.lens.LensResultEmitter
import be.robinj.distrohopper.desktop.dash.lens.LensSearchResult
import be.robinj.distrohopper.desktop.dash.lens.LensSearchResultCollection
import be.robinj.distrohopper.desktop.dash.lens.LensType
import be.robinj.distrohopper.thirdparty.ProgressWheel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Runs a dash search, replacing the old AsyncSearch (an AsyncTask). Like
 * StartupLoader it runs in the activity's lifecycleScope and uses the injected
 * DispatcherProvider, so it is cancelled on destroy and uses deterministic
 * dispatchers under test.
 *
 * Why this fixes "installed apps take too long to appear": AsyncTask ran every
 * keystroke's search on one shared SERIAL_EXECUTOR thread, so a prior search
 * stuck in a blocking network download (sockets aren't reliably interrupted by
 * cancel()) held up the next keystroke's instant local lookup. Each query is
 * now its own cancellable coroutine on the pooled IO dispatcher, so a new
 * search starts immediately even while a cancelled one's socket drains.
 *
 * Lenses are still processed strictly one after another (intentional — fanning
 * them out in parallel is too expensive). The speed-ups instead come from:
 *   - LOCAL lenses (installed apps) run first with no debounce, so they appear
 *     on every keystroke;
 *   - IO and NETWORK lenses run only after a short debounce, coalescing bursts
 *     of typing;
 *   - every lens streams its results one at a time through a [LensResultEmitter]
 *     as each finishes (e.g. DuckDuckGo emits each result as its icon
 *     downloads), instead of after the slowest one.
 */
class SearchLoader(
    private val activity: HomeActivity,
    private val dispatchers: DispatcherProvider,
) {
    private var job: Job? = null

    /**
     * Starts a fresh search, cancelling any in-flight one. [results] is the
     * list backing [adapter] (owned by LensManager); it must already be cleared.
     * Sections are appended in processing order: LOCAL lenses first, then the
     * debounced IO/NETWORK lenses, each group in enabled order.
     */
    fun start(
        pattern: String,
        lenses: List<Lens>,
        maxResults: Int,
        adapter: CollectionGridAdapter,
        results: MutableList<LensSearchResultCollection>,
        progressWheel: ProgressWheel,
    ) {
        this.job?.cancel()
        progressWheel.setProgress(0)

        val total = lenses.size
        var completed = 0

        this.job = this.activity.lifecycleScope.launch {
            // The wheel only appears if the search outlives the delay, matching the
            // old AsyncSearch: a fast local-only search never flashes it //
            val wheelReveal = launch {
                delay(PROGRESS_WHEEL_DELAY_MS)
                progressWheel.visibility = View.VISIBLE
            }

            try {
                val local = lenses.filter { it.type == LensType.LOCAL }
                val deferred = lenses.filterNot { it.type == LensType.LOCAL }

                for (lens in local) {
                    runLens(lens, pattern, maxResults, adapter, results)
                    progressWheel.setProgress(progressAngle(++completed, total))
                }

                if (deferred.isNotEmpty()) {
                    // A new keystroke cancels this job before the delay elapses, so
                    // the IO/NETWORK lenses only fire once typing settles //
                    delay(DEBOUNCE_MS)

                    for (lens in deferred) {
                        runLens(lens, pattern, maxResults, adapter, results)
                        progressWheel.setProgress(progressAngle(++completed, total))
                    }
                }

                progressWheel.visibility = View.GONE
            } finally {
                wheelReveal.cancel()
            }
        }
    }

    /**
     * Cancels the in-flight search. Deliberately leaves the progress wheel as-is:
     * a cancelled search almost always means another keystroke is starting a new
     * one, so hiding it here would make it flicker per keystroke (the new search
     * resets the progress to 0 itself).
     */
    fun cancel() {
        this.job?.cancel()
        this.job = null
    }

    /** Searches one lens, streaming its results, and turns a failure into an error section. */
    private suspend fun runLens(
        lens: Lens,
        pattern: String,
        maxResults: Int,
        adapter: CollectionGridAdapter,
        results: MutableList<LensSearchResultCollection>,
    ) {
        try {
            val emitter = CollectionEmitter(lens, adapter, results, maxResults)
            withContext(this.dispatchers.io) {
                lens.search(pattern, maxResults, emitter)
            }
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            currentCoroutineContext().ensureActive()
            appendErrorSection(LensSearchResultCollection(lens, ex), adapter, results)
        }
    }

    /** Appends an error section (null results, rendered as a synthetic error tile). On the main thread. */
    private fun appendErrorSection(
        collection: LensSearchResultCollection,
        adapter: CollectionGridAdapter,
        results: MutableList<LensSearchResultCollection>,
    ) {
        results.add(collection)
        adapter.notifyDataSetChanged()
    }

    private fun progressAngle(completed: Int, total: Int): Int =
        if (total == 0) 0 else Math.round(completed.toFloat() / total.toFloat() * 360f)

    /**
     * Receives a lens's results one at a time, materialising each section on its
     * first result and appending to that section's live list thereafter. Every
     * mutation is marshalled to the main thread (inline when already there) and
     * honours cancellation, so a stale search stops touching the adapter.
     */
    private inner class CollectionEmitter(
        private val lens: Lens,
        private val adapter: CollectionGridAdapter,
        private val results: MutableList<LensSearchResultCollection>,
        private val maxResults: Int,
    ) : LensResultEmitter {
        // Keyed by section name; null is the lens's default (lens-named) section.
        private val sections = LinkedHashMap<String?, MutableList<LensSearchResult>>()

        override suspend fun emit(result: LensSearchResult) = emitInto(null, result)

        override suspend fun emit(sectionName: String, result: LensSearchResult) =
            emitInto(sectionName, result)

        private suspend fun emitInto(sectionName: String?, result: LensSearchResult) {
            // .immediate so that a LOCAL lens running inline on the main thread
            // applies its results without waiting for the next looper turn //
            withContext(Dispatchers.Main.immediate) {
                currentCoroutineContext().ensureActive()

                val items = this@CollectionEmitter.sections.getOrPut(sectionName) {
                    mutableListOf<LensSearchResult>().also { fresh ->
                        // The section shares this live list, so later adds show too //
                        this@CollectionEmitter.results.add(
                            if (sectionName == null)
                                LensSearchResultCollection(this@CollectionEmitter.lens, fresh)
                            else
                                LensSearchResultCollection(this@CollectionEmitter.lens, sectionName, fresh))
                    }
                }

                if (items.size >= this@CollectionEmitter.maxResults) {
                    return@withContext
                }

                items.add(result)
                this@CollectionEmitter.adapter.notifyDataSetChanged()
            }
        }
    }

    companion object {
        private const val PROGRESS_WHEEL_DELAY_MS = 240L
        /** Below the ~150 ms perception threshold, so local apps still feel instant. */
        private const val DEBOUNCE_MS = 150L
    }
}
