package be.robinj.distrohopper.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import be.robinj.distrohopper.R;
import be.robinj.distrohopper.theme.Theme;

/**
 * Created by robin on 02/07/15.
 */
public class ThemePreferencesListViewAdapter extends ArrayAdapter<Theme>
{
	private ThemePreferencesActivity parent;
	private SharedPreferences prefs;

	public ThemePreferencesListViewAdapter (ThemePreferencesActivity parent, List<Theme> themes)
	{
		super (parent, R.layout.widget_theme_preferences_list_item, themes);

		this.parent = parent;
		this.prefs = Preferences.getSharedPreferences(this.getContext(), Preferences.PREFERENCES);
	}

	@Override
	public View getView (int position, View view, ViewGroup parent)
	{
		Theme theme = this.getItem (position);

		if (view == null)
			view = LayoutInflater.from (this.getContext ()).inflate (R.layout.widget_theme_preferences_list_item, parent, false);

		TextView tvName = (TextView) view.findViewById (R.id.tvName);
		TextView tvDescription = (TextView) view.findViewById (R.id.tvDescription);
		Button btnApplyTheme = (Button) view.findViewById (R.id.btnApplyTheme);
		LinearLayout llScreenshots = (LinearLayout) view.findViewById (R.id.llScreenshots);

		tvName.setText (theme.name);
		tvDescription.setText (theme.description);
		llScreenshots.removeAllViews ();
		for (int res_screenshot : theme.res_screenshots)
		{
			ImageView ivScreenshot = new ImageView (this.getContext ());
			ivScreenshot.setImageResource (res_screenshot);
			ivScreenshot.setScaleType (ImageView.ScaleType.CENTER_INSIDE);

			llScreenshots.addView (ivScreenshot);
		}

		ThemePreferencesButtonClickListener clickListener = new ThemePreferencesButtonClickListener (this.parent, this.prefs);
		btnApplyTheme.setOnClickListener (clickListener);
		btnApplyTheme.setTag (theme);

		return view;
	}
}
