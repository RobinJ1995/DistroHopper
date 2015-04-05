package be.robinj.ubuntu.unity.dash;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import be.robinj.ubuntu.App;
import be.robinj.ubuntu.HomeActivity;
import be.robinj.ubuntu.R;
import be.robinj.ubuntu.unity.AppIcon;

/**
 * Created by robin on 8/20/14.
 */
public class AppLauncher extends be.robinj.ubuntu.unity.AppLauncher
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