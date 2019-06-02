package be.robinj.distrohopper.desktop.dash.lens;

import android.content.Context;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.AppManager;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.R;

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

	private InstalledApps (Context context)
	{
		super (context);

		this.icon = context.getResources ().getDrawable (R.mipmap.ic_launcher);
	}

	public String getName ()
	{
		return "Installed apps";
	}

	public String getDescription ()
	{
		return "Search installed apps";
	}

	public List<LensSearchResult> search (final String str, final int maxResults) throws IOException, JSONException
	{
		List<App> appResults = this.apps.search (str, maxResults);
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
		try
		{
			App app = (App) obj;

			this.apps.pin (app);
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this.context);
		}
	}
}
