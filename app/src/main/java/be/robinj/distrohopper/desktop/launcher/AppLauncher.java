package be.robinj.distrohopper.desktop.launcher;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.DependencyContainer;
import be.robinj.distrohopper.theme.Theme;
import be.robinj.distrohopper.R;

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
		int width = LauncherIconGrid.iconSizePx (this.getContext ());
		int height = LauncherIconGrid.iconHeightPx (this.getContext ());

		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams (width, height);

		this.setLayoutParams (layoutParams);

		this.applyTheme ();
	}

	protected void applyTheme ()
	{
		final Theme theme = DependencyContainer.of (this.getContext ()).getThemeManager ().getCurrent ();

		ViewGroup llBackground = (ViewGroup) this.findViewById (R.id.llBackground);
		llBackground.setBackgroundResource (theme.launcher_applauncher_background);

		final int margin = this.getResources ().getDimensionPixelSize (theme.launcher_applauncher_margin);
		final int marginEdge = this.getResources ().getDimensionPixelSize (theme.launcher_applauncher_margin_edge);
		final LinearLayout.LayoutParams llBackground_layoutParams =
				(LinearLayout.LayoutParams) llBackground.getLayoutParams ();
		llBackground_layoutParams.setMargins (0, marginEdge, margin, marginEdge);
		llBackground.setLayoutParams (llBackground_layoutParams);

		ViewGroup llGradient = (ViewGroup) this.findViewById (R.id.llGradient);
		llGradient.setBackgroundResource (theme.launcher_applauncher_gradient);

		if (this.getId () != R.id.lalSpinner)
		{
			ImageView imgRunning = (ImageView) this.findViewById (R.id.imgRunning);
			imgRunning.setImageResource (theme.launcher_applauncher_running);
		}

		if (! this.getResources ().getBoolean (theme.launcher_applauncher_backgroundcolour_dynamic))
			this.setColour (ContextCompat.getColor (this.getContext (), theme.launcher_applauncher_backgroundcolour));
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