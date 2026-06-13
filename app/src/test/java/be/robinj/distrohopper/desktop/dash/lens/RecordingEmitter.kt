package be.robinj.distrohopper.desktop.dash.lens

import kotlinx.coroutines.runBlocking

/**
 * Test emitter that records everything a [Lens] streams: a flat list in emit
 * order, plus the per-named-section grouping (sections in first-emit order).
 */
internal class RecordingEmitter : LensResultEmitter {
    val results = mutableListOf<LensSearchResult>()
    val sections = LinkedHashMap<String, MutableList<LensSearchResult>>()

    override suspend fun emit(result: LensSearchResult) {
        results.add(result)
    }

    override suspend fun emit(sectionName: String, result: LensSearchResult) {
        results.add(result)
        sections.getOrPut(sectionName) { mutableListOf() }.add(result)
    }
}

/** Runs [Lens.search] to completion against a fresh [RecordingEmitter] and returns it. */
internal fun Lens.collect(query: String, maxResults: Int): RecordingEmitter =
    RecordingEmitter().also { emitter -> runBlocking { search(query, maxResults, emitter) } }
