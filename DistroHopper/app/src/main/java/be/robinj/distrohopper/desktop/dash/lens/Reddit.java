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
public class Reddit extends Lens
{
	private final String API = "https://www.reddit.com/search.json?q={:QUERY:}";

	public Reddit (Context context)
	{
		super (context);

		this.icon = context.getResources ().getDrawable (R.drawable.dash_search_lens_reddit);
	}

	public String getName ()
	{
		return "Reddit";
	}

	public String getDescription ()
	{
		return "Reddit search results";
	}

	public List<LensSearchResult> search (String str) throws IOException, JSONException
	{
		String apiResults = this.downloadStr (this.API.replace ("{:QUERY:}", URLEncoder.encode (str, "UTF-8")));

		List<LensSearchResult> results = new ArrayList<LensSearchResult> ();
		JSONObject json = new JSONObject (apiResults);

		if (json.has ("data"))
		{
			JSONObject data = json.getJSONObject ("data");
			JSONArray items = data.getJSONArray ("children");

			for (int i = 0; i < items.length (); i++)
			{
				JSONObject item = items.getJSONObject (i);
				if (item.has ("data"))
				{
					JSONObject itemData = item.getJSONObject ("data");

					if (itemData.has ("title") && itemData.has ("url"))
					{
						LensSearchResult result = new LensSearchResult (this.context, itemData.getString ("title"), itemData.getString ("url"), this.icon);

						results.add (result);
					}
				}
			}
		}

		return results;
	}
}
