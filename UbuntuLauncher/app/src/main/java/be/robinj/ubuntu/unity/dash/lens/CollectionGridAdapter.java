package be.robinj.ubuntu.unity.dash.lens;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import be.robinj.ubuntu.R;

/**
 * Created by robin on 8/21/14.
 */
public class CollectionGridAdapter extends ArrayAdapter<LensSearchResultCollection>
{
	public CollectionGridAdapter (Context context, List<LensSearchResultCollection> coll)
	{
		super (context, R.layout.widget_dash_lens_result_collection, coll);
	}

	@Override
	public View getView (int position, View view, ViewGroup parent)
	{
		LensSearchResultCollection coll = this.getItem (position);

		if (view == null)
			view = LayoutInflater.from (this.getContext ()).inflate (R.layout.widget_dash_lens_result_collection, parent, false);

		TextView tvLabel = (TextView) view.findViewById (R.id.tvLabel);
		GridView gvResults = (GridView) view.findViewById (R.id.gvResults);

		tvLabel.setText (coll.getLens ().getName ());
		gvResults.setAdapter (new GridAdapter (this.getContext (), coll.getResults ()));
		gvResults.setOnItemClickListener (new LensSearchResultClickListener (coll.getLens ()));
		gvResults.setOnItemLongClickListener (new LensSearchResultLongClickListener (coll.getLens ()));

		view.setTag (coll);

		return view;
	}
}
