package be.robinj.distrohopper.dev

/** Severity of a log entry. The letter is what the one-line rendering prints. */
enum class LogLevel(val letter: String) {
	VERBOSE("V"),
	DEBUG("D"),
	INFO("I"),
	WARN("W"),
	ERROR("E"),
}

/**
 * One entry in the in-app developer log.
 *
 * The metadata around the message is what makes the log readable: [timestampMillis]
 * gives the timing traces in AppsLoader a scale, and [threadName] tells the loaders
 * (which log off the main thread) apart from the launcher UI.
 *
 * Deliberately free of Android imports so it stays testable without Robolectric —
 * only [Log] itself talks to android.util.Log.
 */
data class LogEntry(
	val level: LogLevel,
	val tag: String,
	val message: String,
	val timestampMillis: Long,
	val threadName: String,
) {
	/**
	 * The historical one-line rendering, `"[W] Tag: message"`.
	 *
	 * LogToaster shows this verbatim and [Log.getLog] joins it, so its shape is a
	 * compatibility contract rather than a formatting preference — don't "improve" it.
	 */
	fun format(): String = "[${this.level.letter}] ${this.tag}: ${this.message}"
}
