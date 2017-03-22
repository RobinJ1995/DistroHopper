package be.robinj.distrohopper.desktop.dash;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.HomeActivity;
import be.robinj.distrohopper.R;

/**
 * Created by robin on 8/21/14.
 */
public class GridAdapter extends ArrayAdapter<App>
{
	public GridAdapter (Context context, List<App> apps)
	{
		super (context, R.layout.widget_dash_applauncher, apps);
	}

	@Override
	public View getView (int position, View view, ViewGroup parent)
	{
		AppLauncher appLauncher = (this.getItem (position)).getDashAppLauncher ();

		if (view == null)
			view = LayoutInflater.from (this.getContext ()).inflate (R.layout.widget_dash_applauncher, parent, false);

		TextView tvLabel = (TextView) view.findViewById (R.id.tvLabel);
		ImageView imgIcon = (ImageView) view.findViewById (R.id.imgIcon);

		tvLabel.setText (appLauncher.getLabel ());
		tvLabel.setTextColor (view.getResources ().getColor (HomeActivity.theme.dash_applauncher_text_colour));
		tvLabel.setShadowLayer (5, 2, 2, view.getResources ().getColor (HomeActivity.theme.dash_applauncher_text_shadow_colour));
		imgIcon.setImageDrawable (appLauncher.getIcon ().getDrawable ());

		view.setTag (appLauncher);

		return view;
	}
}
