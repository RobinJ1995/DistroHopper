package be.robinj.distrohopper.desktop.dash.lens;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import be.robinj.distrohopper.R;

/**
 * Searches the Google Play Store for apps.
 *
 * Google Play has no official public search API, so this lens does what the
 * established third-party tooling does: it requests Play's own web search page
 * and parses the JSON embedded in its HTML (the {@code AF_initDataCallback}
 * script blocks). The page format is undocumented and can change at any time,
 * so parsing is deliberately tolerant and degrades gracefully (worst case: a
 * single "search Google Play" result) instead of throwing.
 */
public class GooglePlay extends Lens
{
	private final String API = "https://play.google.com/store/search?c=apps&q={:QUERY:}&hl=en&gl=us";

	// The detail URL embedded for every app result, e.g. ".../details?id=com.whatsapp". //
	private static final Pattern DETAIL_PATTERN = Pattern.compile ("details\\?id=([A-Za-z0-9._]+)");
	// The CDN that serves app icons (and screenshots). //
	private static final String ICON_HOST = "play-lh.googleusercontent.com";
	// Strings that look like dotted package names, used to reject them as titles. //
	private static final Pattern PACKAGE_LIKE = Pattern.compile ("^[a-z0-9]+(\\.[A-Za-z0-9_]+)+$");
	// Opaque tokens like "CAE=", "USD", "CgYKBENBRT0=" that are not human titles. //
	private static final Pattern TOKEN_LIKE = Pattern.compile ("^[A-Z0-9+/=_-]+$");

	public GooglePlay (Context context)
	{
		super (context);

		this.icon = context.getResources ().getDrawable (R.drawable.dash_search_lens_googleplay);
	}

	public String getName ()
	{
		return "Google Play Store";
	}

	public String getDescription ()
	{
		return "Google Play Store app search results";
	}

	public List<LensSearchResult> search (final String str, final int maxResults) throws IOException, JSONException
	{
		String html = this.fetchSearchHtml (this.API.replace ("{:QUERY:}", URLEncoder.encode (str, "UTF-8")));

		// packageName -> display title, in first-seen order. //
		Map<String, AppResult> apps = new LinkedHashMap<> ();

		for (String dataArray : extractDataArrays (html))
		{
			try
			{
				analyze (new JSONArray (dataArray), apps);
			}
			catch (JSONException ex)
			{
				// A block we can't parse is simply skipped; never fatal. //
			}

			if (apps.size () >= maxResults) {
				break;
			}
		}

		List<LensSearchResult> results = new ArrayList<> ();

		for (AppResult app : apps.values ())
		{
			results.add (new LensSearchResult (this.context, app.title,
				"market://details?id=" + app.packageName, this.iconFor (app.iconUrl)));

			if (results.size () >= maxResults) {
				break;
			}
		}

		// Graceful fallback: even if parsing yields nothing, hand back a single //
		// result that opens the Play Store search for the typed query.          //
		if (results.isEmpty ())
		{
			String encoded = URLEncoder.encode (str, "UTF-8");
			results.add (new LensSearchResult (this.context, "Search Google Play for “" + str + "”",
				"market://search?q=" + encoded + "&c=apps", this.icon));
		}

		return results;
	}

	/**
	 * Downloads the icon for a single app, falling back to the lens icon if the
	 * download fails so that one bad image never drops the whole result list.
	 */
	private Drawable iconFor (String iconUrl)
	{
		if (iconUrl == null) {
			return this.icon;
		}

		try
		{
			// Request a small icon to keep the download cheap. //
			String sized = iconUrl.contains ("=") ? iconUrl : iconUrl + "=s64";

			return this.downloadImage (sized);
		}
		catch (IOException ex)
		{
			return this.icon;
		}
	}

	/**
	 * Seam for tests: fetches the raw search HTML. Overridable so tests can feed
	 * a canned fixture without hitting the network.
	 */
	protected String fetchSearchHtml (String url) throws IOException
	{
		return this.downloadStr (url);
	}

	/**
	 * Extracts the {@code data:[...]} JSON array from every
	 * {@code AF_initDataCallback(...)} block in the page. Uses a string-literal
	 * aware bracket scanner rather than a regex so nested brackets and quotes do
	 * not trip it up.
	 */
	private static List<String> extractDataArrays (String html)
	{
		List<String> out = new ArrayList<> ();

		if (html == null) {
			return out;
		}

		final String marker = "AF_initDataCallback(";
		int from = 0;

		while (true)
		{
			int call = html.indexOf (marker, from);
			if (call < 0) {
				break;
			}

			from = call + marker.length ();

			int dataKey = html.indexOf ("data:", call);
			if (dataKey < 0) {
				break;
			}

			int open = html.indexOf ('[', dataKey);
			if (open < 0) {
				continue;
			}

			int close = matchingBracket (html, open);
			if (close > open) {
				out.add (html.substring (open, close + 1));
				from = close;
			}
		}

		return out;
	}

