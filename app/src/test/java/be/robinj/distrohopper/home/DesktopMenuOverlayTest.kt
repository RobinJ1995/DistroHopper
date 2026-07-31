package be.robinj.distrohopper.home

import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.pressBack
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.preferences.PreferencesActivity
import be.robinj.distrohopper.widgets.WidgetsPager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowDialog

/*
 * The long-press-on-empty-desktop menu (DesktopMenuOverlay): a bottom sheet
 * with the desktop-level actions, shown over a scrim that zooms the home
 * screen out behind it. Runs PAUSED so the open/close animations can be
 * drained deterministically.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class DesktopMenuOverlayTest {
    private lateinit var scenario: ActivityScenario<HomeActivity>

    @Before fun setUp() { this.scenario = ActivityTestSupport.launchHome() }
    @After fun tearDown() { this.scenario.close() }

    private fun longPressDesktop(activity: HomeActivity) {
        activity.findViewById<WidgetsPager>(R.id.vgWidgets).performLongClick()
    }

    /* Long-pressing empty desktop space opens the menu (and captures Back). */
    @Test fun longPressOnEmptyDesktopOpensTheMenu() {
        this.scenario.onActivity { activity ->
            assertFalse(DesktopMenuOverlay.isShowingIn(activity))

            this.longPressDesktop(activity)
            ActivityTestSupport.drainTasks()

            assertTrue(DesktopMenuOverlay.isShowingIn(activity))
            assertNotNull(activity.findViewById<View>(R.id.rowDesktopMenuAddWidget))
            assertNotNull(activity.findViewById<View>(R.id.rowDesktopMenuCustomise))
            assertNotNull(activity.findViewById<View>(R.id.rowDesktopMenuSettings))
            assertTrue(activity.onBackPressedDispatcher.hasEnabledCallbacks())
        }
    }

    /* While the menu is open the home screen behind it is zoomed out; the zoom
     * reverts once the menu is dismissed. */
    @Test fun menuZoomsTheHomeScreenOutAndBackIn() {
        this.scenario.onActivity { activity ->
            val backdrop = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
                .getChildAt(0)

            this.longPressDesktop(activity)
            ActivityTestSupport.drainTasks()
            assertTrue(backdrop.scaleX < 1f)

            DesktopMenuOverlay.dismissActive(activity)
            ActivityTestSupport.drainTasks()
            assertEquals(1f, backdrop.scaleX, 0.001f)
        }
    }

    /* HomeActivity is not recreated on rotation, so an open menu survives it:
     * the sheet's width cap must be re-derived when the overlay resizes (a
     * 480dp-capped landscape sheet must widen to full width on a narrow
     * portrait screen, and vice versa). */
    @Test fun sheetWidthIsRecomputedWhenTheOverlayResizes() {
        this.scenario.onActivity { activity ->
            this.longPressDesktop(activity)
            ActivityTestSupport.drainTasks()

            val sheet = activity.findViewById<View>(R.id.rowDesktopMenuAddWidget).parent as View
            val scrim = sheet.parent as View
            val cap = (480 * activity.resources.displayMetrics.density).toInt()

            scrim.layout(0, 0, cap + 1000, 1000) // wide (landscape/tablet): capped //
            assertEquals(cap, sheet.layoutParams.width)

            scrim.layout(0, 0, cap - 100, 800) // narrow (portrait phone): full width //
            assertEquals(FrameLayout.LayoutParams.MATCH_PARENT, sheet.layoutParams.width)
        }
    }

    /* Back closes the menu (and the callback relaxes again afterwards). */
    @Test fun backClosesTheMenu() {
        this.scenario.onActivity { activity ->
            this.longPressDesktop(activity)
            ActivityTestSupport.drainTasks()
            assertTrue(DesktopMenuOverlay.isShowingIn(activity))
        }

        pressBack()
        ActivityTestSupport.drainTasks() // let the close animation finish //

        this.scenario.onActivity { activity ->
            assertFalse(DesktopMenuOverlay.isShowingIn(activity))
            // Not the default launcher and nothing open: stop consuming Back //
            assertFalse(activity.onBackPressedDispatcher.hasEnabledCallbacks())
        }
    }

    /* Tapping the scrim outside the sheet dismisses the menu. */
    @Test fun tappingOutsideTheSheetClosesTheMenu() {
        this.scenario.onActivity { activity ->
            this.longPressDesktop(activity)
            ActivityTestSupport.drainTasks()

            // The scrim is the sheet row's grandparent (sheet -> scrim) //
            val sheet = activity.findViewById<View>(R.id.rowDesktopMenuAddWidget).parent as View
            (sheet.parent as View).performClick()
            ActivityTestSupport.drainTasks()

            assertFalse(DesktopMenuOverlay.isShowingIn(activity))
        }
    }

    /* The "Add widget" row dismisses the menu and opens the widget picker. */
    @Test fun addWidgetRowOpensTheWidgetPicker() {
        this.scenario.onActivity { activity ->
            this.longPressDesktop(activity)
            ActivityTestSupport.drainTasks()

            activity.findViewById<View>(R.id.rowDesktopMenuAddWidget).performClick()
            ActivityTestSupport.drainTasks()

            assertFalse(DesktopMenuOverlay.isShowingIn(activity))
            val dialog = ShadowDialog.getLatestDialog()
            assertNotNull(dialog)
            assertTrue(dialog.isShowing)
        }
    }

    /* The "Customise" row dismisses the menu and relaunches into the customise
     * UI, the same path as the Preferences screen's "Customise" entry: the
     * activity recreates with a customise=true intent (which onCreate() reads). */
    @Test fun customiseRowEntersCustomiseMode() {
        this.scenario.onActivity { activity ->
            this.longPressDesktop(activity)
            ActivityTestSupport.drainTasks()

            activity.findViewById<View>(R.id.rowDesktopMenuCustomise).performClick()

            assertFalse(DesktopMenuOverlay.isShowingIn(activity))
            assertTrue(activity.intent.getBooleanExtra("customise", false))
        }
    }

    /* The "Settings" row dismisses the menu and opens the preferences screen. */
    @Test fun settingsRowOpensThePreferences() {
        this.scenario.onActivity { activity ->
            this.longPressDesktop(activity)
            ActivityTestSupport.drainTasks()

            activity.findViewById<View>(R.id.rowDesktopMenuSettings).performClick()
            ActivityTestSupport.drainTasks()

            assertFalse(DesktopMenuOverlay.isShowingIn(activity))
            val started = Shadows.shadowOf(activity).nextStartedActivity
            assertNotNull(started)
            assertEquals(PreferencesActivity::class.java.name, started.component?.className)
        }
    }

}
