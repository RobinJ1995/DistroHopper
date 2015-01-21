package be.robinj.ubuntu.unity.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import be.robinj.ubuntu.App;
import be.robinj.ubuntu.HomeActivity;
import be.robinj.ubuntu.R;
import be.robinj.ubuntu.unity.AppIcon;

/**
 * Created by robin on 8/20/14.
 */
public class AppLauncher extends be.robinj.ubuntu.unity.AppLauncher
{
	private int colour;
	private boolean running;

	public AppLauncher (Context context, AttributeSet attrs)
	{
		super (context, attrs, R.layout.widget_launcher_applauncher, R.layout.widget_launcher_applauncher_special);
	}

	public AppLauncher (Context context, AttributeSet attrs, int layoutNormal, int layoutSpecial)
	{
		super (context, attrs, R.layout.widget_launcher_applauncher_spinner, layoutSpecial);
	}

	public AppLauncher (Context context, App app)
	{
		super (context, app, R.layout.widget_launcher_applauncher, R.layout.widget_launcher_applauncher_special);

		this.setTag (app);
	}

	@Override
	public void init ()
	{
		float density = this.getResources ().getDisplayMetrics ().density;

		SharedPreferences prefs = this.getContext ().getSharedPreferences ("prefs", Context.MODE_PRIVATE);
		int width = (int) ((float) (48 + prefs.getInt ("launchericon_width", 36)) * density);
		int height = width - (int) (4F * density);

		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams (width, height);

		this.setLayoutParams (layoutParams);

		this.applyTheme ();
	}

	private void applyTheme ()
	{
		if (this.getId () == R.id.lalBfb)
		{
			Drawable bfbImage = this.getContext ().getResources ().getDrawable (HomeActivity.theme.launcher_bfb_image);
			this.setIcon (new AppIcon (bfbImage));
		}

		ViewGroup llBackground = (ViewGroup) this.findViewById (R.id.llBackground);
		llBackground.setBackgroundResource (HomeActivity.theme.launcher_applauncher_background);

		ViewGroup llGradient = (ViewGroup) this.findViewById (R.id.llGradient);
		llGradient.setBackgroundResource (HomeActivity.theme.launcher_applauncher_gradient);


		if (this.getId () != R.id.lalSpinner)
		{
			ImageView imgRunning = (ImageView) this.findViewById (R.id.imgRunning);
			imgRunning.setImageResource (HomeActivity.theme.launcher_applauncher_running);
		}
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

	protected void colourChanged ()
	{
		LinearLayout llBackground = (LinearLayout) this.findViewById (R.id.llBackground);
		GradientDrawable gd = (GradientDrawable) llBackground.getBackground ();
		gd.setColor (this.colour);
	}

	public boolean isRunning ()
	{
		return running;
	}

	public void setRunning (boolean running)
	{
		this.running = running;

		this.runningChanged ();
	}

	private void runningChanged ()
	{
		ImageView imgRunning = (ImageView) this.findViewById (R.id.imgRunning);
		imgRunning.setVisibility (this.running ? View.VISIBLE : View.INVISIBLE);
	}

	public void checkRunning ()
	{

	}
}