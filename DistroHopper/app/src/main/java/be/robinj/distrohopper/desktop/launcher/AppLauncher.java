package be.robinj.distrohopper.desktop.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.HomeActivity;
import be.robinj.distrohopper.R;
import be.robinj.distrohopper.preferences.Preference;
import be.robinj.distrohopper.preferences.Preferences;

/**
 * Created by robin on 8/20/14.
 */
public class AppLauncher extends be.robinj.distrohopper.desktop.AppLauncher
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

		SharedPreferences prefs = Preferences.getSharedPreferences(this.getContext(), Preferences.PREFERENCES);
		int width = (int) ((float) (48 + prefs.getInt (Preference.LAUNCHERICON_WIDTH.getName(), 36)) * density);
		int height = width - (int) (4F * density);

		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams (width, height);

		this.setLayoutParams (layoutParams);

		this.applyTheme ();
	}

	protected void applyTheme ()
	{
		ViewGroup llBackground = (ViewGroup) this.findViewById (R.id.llBackground);
		llBackground.setBackgroundResource (HomeActivity.theme.launcher_applauncher_background);

		ViewGroup llGradient = (ViewGroup) this.findViewById (R.id.llGradient);
		llGradient.setBackgroundResource (HomeActivity.theme.launcher_applauncher_gradient);

		if (this.getId () != R.id.lalSpinner)
		{
			ImageView imgRunning = (ImageView) this.findViewById (R.id.imgRunning);
			imgRunning.setImageResource (HomeActivity.theme.launcher_applauncher_running);
		}

		if (! this.getResources ().getBoolean (HomeActivity.theme.launcher_applauncher_backgroundcolour_dynamic))
			this.setColour (this.getResources ().getColor (HomeActivity.theme.launcher_applauncher_backgroundcolour));
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