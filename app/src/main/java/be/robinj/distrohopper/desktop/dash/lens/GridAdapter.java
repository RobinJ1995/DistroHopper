package be.robinj.distrohopper.desktop.dash.lens;

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

import be.robinj.distrohopper.DependencyContainer;
import be.robinj.distrohopper.desktop.dash.DashGrid;
import be.robinj.distrohopper.theme.Theme;
import be.robinj.distrohopper.R;

/**
 * Created by robin on 8/21/14.
 */
public class GridAdapter extends ArrayAdapter<LensSearchResult> {
	public GridAdapter(final Context context, final List<LensSearchResult> results) {
		super (context, R.layout.widget_dash_lens_result, results);
	}

	@Override
	public View getView (int position, View view, ViewGroup parent)
	{
		LensSearchResult result = this.getItem (position);

		if (view == null)
			view = LayoutInflater.from (parent.getContext ()).inflate (R.layout.widget_dash_lens_result, parent, false);

		TextView tvLabel = (TextView) view.findViewById (R.id.tvLabel);
		ImageView imgIcon = (ImageView) view.findViewById (R.id.imgIcon);

		tvLabel.setText (result.getName ());
		final Theme theme = DependencyContainer.of (view.getContext ()).getThemeManager ().getCurrent ();
		tvLabel.setTextColor (view.getResources ().getColor (theme.dash_applauncher_text_colour));
		tvLabel.setShadowLayer (5, 2, 2, view.getResources ().getColor (theme.dash_applauncher_text_shadow_colour));
		imgIcon.setImageDrawable (result.getIcon ());

		// Square cells that stretch to fill their column (see DashGrid); the
		// column width is set by DashGridSizer for the unified column count //
		int size = (parent instanceof GridView) ? ((GridView) parent).getColumnWidth() : 0;
		if (size <= 0)
			size = DashGrid.cellSizePx(parent.getContext());
		view.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, size));

		view.setTag (result);

		return view;
	}
}
