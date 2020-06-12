package be.robinj.distrohopper.desktop.dash;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.R;
import be.robinj.distrohopper.desktop.AppIcon;
import be.robinj.distrohopper.preferences.Preference;
import be.robinj.distrohopper.preferences.Preferences;

/**
 * Created by robin on 8/20/14.
 */
public class AppLauncher extends be.robinj.distrohopper.desktop.AppLauncher {

	public AppLauncher (Context context, AttributeSet attrs)
	{
		super (context, attrs, R.layout.widget_dash_applauncher, R.layout.widget_dash_applauncher_special);
	}

	public AppLauncher (Context context, App app)
	{
		super (context, app, R.layout.widget_dash_applauncher, R.layout.widget_dash_applauncher_special);
	}
}