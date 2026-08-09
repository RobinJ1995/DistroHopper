package be.robinj.distrohopper.desktop.dash.lens.localfiles

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.DocumentsContract
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
class SearchFolderStoreTest {
    private lateinit var application: Application
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var store: SearchFolderStore

    @Before fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        prefs = SearchFolderStore.preferences(application)
        put(emptySet())

        ShadowContentResolver.registerProviderInternal(AUTHORITY, NamingProvider())

        store = SearchFolderStore(application)
    }

    private fun treeUri(documentId: String): Uri =
        DocumentsContract.buildTreeDocumentUri(AUTHORITY, documentId)

    private fun stored(): Set<String> =
        prefs.getStringSet(SearchFolderStore.KEY_FOLDERS, emptySet())!!.toSet()

    private fun put(values: Set<String>) {
        prefs.edit().putStringSet(SearchFolderStore.KEY_FOLDERS, values).commit()
    }

    @Test fun startsEmpty() {
        assertTrue(store.folders().isEmpty())
    }

    @Test fun addingAFolderRecordsIt() {
        val uri = treeUri("documents")

        store.add(uri)

        assertEquals(listOf(uri), store.folders())
    }

    @Test fun addingAFolderTakesAPersistableReadGrant() {
        val uri = treeUri("documents")

        store.add(uri)

        assertEquals(
            listOf(uri),
            application.contentResolver.persistedUriPermissions
                .filter { it.isReadPermission }
                .map { it.uri },
        )
    }

    @Test fun addingTheSameFolderTwiceKeepsOneEntry() {
        val uri = treeUri("documents")

        store.add(uri)
        store.add(uri)

        assertEquals(listOf(uri), store.folders())
    }

    @Test fun foldersSurviveANewStoreInstance() {
        store.add(treeUri("documents"))

        assertEquals(listOf(treeUri("documents")), SearchFolderStore(application).folders())
    }

    @Test fun removingAFolderForgetsIt() {
        store.add(treeUri("documents"))
        store.add(treeUri("pictures"))

        store.remove(treeUri("documents"))

        assertEquals(listOf(treeUri("pictures")), store.folders())
    }

    @Test fun removingAFolderReleasesItsGrant() {
        store.add(treeUri("documents"))

        store.remove(treeUri("documents"))

        assertTrue(application.contentResolver.persistedUriPermissions.isEmpty())
    }

    @Test fun removingAFolderThatWasNeverGrantedDoesNotThrow() {
        store.remove(treeUri("never-added"))

        assertTrue(store.folders().isEmpty())
    }

    /** A grant can vanish while the app is away: the folder must not linger. */
    @Test fun aFolderWhoseGrantIsGoneIsDropped() {
        store.add(treeUri("documents"))
        put(stored() + treeUri("unmounted-sd-card").toString())

        assertEquals(listOf(treeUri("documents")), store.folders())
    }

    @Test fun droppedFoldersArePrunedFromStorageNotJustHidden() {
        put(setOf(treeUri("gone").toString()))

        store.folders()

        assertTrue(stored().isEmpty())
    }

    @Test fun foldersAreReturnedInAStableOrder() {
        store.add(treeUri("pictures"))
        store.add(treeUri("documents"))
        store.add(treeUri("music"))

        assertEquals(SearchFolderStore(application).folders(), store.folders())
    }

    // ── Display names ──────────────────────────────────────────────────────────

    @Test fun displayNameComesFromTheProvider() {
        assertEquals("Documents", store.displayName(treeUri("documents")))
    }

    @Test fun displayNameFallsBackWhenTheProviderSaysNothing() {
        assertEquals("unknown", store.displayName(treeUri("unknown")))
    }

    @Test fun displayNameFallsBackForAUriThatIsNotATree() {
        val notATree = Uri.parse("content://$AUTHORITY/nonsense")

        assertEquals("nonsense", store.displayName(notATree))
    }

    /** Names only the picker knows; the store must not fail if it can't get one. */
    private class NamingProvider : ContentProvider() {
        override fun onCreate() = true

        override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
                           selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
            val documentId = DocumentsContract.getDocumentId(uri)

            if (documentId != "documents") {
                return null
            }

            return MatrixCursor(arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
                .apply { addRow(arrayOf("Documents")) }
        }

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?) = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?,
                            selectionArgs: Array<out String>?) = 0
    }

    private companion object {
        const val AUTHORITY = "be.robinj.distrohopper.test.folders"
    }
}
