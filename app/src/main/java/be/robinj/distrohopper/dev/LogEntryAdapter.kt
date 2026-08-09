package be.robinj.distrohopper.dev

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import be.robinj.distrohopper.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Renders [LogEntry] records as rows: a severity stripe, a metadata line, the message. */
class LogEntryAdapter : RecyclerView.Adapter<LogEntryAdapter.EntryViewHolder>() {
	private val entries = mutableListOf<LogEntry>()

	class EntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val level: View = view.findViewById(R.id.vLogLevel)
		val meta: TextView = view.findViewById(R.id.tvLogMeta)
		val message: TextView = view.findViewById(R.id.tvLogMessage)
	}

	/**
	 * No DiffUtil: the log is append-only and capped, and DevLogsActivity already
	 * coalesces refreshes, so per-item diffing would buy nothing here.
	 */
	@SuppressLint("NotifyDataSetChanged")
	fun submit(entries: List<LogEntry>) {
		this.entries.clear()
		this.entries.addAll(entries)
		this.notifyDataSetChanged()
	}

	override fun getItemCount(): Int = this.entries.size

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder =
		EntryViewHolder(LayoutInflater.from(parent.context)
			.inflate(R.layout.widget_dev_log_entry, parent, false))

	override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
		val entry = this.entries[position]

		holder.level.backgroundTintList = ColorStateList.valueOf(
			ContextCompat.getColor(holder.itemView.context, colourOf(entry.level)))
		holder.meta.text = metaOf(entry)
		holder.message.text = entry.message
	}

	private fun colourOf(level: LogLevel): Int = when (level) {
		LogLevel.VERBOSE -> R.color.log_level_verbose
		LogLevel.DEBUG -> R.color.log_level_debug
		LogLevel.INFO -> R.color.log_level_info
		LogLevel.WARN -> R.color.log_level_warn
		LogLevel.ERROR -> R.color.log_level_error
	}

	private fun metaOf(entry: LogEntry): CharSequence {
		val meta = SpannableStringBuilder(TIME_FORMATTER.format(Instant.ofEpochMilli(entry.timestampMillis)))

		meta.append("  ")
		val letterStart = meta.length
		meta.append(entry.level.letter)
		meta.setSpan(StyleSpan(Typeface.BOLD), letterStart, meta.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

		meta.append("  ").append(entry.tag)

		// The thread only earns a mention when it isn't the one everything else is on,
		// so the common case doesn't repeat "main" down the whole list. Asking the
		// looper rather than comparing to "main": the name isn't the same everywhere,
		// and under Robolectric it isn't "main" at all. //
		if (entry.threadName != Looper.getMainLooper().thread.name)
			meta.append("  ·  ").append(entry.threadName)

		return meta
	}

	companion object {
		// One shared formatter: allocating a date format per bind would be the most
		// expensive thing in an otherwise trivial row. //
		private val TIME_FORMATTER: DateTimeFormatter =
			DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())
	}
}
