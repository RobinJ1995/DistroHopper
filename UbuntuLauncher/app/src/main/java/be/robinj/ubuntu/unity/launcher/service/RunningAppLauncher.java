package be.robinj.ubuntu.unity.launcher.service;

import android.os.Build;
import android.widget.ImageView;
import android.widget.LinearLayout;

import be.robinj.ubuntu.App;
import be.robinj.ubuntu.R;

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

			if (Build.VERSION.SDK_INT >= 11)
				imgIcon.setAlpha (0.9F);
		}
	}
}
