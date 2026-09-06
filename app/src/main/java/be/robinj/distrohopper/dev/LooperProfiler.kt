package be.robinj.distrohopper.dev

import android.os.Looper
import android.util.Printer

/**
 * Logs every message dispatched on the main looper, to find work running when
 * nothing should be. A spin shows up as one line repeating; the entry
 * timestamps give its rate. The "finished" line is dropped as a duplicate.
 */
class LooperProfiler(private val log: Log) : Printer {
	override fun println(x: String?) {
		val line = x ?: return

		if (! line.startsWith(DISPATCH_PREFIX)) {
			return
		}

		// Quietly: a nudge posts to this looper, which would be logged, which
		// would nudge again //
		this.log.appendQuietly(LogLevel.VERBOSE, TAG,
			line.substring(DISPATCH_PREFIX.length))
	}

	companion object {
		private const val TAG = "MainLooper"

		/** What Looper prefixes the line it prints before dispatching a message. */
		private const val DISPATCH_PREFIX = ">>>>> Dispatching to "

		/** On, the looper builds a string per message; off, it does nothing. */
		@JvmStatic
		fun setEnabled(enabled: Boolean) {
			Looper.getMainLooper().setMessageLogging(
				if (enabled) LooperProfiler(Log.getInstance()) else null)
		}
	}
}
