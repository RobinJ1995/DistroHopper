package be.robinj.distrohopper.dev

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The deltas the dialog renders: only what moved, signed. */
class MemorySnapshotTest {
	private fun snapshot(uncollected: Int = 0, nativeHeap: Long = 1_000L) =
		MemorySnapshot(1, uncollected, nativeHeap)

	@Test fun `the first reading has nothing to compare against`() {
		val deltas = this.snapshot().minus(null)

		assertNull(deltas.uncollectedActivities)
		assertNull(deltas.nativeHeapBytes)
	}

	@Test fun `a figure that did not move has no delta`() {
		val before = this.snapshot(uncollected = 2, nativeHeap = 5_000L)
		val deltas = this.snapshot(uncollected = 2, nativeHeap = 5_000L).minus(before)

		assertNull(deltas.uncollectedActivities)
		assertNull(deltas.nativeHeapBytes)
	}

	@Test fun `what a collection freed reads as a negative delta`() {
		val before = this.snapshot(uncollected = 2, nativeHeap = 9_000L)
		val deltas = this.snapshot(uncollected = 1, nativeHeap = 4_000L).minus(before)

		assertEquals(-1, deltas.uncollectedActivities)
		assertEquals(-5_000L, deltas.nativeHeapBytes)
	}

	@Test fun `growth reads as a positive delta`() {
		val before = this.snapshot(uncollected = 0, nativeHeap = 1_000L)
		val deltas = this.snapshot(uncollected = 2, nativeHeap = 3_000L).minus(before)

		assertEquals(2, deltas.uncollectedActivities)
		assertEquals(2_000L, deltas.nativeHeapBytes)
	}
}
