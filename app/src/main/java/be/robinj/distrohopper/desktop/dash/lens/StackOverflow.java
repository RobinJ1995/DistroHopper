package be.robinj.distrohopper.desktop.dash.lens;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import be.robinj.distrohopper.R;

/**
 * Created by robin on 4/11/14.
 */
public class StackOverflow extends Lens
{
	private final String API = "https://api.stackexchange.com/2.2/search?order=desc&sort=activity&intitle={:QUERY:}&site=stackoverflow";

	public StackOverflow (Context context)
	{
		super (context);

		this.icon = context.getResources ().getDrawable (R.drawable.dash_search_lens_stackoverflow);
	}

	public String getName ()
	{
		return "Stack Overflow";
	}

	public String getDescription ()
	{
		return "Stack Overflow search results";
	}

	public List<LensSearchResult> search (final String str, final int maxResults) throws IOException, JSONException
	{
		String apiResults = this.downloadStr (this.API.replace ("{:QUERY:}", URLEncoder.encode (str, "UTF-8")));

		JSONObject json = new JSONObject (apiResults);
		JSONArray items = json.getJSONArray ("items");
		List<LensSearchResult> results = new ArrayList<LensSearchResult> ();

		int nResults = 0;

		for (int i = 0; i < items.length (); i++)
		{
			JSONObject item = items.getJSONObject (i);

			if (item.has ("title") && item.has ("link"))
			{
				LensSearchResult result = new LensSearchResult (this.context, item.getString ("title"), item.getString ("link"), this.icon);

				results.add (result);

				if (++nResults >= maxResults) {
					return results;
				}
			}
		}

		return results;
	}
}
