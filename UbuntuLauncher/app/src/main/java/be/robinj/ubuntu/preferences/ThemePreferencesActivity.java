package be.robinj.ubuntu.preferences;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import be.robinj.ubuntu.R;
import be.robinj.ubuntu.theme.*;

public class ThemePreferencesActivity extends Activity
{

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.setTheme (R.style.PreferencesTheme);
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_theme_preferences);

		List<Theme> themes = new ArrayList<Theme> ();
		themes.add (new Default ());
		themes.add (new Elementary ());
		themes.add (new Gnome ());

		for (int i = 0; i < themes.size (); i++)
		{
			if (themes.get (i).dev_only)
				themes.remove (i);
		}

		ListView lvThemeList = (ListView) this.findViewById (R.id.lvThemeList);
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
