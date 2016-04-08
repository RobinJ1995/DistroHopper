package be.robinj.distrohopper.unity.launcher.service;

import be.robinj.distrohopper.App;

/**
 * Created by robin on 8/28/14.
 */
public class AppLauncher extends be.robinj.distrohopper.unity.launcher.AppLauncher
{
	private LauncherService parent;

	public AppLauncher (LauncherService parent, App app)
	{
		super (parent.getApplicationContext (), app);

		this.parent = parent;
	}

	public void launch ()
	{
		this.getApp ().launch ();

		this.parent.swipeLeft ();
	}
}
