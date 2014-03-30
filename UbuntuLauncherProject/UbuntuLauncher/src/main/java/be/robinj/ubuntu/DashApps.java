package be.robinj.ubuntu;

import android.webkit.JavascriptInterface;

/**
 * Created by robin on 10/19/13.
 */
public class DashApps
{
    private AppManager manager;

    public DashApps (AppManager manager)
    {
        this.manager = manager;
    }

    @JavascriptInterface
    public void launchApp (int i)
    {
        this.manager.get (i).launch ();
    }

    @JavascriptInterface
    public void buildPage ()
    {
        StringBuilder buildStr = new StringBuilder ();

        for (short i = 0; i < this.manager.size (); i++)
            buildStr.append (this.manager.get (i).toHtml ("dashItem", new String[][] { { "index", Integer.toString (i) } } ));
    }
}
