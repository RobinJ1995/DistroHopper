package be.robinj.distrohopper.desktop.dash.lens.localfiles

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import be.robinj.distrohopper.dev.Log
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.PreferencesRepository

/**
 * The folders the Local files lens is allowed to search: SAF document trees the
 * user picked through the system directory picker, held as persisted URI
 * permission grants so they survive reboots.
 *
 * Nothing here needs a manifest permission — a granted tree *is* the permission.
 */
class SearchFolderStore(private val context: Context) {
	private val prefs = PreferencesRepository(this.context)

	/**
	 * The granted folders, minus any whose grant the system has since dropped
	 * (the folder was deleted, or its storage was unmounted). Those are pruned
	 * from the stored set as they're noticed, so nothing lists a dead grant.
	 */
	fun folders(): List<Uri> {
		val stored = this.prefs.getStringSet(Preference.LENS_LOCALFILES_V2_FOLDERS)
		val live = this.readableUris()
		val kept = stored.filter { live.contains(it) }

		if (kept.size != stored.size) {
			LOG.i(TAG, "Dropping ${stored.size - kept.size} folder(s) whose grant is gone.")
			this.prefs.putStringSet(Preference.LENS_LOCALFILES_V2_FOLDERS, kept.toSet())
		}

		return kept.sorted().map(Uri::parse)
	}

	/** Takes a lasting read grant on [treeUri] and records it. */
	fun add(treeUri: Uri) {
		this.context.contentResolver
			.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

		val stored = this.prefs.getStringSet(Preference.LENS_LOCALFILES_V2_FOLDERS)
		this.prefs.putStringSet(
			Preference.LENS_LOCALFILES_V2_FOLDERS, stored + treeUri.toString())
	}

	/** Forgets [treeUri] and gives its grant back to the system. */
	fun remove(treeUri: Uri) {
		val stored = this.prefs.getStringSet(Preference.LENS_LOCALFILES_V2_FOLDERS)
		this.prefs.putStringSet(
			Preference.LENS_LOCALFILES_V2_FOLDERS, stored - treeUri.toString())

		try {
			this.context.contentResolver
				.releasePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
		} catch (ex: SecurityException) {
			// Already gone (revoked, or released twice); the stored entry is what
			// mattered and it is already removed. //
			LOG.v(TAG, "Nothing to release for $treeUri: ${ex.message}")
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
			LOG.v(TAG, "Couldn't read the name of $treeUri: ${ex.message}")
			null
		} ?: (treeUri.lastPathSegment ?: treeUri.toString())
	}

	private fun readableUris(): Set<String> =
		this.context.contentResolver.persistedUriPermissions
			.filter { it.isReadPermission }
			.map { it.uri.toString() }
			.toSet()

	companion object {
		private const val TAG = "SearchFolderStore"
		private val LOG: Log = Log.getInstance()
	}
}
