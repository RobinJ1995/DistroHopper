package be.robinj.ubuntu.unity.dash.lens;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import be.robinj.ubuntu.R;

/**
 * Created by robin on 27/11/14.
 */
public class LensPreferencesListViewAdapter extends ArrayAdapter<Lens>
{
	public LensPreferencesListViewAdapter (Context context, Lens[] lenses)
	{
		super (context, R.layout.widget_lens_preferences_list_item, lenses);
	}

	@Override
	public View getView (int position, View view, ViewGroup parent)
	{
		Lens lens = this.getItem (position);

		if (view == null)
			view = LayoutInflater.from (this.getContext ()).inflate (R.layout.widget_lens_preferences_list_item, parent, false);

		TextView tvName = (TextView) view.findViewById (R.id.tvName);
		TextView tvDescription = (TextView) view.findViewById (R.id.tvDescription);
		ImageView imgIcon = (ImageView) view.findViewById (R.id.imgIcon);

		tvName.setText (lens.getName ());
		tvDescription.setText (lens.getDescription ());
		imgIcon.setImageDrawable (lens.getIcon ());

		view.setTag (lens);

		return view;
	}
}
