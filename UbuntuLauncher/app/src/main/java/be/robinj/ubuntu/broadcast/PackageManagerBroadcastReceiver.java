package be.robinj.ubuntu.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import be.robinj.ubuntu.HomeActivity;

/**
 * Created by robin on 28/06/15.
 */
public class PackageManagerBroadcastReceiver extends BroadcastReceiver
{
	private HomeActivity parent;

	public PackageManagerBroadcastReceiver (HomeActivity parent)
	{
		this.parent = parent;
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		Log.v (this.getClass ().getSimpleName (), "Broadcast received");

		this.parent.installedAppsChanged ();
	}
}
