package be.robinj.distrohopper.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.AppManager;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.HomeActivity;
import be.robinj.distrohopper.R;
import be.robinj.distrohopper.dev.Log;

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
	public void onReceive (Context context, Intent intent)
	{
		try
		{
			AppManager appManager = this.parent.getAppManager ();
			if (appManager != null) // If AsyncLoadApps isn't finished then there is no AppManager yet. In theory this class should only be instantiated once AsyncLoadApps is finished, but it doesn't hurt to make sure. //
			{
				String action = intent.getAction ();
				String friendlyAction = null;
				String packageName = intent.getData ().getEncodedSchemeSpecificPart ();
				boolean replacing = intent.getBooleanExtra (Intent.EXTRA_REPLACING, false);
				Resources res = context.getResources ();

				if (! replacing)
				{
					if (action.equals (res.getString (R.string.intent_action_package_added_legacy)) || action.equals (res.getString (R.string.intent_action_package_added)))
					{
						friendlyAction = "added";

						for (ResolveInfo resInf : appManager.queryInstalledApps (packageName))
						{
							final App app = new App(context, appManager, resInf);
							appManager.add (app, true, true);
						}
					}
					else if (action.equals (res.getString (R.string.intent_action_package_removed)))
					{
						friendlyAction = "removed";

						for (App app : appManager.findAppsByPackageName (packageName))
							appManager.remove (app);
					}
				}
				else
				{
					friendlyAction = "being replaced";
				}

				Log.getInstance ().v (this.getClass ().getSimpleName (), "Package " + friendlyAction + ": " + packageName);
			}
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this.parent);
		}
	}
}
