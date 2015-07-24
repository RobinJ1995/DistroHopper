package be.robinj.ubuntu.dev;

/**
 * Created by robin on 22/07/15.
 */
public class Debug
{
	public static void assertCondition (boolean condition)
	{
		if (! condition)
		{
			String message = null;
			try
			{
				boolean nextLineIsTheChosenOne = false;

				for (StackTraceElement ste : Thread.currentThread ().getStackTrace ())
				{
					if (! nextLineIsTheChosenOne)
					{
						if (ste.toString ().contains ("Debug.assertCondition"))
							nextLineIsTheChosenOne = true;
					}
					else
					{
						message = ste.toString ();
						break;
					}
				}
			}
			catch (Exception ex)
			{
				message = "Failed to get stack trace: " + ex.toString ();
			}

			Log.e (Debug.class.getSimpleName (), "Assertion failed: " + message);
			throw new AssertionError ("Assertion failed: " + message);
		}
	}
}
