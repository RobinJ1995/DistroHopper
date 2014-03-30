package be.robinj.ubuntu;

import java.util.Comparator;

public class AppLauncherComparatorAlphabetical implements Comparator<AppLauncher>
{
	@Override
	public int compare (AppLauncher app1, AppLauncher app2)
	{
		String label1 = app1.getLabel ();
		String label2 = app2.getLabel ();
		
		return label1.compareToIgnoreCase (label2);
	}
}