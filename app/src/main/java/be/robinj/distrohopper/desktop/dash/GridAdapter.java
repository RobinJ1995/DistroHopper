package be.robinj.distrohopper.desktop.dash;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.core.content.ContextCompat;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.DependencyContainer;
import be.robinj.distrohopper.folder.FolderIconDrawable;
import be.robinj.distrohopper.theme.Theme;
import be.robinj.distrohopper.R;

/**
 * Renders the dash grid's heterogeneous items ({@link DashItem}): a standalone
 * app, or a folder drawn as a miniature of its contents grid. Both use the same
 * cell layout (so the grid stays uniform); a folder simply shows the
 * {@link FolderIconDrawable} and no label (folders are unnamed). The cell's tag
 * is the {@link DashItem}, which the dash click/long-press listeners branch on.
 *
 * Created by robin on 8/21/14.
 */
public class GridAdapter extends ArrayAdapter<DashItem> {
	public GridAdapter(final Context context, final List<DashItem> items) {
		super (context, R.layout.widget_dash_applauncher, items);
	}

	@Override
	public View getView (int position, View view, ViewGroup parent)
	{
		DashItem item = this.getItem (position);

		if (view == null)
			view = LayoutInflater.from (parent.getContext ()).inflate (R.layout.widget_dash_applauncher, parent, false);

		TextView tvLabel = (TextView) view.findViewById (R.id.tvLabel);
		ImageView imgIcon = (ImageView) view.findViewById (R.id.imgIcon);

		final Theme theme = DependencyContainer.of (view.getContext ()).getThemeManager ().getCurrent ();
		tvLabel.setTextColor (ContextCompat.getColor (view.getContext (), theme.dash_applauncher_text_colour));
		tvLabel.setShadowLayer (5, 2, 2, ContextCompat.getColor (view.getContext (), theme.dash_applauncher_text_shadow_colour));

		if (item instanceof DashItem.AppItem) {
			App app = ((DashItem.AppItem) item).getApp ();
			tvLabel.setText (app.getLabel ());
			imgIcon.setImageDrawable (app.getIcon ().getDrawable ());
		} else if (item instanceof DashItem.FolderItem) {
			// Folders are unnamed: a blank label keeps the cell the same height. //
			tvLabel.setText ("");
			List<Drawable> memberIcons = new ArrayList<> ();
			for (App app : ((DashItem.FolderItem) item).getApps ())
				memberIcons.add (app.getIcon ().getDrawable ());
			imgIcon.setImageDrawable (new FolderIconDrawable (memberIcons));
		}

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

		view.setTag (item);

		return view;
	}
}
