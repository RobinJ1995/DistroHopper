package be.robinj.distrohopper.desktop.dash.lens;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;

import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;

/**
 * Created by robin on 4/11/14.
 */
public abstract class Lens
{
	private static final int DEFAULT_MAX_RESULTS = 20;
	private static final int CONNECT_TIMEOUT_MS = 10000;
	private static final int READ_TIMEOUT_MS = 10000;

	protected Context context;

	protected Drawable icon;

	protected Lens (Context context)
	{
		this.context = context;
	}

	public List<LensSearchResult> search (final String str) throws IOException, JSONException {
		return this.search(str, DEFAULT_MAX_RESULTS);
	}

	public abstract List<LensSearchResult> search (final String str, final int maxResults) throws IOException, JSONException;

	/**
	 * Searches and groups the results into one or more named collections, each
	 * shown as its own section in the dash. Most lenses return a single
	 * collection wrapping {@link #search(String, int)}; the InstalledApps lens
	 * returns one per profile (personal/work profile). maxResults applies
	 * per collection.
	 */
	public List<LensSearchResultCollection> searchCollections (final String str, final int maxResults) throws IOException, JSONException
	{
		return Collections.singletonList (
				new LensSearchResultCollection (this, this.search (str, maxResults)));
	}

	public abstract String getName ();

	public abstract String getDescription ();

	/**
	 * How expensive this lens is to search, which drives scheduling in
	 * SearchLoader (LOCAL lenses run on every keystroke; IO and NETWORK lenses
	 * are debounced). Defaults to NETWORK; cheap lenses override.
	 */
	public LensType getType ()
	{
		return LensType.NETWORK;
	}

	public Drawable getIcon ()
	{
		return this.icon;
	}

	public int getMinSDKVersion ()
	{
		return -1;
	}

	/**
	 * Runtime permissions this lens needs to deliver results. Lenses missing
	 * any of these are disabled by default; enabling one re-requests them.
	 */
	public String[] requiredPermissions ()
	{
		return new String[0];
	}

	public void onClick (String url)
	{
		if (url.startsWith ("http://") || url.startsWith ("https://"))
		{
			Intent intent = new Intent (Intent.ACTION_VIEW);
			intent.setData (Uri.parse (url));
			intent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK);

			this.context.startActivity (intent);
		}
		else if (url.startsWith ("error://"))
		{
			String message = url.substring ("error://".length ());

			this.showDialog (message, true);
		}
		else if (url.startsWith ("message://"))
		{
			String message = url.substring ("message://".length ());

			this.showDialog (message, false);
		}
	}

	public void onClick (String url, Object obj)
	{
		if (obj == null) {
			this.onClick(url);
		}
	}

	public void onLongClick (String url, Object obj, View view)
	{
		if (obj == null) {
			this.onClick(url);
		}
	}

	protected URLConnection openConnection (String url) throws IOException
	{
		URLConnection connection = new URL (url).openConnection ();
		connection.setConnectTimeout (CONNECT_TIMEOUT_MS);
		connection.setReadTimeout (READ_TIMEOUT_MS);

		return connection;
	}

	protected String downloadStr (String url) throws IOException
	{
		try (BufferedReader reader = new BufferedReader (new InputStreamReader (this.openConnection (url).getInputStream ())))
		{
			StringBuilder str = new StringBuilder ();
			String line = null;

			while ((line = reader.readLine ()) != null)
				str.append (line);

			return str.toString ();
		}
	}

	protected Drawable downloadImage (String url) throws IOException
	{
		try (InputStream in = new BufferedInputStream (this.openConnection (url).getInputStream ());
			ByteArrayOutputStream out = new ByteArrayOutputStream ())
		{
			byte[] buffer = new byte[1024];

			int x = 0;

			while ((x = in.read (buffer)) != -1)
				out.write (buffer, 0, x);

			byte[] imageBytes = out.toByteArray ();

			return new BitmapDrawable (BitmapFactory.decodeByteArray (imageBytes, 0, imageBytes.length));
		}
	}

	protected void showDialog (String message)
	{
		this.showDialog (message, false);
	}

	protected void showDialog (String message, boolean error)
	{
		AlertDialog.Builder dlg;
		dlg = new AlertDialog.Builder (this.context, error ? AlertDialog.THEME_HOLO_DARK : AlertDialog.THEME_HOLO_LIGHT);
		dlg.setMessage (message);
		dlg.setCancelable (true);
		dlg.setNeutralButton ("OK", null);

		dlg.show ();
	}
}