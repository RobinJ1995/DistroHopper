package be.robinj.distrohopper.preferences;

import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;

import be.robinj.distrohopper.ExceptionHandler;

/**
 * Created by robin on 29/11/14.
 */
public class ThemePreferencesButtonClickListener implements View.OnClickListener
{
	private ThemePreferencesActivity parent;
	private SharedPreferences prefs;

	public ThemePreferencesButtonClickListener (ThemePreferencesActivity parent, SharedPreferences prefs)
	{
		super ();

		this.parent = parent;
		this.prefs = prefs;
	}

	@Override
	public void onClick (View view)
	{
		try
		{
			SharedPreferences.Editor editor = this.prefs.edit ();
			editor.putString ("theme", (String) view.getTag ());
			editor.apply ();

			this.parent.finish ();
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this.parent, ex);
			exh.show ();
		}
	}
}
