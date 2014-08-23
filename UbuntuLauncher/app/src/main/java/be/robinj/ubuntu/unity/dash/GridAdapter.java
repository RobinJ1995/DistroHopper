package be.robinj.ubuntu.unity.dash;

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
 * Created by robin on 8/21/14.
 */
public class GridAdapter extends ArrayAdapter<AppLauncher>
{
	public GridAdapter (Context context, List<AppLauncher> appLaunchers)
	{
		super (context, R.layout.widget_dash_applauncher, appLaunchers);
	}

	@Override
	public View getView (int position, View view, ViewGroup parent)
	{
		AppLauncher appLauncher = this.getItem (position);

		if (view == null)
			view = LayoutInflater.from (this.getContext ()).inflate (R.layout.widget_dash_applauncher, parent, false);

		TextView tvLabel = (TextView) view.findViewById (R.id.tvLabel);
		ImageView imgIcon = (ImageView) view.findViewById (R.id.imgIcon);

		tvLabel.setText (appLauncher.getLabel ());
		imgIcon.setImageDrawable (appLauncher.getIcon ().getDrawable ());

		view.setTag (appLauncher);

		return view;
	}
}
