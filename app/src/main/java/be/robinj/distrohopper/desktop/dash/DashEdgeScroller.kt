package be.robinj.distrohopper.desktop.dash

import android.os.Handler
import android.os.Looper
import android.widget.GridView

/**
 * Auto-scrolls the dash grid while a drag hovers near its top or bottom edge, so
 * an app can be dragged onto a row that isn't currently on screen. Mirrors the
 * platform's built-in list drag-scroll: the finger only has to rest in an edge
 * zone; a self-repeating tick keeps scrolling (accelerating the deeper into the
 * zone the pointer sits) until the pointer leaves the zone, the grid can't
 * scroll any further, or the drag ends.
 *
 * Android only delivers [android.view.DragEvent.ACTION_DRAG_LOCATION] while the
 * pointer *moves*, so edge-scroll can't be driven by drag events alone — a
 * stationary finger in the edge zone gets no more events. Hence the ticking
 * runnable: [onDrag] just sets the current velocity from the pointer position;
 * the tick does the scrolling frame by frame until [stop].
 */
class DashEdgeScroller(private val grid: GridView) {
	private val handler = Handler(Looper.getMainLooper())

	/** Signed pixels to scroll per frame: <0 up (toward the start), >0 down. */
	private var velocityPx = 0
	private var ticking = false

	private val tick = object : Runnable {
		override fun run() {
			val v = this@DashEdgeScroller.velocityPx
			// Stop once out of the zone, or the grid can't scroll any further. //
			if (v == 0 || !this@DashEdgeScroller.grid.canScrollList(if (v < 0) -1 else 1)) {
				this@DashEdgeScroller.ticking = false
				return
			}
			this@DashEdgeScroller.grid.scrollListBy(v)
			this@DashEdgeScroller.handler.postDelayed(this, FRAME_MS)
		}
	}

	/** Updates the auto-scroll from the drag's vertical position within the grid. */
	fun onDrag(y: Float) {
		this.velocityPx = velocityFor(y, this.grid.height)
		if (this.velocityPx != 0 && !this.ticking) {
			this.ticking = true
			this.handler.post(this.tick)
		}
	}

	/** Halts any auto-scroll (drag left the grid, dropped, or ended). */
	fun stop() {
		this.velocityPx = 0
		this.ticking = false
		this.handler.removeCallbacks(this.tick)
	}

	companion object {
		/** Fraction of the grid's height, at each end, that triggers auto-scroll. */
		private const val EDGE_FRACTION = 0.15f

		/** Roughly one frame at 60Hz, so scrolling looks continuous. */
		private const val FRAME_MS = 16L

		private const val MIN_STEP_PX = 6
		private const val MAX_STEP_PX = 30

		/**
		 * The per-frame scroll velocity for a pointer at [y] in a grid of [height]:
		 * 0 through the middle, ramping from [MIN_STEP_PX] at a zone's inner edge to
		 * [MAX_STEP_PX] at the grid's edge, negative (scroll up) at the top and
		 * positive (scroll down) at the bottom. Pure geometry, exposed for testing.
		 */
		internal fun velocityFor(y: Float, height: Int): Int {
			val zone = height * EDGE_FRACTION
			if (zone <= 0f) {
				return 0
			}

			return when {
				y < zone -> -stepFor((zone - y) / zone)
				y > height - zone -> stepFor((y - (height - zone)) / zone)
				else -> 0
			}
		}

		/** Maps a 0..1 depth into the edge zone to a scroll step in pixels. */
		private fun stepFor(depth: Float): Int {
			val d = depth.coerceIn(0f, 1f)
			return (MIN_STEP_PX + (MAX_STEP_PX - MIN_STEP_PX) * d).toInt()
		}
	}
}
