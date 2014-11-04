package be.robinj.ubuntu.unity.dash.lens;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

/**
 * Created by robin on 4/11/14.
 */
public abstract class Lens
{
	public abstract List<LensSearchResult> search (String str) throws IOException, JSONException;

	protected String downloadStr (String url) throws IOException, JSONException
	{
		URL urlObj = new URL (url);
		InputStreamReader streamReader = new InputStreamReader (urlObj.openStream ());
		BufferedReader reader = new BufferedReader (streamReader);

		StringBuilder str = new StringBuilder ();
		String line = null;

		while ((line = reader.readLine ()) != null)
			str.append (line);

		reader.close ();

		return str.toString ();
	}
}
