package be.robinj.distrohopper.desktop.dash.lens

import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import be.robinj.distrohopper.Permission
import be.robinj.distrohopper.R
import org.json.JSONException
import java.io.File
import java.io.IOException

class LocalFiles(context: Context) : Lens(context) {

	init {
		icon = context.resources.getDrawable(R.drawable.dash_search_lens_localfiles)
	}

	override fun getName() = "Local files"

	override fun getDescription() = "Search results for files on your device"

	override fun requiredPermissions(): Array<String> = Permission.storagePermissions()

	@Throws(IOException::class, JSONException::class)
	override fun search(str: String, maxResults: Int): List<LensSearchResult> {
		val results = mutableListOf<LensSearchResult>()

		val projection = arrayOf(
			MediaStore.Files.FileColumns._ID,
			MediaStore.Files.FileColumns.DISPLAY_NAME,
			MediaStore.Files.FileColumns.DATA,
			MediaStore.Files.FileColumns.MIME_TYPE,
		)
		val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?" +
			" AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} NOT LIKE '.%'"
		val selectionArgs = arrayOf("%$str%")
		val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
		val contentUri = MediaStore.Files.getContentUri("external")

		context.contentResolver.query(contentUri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
			while (cursor.moveToNext() && results.size < maxResults) {
				val id = cursor.getLong(0)
				val name = cursor.getString(1)
					?: cursor.getString(2)?.let { File(it).name }
					?: id.toString()

				if (name.startsWith(".")) continue

				val icon = iconForMime(cursor.getString(3))
				val fileUri = ContentUris.withAppendedId(contentUri, id)
				results += LensSearchResult(context, name, fileUri.toString(), icon)
			}
		}

		return results
	}

	internal fun mimeTypeIconRes(mime: String?): Int = when {
		mime == null -> R.drawable.ic_file_generic
		mime.startsWith("image/") -> R.drawable.ic_file_image
		mime.startsWith("video/") -> R.drawable.ic_file_video
		mime.startsWith("audio/") -> R.drawable.ic_file_audio
		mime.startsWith("text/")
			|| mime == "application/pdf"
			|| mime.startsWith("application/msword")
			|| mime.startsWith("application/vnd.openxmlformats-officedocument")
			|| mime.startsWith("application/vnd.oasis.opendocument") -> R.drawable.ic_file_document
		else -> R.drawable.ic_file_generic
	}

	private fun iconForMime(mime: String?): Drawable =
		context.resources.getDrawable(mimeTypeIconRes(mime))

	override fun onClick(url: String) {
		try {
			val uri = Uri.parse(url)
			val mime = context.contentResolver.getType(uri) ?: "*/*"
			val intent = Intent(Intent.ACTION_VIEW).apply {
				setDataAndType(uri, mime)
				flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
			}
			context.startActivity(intent)
		} catch (ex: ActivityNotFoundException) {
			showDialog("It looks like you don't have any apps installed that can open this type of file.", false)
		}
	}
}
