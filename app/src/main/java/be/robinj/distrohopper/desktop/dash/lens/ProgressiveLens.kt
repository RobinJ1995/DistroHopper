package be.robinj.distrohopper.desktop.dash.lens

/**
 * A lens that can stream its results progressively rather than returning them
 * all at once. The runner (home/SearchLoader) prefers [searchInto] when a lens
 * implements this interface, falling back to the synchronous
 * [Lens.searchCollections] for every other lens.
 *
 * Implemented by lenses that download a separate asset (e.g. an icon) per
 * result — DuckDuckGo — so each result appears the moment its own download
 * finishes instead of after the slowest one. Keeping the suspend contract in
 * Kotlin leaves the Java [Lens] base class untouched.
 */
interface ProgressiveLens {
    /**
     * Searches and emits each result through [emitter] as it becomes ready,
     * up to [maxResults]. Runs on a background dispatcher inside the runner's
     * cancellable job; implementations should honour coroutine cancellation
     * (the emitter checks it before touching the UI).
     */
    suspend fun searchInto(str: String, maxResults: Int, emitter: LensResultEmitter)
}
