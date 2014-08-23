package be.robinj.ubuntu;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

/**
 * Created by robin on 7/31/14.
 */
public class PackageManagerExt
{
	private PackageManager pacMan;

	public PackageManagerExt ()
	{
		this.pacMan = MainActivity.getContext ().getPackageManager ();
	}

	public Drawable recoverIcon (String packageName)
	{
		Intent launchIntent = this.pacMan.getLaunchIntentForPackage (packageName);
		ResolveInfo resInf = this.pacMan.resolveActivity (launchIntent, 0);

		return resInf.loadIcon (this.pacMan);
	}
}
