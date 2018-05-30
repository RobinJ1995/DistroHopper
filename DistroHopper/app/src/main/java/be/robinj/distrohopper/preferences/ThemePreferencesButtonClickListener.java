package be.robinj.distrohopper.preferences;

import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;

import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.theme.Theme;

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
			Theme theme = (Theme) view.getTag ();
			editor.putString ("theme", theme.getName ());
			editor.putInt ("launcher_edge", parent.getResources ().getInteger (theme.launcher_location));
			editor.putInt ("panel_edge", parent.getResources ().getInteger (theme.panel_location));
			editor.apply ();

			this.parent.finish ();
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this.parent);
		}
	}
}