	/**
	 * Returns the index of the {@code ]} that closes the {@code [} at
	 * {@code open}, skipping over brackets that appear inside string literals.
	 */
	private static int matchingBracket (String s, int open)
	{
		int depth = 0;
		boolean inString = false;

		for (int i = open; i < s.length (); i++)
		{
			char c = s.charAt (i);

			if (inString)
			{
				if (c == '\\') {
					i++; // Skip the escaped character. //
				} else if (c == '"') {
					inString = false;
				}
			}
			else if (c == '"')
			{
				inString = true;
			}
			else if (c == '[')
			{
				depth++;
			}
			else if (c == ']')
			{
				depth--;
				if (depth == 0) {
					return i;
				}
			}
		}

		return -1;
	}

	/**
	 * Recursively walks the parsed JSON, emitting one app per minimal subtree
	 * that contains both an app detail URL and an icon URL. This is
	 * position-independent: it does not depend on the exact array indices Google
	 * uses (which drift over time), only on the structural pairing of a detail
	 * link with an icon.
	 */
	private static Aggregate analyze (Object node, Map<String, AppResult> apps)
	{
		if (node instanceof String)
		{
			return fromString ((String) node);
		}

		if (! (node instanceof JSONArray))
		{
			return new Aggregate ();
		}

		JSONArray array = (JSONArray) node;
		Aggregate agg = new Aggregate ();

		for (int i = 0; i < array.length (); i++)
		{
			Aggregate child = analyze (array.opt (i), apps);

			agg.emitted |= child.emitted;
			agg.hasDetail |= child.hasDetail;
			agg.hasIcon |= child.hasIcon;

			if (agg.packageName == null) agg.packageName = child.packageName;
			if (agg.iconUrl == null) agg.iconUrl = child.iconUrl;
			if (agg.title == null) agg.title = child.title;
		}

		// This node is the minimal one containing both a detail link and an //
		// icon iff no descendant already emitted an app from them.          //
		if (! agg.emitted && agg.hasDetail && agg.hasIcon && agg.packageName != null)
		{
			String title = agg.title != null ? agg.title : agg.packageName;

			if (! apps.containsKey (agg.packageName)) {
				apps.put (agg.packageName, new AppResult (agg.packageName, title, agg.iconUrl));
			}

			agg.emitted = true;
		}

		return agg;
	}

	private static Aggregate fromString (String s)
	{
		Aggregate agg = new Aggregate ();

		Matcher detail = DETAIL_PATTERN.matcher (s);
		if (detail.find ())
		{
			agg.hasDetail = true;
			agg.packageName = detail.group (1);
		}
		else if (s.contains (ICON_HOST))
		{
			agg.hasIcon = true;
			agg.iconUrl = s;
		}
		else if (isTitleLike (s))
		{
			agg.title = s;
		}

		return agg;
	}

	private static boolean isTitleLike (String s)
	{
		return ! s.isEmpty ()
			&& ! s.contains ("://")
			&& ! s.startsWith ("/")
			&& ! TOKEN_LIKE.matcher (s).matches ()
			&& ! PACKAGE_LIKE.matcher (s).matches ();
	}

	@Override
	public void onClick (String url)
	{
		if (url.startsWith ("market://"))
		{
			Intent intent = new Intent (Intent.ACTION_VIEW, Uri.parse (url));
			intent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK);

			try
			{
				this.context.startActivity (intent);
			}
			catch (ActivityNotFoundException ex)
			{
				// No Play Store app installed (e.g. de-Googled device): fall back to the web. //
				String web = url
					.replace ("market://details?id=", "https://play.google.com/store/apps/details?id=")
					.replace ("market://search?q=", "https://play.google.com/store/search?c=apps&q=");

				super.onClick (web);
			}
		}
		else
		{
			super.onClick (url);
		}
	}

	/** A parsed app result before it is turned into a {@link LensSearchResult}. */
	private static class AppResult
	{
		final String packageName;
		final String title;
		final String iconUrl;

		AppResult (String packageName, String title, String iconUrl)
		{
			this.packageName = packageName;
			this.title = title;
			this.iconUrl = iconUrl;
		}
	}

	/** Aggregated facts about a JSON subtree, bubbled up during {@link #analyze}. */
	private static class Aggregate
	{
		boolean emitted;
		boolean hasDetail;
		boolean hasIcon;
		String packageName;
		String iconUrl;
		String title;
	}
}
