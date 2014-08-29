package be.robinj.ubuntu.unity.launcher.service;

import android.content.Context;

import be.robinj.ubuntu.App;

/**
 * Created by robin on 8/28/14.
 */
public class AppLauncher extends be.robinj.ubuntu.unity.launcher.AppLauncher
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
