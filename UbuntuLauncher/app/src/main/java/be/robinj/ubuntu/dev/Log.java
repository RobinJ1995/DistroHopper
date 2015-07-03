package be.robinj.ubuntu.dev;

/**
 * Created by robin on 03/07/15.
 */
public class Log
{
	private static StringBuilder log = new StringBuilder ();
	private static boolean enabled = false;

	public static void v (String tag, String message)
	{
		android.util.Log.v (tag, message);

		Log.appendToDevLog ("v", tag, message);
	}

	public static void d (String tag, String message)
	{
		android.util.Log.d (tag, message);

		Log.appendToDevLog ("d", tag, message);
	}

	public static void i (String tag, String message)
	{
		android.util.Log.i (tag, message);

		Log.appendToDevLog ("i", tag, message);
	}

	public static void w (String tag, String message)
	{
		android.util.Log.w (tag, message);

		Log.appendToDevLog ("w", tag, message);
	}

	public static void e (String tag, String message)
	{
		android.util.Log.e (tag, message);

		Log.appendToDevLog ("e", tag, message);
	}

	public static String getLog ()
	{
		return Log.log.toString ();
	}

	public static void setEnabled (boolean enabled)
	{
		Log.enabled = enabled;
	}

	private static void appendToDevLog (String type, String tag, String message)
	{
		if (Log.enabled)
		{
			Log.log.append ("[")
				.append (type.toUpperCase ())
				.append ("] ")
				.append (tag)
				.append (": ")
				.append (message)
				.append ("\n");
		}
	}
}
