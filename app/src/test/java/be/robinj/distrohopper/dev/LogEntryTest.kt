package be.robinj.distrohopper.dev

import org.junit.Assert.assertEquals
import org.junit.Test

class LogEntryTest {
	private fun entry(level: LogLevel, tag: String = "Tag", message: String = "message") =
		LogEntry(level, tag, message, 0L, "main")

	// LogToaster shows format() verbatim, so this shape is a compatibility contract. //
	@Test fun formatMatchesTheHistoricalOneLineShape() {
		assertEquals("[W] Tag: message", entry(LogLevel.WARN).format())
	}

	@Test fun formatUsesTheLevelLetter() {
		assertEquals("[V] Tag: message", entry(LogLevel.VERBOSE).format())
		assertEquals("[D] Tag: message", entry(LogLevel.DEBUG).format())
		assertEquals("[I] Tag: message", entry(LogLevel.INFO).format())
		assertEquals("[E] Tag: message", entry(LogLevel.ERROR).format())
	}

	@Test fun formatLeavesTheMessageUntouched() {
		val message = "Loaded 214 apps in 118ms: [a, b] 100%"

		assertEquals(
			"[I] AppsLoader: $message",
			entry(LogLevel.INFO, "AppsLoader", message).format())
	}

	@Test fun formatKeepsMultiLineMessagesIntact() {
		assertEquals(
			"[E] ExceptionHandler: first\n\nsecond",
			entry(LogLevel.ERROR, "ExceptionHandler", "first\n\nsecond").format())
	}
}
