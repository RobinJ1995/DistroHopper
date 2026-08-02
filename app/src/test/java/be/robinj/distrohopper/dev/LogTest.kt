package be.robinj.distrohopper.dev

import be.robinj.distrohopper.IObserver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class LogTest {
	private lateinit var log: Log

	// Log is a process-global singleton, so tests have to start from a fresh one or
	// state (and the enabled flag) leaks between them. //
	@Before fun setUp() {
		val instance = Log::class.java.getDeclaredField("instance")
		instance.isAccessible = true
		instance.set(null, null)

		log = Log.getInstance()
	}

	private fun enabled(): Log = log.apply { setEnabled(true) }

	@Test fun nothingIsRecordedUntilDevModeEnablesLogging() {
		log.w("Tag", "message")

		assertFalse(log.isEnabled)
		assertTrue(log.entries.isEmpty())
		assertNull(log.lastEntry)
	}

	@Test fun disablingStopsRecording() {
		enabled().w("Tag", "first")
		log.setEnabled(false)
		log.w("Tag", "second")

		assertEquals(listOf("first"), log.entries.map { it.message })
	}

	@Test fun eachLevelIsRecordedWithItsOwnSeverity() {
		enabled()
		log.v("Tag", "v")
		log.d("Tag", "d")
		log.i("Tag", "i")
		log.w("Tag", "w")
		log.e("Tag", "e")

		assertEquals(
			listOf(LogLevel.VERBOSE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR),
			log.entries.map { it.level })
	}

	@Test fun entriesCarryTagMessageThreadAndTimestamp() {
		val before = System.currentTimeMillis()
		enabled().i("AppsLoader", "Loaded 214 apps")

		val entry = log.entries.single()
		assertEquals("AppsLoader", entry.tag)
		assertEquals("Loaded 214 apps", entry.message)
		assertEquals(Thread.currentThread().name, entry.threadName)
		assertTrue(entry.timestampMillis >= before)
	}

	@Test fun entriesAreKeptInTheOrderTheyWereLogged() {
		enabled()
		repeat(5) { log.i("Tag", "message $it") }

		assertEquals((0 until 5).map { "message $it" }, log.entries.map { it.message })
	}

	@Test fun theBufferIsCappedAndDropsTheOldest() {
		enabled()
		repeat(Log.CAPACITY + 10) { log.i("Tag", "message $it") }

		val entries = log.entries
		assertEquals(Log.CAPACITY, entries.size)
		assertEquals("message 10", entries.first().message)
		assertEquals("message ${Log.CAPACITY + 9}", entries.last().message)
	}

	@Test fun getLogKeepsTheHistoricalLineShape() {
		enabled()
		log.w("Tag", "first")
		log.e("Other", "second")

		assertEquals("[W] Tag: first\n[E] Other: second\n", log.log)
	}

	@Test fun getLogIsEmptyWithoutEntries() {
		assertEquals("", log.log)
	}

	@Test fun getLastEntryReturnsTheFormattedLatest() {
		enabled()
		log.w("Tag", "first")
		log.e("Other", "second")

		assertEquals("[E] Other: second", log.lastEntry)
	}

	@Test fun clearEmptiesTheBuffer() {
		enabled().w("Tag", "message")
		log.clear()

		assertTrue(log.entries.isEmpty())
		assertNull(log.lastEntry)
	}

	@Test fun getEntriesIsASnapshotUnaffectedByLaterLogging() {
		enabled().w("Tag", "first")
		val snapshot = log.entries
		log.w("Tag", "second")

		assertEquals(1, snapshot.size)
	}

	@Test fun observersAreNudgedForEveryRecordedEntry() {
		var nudges = 0
		enabled().attachObserver(IObserver { nudges++ })

		log.w("Tag", "first")
		log.w("Tag", "second")

		assertEquals(2, nudges)
	}

	@Test fun observersAreNotNudgedWhileLoggingIsDisabled() {
		var nudges = 0
		log.attachObserver(IObserver { nudges++ })

		log.w("Tag", "message")

		assertEquals(0, nudges)
	}

	@Test fun concurrentLoggingStaysWithinCapacityAndLosesNothingElse() {
		enabled()
		val threads = 8
		val perThread = 500
		val executor = Executors.newFixedThreadPool(threads)
		val start = CountDownLatch(1)
		val done = CountDownLatch(threads)

		repeat(threads) { thread ->
			executor.execute {
				start.await()
				repeat(perThread) { log.i("Tag", "$thread-$it") }
				done.countDown()
			}
		}

		start.countDown()
		assertTrue(done.await(30, TimeUnit.SECONDS))
		executor.shutdown()

		assertEquals(Log.CAPACITY, log.entries.size)
	}

	@Test fun getInstanceHandsOutOneInstanceToConcurrentCallers() {
		val executor = Executors.newFixedThreadPool(2)
		val first = executor.submit<Log> { Log.getInstance() }
		val second = executor.submit<Log> { Log.getInstance() }

		assertSame(first.get(), second.get())
		executor.shutdown()
	}
}
