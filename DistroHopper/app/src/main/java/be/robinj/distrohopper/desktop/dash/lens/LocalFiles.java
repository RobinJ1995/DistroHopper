package be.robinj.distrohopper.desktop.dash.lens;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.robinj.distrohopper.R;

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

	@TargetApi (Build.VERSION_CODES.HONEYCOMB)
	public List<LensSearchResult> search (final String str, final int maxResults) throws IOException, JSONException
	{
		List<LensSearchResult> results = new ArrayList<LensSearchResult> ();
		int nResults = 0;

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

			if (++nResults >= maxResults) {
				break;
			}
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
			intent.setDataAndType (Uri.parse (file.getPath()), mime);
			intent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK);

			this.context.startActivity (intent);
		}
		catch (ActivityNotFoundException ex)
		{
			this.showDialog ("It looks like you don't have any apps installed that can open this type of file.", false);
		}
	}
}
