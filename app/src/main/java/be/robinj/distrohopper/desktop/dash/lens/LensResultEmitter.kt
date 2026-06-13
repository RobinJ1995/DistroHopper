package be.robinj.distrohopper.desktop.dash.lens

/**
 * Lets a [ProgressiveLens] push results to the dash one at a time, as soon as
 * each is fully ready, instead of returning the whole batch at once. The
 * runner (home/SearchLoader) marshals every call onto the UI thread, creating
 * the lens's collection on the first emit and notifying the adapter after each.
 */
interface LensResultEmitter {
    /**
     * Append one fully-loaded result — its icon already resolved, no
     * placeholders — to this lens's collection. The first call materialises
     * the collection and inserts it at the lens's position in the dash.
     */
    suspend fun emit(result: LensSearchResult)
}
