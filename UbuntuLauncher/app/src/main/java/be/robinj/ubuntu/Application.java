package be.robinj.ubuntu;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.util.HashMap;

/**
 * Created by robin on 8/22/14.
 */
public class Application extends android.app.Application
{
	HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker> ();

	/**
	 * Enum used to identify the tracker that needs to be used for tracking.
	 * <p/>
	 * A single tracker is usually enough for most purposes. In case you do need multiple trackers,
	 * storing them all in Application object helps ensure that they are created only once per
	 * application instance.
	 */
	public enum TrackerName
	{
		APP_TRACKER, // Tracker used only in this app.
		// GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
	}

	synchronized Tracker getTracker (TrackerName trackerId)
	{
		if (! mTrackers.containsKey (trackerId))
		{
			GoogleAnalytics analytics = GoogleAnalytics.getInstance (this);
			Tracker t = analytics.newTracker (R.xml.app_tracker);
				/*(trackerId == TrackerName.APP_TRACKER)
				? analytics.newTracker (R.xml.app_tracker)
				: analytics.newTracker (R.xml.global_tracker);*/
			mTrackers.put (trackerId, t);
		}

		return mTrackers.get (trackerId);
	}
}
