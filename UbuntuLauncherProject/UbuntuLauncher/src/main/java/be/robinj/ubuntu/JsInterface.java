package be.robinj.ubuntu;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import java.util.List;

/**
 * Created by robin on 10/19/13.
 */

public class JsInterface
{
	public static Html html;
	private MainActivity parent;
	private WebView webView;
	private SharedPreferences prefs;
	private WallpaperExt wallpaper;
	private AppManager installedApps;
	private AppManager pinnedApps;
	private Dash dash;
	private DebugBenchmark bench = new DebugBenchmark ();
	private KeyValuePair cache = new KeyValuePair ();

	public JsInterface (MainActivity parent, WebView webView, SharedPreferences prefs)
	{
		this.parent = parent;
		this.webView = webView;
		this.prefs = prefs;

		this.wallpaper = new WallpaperExt ();
		this.installedApps = AppManager.installedApps (parent);
		this.pinnedApps = new AppManager ();
		this.loadPinnedApps ();
	}

	@JavascriptInterface
	public boolean isRunningJellyBean ()
	{
		if (Build.VERSION.SDK_INT < 16)
			return false;
		else
			return true;
	}

	@JavascriptInterface
	public void openInBrowser (String url)
	{
		this.parent.openInBrowser (url);
	}

	@JavascriptInterface
	public void openMenu ()
	{
		this.parent.openOptionsMenu ();
	}

	@JavascriptInterface
	public void runJs (String script)
	{
		this.parent.runJs (script);
	}

	@JavascriptInterface
	public WallpaperExt getWallpaper ()
	{
		return this.wallpaper;
	}

	@JavascriptInterface
	public AppManager getInstalledApps ()
	{
		return this.installedApps;
	}

	@JavascriptInterface
	public List<AppLauncherSimplified> getInstalledAppsSimplified ()
	{
		return this.installedApps.toAppLauncherSimplifiedList ();
	}

	@JavascriptInterface
	public String getInstalledAppsHtml ()
	{
		if (this.cache.exists ("installedAppsHtml"))
		{
			return this.cache.get ("installedAppsHtml");
		}
		else
		{
			String html = this.getInstalledApps ().toHtml ("");
			this.cache.set ("installedAppsHtml", html);

			return html;
		}
	}

	@JavascriptInterface
	public String getInstalledAppInfoHtml (int index)
	{
		return this.getInstalledApps ().get (index).infoToHtml ();
	}

	@JavascriptInterface
	public AppManager getPinnedApps ()
	{
		return this.pinnedApps;
	}

	@JavascriptInterface
	public String getPinnedAppsHtml (String htmlClass)
	{
		return this.pinnedApps.toHtml (htmlClass, true);
	}

	@JavascriptInterface
	public String getPinnedAppInfoHtml (int index)
	{
		return this.getPinnedApps ().get (index).infoToHtml ();
	}

	@JavascriptInterface
	public void makeDash ()
	{
		this.dash = new Dash (this.getInstalledApps ());
	}

	@JavascriptInterface
	public Dash getDash ()
	{
		return this.dash;
	}

	@JavascriptInterface
	public int getOrientation ()
	{
		// Portrait // 1 //
		// Landscape // 2 //
		return this.parent.getResources ().getConfiguration ().orientation;
	}

	@JavascriptInterface
	public int pinApp (int index)
	{
		AppLauncher app = this.installedApps.get (index);
		this.pinnedApps.add (app);

		this.showToast (app.getLabel () + " was pinned to the Launcher.");

		this.savePinnedApps ();

		return this.pinnedApps.indexOf (app);
	}

	@JavascriptInterface
	public void unpinApp (int index)
	{
		AppLauncher app = this.pinnedApps.get (index);
		this.pinnedApps.remove (index);

		this.savePinnedApps ();

		this.showToast (app.getLabel () + " was unpinned from the Launcher.");
	}

	@JavascriptInterface
	public void savePinnedApps ()
	{
		String json = this.getPinnedApps ().toJSON ();

		Editor editor = this.prefs.edit ();
		editor.putString ("pinnedApps", json);
		editor.apply (); // apply () async (= faster) but ignores failures, commit sync (= slower) but returns false on failure //
	}

	@JavascriptInterface
	public void loadPinnedApps ()
	{
		String json = this.prefs.getString ("pinnedApps", null);
		if (json != null)
			this.pinnedApps = AppManager.fromJSON (json);
	}

	@JavascriptInterface
	public void launchPinnedApp (int index)
	{
		this.pinnedApps.get (index).launch ();
	}

	@JavascriptInterface
	public void launchApp (int index)
	{
		this.installedApps.get (index).launch ();
	}

	@JavascriptInterface
	public List<Integer> searchApps (String pattern)
	{
		return this.installedApps.find (pattern);
	}

	@JavascriptInterface
	public SystemSettings getSystemSettings ()
	{
		return new SystemSettings (this.parent);
	}

	@JavascriptInterface
	public void showToast (String toast)
	{
		Toast.makeText (this.parent, toast, Toast.LENGTH_SHORT).show ();
	}

	@JavascriptInterface
	public void appQuit ()
	{
		this.parent.finish ();
	}

	@JavascriptInterface
	public void debug (String anything)
	{
		this.debug (anything, false);
	}

	@JavascriptInterface
	public void debug (String anything, boolean endBenchmark)
	{
		if (endBenchmark)
			this.bench.end ();
		else
			this.bench.checkpoint ();

		String something = anything;
	}
}
