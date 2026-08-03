package be.robinj.distrohopper.desktop.dash.lens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.DocumentsContract
import be.robinj.distrohopper.R
import be.robinj.distrohopper.desktop.dash.lens.localfiles.SearchFolderStore
import be.robinj.distrohopper.dev.Log
import be.robinj.distrohopper.preferences.LocalFilesFoldersActivity
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Searches files by name inside the folders the user has granted through the
 * system directory picker (SAF), walking each granted document tree.
 *
 * It deliberately holds no storage permission: a granted tree *is* the
 * permission, which also means the lens finds nothing until the user has added
 * at least one folder (see [LocalFilesFoldersActivity]).
 */
class LocalFiles(context: Context) : Lens(context) {

	private val folders = SearchFolderStore(context)

	init {
		icon = context.resources.getDrawable(R.drawable.dash_search_lens_localfiles, null)
	}

	// Bumped from "LocalFiles" when this lens moved from a MediaStore query to
	// user-granted SAF folders: a different feature, so the old one retires
	// rather than silently becoming an empty version of the new one. //
	override val key = "LocalFiles_v2"

	override fun getName() = "Local files"

	override fun getDescription() = "Search files in folders you choose"

	// Walks document trees through a ContentProvider — local, but disk-backed
	// and can be slow on deep folders, so it is debounced like the network
	// lenses //
	override val type = LensType.IO

	override fun settingsActivity(): Class<out Activity> = LocalFilesFoldersActivity::class.java

	override suspend fun search(query: String, maxResults: Int, emitter: LensResultEmitter) {
		val needle = query.lowercase()
		var emitted = 0

		for (treeUri in this.folders.folders()) {
			if (emitted >= maxResults) {
				return
			}

			emitted += this.walk(treeUri, needle, maxResults - emitted, emitter)
		}
	}

	/**
	 * Breadth-first through one granted tree, so shallow matches — the likelier
	 * ones — are emitted before anything buried deep. Returns how many it
	 * emitted.
	 */
	private suspend fun walk(
		treeUri: Uri,
		needle: String,
		maxResults: Int,
		emitter: LensResultEmitter,
	): Int {
		val rootId = try {
			DocumentsContract.getTreeDocumentId(treeUri)
		} catch (ex: IllegalArgumentException) {
			LOG.w(TAG, "Not a document tree, skipping: $treeUri")
			return 0
		}

		val queue = ArrayDeque<String>()
		queue.add(rootId)
		var emitted = 0

		while (queue.isNotEmpty() && emitted < maxResults) {
			// A deep tree can take a while, and the runner cancels this job on
			// every keystroke, so bail out promptly rather than finish the walk //
			currentCoroutineContext().ensureActive()

			for (entry in this.childrenOf(treeUri, queue.removeFirst())) {
				currentCoroutineContext().ensureActive()

				// Hidden files and hidden directories alike are not what anyone
				// is searching for //
				val name = entry.name ?: continue
				if (name.startsWith(".")) {
					continue
				}

				if (entry.isDirectory) {
					queue.add(entry.documentId) // Traversed, never offered as a result //
					continue
				}

				if (!name.lowercase().contains(needle)) {
					continue
				}

				emitter.emit(LensSearchResult(
					this.context,
					name,
					DocumentsContract.buildDocumentUriUsingTree(treeUri, entry.documentId).toString(),
					this.iconForMime(entry.mimeType),
				))

				if (++emitted >= maxResults) {
					break
				}
			}
		}

		return emitted
	}

	/**
	 * One directory's children. A tree whose grant has gone, or a provider that
	 * misbehaves, yields nothing rather than failing the whole search — the
	 * user's other folders are still worth searching.
	 */
	private fun childrenOf(treeUri: Uri, parentDocumentId: String): List<Entry> {
		val childrenUri =
			DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)

		return try {
			this.context.contentResolver
				.query(childrenUri, PROJECTION, null, null, null)
				?.use { cursor ->
					val entries = ArrayList<Entry>(cursor.count)

					while (cursor.moveToNext()) {
						val documentId = cursor.getString(0) ?: continue
						entries.add(Entry(documentId, cursor.getString(1), cursor.getString(2)))
					}

					entries
				} ?: emptyList()
		} catch (ex: Exception) {
			LOG.w(TAG, "Couldn't list $childrenUri: ${ex.message}")
			emptyList()
		}
	}

	private class Entry(val documentId: String, val name: String?, val mimeType: String?) {
		val isDirectory: Boolean
			get() = this.mimeType == DocumentsContract.Document.MIME_TYPE_DIR
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
		context.resources.getDrawable(mimeTypeIconRes(mime), null)

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

	companion object {
		private const val TAG = "LocalFiles"
		private val LOG: Log = Log.getInstance()

		private val PROJECTION = arrayOf(
			DocumentsContract.Document.COLUMN_DOCUMENT_ID,
			DocumentsContract.Document.COLUMN_DISPLAY_NAME,
			DocumentsContract.Document.COLUMN_MIME_TYPE,
		)
	}
}
