package be.robinj.distrohopper.unity.dash;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.R;
import be.robinj.distrohopper.unity.AppIcon;

/**
 * Created by robin on 8/20/14.
 */
public class AppLauncher extends be.robinj.distrohopper.unity.AppLauncher
{
	private String name;
	private String description;
	private boolean special;
	private AppIcon icon;
	private View view;

	public AppLauncher (Context context, AttributeSet attrs)
	{
		super (context, attrs, R.layout.widget_dash_applauncher, R.layout.widget_dash_applauncher_special);
	}

	public AppLauncher (Context context, App app)
	{
		super (context, app, R.layout.widget_dash_applauncher, R.layout.widget_dash_applauncher_special);
	}
}