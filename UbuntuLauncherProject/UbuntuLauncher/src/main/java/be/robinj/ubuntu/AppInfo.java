package be.robinj.ubuntu;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.webkit.JavascriptInterface;

/**
 * Created by robin on 28/10/13.
 */
public class AppInfo extends AppLauncher
{
    public AppInfo (Context context)
    {
        super (context);
    }

    public AppInfo (Context context, ResolveInfo resInf)
    {
        super (context, resInf);
    }

    @JavascriptInterface
    public String toHtml ()
    {
        return Html.appInfo (this);
    }
}