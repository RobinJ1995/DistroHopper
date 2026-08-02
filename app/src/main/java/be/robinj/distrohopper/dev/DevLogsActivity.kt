package be.robinj.distrohopper.dev

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.robinj.distrohopper.IObserver
import be.robinj.distrohopper.InsetsHelper
import be.robinj.distrohopper.R

class DevLogsActivity : AppCompatActivity(), IObserver {
	private val log = Log.getInstance()
	private val entryAdapter = LogEntryAdapter()
	private val handler = Handler(Looper.getMainLooper())
	private val refreshRunnable = Runnable { this.refresh() }

	private lateinit var rvLogs: RecyclerView
	private lateinit var tvLogsEmpty: TextView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		this.setContentView(R.layout.activity_dev_logs)
		InsetsHelper.applySystemBarsPadding(this)

		this.supportActionBar?.setDisplayHomeAsUpEnabled(true)

		this.rvLogs = this.findViewById(R.id.rvLogs)
		this.tvLogsEmpty = this.findViewById(R.id.tvLogsEmpty)

		// Top-aligned rather than stackFromEnd: a terminal's bottom-anchored log would
		// leave a screen-high void above the first few entries, and refresh() already
		// keeps the newest entry in view once there are enough to scroll. //
		this.rvLogs.layoutManager = LinearLayoutManager(this)
		this.rvLogs.adapter = this.entryAdapter

		this.refresh()
	}

	override fun onStart() {
		super.onStart()

		// Attached here rather than in onCreate: a stop/start cycle doesn't re-run
		// onCreate, so attaching there left the screen stale for the rest of its life. //
		this.log.attachObserver(this)
		this.refresh()
	}

	override fun onStop() {
		this.log.detachObserver(this)
		this.handler.removeCallbacks(this.refreshRunnable)

		super.onStop()
	}

	/**
	 * Nudged on whichever thread logged — for the app loaders, a background one. Hop to
	 * the main thread, and coalesce, so a burst of entries costs one pass and not one
	 * per line.
	 */
	override fun nudge() {
		this.handler.removeCallbacks(this.refreshRunnable)
		this.handler.postDelayed(this.refreshRunnable, REFRESH_DELAY_MS)
	}

	private fun refresh() {
		val entries = this.log.entries

		// Whether the view was pinned to the newest entry before this batch: only then
		// should it follow along, so scrolling back through history isn't yanked away. //
		val following = !this.rvLogs.canScrollVertically(1)

		this.entryAdapter.submit(entries)

		this.tvLogsEmpty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
		this.rvLogs.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE

		if (following && entries.isNotEmpty())
			this.rvLogs.scrollToPosition(entries.size - 1)
	}

	companion object {
		private const val REFRESH_DELAY_MS = 100L
	}
}
