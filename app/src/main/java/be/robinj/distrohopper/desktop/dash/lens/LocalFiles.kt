package be.robinj.distrohopper.desktop.dash.lens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.database.Cursor
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
 * system directory picker (SAF), walking their document trees together.
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
	override val key = KEY

	override fun getName() = "Local files"

	override fun getDescription() = "Search files in folders you choose"

	// Walks document trees through a ContentProvider — local, but disk-backed
	// and can be slow on deep folders, so it is debounced like the network
	// lenses //
	override val type = LensType.IO

	override fun settingsActivity(): Class<out Activity> = LocalFilesFoldersActivity::class.java

	/**
	 * Breadth-first across **all** granted folders at once — one queue seeded
	 * with every root, rather than a walk per folder — so depth is compared
	 * between folders too. Otherwise something buried deep in the first folder
	 * would outrank a direct child of the second and could eat the whole result
	 * allowance before the second was even queried.
	 */
	override suspend fun search(query: String, maxResults: Int, emitter: LensResultEmitter) {
		val needle = query.lowercase()
		val queue = ArrayDeque<Branch>()
		val seen = HashSet<String>()

		for (treeUri in this.folders.folders()) {
			val rootId = try {
				DocumentsContract.getTreeDocumentId(treeUri)
			} catch (ex: IllegalArgumentException) {
				LOG.w(TAG, "Not a document tree, skipping: $treeUri")
				continue
			}

			val root = Branch(treeUri, rootId)
			if (seen.add(root.identity())) {
				queue.add(root)
			}
		}

		var emitted = 0

		while (queue.isNotEmpty() && emitted < maxResults) {
			// A deep tree can take a while, and the runner cancels this job on
			// every keystroke, so bail out promptly rather than finish the walk //
			currentCoroutineContext().ensureActive()

			emitted += this.searchBranch(
				queue.removeFirst(), needle, maxResults - emitted, queue, seen, emitter)
		}
	}

	/**
	 * Reads one directory, emitting matching files and queueing sub-directories
	 * for later. Rows are handled as they arrive off the cursor rather than
	 * collected first, so a cancelled keystroke stops mid-directory and early
	 * matches don't wait on a slow provider finishing the rest.
	 */
	private suspend fun searchBranch(
		branch: Branch,
		needle: String,
		maxResults: Int,
		queue: ArrayDeque<Branch>,
		seen: MutableSet<String>,
		emitter: LensResultEmitter,
	): Int {
		val cursor = this.childrenOf(branch) ?: return 0
		var emitted = 0

		cursor.use {
			while (it.moveToNext() && emitted < maxResults) {
				currentCoroutineContext().ensureActive()

				val documentId = it.getString(0) ?: continue
				val name = it.getString(1) ?: continue
				val mimeType = it.getString(2)

				// Hidden files and hidden directories alike are not what anyone
				// is searching for //
				if (name.startsWith(".")) {
					continue
				}

				// The same document can be reached twice: when a folder and one of
				// its own ancestors are both granted, or when a provider lists a
				// directory under more than one parent. Emitting it twice would
				// also spend the result allowance on a duplicate //
				if (!seen.add(branch.identityOf(documentId))) {
					continue
				}

				if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
					// Traversed, never offered as a result //
					queue.add(Branch(branch.treeUri, documentId))
					continue
				}

				if (!name.lowercase().contains(needle)) {
					continue
				}

				emitter.emit(LensSearchResult(
					this.context,
					name,
					DocumentsContract.buildDocumentUriUsingTree(branch.treeUri, documentId).toString(),
					this.iconForMime(mimeType),
				))
				emitted++
			}
		}

		return emitted
	}

	/**
	 * A directory's children, or null if it can't be read. A tree whose grant has
	 * gone, or a provider that misbehaves, costs only its own subtree — the
	 * user's other folders are still worth searching.
	 */
	private fun childrenOf(branch: Branch): Cursor? {
		val childrenUri = DocumentsContract
			.buildChildDocumentsUriUsingTree(branch.treeUri, branch.documentId)

		return try {
			this.context.contentResolver.query(childrenUri, PROJECTION, null, null, null)
		} catch (ex: Exception) {
			LOG.w(TAG, "Couldn't list $childrenUri: ${ex.message}")
			null
		}
	}

	/** A directory still to be searched, and the granted tree it was reached through. */
	private class Branch(val treeUri: Uri, val documentId: String) {
		fun identity(): String = this.identityOf(this.documentId)

		/**
		 * Identifies a document by provider rather than by tree, so the same file
		 * reached through two different grants compares equal.
		 */
		fun identityOf(documentId: String): String =
			"${this.treeUri.authority}\u0000$documentId"
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
		/** This lens's stable identifier; also names its preferences file. */
		const val KEY = "LocalFiles_v2"

		private const val TAG = "LocalFiles"
		private val LOG: Log = Log.getInstance()

		private val PROJECTION = arrayOf(
			DocumentsContract.Document.COLUMN_DOCUMENT_ID,
			DocumentsContract.Document.COLUMN_DISPLAY_NAME,
			DocumentsContract.Document.COLUMN_MIME_TYPE,
		)
	}
}
