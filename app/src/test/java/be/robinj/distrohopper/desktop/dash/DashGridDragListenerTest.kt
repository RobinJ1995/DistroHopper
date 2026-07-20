package be.robinj.distrohopper.desktop.dash

import android.content.Context
import android.view.DragEvent
import android.view.View
import android.widget.AdapterView
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

/**
 * The dash grid's drag listener: the spatial reorder-preview / fold-on-centre
 * state machine for loose apps and folders, plus the folder-member-extraction
 * gesture. The folder + custom-order model itself is covered by
 * [be.robinj.distrohopper.DashLayoutRepositoryTest]; this verifies the listener
 * glue drives it correctly from DragEvents.
 *
 * The grid is a [GeoGrid] with a real [GridAdapter] and a deterministic cell
 * geometry (fixed column width / row height), so a drag can be aimed at a cell's
 * centre (a fold) or its edge (a reorder gap) without relying on Robolectric to
 * lay a GridView out. A drag is delivered as the real event sequence — STARTED
 * (which arms the preview), LOCATION (which resolves fold vs reorder), then DROP.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class DashGridDragListenerTest {
    private lateinit var scenario: ActivityScenario<HomeActivity>

    @Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
    @After fun tearDown() { scenario.close() }

    private companion object {
        const val COLS = 3
        const val CELL = 100
    }

    /**
     * A grid with a real [GridAdapter] and a fixed COLS-wide layout of CELL-sized
     * cells, so `pointToPosition` and each child's bounds are known: a point can
     * be aimed at a cell's centre or edge to pick the fold / reorder branch. The
     * children are stubbed on demand (positioned from the adapter's current order)
     * since Robolectric doesn't lay a GridView out.
     */
    private class GeoGrid(context: Context, items: List<DashItem>) : GridView(context) {
        init { adapter = GridAdapter(context, ArrayList(items)) }

        // Read through getAdapter() (null-safe): the GridView super-constructor
        // calls the overrides below before any field of this subclass is set.
        private fun items(): GridAdapter? = this.adapter as? GridAdapter

        override fun getChildCount(): Int = this.items()?.count ?: 0
        override fun getFirstVisiblePosition(): Int = 0

        override fun getChildAt(index: Int): View? {
            val adapter = this.items() ?: return null
            if (index < 0 || index >= adapter.count) return null
            val col = index % COLS
            val row = index / COLS
            return View(context).apply {
                layout(col * CELL, row * CELL, col * CELL + CELL, row * CELL + CELL)
                tag = adapter.getItem(index)
            }
        }

        override fun pointToPosition(x: Int, y: Int): Int {
            val adapter = this.items() ?: return AdapterView.INVALID_POSITION
            if (x < 0 || y < 0) return AdapterView.INVALID_POSITION
            val col = x / CELL
            val row = y / CELL
            if (col >= COLS) return AdapterView.INVALID_POSITION
            val pos = row * COLS + col
            return if (pos < adapter.count) pos else AdapterView.INVALID_POSITION
        }

        /** Screen point at the centre of the cell currently showing [position] (a fold). */
        fun centreOf(position: Int): Pair<Float, Float> {
            val col = position % COLS
            val row = position / COLS
            return (col * CELL + CELL / 2f) to (row * CELL + CELL / 2f)
        }

        /** Screen point near the left edge of [position]'s cell (a reorder gap before it). */
        fun leftEdgeOf(position: Int): Pair<Float, Float> {
            val col = position % COLS
            val row = position / COLS
            return (col * CELL + 5f) to (row * CELL + CELL / 2f)
        }

        /** Screen point near the right edge of [position]'s cell (a reorder gap after it). */
        fun rightEdgeOf(position: Int): Pair<Float, Float> {
            val col = position % COLS
            val row = position / COLS
            return (col * CELL + CELL - 5f) to (row * CELL + CELL / 2f)
        }

        /** Screen point in the empty area below the last laid-out cell (a genuine append). */
        fun belowLastCell(): Pair<Float, Float> {
            val count = this.items()?.count ?: 0
            val lastRow = if (count == 0) 0 else (count - 1) / COLS
            return 5f to ((lastRow + 1) * CELL + 5f)
        }

        /** An invalid point that is NOT below the content (like the reserved title padding). */
        fun invalidNearTop(): Pair<Float, Float> = (COLS * CELL + 5f) to 5f
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

    private fun indexOf(layout: DashLayoutRepository, label: String): Int =
        layout.dashItems(null).indexOfFirst { it is DashItem.AppItem && it.app.label == label }

    private fun folderIndex(layout: DashLayoutRepository): Int =
        layout.dashItems(null).indexOfFirst { it is DashItem.FolderItem }

    // --- ACTION_DRAG_STARTED: which drags this listener claims ---

    @Test fun dragStartedClaimsLooseAppAndFolderPayloadDrags() {
        scenario.onActivity { activity ->
            val listener = DashGridDragListener(activity, activity.appManager, null)
            val grid = GeoGrid(activity, activity.appManager.dashLayout.dashItems(null))
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
            val grid = GeoGrid(activity, activity.appManager.dashLayout.dashItems(null))

            assertFalse("no local state (e.g. a widget drag) is left to other listeners",
                listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_STARTED, localState = null)))
            assertFalse("a non-dash payload is not claimed",
                listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_STARTED, localState = "widget")))
        }
    }

    // --- Folder create / add (drop over a cell's centre) ---

    @Test fun droppingOverAnotherAppsCentreCreatesAFolder() {
        scenario.onActivity { activity ->
            val appManager = activity.appManager
            val layout = appManager.dashLayout
            val alpha = this.app(layout, "Alpha")
            val grid = GeoGrid(activity, layout.dashItems(null))
            val (x, y) = grid.centreOf(this.indexOf(layout, "Beta"))
            val listener = DashGridDragListener(activity, appManager, null)

            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_STARTED, localState = alpha))
            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_LOCATION, x, y, alpha))
            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DROP, x, y, alpha))

            val folders = layout.dashItems(null).filterIsInstance<DashItem.FolderItem>()
            assertEquals(1, folders.size)
            assertEquals(setOf("Alpha", "Beta"), folders[0].apps.map { it.label }.toSet())
        }
    }

    @Test fun droppingOverAFoldersCentreAddsTheAppToIt() {
        scenario.onActivity { activity ->
            val appManager = activity.appManager
            val layout = appManager.dashLayout
            layout.createFolder(this.app(layout, "Alpha"), this.app(layout, "Beta"))
            val gamma = this.app(layout, "Gamma")
            val grid = GeoGrid(activity, layout.dashItems(null))
            val (x, y) = grid.centreOf(this.folderIndex(layout))
            val listener = DashGridDragListener(activity, appManager, null)

            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_STARTED, localState = gamma))
            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_LOCATION, x, y, gamma))
            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DROP, x, y, gamma))

            val folder = layout.dashItems(null).filterIsInstance<DashItem.FolderItem>().single()
            assertTrue(folder.apps.map { it.label }.containsAll(listOf("Alpha", "Beta", "Gamma")))
        }
    }

    @Test fun draggingOverAnAppsEdgeDoesNotFoldButReorders() {
        scenario.onActivity { activity ->
            val appManager = activity.appManager
            val layout = appManager.dashLayout
            this.setCustomOrder(activity)
            val settings = this.app(layout, "Settings")
            val grid = GeoGrid(activity, layout.dashItems(null))
            val (x, y) = grid.leftEdgeOf(0) // the front cell's edge is a reorder gap, not a fold
            val listener = DashGridDragListener(activity, appManager, null)

            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_STARTED, localState = settings))
            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_LOCATION, x, y, settings))
            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DROP, x, y, settings))

            assertTrue("no folder is created by an edge drop",
                layout.dashItems(null).none { it is DashItem.FolderItem })
            assertEquals("Settings", (layout.dashItems(null).first() as DashItem.AppItem).app.label)
        }
    }

    // --- Reorder (custom order only) ---

    @Test fun draggingToACellsEdgeReordersInCustomOrder() {
        scenario.onActivity { activity ->
            val appManager = activity.appManager
            val layout = appManager.dashLayout
            this.setCustomOrder(activity)
            val settings = this.app(layout, "Settings")
            val grid = GeoGrid(activity, layout.dashItems(null))
            val (x, y) = grid.leftEdgeOf(0) // open a gap before the first cell
            val listener = DashGridDragListener(activity, appManager, null)

            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_STARTED, localState = settings))
            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_LOCATION, x, y, settings))
            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DROP, x, y, settings))

            assertEquals("Settings", (layout.dashItems(null).first() as DashItem.AppItem).app.label)
        }
    }

    @Test fun draggingToACellsEdgeDoesNotReorderWhenNotInCustomOrder() {
        scenario.onActivity { activity ->
            val appManager = activity.appManager
            val layout = appManager.dashLayout // default alphabetical order
            val before = this.labels(layout)
            val zeta = this.app(layout, "Zeta")
            val grid = GeoGrid(activity, layout.dashItems(null))
            val (x, y) = grid.leftEdgeOf(0)
            val listener = DashGridDragListener(activity, appManager, null)

            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_STARTED, localState = zeta))
            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_LOCATION, x, y, zeta))
            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DROP, x, y, zeta))

            assertEquals(before, this.labels(layout))
        }
    }

    @Test fun draggingBelowTheLastCellAppendsToTheEndInCustomOrder() {
        scenario.onActivity { activity ->
            val appManager = activity.appManager
            val layout = appManager.dashLayout
            this.setCustomOrder(activity)
            val alpha = this.app(layout, "Alpha") // alphabetically first
            val grid = GeoGrid(activity, layout.dashItems(null))
            val (x, y) = grid.belowLastCell()
            val listener = DashGridDragListener(activity, appManager, null)

            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_STARTED, localState = alpha))
            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_LOCATION, x, y, alpha))
            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DROP, x, y, alpha))

            assertEquals("Alpha", (layout.dashItems(null).last() as DashItem.AppItem).app.label)
        }
    }

    @Test fun draggingIntoInvalidPaddingAboveTheGridDoesNotAppendToTheEnd() {
        scenario.onActivity { activity ->
            val appManager = activity.appManager
            val layout = appManager.dashLayout
            this.setCustomOrder(activity)
            val before = this.labels(layout)
            val alpha = this.app(layout, "Alpha")
            val grid = GeoGrid(activity, layout.dashItems(null))
            // An invalid point that isn't below the last cell (the profile-title
            // padding above the first row) must not move the item to the end.
            val (x, y) = grid.invalidNearTop()
            val listener = DashGridDragListener(activity, appManager, null)

            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_STARTED, localState = alpha))
            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_LOCATION, x, y, alpha))
            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DROP, x, y, alpha))

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
            val grid = GeoGrid(activity, layout.dashItems(null))
            val listener = DashGridDragListener(activity, appManager, null)

            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DROP, -1f, -1f,
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
            val targetPos = layout.dashItems(null).size - 1 // send the folder to the end
            val grid = GeoGrid(activity, layout.dashItems(null))
            val (x, y) = grid.rightEdgeOf(targetPos) // a gap after the last cell
            val listener = DashGridDragListener(activity, appManager, null)

            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_STARTED,
                localState = DashDragPayload.FolderDrag(folderId)))
            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_LOCATION, x, y,
                localState = DashDragPayload.FolderDrag(folderId)))
            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DROP, x, y,
                localState = DashDragPayload.FolderDrag(folderId)))

            assertEquals(targetPos, this.folderIndex(layout))
        }
    }

    @Test fun droppingAFolderDoesNotRepositionWhenNotInCustomOrder() {
        scenario.onActivity { activity ->
            val appManager = activity.appManager
            val layout = appManager.dashLayout // alphabetical: folder sorts first
            val folderId = layout.createFolder(this.app(layout, "Alpha"), this.app(layout, "Beta"))!!
            val before = this.folderIndex(layout)
            val targetPos = layout.dashItems(null).size - 1
            val grid = GeoGrid(activity, layout.dashItems(null))
            val (x, y) = grid.rightEdgeOf(targetPos)
            val listener = DashGridDragListener(activity, appManager, null)

            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_STARTED,
                localState = DashDragPayload.FolderDrag(folderId)))
            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DRAG_LOCATION, x, y,
                localState = DashDragPayload.FolderDrag(folderId)))
            listener.onDrag(grid, DragEvents.obtain(DragEvent.ACTION_DROP, x, y,
                localState = DashDragPayload.FolderDrag(folderId)))

            assertEquals(before, this.folderIndex(layout))
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
