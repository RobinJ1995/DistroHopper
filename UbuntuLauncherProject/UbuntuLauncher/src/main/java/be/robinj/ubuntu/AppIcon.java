package be.robinj.ubuntu;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.webkit.JavascriptInterface;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created by robin on 28/10/13.
 */
public class AppIcon extends ImageExt
{
    private AppLauncher app;

    public AppIcon (Context context, AppLauncher app)
    {
        super (context, app.getIcon ().get ());

        this.app = app;
    }

    public AppIcon (Context context, ResolveInfo resInf, AppLauncher app)
    {
        super (context, resInf.loadIcon (context.getPackageManager ()));

        this.app = app;
    }

    @Override
    @JavascriptInterface
    public String getPath ()
    {
        String name = "app" + Integer.toString (this.app.getId ());
        String path = MainActivity.prefs.getString ("icon:" + name, null);
        boolean changed = false;

        if (path == null)
        {
            path = this.getPath (name);
            changed = true;
        }
        else
        {
            File file = new File (path);

            if (! file.exists ())
            {
                path = this.getPath (name);
                changed = true;
            }
        }

        if (changed)
        {
            SharedPreferences.Editor editor = MainActivity.prefs.edit ();
            editor.putString ("icon:" + name, path);

            editor.apply ();
        }

        return path;
    }
}
