package be.robinj.distrohopper.dev;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import be.robinj.distrohopper.Observed;

/**
 * Created by robin on 03/07/15.
 */
public class Log extends Observed
{
	/**
	 * How many entries to keep in memory. The launcher process lives for days, so the
	 * log has to be bounded; at capacity the oldest entry is dropped. //
	 */
	static final int CAPACITY = 2000;

	private final Deque<LogEntry> entries = new ArrayDeque<LogEntry> ();
	private final Object lock = new Object ();

	// Off unless dev mode turns it on; see HomeActivity.onCreate. //
	private boolean enabled = false;

	private static Log instance;

	// Synchronized because background threads (AppsLoader, WorkProfileAppsCallback,
	// DrawableCache) log too: an unguarded lazy init can hand out two instances, and an
	// observer attached to the losing one never hears anything again. //
	public static synchronized Log getInstance ()
	{
		if (Log.instance == null)
			Log.instance = new Log ();

		return Log.instance;
	}

	private Log ()
	{
	}

	public void v (String tag, String message)
	{
		android.util.Log.v (tag, message);

		this.appendToDevLog (LogLevel.VERBOSE, tag, message);
	}

	public void d (String tag, String message)
	{
		android.util.Log.d (tag, message);

		this.appendToDevLog (LogLevel.DEBUG, tag, message);
	}

	public void i (String tag, String message)
	{
		android.util.Log.i (tag, message);

		this.appendToDevLog (LogLevel.INFO, tag, message);
	}

	public void w (String tag, String message)
	{
		android.util.Log.w (tag, message);

		this.appendToDevLog (LogLevel.WARN, tag, message);
	}

	public void e (String tag, String message)
	{
		android.util.Log.e (tag, message);

		this.appendToDevLog (LogLevel.ERROR, tag, message);
	}

	/**
	 * The whole log as one string, in the historical one-entry-per-line format.
	 *
	 * O(n) now that entries are kept as records rather than pre-joined text. Nothing on
	 * a hot path calls this — the viewer renders {@link #getEntries()} instead. //
	 */
	public String getLog ()
	{
		final StringBuilder sb = new StringBuilder ();

		for (final LogEntry entry : this.getEntries ())
			sb.append (entry.format ()).append ("\n");

		return sb.toString ();
	}

	public String getLastEntry ()
	{
		synchronized (this.lock)
		{
			return this.entries.isEmpty () ? null : this.entries.getLast ().format ();
		}
	}

	/** A snapshot, safe to iterate without holding the lock. Oldest entry first. */
	public List<LogEntry> getEntries ()
	{
		synchronized (this.lock)
		{
			return new ArrayList<LogEntry> (this.entries);
		}
	}

	public void clear ()
	{
		synchronized (this.lock)
		{
			this.entries.clear ();
		}

		this.nudgeObservers ();
	}

	public boolean isEnabled ()
	{
		synchronized (this.lock)
		{
			return this.enabled;
		}
	}

	public void setEnabled (boolean enabled)
	{
		synchronized (this.lock)
		{
			this.enabled = enabled;
		}
	}

	private void appendToDevLog (LogLevel level, String tag, String message)
	{
		if (this.append (level, tag, message))
		{
			// Outside the lock: observers touch the UI, and holding the log lock across
			// that would let a logging background thread block on the main thread. //
			this.nudgeObservers ();
		}
	}

	/**
	 * Appends without waking the observers, for sources that fire per main-looper
	 * message ({@link LooperProfiler}): a nudge posts to that looper, and the post
	 * would itself be logged and nudge again. //
	 */
	void appendQuietly (LogLevel level, String tag, String message)
	{
		this.append (level, tag, message);
	}

	/** Records the entry, answering whether it was kept (logging can be off). */
	private boolean append (LogLevel level, String tag, String message)
	{
		synchronized (this.lock)
		{
			if (!this.enabled)
				return false;

			if (this.entries.size () >= Log.CAPACITY)
				this.entries.removeFirst ();

			this.entries.addLast (new LogEntry (level, tag, message,
				System.currentTimeMillis (), Thread.currentThread ().getName ()));
		}

		return true;
	}
}
