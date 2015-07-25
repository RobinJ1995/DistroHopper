package be.robinj.ubuntu.dev;

import be.robinj.ubuntu.Observed;

/**
 * Created by robin on 03/07/15.
 */
public class Log extends Observed
{
	private StringBuilder log = new StringBuilder ();
	private boolean enabled = false;

	private static Log instance;

	public static Log getInstance ()
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

		this.appendToDevLog ("v", tag, message);
	}

	public void d (String tag, String message)
	{
		android.util.Log.d (tag, message);

		this.appendToDevLog ("d", tag, message);
	}

	public void i (String tag, String message)
	{
		android.util.Log.i (tag, message);

		this.appendToDevLog ("i", tag, message);
	}

	public void w (String tag, String message)
	{
		android.util.Log.w (tag, message);

		this.appendToDevLog ("w", tag, message);
	}

	public void e (String tag, String message)
	{
		android.util.Log.e (tag, message);

		this.appendToDevLog ("e", tag, message);
	}

	public String getLog ()
	{
		return this.log.toString ();
	}

	public void setEnabled (boolean enabled)
	{
		this.enabled = enabled;
	}

	private void appendToDevLog (String type, String tag, String message)
	{
		if (this.enabled)
		{
			this.log.append ("[")
				.append (type.toUpperCase ())
				.append ("] ")
				.append (tag)
				.append (": ")
				.append (message)
				.append ("\n");

			this.nudgeObservers ();
		}
	}
}
