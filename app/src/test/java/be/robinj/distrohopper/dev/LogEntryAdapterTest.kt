package be.robinj.distrohopper.dev

import android.content.Context
import android.os.Looper
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LogEntryAdapterTest {
	private val context: Context get() = ApplicationProvider.getApplicationContext()

	private val mainThread: String get() = Looper.getMainLooper().thread.name

	/** Binds one entry and hands back the row, without needing a laid-out window. */
	private fun bind(entry: LogEntry): android.view.View {
		val adapter = LogEntryAdapter()
		adapter.submit(listOf(entry))

		val parent = FrameLayout(context)
		val holder = adapter.createViewHolder(parent, 0)
		adapter.bindViewHolder(holder, 0)

		return holder.itemView
	}

	private fun meta(entry: LogEntry): String =
		bind(entry).findViewById<TextView>(R.id.tvLogMeta).text.toString()

	@Test fun theMessageIsRenderedExactlyAsItWasLogged() {
		val row = bind(LogEntry(LogLevel.INFO, "AppsLoader", "Loaded 214 apps", 0L, mainThread))

		// toString(): textIsSelectable renders the message as a SpannableString. //
		assertEquals(
			"Loaded 214 apps",
			row.findViewById<TextView>(R.id.tvLogMessage).text.toString())
	}

	@Test fun theMetaLineCarriesTheTimestampLevelAndTag() {
		val meta = meta(LogEntry(LogLevel.WARN, "Image", "message", 0L, mainThread))

		assertTrue(meta, meta.contains("W"))
		assertTrue(meta, meta.contains("Image"))
		// Midnight UTC renders as some HH:mm:ss.SSS in the local zone, whichever it is. //
		assertTrue(meta, Regex("^\\d{2}:\\d{2}:\\d{2}\\.\\d{3}").containsMatchIn(meta))
	}

	@Test fun theMainThreadIsNotNamed() {
		val meta = meta(LogEntry(LogLevel.INFO, "AppsLoader", "message", 0L, mainThread))

		assertFalse(meta, meta.contains(mainThread))
	}

	@Test fun anyOtherThreadIsNamed() {
		val meta = meta(LogEntry(LogLevel.WARN, "DrawableCache", "message", 0L, "pool-1-thread-2"))

		assertTrue(meta, meta.contains("pool-1-thread-2"))
	}

	@Test fun submitReplacesTheEntriesItWasGiven() {
		val adapter = LogEntryAdapter()

		adapter.submit(listOf(LogEntry(LogLevel.INFO, "Tag", "first", 0L, mainThread)))
		adapter.submit(listOf(
			LogEntry(LogLevel.INFO, "Tag", "first", 0L, mainThread),
			LogEntry(LogLevel.WARN, "Tag", "second", 0L, mainThread)))

		assertEquals(2, adapter.itemCount)
	}
}
