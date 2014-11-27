package be.robinj.ubuntu.unity.dash.lens;

import android.content.Context;
import android.graphics.drawable.Drawable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import be.robinj.ubuntu.App;
import be.robinj.ubuntu.AppManager;
import be.robinj.ubuntu.R;

/**
 * Created by robin on 5/11/14.
 */
public class InstalledApps extends Lens
{
	private AppManager apps;

	public InstalledApps (Context context, AppManager apps)
	{
		this (context);

		this.apps = apps;
	}

	public InstalledApps (Context context)
	{
		super (context);

		this.icon = context.getResources ().getDrawable (R.drawable.ic_launcher);
	}

	public String getName ()
	{
		return "Installed apps";
	}

	public String getDescription ()
	{
		return "Search installed apps";
	}

	public List<LensSearchResult> search (String str) throws IOException, JSONException
	{
		List<App> appResults = this.apps.search (str, false);
		List<LensSearchResult> results = new ArrayList<LensSearchResult> ();

		for (App app : appResults)
			results.add (new LensSearchResult (this.context, app.getLabel (), app.getPackageName () + ":" + app.getActivityName (), app.getIcon ().getDrawable (), app));

		return results;
	}

	@Override
	public void onClick (String url, Object obj)
	{
		App app = (App) obj;

		app.launch ();
	}

	@Override
	public void onLongClick (String url, Object obj)
	{
		App app = (App) obj;

		this.apps.pin (app);
	}
}
