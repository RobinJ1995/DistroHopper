package be.robinj.distrohopper.desktop.launcher

import android.view.DragEvent
import android.view.View
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.DragEvents
import be.robinj.distrohopper.HomeActivity
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

/**
 * The chrome listener that feeds the cross-surface drag's open/close-the-dash
 * controller. The controller's own state machine is covered by
 * [DashCrossSurfaceControllerTest]; this verifies the DragEvent→controller
 * mapping — which action registers/clears a hover target, and that ENDED counts
 * as an exit — against the real controller wired into a launched [HomeActivity],
 * plus one end-to-end close.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class DashEdgeDragListenerTest {
    private lateinit var scenario: ActivityScenario<HomeActivity>

    // A BFB is an open-target; the launcher/panel are close-targets. The ids only
    // need to be distinct so the controller can track the two as separate sets.
    private val bfbId = 4001
    private val panelId = 4002

    @Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
    @After fun tearDown() { scenario.close() }

    /** The controller tracks hovered view ids in two private sets, keyed by [open]. */
    private fun targets(controller: DashCrossSurfaceController, open: Boolean): Set<Int> {
        val field = DashCrossSurfaceController::class.java
            .getDeclaredField(if (open) "openTargets" else "closeTargets")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return (field.get(controller) as Set<Int>).toSet()
    }

    @Test fun dragStartedReturnsTrueToClaimTheView() {
        scenario.onActivity { activity ->
            val view = View(activity)
            assertTrue(DashEdgeDragListener(activity, open = true)
                .onDrag(view, DragEvents.obtain(DragEvent.ACTION_DRAG_STARTED)))
            assertTrue(DashEdgeDragListener(activity, open = false)
                .onDrag(view, DragEvents.obtain(DragEvent.ACTION_DRAG_STARTED)))
        }
    }

    @Test fun enteringRegistersTheViewAsTheRightKindOfTarget() {
        scenario.onActivity { activity ->
            val controller = activity.dashCrossSurface
            val bfb = View(activity).apply { id = bfbId }
            val panel = View(activity).apply { id = panelId }

            DashEdgeDragListener(activity, open = true)
                .onDrag(bfb, DragEvents.obtain(DragEvent.ACTION_DRAG_ENTERED))
            DashEdgeDragListener(activity, open = false)
                .onDrag(panel, DragEvents.obtain(DragEvent.ACTION_DRAG_ENTERED))

            assertTrue("BFB hover is an open-target", bfbId in targets(controller, open = true))
            assertTrue("panel hover is a close-target", panelId in targets(controller, open = false))
            assertFalse("a BFB is never a close-target", bfbId in targets(controller, open = false))
            controller.reset() // cancel the pending debounced apply before teardown
        }
    }

    @Test fun exitedRemovesTheTargetFromTheController() {
        scenario.onActivity { activity ->
            val controller = activity.dashCrossSurface
            val bfb = View(activity).apply { id = bfbId }
            val listener = DashEdgeDragListener(activity, open = true)

            listener.onDrag(bfb, DragEvents.obtain(DragEvent.ACTION_DRAG_ENTERED))
            assertTrue(bfbId in targets(controller, open = true))

            listener.onDrag(bfb, DragEvents.obtain(DragEvent.ACTION_DRAG_EXITED))
            assertFalse(bfbId in targets(controller, open = true))
            controller.reset()
        }
    }

    @Test fun dragEndedAlsoReportsExitToTheController() {
        scenario.onActivity { activity ->
            val controller = activity.dashCrossSurface
            val bfb = View(activity).apply { id = bfbId }
            val listener = DashEdgeDragListener(activity, open = true)

            listener.onDrag(bfb, DragEvents.obtain(DragEvent.ACTION_DRAG_ENTERED))
            // ENDED maps to the same exit as EXITED, so the target is dropped.
            listener.onDrag(bfb, DragEvents.obtain(DragEvent.ACTION_DRAG_ENDED))

            assertFalse(bfbId in targets(controller, open = true))
            controller.reset()
        }
    }

    @Test fun enteringACloseTargetWhileOpenClosesTheDash() {
        scenario.onActivity { activity ->
            activity.openDash()
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            assertTrue(activity.dashIsOpen())
            val panel = View(activity).apply { id = panelId }

            DashEdgeDragListener(activity, open = false)
                .onDrag(panel, DragEvents.obtain(DragEvent.ACTION_DRAG_ENTERED))
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertFalse(activity.dashIsOpen())
        }
    }
}
