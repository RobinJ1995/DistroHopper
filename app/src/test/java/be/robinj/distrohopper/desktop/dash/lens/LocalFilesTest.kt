package be.robinj.distrohopper.desktop.dash.lens

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
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
    private lateinit var provider: RecordingMediaProvider
    private lateinit var lens: LocalFiles

    @Before fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        provider = RecordingMediaProvider()
        ShadowContentResolver.registerProviderInternal("media", provider)
        lens = LocalFiles(application)
    }

    private fun mediaCursor(vararg rows: Triple<Long, String, String?>): MatrixCursor {
        val cursor = MatrixCursor(arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MIME_TYPE,
        ))
        // DATA is only read when DISPLAY_NAME is null (filename fallback); pass null otherwise.
        rows.forEach { (id, name, mime) ->
            cursor.addRow(arrayOf<Any?>(id, name, null, mime))
        }
        return cursor
    }

    private fun mediaCursor(vararg rows: Pair<Long, String>): MatrixCursor =
        mediaCursor(*rows.map { (id, name) -> Triple(id, name, null) }.toTypedArray())

    // ── Existing tests ─────────────────────────────────────────────────────────

    @Test fun searchReturnsResultsFromMediaStore() {
        provider.cursorToReturn = mediaCursor(1L to "notes.txt", 2L to "notes2.txt")
        val results = lens.search("notes", 10)
        assertEquals(2, results.size)
        assertEquals("notes.txt", results[0].name)
        assertEquals("notes2.txt", results[1].name)
    }

    @Test fun searchReturnsEmptyListWhenQueryFails() {
        provider.cursorToReturn = null
        val results = lens.search("anything", 10)
        assertTrue(results.isEmpty())
    }

    @Test fun searchClosesCursor() {
        val cursor = mediaCursor(1L to "notes.txt")
        provider.cursorToReturn = cursor
        lens.search("notes", 10)
        assertTrue(cursor.isClosed)
    }

    @Test fun searchClosesCursorWhenNoResults() {
        val cursor = mediaCursor()
        provider.cursorToReturn = cursor
        lens.search("nothing", 10)
        assertTrue(cursor.isClosed)
    }

    @Test fun searchUsesSelectionArgsInsteadOfStringConcatenation() {
        provider.cursorToReturn = mediaCursor()
        lens.search("o'brien", 10)
        assertNotNull(provider.lastSelection)
        assertTrue(provider.lastSelection!!.contains("?"))
        assertFalse(provider.lastSelection!!.contains("o'brien"))
        assertFalse(provider.lastSelection!!.contains("o''brien"))
        assertArrayEquals(arrayOf("%o'brien%"), provider.lastSelectionArgs)
    }

    @Test fun searchRespectsMaxResults() {
        provider.cursorToReturn = mediaCursor(1L to "a.txt", 2L to "b.txt", 3L to "c.txt", 4L to "d.txt")
        val results = lens.search("txt", 2)
        assertEquals(2, results.size)
    }

    @Test fun resultUrlIsAContentUriForTheMediaStoreEntry() {
        provider.cursorToReturn = mediaCursor(42L to "notes.txt")
        val results = lens.search("notes", 10)
        val uri = Uri.parse(results[0].url)
        assertEquals("content", uri.scheme)
        assertEquals("42", uri.lastPathSegment)
    }

    @Test fun clickStartsViewIntentWithContentUri() {
        provider.typeToReturn = "text/plain"
        lens.onClick("content://media/external/file/42")
        val intent = Shadows.shadowOf(application).nextStartedActivity
        assertNotNull(intent)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("content://media/external/file/42", intent.dataString)
        assertEquals("text/plain", intent.type)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test fun clickFallsBackToWildcardMimeTypeWhenUnknown() {
        provider.typeToReturn = null
        lens.onClick("content://media/external/file/42")
        val intent = Shadows.shadowOf(application).nextStartedActivity
        assertNotNull(intent)
        assertEquals("*/*", intent.type)
    }

    // ── New tests ──────────────────────────────────────────────────────────────

    @Test fun searchPassesSortOrderDateModifiedDesc() {
        provider.cursorToReturn = mediaCursor()
        lens.search("txt", 10)
        assertNotNull(provider.lastSortOrder)
        assertTrue(provider.lastSortOrder!!.contains(MediaStore.Files.FileColumns.DATE_MODIFIED))
        assertTrue(provider.lastSortOrder!!.uppercase().contains("DESC"))
    }

    @Test fun searchUsesDisplayNameColumnNotTitle() {
        provider.cursorToReturn = mediaCursor()
        lens.search("notes", 10)
        assertNotNull(provider.lastSelection)
        assertTrue(provider.lastSelection!!.contains(MediaStore.Files.FileColumns.DISPLAY_NAME))
        assertFalse(provider.lastSelection!!.contains(MediaStore.Files.FileColumns.TITLE))
    }

    @Test fun searchExcludesHiddenFiles() {
        provider.cursorToReturn = mediaCursor(
            Triple(1L, ".hidden", null),
            Triple(2L, "visible.txt", null),
        )
        val results = lens.search("", 10)
        assertEquals(1, results.size)
        assertEquals("visible.txt", results[0].name)
    }

    @Test fun mimeTypeImageResultUsesImageIcon() {
        provider.cursorToReturn = mediaCursor(Triple(1L, "photo.jpg", "image/jpeg"))
        val results = lens.search("photo", 10)
        assertEquals(1, results.size)
        val expected = application.resources.getDrawable(be.robinj.distrohopper.R.drawable.ic_file_image)
        val notExpected = application.resources.getDrawable(be.robinj.distrohopper.R.drawable.ic_file_generic)
        assertEquals(expected.constantState, results[0].icon.constantState)
        assertNotEquals(notExpected.constantState, results[0].icon.constantState)
    }

    @Test fun mimeTypeVideoResultUsesVideoIcon() {
        provider.cursorToReturn = mediaCursor(Triple(1L, "movie.mp4", "video/mp4"))
        val results = lens.search("movie", 10)
        assertEquals(1, results.size)
        val expected = application.resources.getDrawable(be.robinj.distrohopper.R.drawable.ic_file_video)
        assertEquals(expected.constantState, results[0].icon.constantState)
    }

    @Test fun mimeTypeAudioResultUsesAudioIcon() {
        provider.cursorToReturn = mediaCursor(Triple(1L, "song.mp3", "audio/mpeg"))
        val results = lens.search("song", 10)
        assertEquals(1, results.size)
        val expected = application.resources.getDrawable(be.robinj.distrohopper.R.drawable.ic_file_audio)
        assertEquals(expected.constantState, results[0].icon.constantState)
    }

    @Test fun mimeTypePdfResultUsesDocumentIcon() {
        provider.cursorToReturn = mediaCursor(Triple(1L, "report.pdf", "application/pdf"))
        val results = lens.search("report", 10)
        assertEquals(1, results.size)
        val expected = application.resources.getDrawable(be.robinj.distrohopper.R.drawable.ic_file_document)
        assertEquals(expected.constantState, results[0].icon.constantState)
    }

    @Test fun mimeTypeTextResultUsesDocumentIcon() {
        provider.cursorToReturn = mediaCursor(Triple(1L, "readme.txt", "text/plain"))
        val results = lens.search("readme", 10)
        assertEquals(1, results.size)
        val expected = application.resources.getDrawable(be.robinj.distrohopper.R.drawable.ic_file_document)
        assertEquals(expected.constantState, results[0].icon.constantState)
    }

    @Test fun nullMimeTypeResultUsesGenericIcon() {
        provider.cursorToReturn = mediaCursor(Triple(1L, "unknown.bin", null))
        val results = lens.search("unknown", 10)
        assertEquals(1, results.size)
        val expected = application.resources.getDrawable(be.robinj.distrohopper.R.drawable.ic_file_generic)
        assertEquals(expected.constantState, results[0].icon.constantState)
    }

    // ── Infrastructure ─────────────────────────────────────────────────────────

    private class RecordingMediaProvider : ContentProvider() {
        var cursorToReturn: Cursor? = null
        var typeToReturn: String? = null
        var lastSelection: String? = null
        var lastSelectionArgs: Array<out String>? = null
        var lastSortOrder: String? = null

        override fun onCreate() = true

        override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
                           selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
            this.lastSelection = selection
            this.lastSelectionArgs = selectionArgs
            this.lastSortOrder = sortOrder
            return this.cursorToReturn
        }

        override fun getType(uri: Uri) = this.typeToReturn
        override fun insert(uri: Uri, values: ContentValues?) = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?,
                            selectionArgs: Array<out String>?) = 0
    }
}
