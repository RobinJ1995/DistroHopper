package be.robinj.distrohopper.desktop.dash.lens

/**
 * Lets a [Lens] push results to the dash one at a time, as soon as each is
 * fully ready (its icon already resolved — no placeholders), instead of
 * returning the whole batch at once. The runner (home/SearchLoader) marshals
 * every call onto the UI thread, creating each section lazily on its first
 * result and notifying the adapter after each.
 */
interface LensResultEmitter {
    /** Emits one result into the lens's default section (titled by the lens name). */
    suspend fun emit(result: LensSearchResult)

    /**
     * Emits one result into a named section, for lenses that group results into
     * more than one (e.g. InstalledApps, one section per profile). Sections
     * appear in first-emit order.
     */
    suspend fun emit(sectionName: String, result: LensSearchResult)
}
