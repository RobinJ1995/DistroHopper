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
public class DuckDuckGo extends Lens
{
	private final String API = "https://api.duckduckgo.com/?q={:QUERY:}&format=json";

	public DuckDuckGo (Context context)
	{
		super (context);

		this.icon = context.getResources ().getDrawable (R.drawable.dash_search_lens_duckduckgo);
	}

	public String getName ()
	{
		return "DuckDuckGo";
	}

	public String getDescription ()
	{
		return "DuckDuckGo search results";
	}

	public List<LensSearchResult> search (String str) throws IOException, JSONException
	{
		String apiResults = this.downloadStr (this.API.replace ("{:QUERY:}", URLEncoder.encode (str, "UTF-8")));

		JSONObject json = new JSONObject (apiResults);
		JSONArray relatedTopics = json.getJSONArray ("RelatedTopics");
		List<LensSearchResult> results = new ArrayList<LensSearchResult> ();

		for (int i = 0; i < relatedTopics.length (); i++)
		{
			JSONObject relatedTopic = relatedTopics.getJSONObject (i);

			if (relatedTopic.has ("Text") && relatedTopic.has ("FirstURL"))
			{
				LensSearchResult result = new LensSearchResult (this.context, relatedTopic.getString ("Text"), relatedTopic.getString ("FirstURL"), this.icon);

				results.add (result);
			}
			else if (relatedTopic.has ("Topics"))
			{
				JSONArray topics = relatedTopic.getJSONArray ("Topics");

				for (int j = 0; j < topics.length (); j++)
				{
					JSONObject topic = topics.getJSONObject (j);

					if (topic.has ("Text") && topic.has ("FirstURL"))
					{
						LensSearchResult result = new LensSearchResult (this.context, topic.getString ("Text"), topic.getString ("FirstURL"), this.icon);

						results.add (result);
					}
				}
			}
		}

		return results;
	}
}
