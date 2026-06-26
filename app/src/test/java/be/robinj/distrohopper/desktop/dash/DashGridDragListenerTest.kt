package be.robinj.distrohopper.desktop.dash

import android.content.Context
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.GridView
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.App
import be.robinj.distrohopper.DashLayoutRepository
import be.robinj.distrohopper.DragEvents
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.preferences.Preferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

/**
 * The dash grid's drag listener: the folder-create-on-dwell, drop-into-folder,
 * custom-order reposition and folder-member-extraction state machine. The folder
 * model itself is covered by [be.robinj.distrohopper.DashLayoutRepositoryTest];
 * this verifies the listener glue drives it correctly from DragEvents.
 *
 * The grid is a [FixedGrid] whose `pointToPosition` is pinned to a known cell so
 * the coordinate-driven branches are deterministic without Robolectric layout.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class DashGridDragListenerTest {
    private lateinit var scenario: ActivityScenario<HomeActivity>

    @Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
    @After fun tearDown() { scenario.close() }

    /** A grid whose adapter serves a known item list and whose hit-test is fixed. */
    private class FixedGrid(
        context: Context,
        private val items: List<DashItem>,
        private val pos: Int,
    ) : GridView(context) {
        init {
            adapter = object : BaseAdapter() {
                override fun getCount() = items.size
                override fun getItem(position: Int): Any = items[position]
                override fun getItemId(position: Int) = position.toLong()
                override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View =
                    convertView ?: View(context)
            }
        }

        override fun pointToPosition(x: Int, y: Int): Int = this.pos
    }

    private fun appItems(layout: DashLayoutRepository): List<DashItem.AppItem> =
        layout.dashItems(null).filterIsInstance<DashItem.AppItem>()

    private fun app(layout: DashLayoutRepository, label: String): App =
        this.appItems(layout).first { it.app.label == label }.app

    private fun labels(layout: DashLayoutRepository): List<String> =
        layout.dashItems(null).map {
            when (it) {
                is DashItem.AppItem -> it.app.label
                is DashItem.FolderItem -> "[folder]"
            }
        }

    private fun setCustomOrder(activity: HomeActivity) =
        Preferences.getSharedPreferences(activity).edit().putString("app_sort_order", "custom").commit()

    private fun drainDelayed() = ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

    // --- ACTION_DRAG_STARTED: which drags this listener claims ---

    @Test fun dragStartedClaimsLooseAppAndFolderPayloadDrags() {
        scenario.onActivity { activity ->
            val listener = DashGridDragListener(activity, activity.appManager, null)
            val grid = FixedGrid(activity, emptyList(), AdapterView.INVALID_POSITION)
            val anApp = this.app(activity.appManager.dashLayout, "Alpha")

            assertTrue("a loose app drag is claimed",
                listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_STARTED, localState = anApp)))
            assertTrue("a folder drag is claimed", listener.onDrag(grid,
                DragEvents.obtain(DragEvent.ACTION_DRAG_STARTED, localState = DashDragPayload.FolderDrag("f"))))
            assertTrue("a folder-member drag is claimed", listener.onDrag(grid,
                DragEvents.obtain(DragEvent.ACTION_DRAG_STARTED, localState = DashDragPayload.FolderMemberDrag("f", anApp))))
        }
    }

    @Test fun dragStartedIgnoresForeignDrags() {
        scenario.onActivity { activity ->
            val listener = DashGridDragListener(activity, activity.appManager, null)
            val grid = FixedGrid(activity, emptyList(), AdapterView.INVALID_POSITION)

            assertFalse("no local state (e.g. a widget drag) is left to other listeners",
                listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_STARTED, localState = null)))
            assertFalse("a non-dash payload is not claimed",
                listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_STARTED, localState = "widget")))
        }
    }

    // --- Folder create / add ---

    @Test fun dwellingOverAnotherAppAndDroppingCreatesAFolder() {
        scenario.onActivity { activity ->
            val appManager = activity.appManager
            val layout = appManager.dashLayout
            val items = layout.dashItems(null)
            val alpha = this.app(layout, "Alpha")
            val betaIndex = items.indexOfFirst { it is DashItem.AppItem && it.app.label == "Beta" }
            val grid = FixedGrid(activity, items, betaIndex)
            val listener = DashGridDragListener(activity, appManager, null)

            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_LOCATION, 10f, 10f, alpha))
            this.drainDelayed() // fire the dwell timer that arms the fold
            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DROP, 10f, 10f, alpha))

            val folders = layout.dashItems(null).filterIsInstance<DashItem.FolderItem>()
            assertEquals(1, folders.size)
            assertEquals(setOf("Alpha", "Beta"), folders[0].apps.map { it.label }.toSet())
        }
    }

    @Test fun droppingOnAFolderAfterDwellAddsTheAppToIt() {
        scenario.onActivity { activity ->
            val appManager = activity.appManager
            val layout = appManager.dashLayout
            layout.createFolder(this.app(layout, "Alpha"), this.app(layout, "Beta"))
            val gamma = this.app(layout, "Gamma")
            val items = layout.dashItems(null)
            val folderIndex = items.indexOfFirst { it is DashItem.FolderItem }
            val grid = FixedGrid(activity, items, folderIndex)
            val listener = DashGridDragListener(activity, appManager, null)

            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_LOCATION, 10f, 10f, gamma))
            this.drainDelayed()
            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DROP, 10f, 10f, gamma))

            val folder = layout.dashItems(null).filterIsInstance<DashItem.FolderItem>().single()
            assertTrue(folder.apps.map { it.label }.containsAll(listOf("Alpha", "Beta", "Gamma")))
        }
    }

    // --- Reorder (custom order only) ---

    @Test fun droppingALooseAppReordersItInCustomOrder() {
        scenario.onActivity { activity ->
            val appManager = activity.appManager
            val layout = appManager.dashLayout
            this.setCustomOrder(activity)
            val settings = this.app(layout, "Settings")
            val grid = FixedGrid(activity, layout.dashItems(null), 0) // drop at the front
            val listener = DashGridDragListener(activity, appManager, null)

            // No dwell, so this is a reorder rather than a fold.
            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DROP, 10f, 10f, settings))

            assertEquals("Settings", (layout.dashItems(null).first() as DashItem.AppItem).app.label)
        }
    }

    @Test fun droppingALooseAppDoesNotReorderWhenNotInCustomOrder() {
        scenario.onActivity { activity ->
            val appManager = activity.appManager
            val layout = appManager.dashLayout // default alphabetical order
            val before = this.labels(layout)
            val zeta = this.app(layout, "Zeta")
            val grid = FixedGrid(activity, layout.dashItems(null), 0)
            val listener = DashGridDragListener(activity, appManager, null)

            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DROP, 10f, 10f, zeta))

            assertEquals(before, this.labels(layout))
        }
    }

    // --- Folder members ---

    @Test fun droppingAFolderMemberOnTheDashExtractsIt() {
        scenario.onActivity { activity ->
            val appManager = activity.appManager
            val layout = appManager.dashLayout
            val alpha = this.app(layout, "Alpha")
            val folderId = layout.createFolder(alpha, this.app(layout, "Beta"))!!
            // Drop onto empty space (no cell under the pointer): a plain extraction.
            val grid = FixedGrid(activity, layout.dashItems(null), AdapterView.INVALID_POSITION)
            val listener = DashGridDragListener(activity, appManager, null)

            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DROP,
                localState = DashDragPayload.FolderMemberDrag(folderId, alpha)))

            // Removing a member down to one dissolves the folder: both apps loose.
            assertTrue(layout.dashItems(null).none { it is DashItem.FolderItem })
            assertTrue(this.appItems(layout).map { it.app.label }.containsAll(listOf("Alpha", "Beta")))
        }
    }

    // --- Whole-folder reposition ---

    @Test fun droppingAFolderRepositionsItInCustomOrder() {
        scenario.onActivity { activity ->
            val appManager = activity.appManager
            val layout = appManager.dashLayout
            this.setCustomOrder(activity)
            val folderId = layout.createFolder(this.app(layout, "Alpha"), this.app(layout, "Beta"))!!
            val items = layout.dashItems(null)
            val targetPos = items.size - 1 // send the folder to the end
            val grid = FixedGrid(activity, items, targetPos)
            val listener = DashGridDragListener(activity, appManager, null)

            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DROP,
                localState = DashDragPayload.FolderDrag(folderId)))

            assertEquals(targetPos, layout.dashItems(null).indexOfFirst { it is DashItem.FolderItem })
        }
    }

    @Test fun droppingAFolderDoesNotRepositionWhenNotInCustomOrder() {
        scenario.onActivity { activity ->
            val appManager = activity.appManager
            val layout = appManager.dashLayout // alphabetical: folder sorts first
            val folderId = layout.createFolder(this.app(layout, "Alpha"), this.app(layout, "Beta"))!!
            val before = layout.dashItems(null).indexOfFirst { it is DashItem.FolderItem }
            val grid = FixedGrid(activity, layout.dashItems(null), layout.dashItems(null).size - 1)
            val listener = DashGridDragListener(activity, appManager, null)

            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DROP,
                localState = DashDragPayload.FolderDrag(folderId)))

            assertEquals(before, layout.dashItems(null).indexOfFirst { it is DashItem.FolderItem })
        }
    }

    // --- ACTION_DRAG_ENDED housekeeping ---

    @Test fun dragEndedClearsHoverAndNotifiesWithoutThrowing() {
        scenario.onActivity { activity ->
            ActivityTestSupport.layoutDashApps(activity)
            val grid = ActivityTestSupport.dashGrid(activity) ?: return@onActivity
            val listener = DashGridDragListener(activity, activity.appManager, null)

            assertTrue(listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_ENDED)))
            ActivityTestSupport.drainTasks() // runs the posted stoppedDragging()
        }
    }
}
