package be.robinj.distrohopper.desktop.dash;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.DependencyContainer;
import be.robinj.distrohopper.theme.Theme;
import be.robinj.distrohopper.R;

/**
 * Created by robin on 8/21/14.
 */
public class GridAdapter extends ArrayAdapter<App> {
	public GridAdapter(final Context context, final List<App> apps) {
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
		final Theme theme = DependencyContainer.of (view.getContext ()).getThemeManager ().getCurrent ();
		tvLabel.setTextColor (view.getResources ().getColor (theme.dash_applauncher_text_colour));
		tvLabel.setShadowLayer (5, 2, 2, view.getResources ().getColor (theme.dash_applauncher_text_shadow_colour));
		imgIcon.setImageDrawable (appLauncher.getIcon ().getDrawable ());

		// Square cells that stretch to fill their column: the GridView's column
		// width is set by DashGridSizer for the unified column count. Fall back
		// to the screen-derived cell size before the grid has been laid out //
		int size = (parent instanceof GridView) ? ((GridView) parent).getColumnWidth() : 0;
		if (size <= 0)
			size = DashGrid.cellSizePx(parent.getContext());
		view.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, size));

		// A recycled view may keep transforms from an interrupted dash animation //
		view.setTranslationX (0);
		view.setTranslationY (0);
		view.setScaleX (1);
		view.setScaleY (1);

		view.setTag (appLauncher);

		return view;
	}
}
