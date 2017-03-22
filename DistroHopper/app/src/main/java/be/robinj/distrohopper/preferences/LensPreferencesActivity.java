package be.robinj.distrohopper.preferences;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;
import java.util.List;

import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.R;
import be.robinj.distrohopper.desktop.dash.lens.Lens;
import be.robinj.distrohopper.desktop.dash.lens.LensManager;


public class LensPreferencesActivity extends AppCompatActivity
{
	private LensManager lensManager;
	private List<Lens> lenses;

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		try
		{
			super.setTheme (R.style.LensPreferencesTheme);
			super.onCreate (savedInstanceState);
			setContentView (R.layout.activity_lens_preferences);

			this.lensManager = new LensManager (this.getApplicationContext (), null, null, null, null);

			this.lenses = new ArrayList<Lens> ();

			for (Lens lens : this.lensManager.getEnabledLenses ())
				this.lenses.add (lens);

			for (Lens lens : this.lensManager.getAvailableLenses ().values ())
			{
				if (!this.lenses.contains (lens))
					this.lenses.add (lens);
			}

			DragSortListView lvList = (DragSortListView) this.findViewById (R.id.lvList);
			lvList.setAdapter (new LensPreferencesListViewAdapter (this.getApplicationContext (), this.lensManager, this.lenses));
			lvList.setDropListener (new LensPreferencesListViewDropListener (lvList, this.lenses));
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this, ex);
			exh.show ();
		}
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
	protected void onPause ()
	{
		try
		{
			this.lensManager.sortEnabledLenses (this.lenses);
			this.lensManager.saveEnabledLenses ();
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this, ex);
			exh.show ();
		}

		super.onPause ();
	}
}
