package be.robinj.distrohopper.preferences;

import android.widget.BaseAdapter;

import com.mobeta.android.dslv.DragSortListView;

import java.util.List;

import be.robinj.distrohopper.desktop.dash.lens.Lens;

/**
 * Created by robin on 30/11/14.
 */
public class LensPreferencesListViewDropListener implements DragSortListView.DropListener
{
	private DragSortListView lvList;
	private List<Lens> lenses;

	public LensPreferencesListViewDropListener (DragSortListView lvList, List<Lens> lenses)
	{
		this.lvList = lvList;
		this.lenses = lenses;
	}

	@Override
	public void drop (int i, int i2)
	{
		Lens lens = this.lenses.remove (i);
		this.lenses.add (i2, lens);

		((BaseAdapter) this.lvList.getAdapter ()).notifyDataSetChanged ();
	}
}
