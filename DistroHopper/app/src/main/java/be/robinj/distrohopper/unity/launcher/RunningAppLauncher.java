package be.robinj.distrohopper.unity.launcher;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.widget.ImageView;
import android.widget.LinearLayout;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.HomeActivity;
import be.robinj.distrohopper.R;

/**
 * Created by robin on 03/09/14.
 */
public class RunningAppLauncher extends AppLauncher
{
	public RunningAppLauncher (Context context, App app)
	{
		super (context, app);
	}

	@Override
	public void init ()
	{
		super.init ();

		this.setRunning (true);
	}

	@Override
	protected void applyTheme ()
	{
		super.applyTheme ();

		if (! this.getResources ().getBoolean (HomeActivity.theme.launcher_applauncher_backgroundcolour_dynamic))
			this.setColour (this.getResources ().getColor (HomeActivity.theme.launcher_applauncher_backgroundcolour));
	}

	@Override
	protected void colourChanged ()
	{
		LinearLayout llBackground = (LinearLayout) this.findViewById (R.id.llBackground);
		GradientDrawable gd = (GradientDrawable) llBackground.getBackground ();
		gd.setColor (this.getColour ());
	}

	@Override
	protected void iconChanged ()
	{
		if (! this.isInEditMode ())
		{
			ImageView imgIcon = (ImageView) this.findViewById (R.id.imgIcon);
			imgIcon.setImageDrawable (this.getIcon ().getDrawable ());

			LinearLayout llBackground = (LinearLayout) this.findViewById (R.id.llBackground);
			if (llBackground != null && (! this.isSpecial ()))
				this.colourChanged ();
			
			imgIcon.setAlpha (0.9F);
		}
	}
}
