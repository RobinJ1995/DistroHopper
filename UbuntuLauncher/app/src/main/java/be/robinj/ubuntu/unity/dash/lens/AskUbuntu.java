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
public class AskUbuntu extends Lens
{
	private final String API = "https://api.stackexchange.com/2.2/search?order=desc&sort=activity&intitle={:QUERY:}&site=askubuntu";

	private Context context;
	private Drawable icon;

	public AskUbuntu (Context context)
	{
		this.context = context;
		this.icon = context.getResources ().getDrawable (R.drawable.dash_search_lens_askubuntu);
	}

	public String getName ()
	{
		return "Ask Ubuntu";
	}

	public String getDescription ()
	{
		return "Ask Ubuntu search results";
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

			if (item.has ("title") && item.has ("link"))
			{
				LensSearchResult result = new LensSearchResult (this.context, item.getString ("title"), item.getString ("link"), this.icon);

				results.add (result);
			}
		}

		return results;
	}
}
