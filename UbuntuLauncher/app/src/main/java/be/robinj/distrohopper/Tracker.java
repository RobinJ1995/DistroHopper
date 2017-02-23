package be.robinj.distrohopper;

import com.google.android.gms.analytics.HitBuilders;

import java.util.Map;

public class Tracker
{
	public static void trackEvent (String category, String action, String label)
	{
		Tracker.getTracker ().send
		(
			new HitBuilders.EventBuilder ()
				.setCategory (category)
				.setAction (action)
				.setLabel (label)
				.build ()
		);
	}

	public static void trackException (Exception ex)
	{
		StringBuilder stackTrace = new StringBuilder ();
		for (StackTraceElement ste : ex.getStackTrace ())
			stackTrace.append (ste.toString ())
				.append ("\n");

		Tracker.trackException (ex, stackTrace);
	}

	public static void trackException (Exception ex, StringBuilder stackTrace)
	{
		String message;

		if (stackTrace.indexOf ("\n") > 0)
			message = stackTrace.substring (0, stackTrace.indexOf ("\n"));
		else
			message = ex.getMessage ();

		Tracker.trackException (ex, message);
	}

	public static void trackException (Exception ex, String message)
	{
		Map<String, String> data = new HitBuilders.ExceptionBuilder ()
			.setDescription (message)
			.build ();

		Tracker.getTracker ().send (data);
	}

	private static com.google.android.gms.analytics.Tracker getTracker ()
	{
		return Application.getTracker ();
	}
}
