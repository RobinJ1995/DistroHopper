package be.robinj.distrohopper.desktop.dash.lens;

import android.content.Context;
import android.os.UserHandle;
import android.view.View;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.AppManager;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.R;
import be.robinj.distrohopper.Profiles;
import be.robinj.distrohopper.desktop.dash.AppLauncherLongClickListener;

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
		return this.toResults (this.apps.search (str, maxResults));
	}

	/**
	 * One collection per profile, so work-profile apps get their own
	 * section — effectively a separate lens per profile, while remaining a
	 * single lens in the preferences.
	 */
	@Override
	public List<LensSearchResultCollection> searchCollections (final String str, final int maxResults) throws IOException, JSONException
	{
		final List<UserHandle> profiles = this.apps.getProfiles ();
		final List<LensSearchResultCollection> collections = new ArrayList<> ();

		for (final UserHandle profile : profiles)
		{
			final List<App> appResults = this.apps.searchProfile (str, maxResults, profile);

			if (appResults.isEmpty ())
				continue;

			final String name = profiles.size () > 1
					? this.context.getString (R.string.lens_profile_section,
							this.getName (), Profiles.label (this.context, profile))
					: this.getName ();

			collections.add (new LensSearchResultCollection (this, name, this.toResults (appResults)));
		}

		return collections;
	}

	private List<LensSearchResult> toResults (final List<App> appResults)
	{
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
	public void onLongClick (String url, Object obj, View view)
	{
		try
		{
			// Same as a long press on the dash grid: drag to the launcher to
			// pin at the drop position, or to move an already pinned icon //
			AppLauncherLongClickListener.startAppDrag (view, (App) obj);
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this.context);
		}
	}
}
