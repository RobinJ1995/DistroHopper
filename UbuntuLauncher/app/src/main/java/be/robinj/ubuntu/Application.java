package be.robinj.ubuntu;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by robin on 8/22/14.
 */
public class Application extends android.app.Application
{
	private static GoogleAnalytics analytics;
	private static Tracker tracker;

	@Override
	public void onCreate ()
	{
		Application.analytics = GoogleAnalytics.getInstance (this);
		Application.analytics.setLocalDispatchPeriod (1800);

		Application.tracker = Application.analytics.newTracker ("UA-37908259-1");
		Application.tracker.enableExceptionReporting (true);
		Application.tracker.enableAutoActivityTracking (true);
	}

	public static GoogleAnalytics getAnalytics ()
	{
		return Application.analytics;
	}

	public static Tracker getTracker ()
	{
		return Application.tracker;
	}
}
