package be.robinj.distrohopper.desktop.dash.lens.localfiles

import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import be.robinj.distrohopper.desktop.dash.lens.LocalFiles
import be.robinj.distrohopper.dev.Log
import be.robinj.distrohopper.preferences.Preferences

/**
 * The folders the Local files lens is allowed to search: SAF document trees the
 * user picked through the system directory picker, held as persisted URI
 * permission grants so they survive reboots.
 *
 * Nothing here needs a manifest permission — a granted tree *is* the permission.
 *
 * These live in the lens's own preferences file rather than the main "prefs"
 * one: a tree URI spells out the folder's name and path, so keeping it out of
 * "prefs" keeps it out of everything scoped to that file, crash reports
 * included.
 */
class SearchFolderStore(private val context: Context) {
	private val prefs = preferences(this.context)

	private fun stored(): Set<String> =
		this.prefs.getStringSet(KEY_FOLDERS, emptySet())?.toSet() ?: emptySet()

	private fun store(values: Set<String>) {
		this.prefs.edit().putStringSet(KEY_FOLDERS, values).apply()
	}

	/**
	 * The granted folders, minus any whose grant the system has since dropped
	 * (the folder was deleted, or its storage was unmounted). Those are pruned
	 * from the stored set as they're noticed, so nothing lists a dead grant.
	 */
	fun folders(): List<Uri> {
		val stored = this.stored()
		val live = this.readableUris()
		val kept = stored.filter { live.contains(it) }

		if (kept.size != stored.size) {
			LOG.i(TAG, "Dropping ${stored.size - kept.size} folder(s) whose grant is gone.")
			this.store(kept.toSet())
		}

		return kept.sorted().map(Uri::parse)
	}

	/** Takes a lasting read grant on [treeUri] and records it. */
	fun add(treeUri: Uri) {
		this.context.contentResolver
			.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

		this.store(this.stored() + treeUri.toString())
	}

	/** Forgets [treeUri] and gives its grant back to the system. */
	fun remove(treeUri: Uri) {
		this.store(this.stored() - treeUri.toString())

		try {
			this.context.contentResolver
				.releasePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
		} catch (ex: SecurityException) {
			// Already gone (revoked, or released twice); the stored entry is what
			// mattered and it is already removed. //
			// Neither the URI nor the exception message: a SecurityException about a
			// grant quotes the URI back, and that spells out the user's folder. //
			LOG.v(TAG, "Nothing to release: ${ex.javaClass.simpleName}")
		}
	}

	/**
	 * The folder's own name, as the picker showed it. Falls back to the tree's
	 * document id, which at least identifies it, if the provider won't say.
	 */
	fun displayName(treeUri: Uri): String {
		val documentUri = try {
			DocumentsContract.buildDocumentUriUsingTree(
				treeUri, DocumentsContract.getTreeDocumentId(treeUri))
		} catch (ex: IllegalArgumentException) {
			return treeUri.lastPathSegment ?: treeUri.toString()
		}

		return try {
			this.context.contentResolver.query(
				documentUri,
				arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
				null, null, null,
			)?.use { cursor ->
				if (cursor.moveToFirst()) cursor.getString(0) else null
			}
		} catch (ex: Exception) {
			LOG.v(TAG, "Couldn't read a folder's name: ${ex.javaClass.simpleName}")
			null
		} ?: (treeUri.lastPathSegment ?: treeUri.toString())
	}

	private fun readableUris(): Set<String> =
		this.context.contentResolver.persistedUriPermissions
			.filter { it.isReadPermission }
			.map { it.uri.toString() }
			.toSet()

	companion object {
		/** The key the granted tree URIs live under, within the lens's own file. */
		const val KEY_FOLDERS = "folders"

		/** The Local files lens's preferences file. */
		@JvmStatic
		fun preferences(context: Context): SharedPreferences =
			Preferences.getSharedPreferences(context, Preferences.forLens(LocalFiles.KEY))

		private const val TAG = "SearchFolderStore"
		private val LOG: Log = Log.getInstance()
	}
}
