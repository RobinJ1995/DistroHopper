package be.robinj.ubuntu;

import java.util.Comparator;

public class AppLauncherComparatorTimesLaunched implements Comparator<AppLauncher>
{
	@Override
	public int compare (AppLauncher app1, AppLauncher app2)
	{
		Long timesLaunched1 = app1.getTimesLaunched ();
		Long timesLaunched2 = app2.getTimesLaunched ();
		
		return timesLaunched1.compareTo (timesLaunched2);
	}
}
