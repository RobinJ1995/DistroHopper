package be.robinj.distrohopper.desktop.dash.lens;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import be.robinj.distrohopper.R;

/**
 * Searches the F-Droid catalogue of free and open source Android apps.
 *
 * Uses F-Droid's official full-text search API
 * ({@code https://search.f-droid.org/api/search_apps}), which returns a tidy
 * JSON list of apps ({@code name}, {@code summary}, {@code icon}, {@code url}).
 * Tapping a result opens the app's F-Droid page in the F-Droid client when it
 * is installed, otherwise in the browser.
 */
public class FDroid extends Lens
{
	private static final String FDROID_PACKAGE = "org.fdroid.fdroid";

	private final String API = "https://search.f-droid.org/api/search_apps?q={:QUERY:}&lang=en";

	// The package name lives in the result url, e.g. ".../packages/com.wire". //
	private static final Pattern PACKAGE_PATTERN = Pattern.compile ("packages/([^/?]+)");

	public FDroid (Context context)
	{
		super (context);

		this.icon = context.getResources ().getDrawable (R.drawable.dash_search_lens_fdroid);
	}

	public String getName ()
	{
		return "F-Droid";
	}

	public String getDescription ()
	{
		return "F-Droid free and open source app search results";
	}

	public List<LensSearchResult> search (final String str, final int maxResults) throws IOException, JSONException
	{
		String apiResults = this.fetchSearch (this.API.replace ("{:QUERY:}", URLEncoder.encode (str, "UTF-8")));

		JSONObject json = new JSONObject (apiResults);
		JSONArray apps = json.getJSONArray ("apps");
		List<LensSearchResult> results = new ArrayList<> ();

		for (int i = 0; i < apps.length (); i++)
		{
			JSONObject app = apps.getJSONObject (i);

			if (! app.has ("name") || ! app.has ("url")) {
				continue;
			}

			String url = app.getString ("url");
			Matcher pkg = PACKAGE_PATTERN.matcher (url);
			if (! pkg.find ()) {
				continue;
			}

			Drawable resultIcon = app.has ("icon") ? this.iconFor (app.getString ("icon")) : this.icon;

			// Normalise to the language-neutral package page so the F-Droid //
			// client's deep link recognises it.                            //
			results.add (new LensSearchResult (this.context, app.getString ("name"),
				"https://f-droid.org/packages/" + pkg.group (1) + "/", resultIcon));

			if (results.size () >= maxResults) {
				break;
			}
		}

		return results;
	}

	/**
	 * Downloads an app's icon, falling back to the lens icon if it fails so that
	 * one bad image never drops the whole result list.
	 */
	private Drawable iconFor (String iconUrl)
	{
		try
		{
			return this.downloadImage (iconUrl);
		}
		catch (IOException ex)
		{
			return this.icon;
		}
	}

	/**
	 * Seam for tests: fetches the raw search API response. Overridable so tests
	 * can feed a canned fixture without hitting the network.
	 */
	protected String fetchSearch (String url) throws IOException
	{
		return this.downloadStr (url);
	}

	@Override
	public void onClick (String url)
	{
		// Open in the F-Droid client if it is installed, otherwise the browser. //
		Intent intent = new Intent (Intent.ACTION_VIEW, Uri.parse (url));
		intent.setPackage (FDROID_PACKAGE);
		intent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK);

		try
		{
			this.context.startActivity (intent);
		}
		catch (ActivityNotFoundException ex)
		{
			super.onClick (url);
		}
	}
}
