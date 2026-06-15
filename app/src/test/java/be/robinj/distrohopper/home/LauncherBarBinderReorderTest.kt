package be.robinj.distrohopper.home

import android.view.View
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.App
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.desktop.launcher.AppLauncher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

/**
 * The drag-to-reorder preview on the launcher bar: while a pinned icon is
 * dragged, its view stays behind as an invisible placeholder (the empty
 * slot), which shifts to whichever icon the drag hovers over; dropping
 * commits the previewed order, cancelling restores the original one.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class LauncherBarBinderReorderTest {
    private lateinit var scenario: ActivityScenario<HomeActivity>

    @Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
    @After fun tearDown() { scenario.close() }

    private fun pinThree(activity: HomeActivity): List<App> =
        listOf("com.example.alpha", "com.example.beta", "com.example.gamma").map { packageName ->
            val app = activity.appManager.findAppsByPackageName(packageName).first()
            activity.appManager.pin(app, false, true)
            app
        }

    private fun pinnedContainer(activity: HomeActivity): LinearLayout =
        activity.findViewById(R.id.llLauncherPinnedApps)

    private fun settingsShortcut(activity: HomeActivity): App =
        ActivityTestSupport.settingsShortcut(activity)

    private fun viewOrder(activity: HomeActivity): List<App> {
        val container = pinnedContainer(activity)
        return (0 until container.childCount).map { container.getChildAt(it).tag as App }
    }

    @Test fun startingADragLeavesAnInvisiblePlaceholderInTheIconsSlot() {
        scenario.onActivity { activity ->
            val (alpha, _, _) = pinThree(activity)

            activity.appManager.startedDraggingPinnedApp(alpha)

            val placeholder = pinnedContainer(activity).findViewWithTag<AppLauncher>(alpha)
            assertNotNull(placeholder)
            assertEquals(View.INVISIBLE, placeholder.visibility)
            assertEquals(0, pinnedContainer(activity).indexOfChild(placeholder))
        }
    }

    @Test fun hoveringOverAnotherIconShiftsThePlaceholderWithoutTouchingTheModel() {
        scenario.onActivity { activity ->
            val (alpha, beta, gamma) = pinThree(activity)

            activity.appManager.startedDraggingPinnedApp(alpha)
            activity.appManager.draggedPinnedAppOver(gamma)

            assertEquals(listOf(beta, gamma, alpha), viewOrder(activity))
            assertEquals(listOf(alpha, beta, gamma), activity.appManager.pinned)
        }
    }

    @Test fun droppingCommitsThePreviewedOrder() {
        scenario.onActivity { activity ->
            val (alpha, beta, gamma) = pinThree(activity)

            activity.appManager.startedDraggingPinnedApp(alpha)
            activity.appManager.draggedPinnedAppOver(gamma)
            activity.appManager.droppedPinnedApp()
            activity.appManager.endedDraggingPinnedApp()

            assertEquals(listOf(beta, gamma, alpha), activity.appManager.pinned)
            assertEquals(listOf(beta, gamma, alpha), viewOrder(activity))
            val dragged = pinnedContainer(activity).findViewWithTag<AppLauncher>(alpha)
            assertEquals(View.VISIBLE, dragged.visibility)
        }
    }

    @Test fun hoveringBackAndForthPreviewsTheLatestPosition() {
        scenario.onActivity { activity ->
            val (alpha, beta, gamma) = pinThree(activity)

            activity.appManager.startedDraggingPinnedApp(beta)
            activity.appManager.draggedPinnedAppOver(gamma) // slot previews [alpha, gamma, _] //
            activity.appManager.draggedPinnedAppOver(alpha) // and now [_, alpha, gamma] //
            activity.appManager.droppedPinnedApp()
            activity.appManager.endedDraggingPinnedApp()

            assertEquals(listOf(beta, alpha, gamma), activity.appManager.pinned)
            assertEquals(listOf(beta, alpha, gamma), viewOrder(activity))
        }
    }

    @Test fun endingWithoutADropRestoresTheOriginalOrder() {
        scenario.onActivity { activity ->
            val (alpha, beta, gamma) = pinThree(activity)

            activity.appManager.startedDraggingPinnedApp(alpha)
            activity.appManager.draggedPinnedAppOver(gamma)
            activity.appManager.endedDraggingPinnedApp()

            assertEquals(listOf(alpha, beta, gamma), activity.appManager.pinned)
            assertEquals(listOf(alpha, beta, gamma), viewOrder(activity))
        }
    }

    @Test fun droppingBackOnTheOriginalSlotLeavesTheOrderUnchanged() {
        scenario.onActivity { activity ->
            val (alpha, beta, gamma) = pinThree(activity)

            activity.appManager.startedDraggingPinnedApp(beta)
            activity.appManager.draggedPinnedAppOver(gamma)
            activity.appManager.draggedPinnedAppOver(gamma) // back to where it started //
            activity.appManager.droppedPinnedApp()
            activity.appManager.endedDraggingPinnedApp()

            assertEquals(listOf(alpha, beta, gamma), activity.appManager.pinned)
            assertEquals(listOf(alpha, beta, gamma), viewOrder(activity))
        }
    }

    @Test fun draggingADashAppOpensAPlaceholderSlotAtTheEndOfTheBar() {
        scenario.onActivity { activity ->
            val (alpha, beta, gamma) = pinThree(activity)
            val zeta = activity.appManager.findAppsByPackageName("com.example.zeta").first()

            activity.appManager.startedDraggingDashApp(zeta)

            assertEquals(listOf(alpha, beta, gamma, zeta), viewOrder(activity))
            val placeholder = pinnedContainer(activity).findViewWithTag<AppLauncher>(zeta)
            assertEquals(View.INVISIBLE, placeholder.visibility)
            assertEquals(listOf(alpha, beta, gamma), activity.appManager.pinned)
        }
    }

    @Test fun droppingSettingsShortcutFromDashPinsItAtThePreviewedSlot() {
        scenario.onActivity { activity ->
            val (alpha, beta, gamma) = pinThree(activity)
            val settings = settingsShortcut(activity)

            activity.appManager.startedDraggingDashApp(settings)
            activity.appManager.draggedPinnedAppOver(beta)
            activity.appManager.droppedPinnedApp()
            activity.appManager.endedDraggingPinnedApp()

            assertEquals(listOf(alpha, settings, beta, gamma), activity.appManager.pinned)
            assertEquals(listOf(alpha, settings, beta, gamma), viewOrder(activity))
            assertEquals(View.VISIBLE,
                pinnedContainer(activity).findViewWithTag<AppLauncher>(settings).visibility)
        }
    }

    @Test fun droppingADashAppPinsItAtThePreviewedSlot() {
        scenario.onActivity { activity ->
            val (alpha, beta, gamma) = pinThree(activity)
            val zeta = activity.appManager.findAppsByPackageName("com.example.zeta").first()

            activity.appManager.startedDraggingDashApp(zeta)
            activity.appManager.draggedPinnedAppOver(beta)
            activity.appManager.droppedPinnedApp()
            activity.appManager.endedDraggingPinnedApp()

            assertEquals(listOf(alpha, zeta, beta, gamma), activity.appManager.pinned)
            assertEquals(listOf(alpha, zeta, beta, gamma), viewOrder(activity))
            assertEquals(View.VISIBLE,
                pinnedContainer(activity).findViewWithTag<AppLauncher>(zeta).visibility)
        }
    }

    @Test fun droppingADashAppWithoutHoveringPinsItAtTheEnd() {
        scenario.onActivity { activity ->
            val (alpha, beta, gamma) = pinThree(activity)
            val zeta = activity.appManager.findAppsByPackageName("com.example.zeta").first()

            activity.appManager.startedDraggingDashApp(zeta)
            activity.appManager.droppedPinnedApp()
            activity.appManager.endedDraggingPinnedApp()

            assertEquals(listOf(alpha, beta, gamma, zeta), activity.appManager.pinned)
            assertEquals(listOf(alpha, beta, gamma, zeta), viewOrder(activity))
        }
    }

    @Test fun cancellingADashAppDragPinsNothingAndRemovesThePlaceholder() {
        scenario.onActivity { activity ->
            val (alpha, beta, gamma) = pinThree(activity)
            val zeta = activity.appManager.findAppsByPackageName("com.example.zeta").first()

            activity.appManager.startedDraggingDashApp(zeta)
            activity.appManager.draggedPinnedAppOver(alpha)
            activity.appManager.endedDraggingPinnedApp()

            assertEquals(listOf(alpha, beta, gamma), activity.appManager.pinned)
            assertEquals(listOf(alpha, beta, gamma), viewOrder(activity))
            assertFalse(activity.appManager.isPinned(zeta))
        }
    }
}
