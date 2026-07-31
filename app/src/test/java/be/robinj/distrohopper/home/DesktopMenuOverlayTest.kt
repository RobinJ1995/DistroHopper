package be.robinj.distrohopper.home

import android.app.Dialog
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.preferences.PreferencesActivity
import be.robinj.distrohopper.widgets.WidgetsPager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowDialog

/*
 * The long-press-on-empty-desktop menu (DesktopMenuOverlay): a bottom sheet in a
 * window of its own (so its blur-behind is clipped to what it covers), over a
 * home screen the overlay zooms out. Runs PAUSED so the open/close animations
 * can be drained deterministically.
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

    /* The sheet lives in the Dialog's window, not in the activity's. */
    private fun openMenu(activity: HomeActivity): Dialog {
        this.longPressDesktop(activity)
        ActivityTestSupport.drainTasks()

        return ShadowDialog.getLatestDialog()
    }

    /* Long-pressing empty desktop space opens the menu. */
    @Test fun longPressOnEmptyDesktopOpensTheMenu() {
        this.scenario.onActivity { activity ->
            assertFalse(DesktopMenuOverlay.isShowingIn(activity))

            val dialog = this.openMenu(activity)

            assertTrue(DesktopMenuOverlay.isShowingIn(activity))
            assertTrue(dialog.isShowing)
            assertNotNull(dialog.findViewById<View>(R.id.rowDesktopMenuAddWidget))
            assertNotNull(dialog.findViewById<View>(R.id.rowDesktopMenuCustomise))
            assertNotNull(dialog.findViewById<View>(R.id.rowDesktopMenuSettings))
        }
    }

    /* The sheet is its own bottom-edge window that blurs only within its own
     * bounds. The whole-screen blur (FLAG_BLUR_BEHIND, which ModernDialogTheme
     * turns on) must stay off, or the entire home screen frosts over. */
    @Test fun sheetBlursWithinItsOwnBoundsOnly() {
        this.scenario.onActivity { activity ->
            val dialog = this.openMenu(activity)
            val attributes = dialog.window!!.attributes

            assertTrue(attributes.gravity and android.view.Gravity.BOTTOM != 0)
            assertEquals(0, attributes.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            assertEquals(0, attributes.blurBehindRadius)

            val blur = dialog.context.theme.obtainStyledAttributes(
                intArrayOf(android.R.attr.windowBackgroundBlurRadius))
            try {
                assertTrue(blur.getDimensionPixelSize(0, 0) > 0)
            } finally {
                blur.recycle()
            }
        }
    }

    /* The sheet's window fits no insets, so it reaches the actual screen edge.
     * BOTTOM gravity resolves against the parent frame, which is inset by the
     * system bars until this says otherwise — leaving a strip of unblurred
     * wallpaper between the sheet and the navigation bar. */
    @Test fun sheetReachesTheScreenEdgeRatherThanTheInsetParentFrame() {
        this.scenario.onActivity { activity ->
            val attributes = this.openMenu(activity).window!!.attributes

            assertEquals(0, attributes.fitInsetsTypes)
        }
    }

    /* While the menu is open the desktop and the launcher behind it are zoomed
     * out; the zoom reverts once the menu is dismissed. */
    @Test fun menuZoomsTheDesktopOutAndBackIn() {
        this.scenario.onActivity { activity ->
            val desktop = activity.findViewById<View>(R.id.vgWidgets)
            val launcher = activity.findViewById<View>(R.id.llLauncherAndDashContainer)

            this.openMenu(activity)
            assertTrue(desktop.scaleX < 1f)
            assertTrue(launcher.scaleX < 1f)

            DesktopMenuOverlay.dismissActive(activity)
            ActivityTestSupport.drainTasks()
            assertEquals(1f, desktop.scaleX, 0.001f)
            assertEquals(1f, launcher.scaleX, 0.001f)
        }
    }

    /* The panel, the status bar and the wallpaper are excluded from the zoom:
     * shrinking them away from the screen edge reads as a glitch. */
    @Test fun menuLeavesThePanelStatusBarAndWallpaperUnzoomed() {
        this.scenario.onActivity { activity ->
            this.openMenu(activity)

            for (id in intArrayOf(R.id.llPanel, R.id.llStatusBar, R.id.wpWallpaper,
                    R.id.flWallpaperOverlayContainer)) {
                assertEquals(1f, activity.findViewById<View>(id).scaleX, 0.001f)
                assertEquals(1f, activity.findViewById<View>(id).scaleY, 0.001f)
            }
        }
    }

    /* The zoomed views share one pivot — the centre of their common parent — so
     * they scale as a single piece rather than each toward its own middle. */
    @Test fun zoomedViewsShareTheParentCentreAsPivot() {
        this.scenario.onActivity { activity ->
            this.openMenu(activity)

            val container = activity.findViewById<View>(R.id.rlContainer)
            for (id in intArrayOf(R.id.vgWidgets, R.id.llLauncherAndDashContainer)) {
                val view = activity.findViewById<View>(id)
                assertEquals(container.width / 2f, view.pivotX + view.left, 0.001f)
                assertEquals(container.height / 2f, view.pivotY + view.top, 0.001f)
            }
        }
    }

    /* On a phone the sheet spans the screen. */
    @Test fun sheetSpansTheWidthOfANarrowScreen() {
        this.scenario.onActivity { activity ->
            val attributes = this.openMenu(activity).window!!.attributes

            assertEquals(WindowManager.LayoutParams.MATCH_PARENT, attributes.width)
        }
    }

    /* On a wide screen it is capped (and centred) rather than stretched across. */
    @Test @Config(qualifiers = "w1200dp-h800dp") fun sheetIsCappedOnAWideScreen() {
        this.scenario.onActivity { activity ->
            val attributes = this.openMenu(activity).window!!.attributes
            val cap = (480 * activity.resources.displayMetrics.density).toInt()

            assertEquals(cap, attributes.width)
        }
    }

    /* Back closes the menu. The sheet's window has focus, so Back reaches the
     * Dialog rather than HomeActivity's callback. */
    @Test fun backClosesTheMenu() {
        this.scenario.onActivity { activity ->
            val dialog = this.openMenu(activity)

            dialog.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
            dialog.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
            ActivityTestSupport.drainTasks()

            assertFalse(DesktopMenuOverlay.isShowingIn(activity))
            // Not the default launcher and nothing open: stop consuming Back //
            assertFalse(activity.onBackPressedDispatcher.hasEnabledCallbacks())
        }
    }

    /* Tapping outside the sheet dismisses it (the same cancel path as Back). */
    @Test fun theSheetIsDismissedByCancelling() {
        this.scenario.onActivity { activity ->
            val dialog = this.openMenu(activity)
            assertTrue(Shadows.shadowOf(dialog).isCancelableOnTouchOutside)

            dialog.cancel()
            ActivityTestSupport.drainTasks()

            assertFalse(DesktopMenuOverlay.isShowingIn(activity))
        }
    }

    /* The "Widgets" action dismisses the menu and opens the widget picker. */
    @Test fun widgetsActionOpensTheWidgetPicker() {
        this.scenario.onActivity { activity ->
            val menu = this.openMenu(activity)

            menu.findViewById<View>(R.id.rowDesktopMenuAddWidget).performClick()
            ActivityTestSupport.drainTasks()

            assertFalse(DesktopMenuOverlay.isShowingIn(activity))
            val picker = ShadowDialog.getLatestDialog()
            assertNotSame(menu, picker)
            assertTrue(picker.isShowing)
        }
    }

    /* The "Customise" action dismisses the menu and relaunches into the customise
     * UI, the same path as the Preferences screen's "Customise" entry: the
     * activity recreates with a customise=true intent (which onCreate() reads). */
    @Test fun customiseActionEntersCustomiseMode() {
        this.scenario.onActivity { activity ->
            val menu = this.openMenu(activity)

            menu.findViewById<View>(R.id.rowDesktopMenuCustomise).performClick()

            assertFalse(DesktopMenuOverlay.isShowingIn(activity))
            assertTrue(activity.intent.getBooleanExtra("customise", false))
        }
    }

    /* The "Settings" action dismisses the menu and opens the preferences screen. */
    @Test fun settingsActionOpensThePreferences() {
        this.scenario.onActivity { activity ->
            val menu = this.openMenu(activity)

            menu.findViewById<View>(R.id.rowDesktopMenuSettings).performClick()
            ActivityTestSupport.drainTasks()

            assertFalse(DesktopMenuOverlay.isShowingIn(activity))
            val started = Shadows.shadowOf(activity).nextStartedActivity
            assertNotNull(started)
            assertEquals(PreferencesActivity::class.java.name, started.component?.className)
        }
    }

}
