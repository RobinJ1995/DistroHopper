package be.robinj.distrohopper.preferences;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;
import java.util.List;

import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.HomeAwareActivity;
import be.robinj.distrohopper.InsetsHelper;
import be.robinj.distrohopper.R;
import be.robinj.distrohopper.desktop.dash.lens.Lens;
import be.robinj.distrohopper.desktop.dash.lens.LensManager;


public class LensPreferencesActivity extends HomeAwareActivity
{
	private LensManager lensManager;
	private List<Lens> lenses;

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		try
		{
			super.onCreate (savedInstanceState);
			setContentView (R.layout.activity_lens_preferences);
			InsetsHelper.applySystemBarsPadding (this);

			this.lensManager = new LensManager (this.getApplicationContext (), null, null, null, null);

			this.lenses = new ArrayList<> ();

			for (Lens lens : this.lensManager.getEnabledLenses ())
				this.lenses.add (lens);

			for (Lens lens : this.lensManager.getAvailableLenses ().values ())
			{
				if (!this.lenses.contains (lens))
					this.lenses.add (lens);
			}

			DragSortListView lvList = this.findViewById (R.id.lvList);
			lvList.setAdapter (new LensPreferencesListViewAdapter (this, this, this.lensManager, this.lenses));
			lvList.setDropListener (new LensPreferencesListViewDropListener (lvList, this.lenses));

			// Drag only via the row handles, so that the rest of the row scrolls //
			final DragSortController dragController = new DragSortController (
				lvList, R.id.ivDragHandle, DragSortController.ON_DOWN, 0);
			dragController.setSortEnabled (true);
			dragController.setRemoveEnabled (false);
			dragController.setBackgroundColor (android.graphics.Color.TRANSPARENT);
			lvList.setFloatViewManager (dragController);
			lvList.setOnTouchListener (dragController);
			lvList.setDragEnabled (true);
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
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
			if (this.lensManager == null) // onCreate failed; nothing to save //
			{
				super.onPause ();
				return;
			}

			this.lensManager.sortEnabledLenses (this.lenses);
			this.lensManager.saveEnabledLenses ();
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
		}

		super.onPause ();
	}
}
