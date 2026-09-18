package be.robinj.distrohopper.dev

/** One reading of what the process is holding. */
data class MemorySnapshot(
	val liveActivities: Int,
	val uncollectedActivities: Int,
	val nativeHeapBytes: Long,
) {
	/** Movement against [previous]; null where a figure did not move. */
	fun minus(previous: MemorySnapshot?): Deltas {
		if (previous == null) {
			return Deltas(null, null)
		}

		return Deltas(
			uncollectedActivities =
				(this.uncollectedActivities - previous.uncollectedActivities)
					.takeIf { it != 0 },
			nativeHeapBytes =
				(this.nativeHeapBytes - previous.nativeHeapBytes).takeIf { it != 0L })
	}

	data class Deltas(val uncollectedActivities: Int?, val nativeHeapBytes: Long?)
}
