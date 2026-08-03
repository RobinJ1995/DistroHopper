package be.robinj.distrohopper.preferences

import android.app.Activity
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.DocumentsContract
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.R
import be.robinj.distrohopper.desktop.dash.lens.localfiles.SearchFolderStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
class LocalFilesFoldersActivityTest {
    private lateinit var application: Application

    @Before fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        PreferencesRepository(application)
            .putStringSet(Preference.LENS_LOCALFILES_V2_FOLDERS, emptySet())
        ShadowContentResolver.registerProviderInternal(AUTHORITY, NamingProvider())
    }

    private fun treeUri(documentId: String): Uri =
        DocumentsContract.buildTreeDocumentUri(AUTHORITY, documentId)

    private fun list(activity: Activity) = activity.findViewById<ListView>(R.id.lvFolders)

    @Test fun startsWithNoFoldersAndShowsTheEmptyState() {
        ActivityScenario.launch(LocalFilesFoldersActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(0, list(activity).adapter.count)
                assertEquals(
                    View.VISIBLE,
                    activity.findViewById<TextView>(R.id.tvEmpty).visibility,
                )
            }
        }
    }

    @Test fun alreadyGrantedFoldersAreListedByName() {
        SearchFolderStore(application).add(treeUri("documents"))

        ActivityScenario.launch(LocalFilesFoldersActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val adapter = list(activity).adapter

                assertEquals(1, adapter.count)
                assertEquals(
                    "Documents",
                    adapter.getView(0, null, list(activity))
                        .findViewById<TextView>(R.id.tvFolderName).text,
                )
            }
        }
    }

    @Test fun addFolderLaunchesTheSystemDirectoryPicker() {
        ActivityScenario.launch(LocalFilesFoldersActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<Button>(R.id.btnAddFolder).performClick()

                val started = Shadows.shadowOf(activity).nextStartedActivityForResult

                assertNotNull(started)
                assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, started.intent.action)
            }
        }
    }

    @Test fun aPickedFolderIsGrantedAndListed() {
        ActivityScenario.launch(LocalFilesFoldersActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                pickFolder(activity, treeUri("documents"))

                assertEquals(1, list(activity).adapter.count)
                assertEquals(
                    listOf(treeUri("documents")),
                    SearchFolderStore(application).folders(),
                )
            }
        }
    }

    @Test fun backingOutOfThePickerAddsNothing() {
        ActivityScenario.launch(LocalFilesFoldersActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<Button>(R.id.btnAddFolder).performClick()
                val started = Shadows.shadowOf(activity).nextStartedActivityForResult

                Shadows.shadowOf(activity)
                    .receiveResult(started.intent, Activity.RESULT_CANCELED, null)

                assertEquals(0, list(activity).adapter.count)
                assertTrue(SearchFolderStore(application).folders().isEmpty())
            }
        }
    }

    @Test fun removingAFolderDropsItAndReleasesTheGrant() {
        SearchFolderStore(application).add(treeUri("documents"))

        ActivityScenario.launch(LocalFilesFoldersActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val listView = list(activity)
                listView.adapter.getView(0, null, listView)
                    .findViewById<ImageView>(R.id.ivRemoveFolder)
                    .performClick()

                assertEquals(0, listView.adapter.count)
                assertTrue(SearchFolderStore(application).folders().isEmpty())
                assertTrue(application.contentResolver.persistedUriPermissions.isEmpty())
            }
        }
    }

    /** Drives the picker end to end, as the user would. */
    private fun pickFolder(activity: Activity, treeUri: Uri) {
        activity.findViewById<Button>(R.id.btnAddFolder).performClick()

        val started = Shadows.shadowOf(activity).nextStartedActivityForResult

        Shadows.shadowOf(activity).receiveResult(
            started.intent,
            Activity.RESULT_OK,
            Intent().setData(treeUri),
        )
    }

    private class NamingProvider : ContentProvider() {
        override fun onCreate() = true

        override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
                           selectionArgs: Array<out String>?, sortOrder: String?): Cursor =
            MatrixCursor(arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
                .apply { addRow(arrayOf(DocumentsContract.getDocumentId(uri).replaceFirstChar { it.uppercase() })) }

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?) = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?,
                            selectionArgs: Array<out String>?) = 0
    }

    private companion object {
        const val AUTHORITY = "be.robinj.distrohopper.test.pickerfolders"
    }
}
