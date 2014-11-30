package be.robinj.ubuntu.unity.dash.lens;

import com.mobeta.android.dslv.DragSortListView;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by robin on 30/11/14.
 */
public class LensPreferencesListViewDropListener implements DragSortListView.DropListener
{
	private List<Lens> lenses;

	public LensPreferencesListViewDropListener (List<Lens> lenses)
	{
		this.lenses = lenses;
	}

	@Override
	public void drop (int i, int i2)
	{
		Lens lens = this.lenses.remove (i);
		this.lenses.add (i2, lens);
	}
}
