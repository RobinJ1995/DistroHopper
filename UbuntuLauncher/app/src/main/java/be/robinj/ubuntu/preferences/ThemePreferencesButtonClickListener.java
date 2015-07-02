package be.robinj.ubuntu.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;
import android.widget.CheckBox;

import be.robinj.ubuntu.ExceptionHandler;
import be.robinj.ubuntu.unity.dash.lens.Lens;
import be.robinj.ubuntu.unity.dash.lens.LensManager;

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

			if (Build.VERSION.SDK_INT >= 9)
				editor.apply ();
			else
				editor.commit ();

			this.parent.finish ();
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this.parent, ex);
			exh.show ();
		}
	}
}
