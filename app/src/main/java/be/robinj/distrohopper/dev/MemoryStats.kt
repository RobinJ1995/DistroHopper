package be.robinj.distrohopper.dev

import android.os.Debug

/** Bitmaps are native since API 26, so Runtime's figures would say nothing. */
class MemoryStats(
	private val nativeHeap: () -> Long = { Debug.getNativeHeapAllocatedSize() },
) {
	fun read(): MemorySnapshot = MemorySnapshot(
		liveActivities = HeapProbe.liveActivities(),
		uncollectedActivities = HeapProbe.uncollectedActivities(),
		nativeHeapBytes = this.nativeHeap())
}
