package be.robinj.ubuntu.unity.dash.lens;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;
import java.util.List;

import be.robinj.ubuntu.R;
import be.robinj.ubuntu.unity.dash.lens.*;


public class LensPreferencesActivity extends Activity
{
	private LensManager lensManager;
	private DragSortListView lvList;

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.setTheme (R.style.PreferencesTheme);
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_lens_preferences);

		this.lensManager = new LensManager (this.getApplicationContext (), null, null, null, null);

		List<Lens> lenses = new ArrayList<Lens> ();
		for (Lens lens : this.lensManager.getAvailableLenses ().values ())
			lenses.add (lens);

		this.lvList = (DragSortListView) this.findViewById (R.id.lvList);
		this.lvList.setAdapter (new LensPreferencesListViewAdapter (this.getApplicationContext (), this.lensManager, lenses));
		this.lvList.setDropListener (new LensPreferencesListViewDropListener (lenses));
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

	@Override
	protected void onStop ()
	{
		super.onStop ();
	}
}
