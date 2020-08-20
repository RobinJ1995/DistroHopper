package be.robinj.distrohopper.desktop.launcher.service;

import android.widget.ImageView;
import android.widget.LinearLayout;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.R;

/**
 * Created by robin on 03/09/14.
 */
public class RunningAppLauncher extends AppLauncher
{
	public RunningAppLauncher (LauncherService parent, App app)
	{
		super (parent, app);
	}

	@Override
	public void init ()
	{
		super.init ();

		this.setRunning (true);
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
