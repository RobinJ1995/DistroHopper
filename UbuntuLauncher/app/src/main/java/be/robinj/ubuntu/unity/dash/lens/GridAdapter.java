package be.robinj.ubuntu.unity.dash.lens;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import be.robinj.ubuntu.HomeActivity;
import be.robinj.ubuntu.R;

/**
 * Created by robin on 8/21/14.
 */
public class GridAdapter extends ArrayAdapter<LensSearchResult>
{
	public GridAdapter (Context context, List<LensSearchResult> results)
	{
		super (context, R.layout.widget_dash_lens_result, results);
	}

	@Override
	public View getView (int position, View view, ViewGroup parent)
	{
		LensSearchResult result = this.getItem (position);

		if (view == null)
			view = LayoutInflater.from (this.getContext ()).inflate (R.layout.widget_dash_lens_result, parent, false);

		TextView tvLabel = (TextView) view.findViewById (R.id.tvLabel);
		ImageView imgIcon = (ImageView) view.findViewById (R.id.imgIcon);

		tvLabel.setText (result.getName ());
		tvLabel.setTextColor (view.getResources ().getColor (HomeActivity.theme.dash_applauncher_text_colour));
		tvLabel.setShadowLayer (5, 2, 2, view.getResources ().getColor (HomeActivity.theme.dash_applauncher_text_shadow_colour));
		imgIcon.setImageDrawable (result.getIcon ());

		view.setTag (result);

		return view;
	}
}
