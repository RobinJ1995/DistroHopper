package be.robinj.distrohopper.desktop.dash.lens;

import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;

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

	public List<LensSearchResult> search (final String str, final int maxResults) throws IOException, JSONException
	{
		List<LensSearchResult> results = new ArrayList<LensSearchResult> ();

		String[] projection = new String[]
		{
			MediaStore.Files.FileColumns._ID,
			MediaStore.Files.FileColumns.DISPLAY_NAME,
			MediaStore.Files.FileColumns.DATA,
			MediaStore.Files.FileColumns.MIME_TYPE
		};
		String selection = MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE ?"
			+ " AND " + MediaStore.Files.FileColumns.DISPLAY_NAME + " NOT LIKE '.%'";
		String[] selectionArgs = new String[] { "%" + str + "%" };
		String sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC";
		Uri contentUri = MediaStore.Files.getContentUri ("external");

		try (Cursor cursor = this.context.getContentResolver ().query (contentUri, projection, selection, selectionArgs, sortOrder))
		{
			if (cursor == null) {
				return results;
			}

			while (cursor.moveToNext () && results.size () < maxResults)
			{
				long id = cursor.getLong (0);
				String name = cursor.getString (1);
				String path = cursor.getString (2);
				String mime = cursor.getString (3);

				if (name == null)
				{
					name = path == null ? Long.toString (id) : new File (path).getName ();
				}

				if (name.startsWith (".")) {
					continue;
				}

				Drawable icon = iconForMime (mime);
				Uri fileUri = ContentUris.withAppendedId (contentUri, id);

				results.add (new LensSearchResult (this.context, name, fileUri.toString (), icon));
			}
		}

		return results;
	}

	private Drawable iconForMime (String mime)
	{
		if (mime == null) {
			return this.context.getResources ().getDrawable (R.drawable.ic_file_generic);
		}
		if (mime.startsWith ("image/")) {
			return this.context.getResources ().getDrawable (R.drawable.ic_file_image);
		}
		if (mime.startsWith ("video/")) {
			return this.context.getResources ().getDrawable (R.drawable.ic_file_video);
		}
		if (mime.startsWith ("audio/")) {
			return this.context.getResources ().getDrawable (R.drawable.ic_file_audio);
		}
		if (mime.startsWith ("text/") || mime.equals ("application/pdf")
				|| mime.startsWith ("application/msword")
				|| mime.startsWith ("application/vnd.openxmlformats-officedocument")
				|| mime.startsWith ("application/vnd.oasis.opendocument")) {
			return this.context.getResources ().getDrawable (R.drawable.ic_file_document);
		}
		return this.context.getResources ().getDrawable (R.drawable.ic_file_generic);
	}

	@Override
	public void onClick (String url)
	{
		try
		{
			Uri uri = Uri.parse (url);

			String mime = this.context.getContentResolver ().getType (uri);
			if (mime == null) {
				mime = "*/*";
			}

			Intent intent = new Intent ();
			intent.setAction (Intent.ACTION_VIEW);
			intent.setDataAndType (uri, mime);
			intent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);

			this.context.startActivity (intent);
		}
		catch (ActivityNotFoundException ex)
		{
			this.showDialog ("It looks like you don't have any apps installed that can open this type of file.", false);
		}
	}
}
