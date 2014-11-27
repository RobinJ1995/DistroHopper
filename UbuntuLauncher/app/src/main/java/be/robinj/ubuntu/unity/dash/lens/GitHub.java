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

import be.robinj.ubuntu.R;

/**
 * Created by robin on 4/11/14.
 */
public class GitHub extends Lens
{
	private final String API = "https://api.github.com/search/repositories?q={:QUERY:}";

	public GitHub (Context context)
	{
		super (context);

		this.icon = context.getResources ().getDrawable (R.drawable.dash_search_lens_github);
	}

	public String getName ()
	{
		return "GitHub";
	}

	public String getDescription ()
	{
		return "GitHub repository search results";
	}

	public List<LensSearchResult> search (String str) throws IOException, JSONException
	{
		String apiResults = this.downloadStr (this.API.replace ("{:QUERY:}", URLEncoder.encode (str, "UTF-8")));

		JSONObject json = new JSONObject (apiResults);
		JSONArray items = json.getJSONArray ("items");
		List<LensSearchResult> results = new ArrayList<LensSearchResult> ();

		for (int i = 0; i < items.length (); i++)
		{
			JSONObject item = items.getJSONObject (i);

			if (item.has ("full_name") && item.has ("html_url"))
			{
				LensSearchResult result = new LensSearchResult (this.context, item.getString ("full_name"), item.getString ("html_url"), this.icon);

				results.add (result);
			}
		}

		return results;
	}
}
