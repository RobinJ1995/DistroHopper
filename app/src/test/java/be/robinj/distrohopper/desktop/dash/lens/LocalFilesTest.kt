package be.robinj.distrohopper.desktop.dash.lens

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.DocumentsContract
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.R
import be.robinj.distrohopper.desktop.dash.lens.localfiles.SearchFolderStore
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
class LocalFilesTest {
    private lateinit var application: Application
    private lateinit var provider: FakeDocumentsProvider
    private lateinit var lens: LocalFiles

    @Before fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        PreferencesRepository(application)
            .putStringSet(Preference.LENS_LOCALFILES_V2_FOLDERS, emptySet())

        provider = FakeDocumentsProvider()
        ShadowContentResolver.registerProviderInternal(AUTHORITY, provider)

        lens = LocalFiles(application)
    }

    // ── Folders ────────────────────────────────────────────────────────────────

    /** Grants [treeUri] and records it, exactly as the folder picker screen does. */
    private fun grantFolder(treeDocumentId: String): Uri {
        val treeUri = treeUriFor(treeDocumentId)
        SearchFolderStore(application).add(treeUri)

        return treeUri
    }

    private fun treeUriFor(treeDocumentId: String): Uri =
        DocumentsContract.buildTreeDocumentUri(AUTHORITY, treeDocumentId)

    // ── Matching ───────────────────────────────────────────────────────────────

    @Test fun findsAFileInAGrantedFolder() {
        provider.contents("root", file("notes.txt", "text/plain"))
        grantFolder("root")

        val results = lens.collect("notes", 10).results

        assertEquals(1, results.size)
        assertEquals("notes.txt", results[0].name)
    }

    @Test fun recursesIntoNestedDirectories() {
        provider.contents("root", folder("docs"))
        provider.contents("docs", folder("invoices"))
        provider.contents("invoices", file("invoice-2026.pdf", "application/pdf"))
        grantFolder("root")

        val results = lens.collect("invoice", 10).results

        assertEquals(listOf("invoice-2026.pdf"), results.map { it.name })
    }

    /** Shallow matches are the likelier ones, so breadth-first is the point. */
    @Test fun emitsShallowMatchesBeforeDeepOnes() {
        provider.contents("root", file("report-top.txt", "text/plain"), folder("deep"))
        provider.contents("deep", file("report-buried.txt", "text/plain"))
        grantFolder("root")

        val results = lens.collect("report", 10).results

        assertEquals(listOf("report-top.txt", "report-buried.txt"), results.map { it.name })
    }

    @Test fun matchesCaseInsensitively() {
        provider.contents("root", file("Rapport.PDF", "application/pdf"))
        grantFolder("root")

        assertEquals(1, lens.collect("rapport", 10).results.size)
        assertEquals(1, lens.collect("RAPPORT", 10).results.size)
    }

    @Test fun matchesOnASubstringNotJustAPrefix() {
        provider.contents("root", file("annual-report-2026.pdf", "application/pdf"))
        grantFolder("root")

        assertEquals(1, lens.collect("report", 10).results.size)
    }

    @Test fun emitsNothingWhenNothingMatches() {
        provider.contents("root", file("notes.txt", "text/plain"))
        grantFolder("root")

        assertTrue(lens.collect("holiday", 10).results.isEmpty())
    }

    // ── What gets skipped ──────────────────────────────────────────────────────

    @Test fun skipsHiddenFiles() {
        provider.contents("root", file(".secret-notes", "text/plain"), file("notes.txt", "text/plain"))
        grantFolder("root")

        assertEquals(listOf("notes.txt"), lens.collect("notes", 10).results.map { it.name })
    }

    @Test fun doesNotDescendIntoHiddenDirectories() {
        provider.contents("root", folder(".thumbnails"))
        provider.contents(".thumbnails", file("notes.txt", "text/plain"))
        grantFolder("root")

        assertTrue(lens.collect("notes", 10).results.isEmpty())
        assertFalse(provider.queriedParents.contains(".thumbnails"))
    }

    /** A directory is somewhere to look, never something to open. */
    @Test fun directoriesAreTraversedButNeverEmitted() {
        provider.contents("root", folder("reports"))
        provider.contents("reports", file("reports-summary.txt", "text/plain"))
        grantFolder("root")

        val results = lens.collect("reports", 10).results

        assertEquals(listOf("reports-summary.txt"), results.map { it.name })
        assertTrue(provider.queriedParents.contains("reports"))
    }

    // ── Limits and cancellation ────────────────────────────────────────────────

    @Test fun respectsMaxResults() {
        provider.contents("root",
            file("a.txt", "text/plain"), file("b.txt", "text/plain"),
            file("c.txt", "text/plain"), file("d.txt", "text/plain"))
        grantFolder("root")

        assertEquals(2, lens.collect("txt", 2).results.size)
    }

    /** Hitting the cap ends the walk; it must not keep touring the tree first. */
    @Test fun stopsWalkingOnceTheCapIsReached() {
        provider.contents("root", file("a.txt", "text/plain"), folder("deeper"))
        provider.contents("deeper", file("b.txt", "text/plain"))
        grantFolder("root")

        lens.collect("txt", 1)

        assertFalse(provider.queriedParents.contains("deeper"))
    }

    /**
     * The runner cancels this job on every keystroke, so a half-finished walk
     * must abandon the rest of the tree rather than run it out.
     */
    @Test fun abandonsTheWalkWhenTheJobIsCancelled() {
        provider.contents("root", file("match-here.txt", "text/plain"), folder("deeper"))
        provider.contents("deeper", file("match-deeper.txt", "text/plain"))
        grantFolder("root")

        val recorded = mutableListOf<LensSearchResult>()

        runBlocking {
            launch(Dispatchers.Unconfined) {
                lens.search("match", 10, CancelAfterFirstResult(recorded))
            }.join()
        }

        assertEquals(listOf("match-here.txt"), recorded.map { it.name })
        assertFalse(provider.queriedParents.contains("deeper"))
    }

    // ── Several folders ────────────────────────────────────────────────────────

    @Test fun searchesEveryGrantedFolder() {
        provider.contents("photos", file("holiday-photo.jpg", "image/jpeg"))
        provider.contents("papers", file("holiday-booking.pdf", "application/pdf"))
        grantFolder("papers")
        grantFolder("photos")

        val results = lens.collect("holiday", 10).results

        assertEquals(
            setOf("holiday-photo.jpg", "holiday-booking.pdf"),
            results.map { it.name }.toSet(),
        )
    }

    @Test fun emitsNothingWhenNoFoldersAreConfigured() {
        provider.contents("root", file("notes.txt", "text/plain"))

        assertTrue(lens.collect("notes", 10).results.isEmpty())
    }

    /**
     * A folder whose grant the system took back must not take the other folders
     * down with it.
     */
    @Test fun aRevokedFolderIsSkippedAndTheRestStillSearched() {
        provider.contents("papers", file("notes.txt", "text/plain"))
        grantFolder("papers")

        // Recorded but never granted, so the store never returns it //
        PreferencesRepository(application).putStringSet(
            Preference.LENS_LOCALFILES_V2_FOLDERS,
            setOf(treeUriFor("papers").toString(), treeUriFor("gone").toString()),
        )

        assertEquals(listOf("notes.txt"), lens.collect("notes", 10).results.map { it.name })
    }

    // ── Misbehaving providers ──────────────────────────────────────────────────

    @Test fun survivesAProviderThatReturnsNoCursor() {
        provider.returnNullCursor = true
        grantFolder("root")

        assertTrue(lens.collect("notes", 10).results.isEmpty())
    }

    @Test fun survivesAProviderThatThrows() {
        provider.throwOnQuery = SecurityException("permission gone")
        grantFolder("root")

        assertTrue(lens.collect("notes", 10).results.isEmpty())
    }

    @Test fun survivesAFolderThatIsNotADocumentTree() {
        PreferencesRepository(application).putStringSet(
            Preference.LENS_LOCALFILES_V2_FOLDERS, setOf("content://$AUTHORITY/nonsense"))
        application.contentResolver.takePersistableUriPermission(
            Uri.parse("content://$AUTHORITY/nonsense"), Intent.FLAG_GRANT_READ_URI_PERMISSION)

        assertTrue(lens.collect("notes", 10).results.isEmpty())
    }

    // ── Results ────────────────────────────────────────────────────────────────

    /** The url must be openable by [LocalFiles.onClick]'s ACTION_VIEW. */
    @Test fun resultUrlIsATreeBackedDocumentUri() {
        provider.contents("root", file("notes.txt", "text/plain"))
        grantFolder("root")

        val url = lens.collect("notes", 10).results[0].url

        assertEquals(
            DocumentsContract.buildDocumentUriUsingTree(treeUriFor("root"), "notes.txt").toString(),
            url,
        )
    }

    @Test fun clickStartsViewIntentWithTheDocumentUri() {
        provider.typeToReturn = "application/pdf"
        val url = DocumentsContract
            .buildDocumentUriUsingTree(treeUriFor("root"), "invoice.pdf").toString()

        lens.onClick(url)

        val intent = Shadows.shadowOf(application).nextStartedActivity
        assertNotNull(intent)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(url, intent.dataString)
        assertEquals("application/pdf", intent.type)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test fun clickFallsBackToWildcardMimeTypeWhenUnknown() {
        provider.typeToReturn = null

        lens.onClick(DocumentsContract
            .buildDocumentUriUsingTree(treeUriFor("root"), "mystery").toString())

        assertEquals("*/*", Shadows.shadowOf(application).nextStartedActivity.type)
    }

    // ── Icons ──────────────────────────────────────────────────────────────────

    @Test fun mimeTypeImageResultUsesImageIcon() {
        assertEquals(R.drawable.ic_file_image, lens.mimeTypeIconRes("image/jpeg"))
    }

    @Test fun mimeTypeVideoResultUsesVideoIcon() {
        assertEquals(R.drawable.ic_file_video, lens.mimeTypeIconRes("video/mp4"))
    }

    @Test fun mimeTypeAudioResultUsesAudioIcon() {
        assertEquals(R.drawable.ic_file_audio, lens.mimeTypeIconRes("audio/mpeg"))
    }

    @Test fun mimeTypePdfResultUsesDocumentIcon() {
        assertEquals(R.drawable.ic_file_document, lens.mimeTypeIconRes("application/pdf"))
    }

    @Test fun mimeTypeTextResultUsesDocumentIcon() {
        assertEquals(R.drawable.ic_file_document, lens.mimeTypeIconRes("text/plain"))
    }

    @Test fun nullMimeTypeResultUsesGenericIcon() {
        assertEquals(R.drawable.ic_file_generic, lens.mimeTypeIconRes(null))
    }

    // ── Infrastructure ─────────────────────────────────────────────────────────

    private fun file(name: String, mime: String) = FakeDocumentsProvider.Entry(name, name, mime)

    /** Cancels the search from within, the moment it produces its first result. */
    private class CancelAfterFirstResult(
        private val recorded: MutableList<LensSearchResult>,
    ) : LensResultEmitter {
        override suspend fun emit(result: LensSearchResult) {
            this.recorded.add(result)
            currentCoroutineContext().cancel()
        }

        override suspend fun emit(sectionName: String, result: LensSearchResult) = this.emit(result)
    }

    private fun folder(name: String) =
        FakeDocumentsProvider.Entry(name, name, DocumentsContract.Document.MIME_TYPE_DIR)

    /**
     * A minimal DocumentsProvider: a flat map of document id to children, which
     * is all a tree walk needs. Document ids double as display names so fixtures
     * stay readable.
     */
    private class FakeDocumentsProvider : ContentProvider() {
        class Entry(val documentId: String, val name: String?, val mimeType: String?)

        private val children = HashMap<String, List<Entry>>()
        val queriedParents = mutableListOf<String>()
        var typeToReturn: String? = null
        var returnNullCursor = false
        var throwOnQuery: RuntimeException? = null

        fun contents(documentId: String, vararg entries: Entry) {
            this.children[documentId] = entries.toList()
        }

        override fun onCreate() = true

        override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
                           selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
            this.throwOnQuery?.let { throw it }

            if (this.returnNullCursor) {
                return null
            }

            val parent = DocumentsContract.getDocumentId(uri)
            this.queriedParents.add(parent)

            val cursor = MatrixCursor(arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            ))
            this.children[parent].orEmpty().forEach {
                cursor.addRow(arrayOf(it.documentId, it.name, it.mimeType))
            }

            return cursor
        }

        override fun getType(uri: Uri) = this.typeToReturn
        override fun insert(uri: Uri, values: ContentValues?) = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?,
                            selectionArgs: Array<out String>?) = 0
    }

    private companion object {
        const val AUTHORITY = "be.robinj.distrohopper.test.documents"
    }
}
