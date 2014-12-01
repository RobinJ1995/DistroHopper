package be.robinj.ubuntu.unity.dash.lens;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.webkit.MimeTypeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import be.robinj.ubuntu.App;
import be.robinj.ubuntu.R;

/**
 * Created by robin on 4/11/14.
 */
public class LocalFiles extends Lens
{
	public LocalFiles (Context context)
	{
		super (context);

		this.icon = context.getResources ().getDrawable (R.drawable.dash_search_lens_localfiles);
	}

	public String getName ()
	{
		return "Local files";
	}

	public String getDescription ()
	{
		return "Search results for files on your device";
	}

	@Override
	public int getMinSDKVersion ()
	{
		return 11;
	}

	public List<LensSearchResult> search (String str) throws IOException, JSONException
	{
		List<LensSearchResult> results = new ArrayList<LensSearchResult> ();

		String[] projection = new String[]
		{
			MediaStore.Files.FileColumns.DATA
		};
		String selection = MediaStore.Files.FileColumns.TITLE + " LIKE '%" + str.replace ("'", "''") + "%'";
		Cursor cursor = this.context.getContentResolver ().query (MediaStore.Files.getContentUri ("external"), projection, selection, null, null);

		while (cursor.moveToNext ())
		{
			String path = cursor.getString (0);
			File file = new File (path);

			LensSearchResult result = new LensSearchResult (this.context, file.getName (), file.toString (), this.icon);

			results.add (result);
		}

		return results;
	}

	@Override
	public void onClick (String url)
	{
		try
		{
			File file = new File (url);

			String extension = MimeTypeMap.getFileExtensionFromUrl (url);
			String mime = "*/*";

			if (extension != null)
			{
				MimeTypeMap map = MimeTypeMap.getSingleton ();
				mime = map.getMimeTypeFromExtension (extension);
			}

			Intent intent = new Intent ();
			intent.setAction (Intent.ACTION_VIEW);
			intent.setDataAndType (Uri.fromFile (file), mime);
			intent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK);

			this.context.startActivity (intent);
		}
		catch (ActivityNotFoundException ex)
		{
			this.showDialog ("It looks like you don't have any apps installed that can open this type of file.", false);
		}
	}
}
