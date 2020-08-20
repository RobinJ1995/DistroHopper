package be.robinj.distrohopper.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import be.robinj.distrohopper.R;
import be.robinj.distrohopper.theme.Cinnamon;
import be.robinj.distrohopper.theme.Default;
import be.robinj.distrohopper.theme.Elementary;
import be.robinj.distrohopper.theme.Gnome;
import be.robinj.distrohopper.theme.Theme;

public class ThemePreferencesActivity extends AppCompatActivity
{

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_theme_preferences);

		List<Theme> themes = new ArrayList<Theme> ();
		themes.add (new Default());
		themes.add (new Gnome());
		themes.add (new Elementary());
		themes.add (new Cinnamon());

		SharedPreferences prefs = Preferences.getSharedPreferences(this, Preferences.PREFERENCES);

		if (! prefs.getBoolean (Preference.DEV.getName(), false))
		{
			for (int i = 0; i < themes.size (); i++)
			{
				if (themes.get (i).dev_only)
					themes.remove (i);
			}
		}

		ListView lvThemeList = this.findViewById (R.id.lvThemeList);
		lvThemeList.setAdapter (new ThemePreferencesListViewAdapter (this, themes));
	}

	@Override
	public boolean onCreateOptionsMenu (Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater ().inflate (R.menu.menu_theme_preferences, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected (MenuItem item)
	{
		return super.onOptionsItemSelected (item);
	}
}
