package be.robinj.ubuntu.unity.launcher;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import be.robinj.ubuntu.App;
import be.robinj.ubuntu.R;

/**
 * Created by robin on 8/20/14.
 */
public class AppLauncher extends be.robinj.ubuntu.unity.AppLauncher
{
	private int colour;

	public AppLauncher (Context context, AttributeSet attrs)
	{
		super (context, attrs, R.layout.widget_launcher_applauncher, R.layout.widget_launcher_applauncher_special);
	}

	public AppLauncher (Context context, AttributeSet attrs, int layoutNormal, int layoutSpecial)
	{
		super (context, attrs, layoutNormal, layoutSpecial);
	}

	public AppLauncher (Context context, App app)
	{
		super (context, app, R.layout.widget_launcher_applauncher, R.layout.widget_launcher_applauncher_special);

		this.setTag (app);
	}

	public int getColour ()
	{
		return colour;
	}

	public void setColour (int colour)
	{
		this.colour = colour;

		this.colourChanged ();
	}

	private void colourChanged ()
	{
		LinearLayout llBackground = (LinearLayout) this.findViewById (R.id.llBackground);
		GradientDrawable gd = (GradientDrawable) llBackground.getBackground ();
		gd.setColor (this.colour);
	}
}