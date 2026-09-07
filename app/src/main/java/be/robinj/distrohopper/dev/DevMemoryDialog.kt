package be.robinj.distrohopper.dev

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Read on open and on demand; never polled. */
class DevMemoryDialog(
	private val activity: DevLogsActivity,
	private val stats: MemoryStats = MemoryStats(),
) {
	private var dialog: AlertDialog? = null
	private var rows: LinearLayout? = null
	private var previous: MemorySnapshot? = null

	fun show() {
		val body = LayoutInflater.from(this.activity)
			.inflate(R.layout.widget_dev_memory, null) as LinearLayout
		this.rows = body

		val dialog = AlertDialog.Builder(this.activity)
			.setTitle(R.string.option_dev_memory)
			.setView(body)
			.setNeutralButton(R.string.option_dev_memory_refresh, null)
			.setNegativeButton(R.string.option_dev_memory_close, null)
			.setPositiveButton(R.string.option_dev_memory_gc, null)
			.create()

		// After show(), or the buttons would dismiss the dialog they refresh //
		dialog.setOnShowListener {
			dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
				.setOnClickListener { this.render(this.stats.read()) }
			dialog.getButton(AlertDialog.BUTTON_POSITIVE)
				.setOnClickListener { this.collect(it) }
		}

		this.dialog = dialog
		dialog.show()
		this.render(this.stats.read())
	}

	fun dismiss() {
		this.dialog?.dismiss()
		this.dialog = null
		this.rows = null
	}

	/** Twice, since the first pass only finalises what the second can free. */
	private fun collect(button: View) {
		button.isEnabled = false

		val dispatchers = DependencyContainer.of(this.activity).dispatchers
		this.activity.lifecycleScope.launch {
			withContext(dispatchers.io) {
				Runtime.getRuntime().gc()
				System.runFinalization()
				Runtime.getRuntime().gc()
			}

			delay(SETTLE_MS)

			val before = this@DevMemoryDialog.previous
			val after = this@DevMemoryDialog.stats.read()
			this@DevMemoryDialog.render(after)
			this@DevMemoryDialog.log(before, after)
			button.isEnabled = true
		}
	}

	private fun render(snapshot: MemorySnapshot) {
		val rows = this.rows ?: return
		val deltas = snapshot.minus(this.previous)
		rows.removeAllViews()

		this.addRow(rows, R.string.dev_memory_activities,
			this.activity.getString(R.string.dev_memory_activities_value,
				snapshot.liveActivities, snapshot.uncollectedActivities),
			count(deltas.uncollectedActivities))

		this.addRow(rows, R.string.dev_memory_native_heap,
			Formatter.formatShortFileSize(this.activity, snapshot.nativeHeapBytes),
			deltas.nativeHeapBytes?.let { this.size(it) })

		this.previous = snapshot
	}

	private fun addRow(rows: LinearLayout, label: Int, value: String, delta: String?) {
		val row = LayoutInflater.from(this.activity)
			.inflate(R.layout.widget_dev_memory_row, rows, false)

		row.findViewById<TextView>(R.id.tvMemoryLabel).setText(label)
		row.findViewById<TextView>(R.id.tvMemoryValue).text = value

		val deltaView = row.findViewById<TextView>(R.id.tvMemoryDelta)
		deltaView.text = delta ?: ""
		deltaView.visibility = if (delta == null) View.GONE else View.VISIBLE

		rows.addView(row)
	}

	/** A signed size, so a delta does not read as another figure. */
	private fun size(bytes: Long): String {
		val sign = if (bytes > 0) "+" else "−"

		return "(" + sign +
			Formatter.formatShortFileSize(this.activity, Math.abs(bytes)) + ")"
	}

	/** Into the log too, so a reading can be exported rather than only seen. */
	private fun log(before: MemorySnapshot?, after: MemorySnapshot) {
		if (before == null) {
			return
		}

		Log.getInstance().i(TAG, "GC: uncollected " + before.uncollectedActivities +
			" → " + after.uncollectedActivities + ", native heap " +
			Formatter.formatShortFileSize(this.activity, before.nativeHeapBytes) +
			" → " + Formatter.formatShortFileSize(this.activity, after.nativeHeapBytes))
	}

	private companion object {
		const val TAG = "Memory"

		/** Reference clearing runs alongside us; counting at once would race it. */
		const val SETTLE_MS = 250L

		fun count(delta: Int?): String? =
			delta?.let { "(" + (if (it > 0) "+" else "−") + Math.abs(it) + ")" }
	}
}
