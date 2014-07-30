package be.robinj.ubuntu;

import java.util.Comparator;

public class AppLauncherComparator implements Comparator<AppLauncher>
{
	private boolean mostUsedFirst;

	public AppLauncherComparator ()
	{
		this.mostUsedFirst = true;
	}

	public AppLauncherComparator (boolean mostUsedFirst)
	{
		this.mostUsedFirst = mostUsedFirst;
	}

	@Override
	public int compare (AppLauncher app1, AppLauncher app2)
	{
		if (this.mostUsedFirst)
		{
			AppLauncherComparatorTimesLaunched timesLaunchedComparator = new AppLauncherComparatorTimesLaunched ();
			int timesLaunched = timesLaunchedComparator.compare (app1, app2);
			if (timesLaunched != 0)
				return timesLaunched;
		}

		AppLauncherComparatorAlphabetical alphabeticalComparator = new AppLauncherComparatorAlphabetical ();
		int alphabetical = alphabeticalComparator.compare (app1, app2);
		if (alphabetical != 0)
			return alphabetical;

		return 0;
	}
}
