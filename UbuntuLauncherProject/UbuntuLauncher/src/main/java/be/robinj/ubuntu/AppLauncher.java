package be.robinj.ubuntu;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.webkit.JavascriptInterface;

public class AppLauncher
{
    private Context context;
    private String label;
    private AppIcon icon;
    private long timesLaunched = 0;
    private String packageName;
    private String activityName;
    private ResolveInfo resInf;
    private ApplicationInfo info;
	
	public AppLauncher (Context context)
	{
		this.context = context;
	}
	
	public AppLauncher (Context context, ResolveInfo resInf)
	{
		this.context = context;
		this.loadFromResolveInfo (resInf);
        this.info = this.resInf.activityInfo.applicationInfo;
	}

    @JavascriptInterface
	public String getLabel ()
	{
		return this.label;
	}

    @JavascriptInterface
	public AppIcon getIcon ()
	{
		return this.icon;
	}

    @JavascriptInterface
	public long getTimesLaunched ()
	{
		return this.timesLaunched;
	}

    @JavascriptInterface
    public ResolveInfo getResolveInfo () // Returns null if this.resInf is not set //
    {
        return this.resInf;
    }

    @JavascriptInterface
    public String getDataFolder ()
    {
        return this.info.dataDir;
    }

    @JavascriptInterface
    public String getDescription ()
    {
        PackageManager pacMan = this.context.getPackageManager ();
        CharSequence description = pacMan.getText(this.resInf.activityInfo.packageName, this.info.descriptionRes, this.info);

        return (description == null ? null : description.toString ());
    }

    @JavascriptInterface
    public int getTargetSdk ()
    {
        return this.info.targetSdkVersion;
    }

    @JavascriptInterface
	public void launch ()
	{
		this.launch (true);
	}

    @JavascriptInterface
	public void launch (boolean countLaunch)
	{
		if (countLaunch)
			this.timesLaunched++;
		
		ComponentName compName = new ComponentName (this.packageName, this.activityName);
		Intent intent = new Intent (Intent.ACTION_MAIN);
		intent.addCategory (Intent.CATEGORY_LAUNCHER);
		intent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		intent.setComponent (compName);
		
		this.context.startActivity (intent);
	}

    @JavascriptInterface
    public String serialize ()
    {
        return this.serialize (true, true);
    }

    @JavascriptInterface
    public String serialize (boolean includeIcon, boolean includeTimesLaunched)
    {
        String splitter = "::";

        StringBuilder builder = new StringBuilder ("-[");
        builder.append (this.packageName);
        builder.append (splitter);
        builder.append (this.activityName);
        builder.append (splitter);
        builder.append (this.label);
        builder.append (splitter);
        builder.append ((includeTimesLaunched ? this.timesLaunched : "#"));
        builder.append (splitter);
        builder.append ((includeIcon ? this.icon.getPath () : "#")); // Might cause trouble when the user clears cache //
        builder.append ("]-");

        return builder.toString ();
    }
	
	@Override
    @JavascriptInterface
    public int hashCode ()
    {
        return this.hashCode (true, true);
    }

    @JavascriptInterface
    public int hashCode (boolean includeIcon, boolean includeTimesLaunched)
    {
        return this.serialize (includeIcon, includeTimesLaunched).hashCode ();
    }

    @JavascriptInterface
	public boolean equals (AppLauncher compareTo)
	{
		boolean equal = false;
		
		if (this.hashCode () == compareTo.hashCode ())
			equal = true;
		
		return equal;
	}

    @JavascriptInterface
	public String toHtml (String htmlClass)
	{
		return this.toHtml (htmlClass, null, false);
	}

    @JavascriptInterface
	public String toHtml (String[][] data)
	{
		return this.toHtml ("", data, false);
	}

    @JavascriptInterface
    public String toHtml (String htmlClass, String[][] data)
    {
        return this.toHtml (htmlClass, data, false);
    }

    @JavascriptInterface
    public String toHtml (String htmlClass, String[][] data, boolean backgroundColour)
    {
        return Html.appLauncher (this, htmlClass, data, backgroundColour);
    }

    @JavascriptInterface
    public String infoToHtml ()
    {
        return Html.appInfo (this);
    }

    @JavascriptInterface
	protected void loadFromResolveInfo (ResolveInfo resInf)
	{
        this.resInf = resInf;
		this.loadFromResolveInfo (resInf, false);
	}

    @JavascriptInterface
    protected void loadFromResolveInfo (ResolveInfo resInf, boolean respectPreferredOrder)
	{
		this.label = resInf.loadLabel (this.context.getPackageManager ()).toString ();
        if (respectPreferredOrder)
            this.timesLaunched = resInf.preferredOrder;
        this.packageName = resInf.activityInfo.applicationInfo.packageName;
        this.activityName = resInf.activityInfo.name;
        this.resInf = resInf;
		this.icon = new AppIcon (this.context, resInf, this);
	}

    @JavascriptInterface
    public int getId ()
    {
        return this.hashCode (false, false);
    }
}
