package be.robinj.distrohopper;

import java.util.Comparator;

/**
 * Created by robin on 8/21/14.
 */
public class AppComparatorAlphabetical implements Comparator<App>
{
	@Override
	public int compare (App app1, App app2)
	{
		String label1 = app1.getLabel ();
		String label2 = app2.getLabel ();

		return label1.compareToIgnoreCase (label2);
	}
}