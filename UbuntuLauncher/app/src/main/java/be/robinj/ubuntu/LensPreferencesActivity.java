package be.robinj.ubuntu;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import be.robinj.ubuntu.unity.dash.lens.*;


public class LensPreferencesActivity extends Activity
{

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.setTheme (R.style.PreferencesTheme);
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_lens_preferences);

		Context context = this.getApplicationContext ();

		Lens[] lenses = new Lens[] // Application Context will suffice here. Only need to get information from the lenses, no actual searches happening here. //
		{
			new AskUbuntu (context),
			new DuckDuckGo (context),
			new GitHub (context),
			new GooglePlus (context),
			new InstalledApps (context),
			new LocalFiles (context),
			new Reddit (context),
			new ServerFault (context),
			new StackOverflow (context),
			new SuperUser (context)
		};

		ListView lvList = (ListView) this.findViewById (R.id.lvList);
		lvList.setAdapter (new LensPreferencesListViewAdapter (context, lenses));
	}


	@Override
	public boolean onCreateOptionsMenu (Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater ().inflate (R.menu.lens_preferences, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected (MenuItem item)
	{
		/*// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId ();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings)
		{
			return true;
		}*/

		return super.onOptionsItemSelected (item);
	}
}
