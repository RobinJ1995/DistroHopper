package be.robinj.ubuntu.unity.dash.lens;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;

import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

/**
 * Created by robin on 4/11/14.
 */
public abstract class Lens
{
	protected Context context;

	protected Drawable icon;

	protected Lens (Context context)
	{
		this.context = context;
	}

	public abstract List<LensSearchResult> search (String str) throws IOException, JSONException;

	public abstract String getName ();

	public abstract String getDescription ();

	public Drawable getIcon ()
	{
		return this.icon;
	}

	public int getMinSDKVersion ()
	{
		return -1;
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
		else
		{
			throw new UnsupportedOperationException ();
		}
	}

	public void onClick (String url, Object obj)
	{
		if (obj == null)
			this.onClick (url);
		else
			throw new UnsupportedOperationException ();
	}

	public void onLongClick (String url)
	{
		throw new UnsupportedOperationException ();
	}

	public void onLongClick (String url, Object obj)
	{
		if (obj == null)
			this.onClick (url);
		else
			throw new UnsupportedOperationException ();
	}

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

	protected Drawable downloadImage (String url) throws IOException, JSONException
	{
		URL urlObj = new URL (url);
		InputStream in = new BufferedInputStream (urlObj.openStream ());
		ByteArrayOutputStream out = new ByteArrayOutputStream ();
		byte[] buffer = new byte[1024];

		int x = 0;

		while ((x = in.read (buffer)) != -1)
			out.write (buffer, 0, x);

		out.close ();
		in.close ();

		byte[] imageBytes = out.toByteArray ();
		Drawable image = new BitmapDrawable (BitmapFactory.decodeByteArray (imageBytes, 0, imageBytes.length));

		return image;
	}

	protected void showDialog (String message)
	{
		this.showDialog (message, false);
	}

	protected void showDialog (String message, boolean error)
	{
		AlertDialog.Builder dlg;
		if (Build.VERSION.SDK_INT >= 11)
			dlg = new AlertDialog.Builder (this.context, error ? AlertDialog.THEME_HOLO_DARK : AlertDialog.THEME_HOLO_LIGHT);
		else
			dlg = new AlertDialog.Builder (this.context);

		dlg.setMessage (message);
		dlg.setCancelable (true);
		dlg.setNeutralButton ("OK", null);

		dlg.show ();
	}
}