package be.robinj.distrohopper

import android.app.Application
import android.content.Intent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.folder.FolderOverlay
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.LooperMode

/*
 * Pressing Home (or the home gesture) while the launcher is already foreground
 * is delivered to the running, singleTop activity as onNewIntent with an
 * ACTION_MAIN / CATEGORY_HOME intent. Besides returning to the first desktop,
 * that must close an open dash. Driven through an ActivityController because
 * ActivityScenario cannot deliver a new intent to onNewIntent.
 *
 * Runs under the PAUSED looper so the dash close animation can finish.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class HomeHomeButtonTest {
    private lateinit var controller: ActivityController<HomeActivity>

    @Before fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        listOf(Preferences.PREFERENCES, Preferences.PINNED_APPS, Preferences.LENSES).forEach {
            application.getSharedPreferences(it, 0).edit().clear().commit()
        }
        // Without this HomeActivity redirects to the first-run wizard //
        application.getSharedPreferences(Preferences.PREFERENCES, 0).edit()
            .putBoolean(Preference.SETUP_COMPLETED.getName(), true).commit()
        DependencyContainer.of(application).customiseMode.value = false
        ActivityTestSupport.installTestDispatchers()
        ActivityTestSupport.seedPackageManager()

        this.controller = Robolectric.buildActivity(HomeActivity::class.java).setup()
        ActivityTestSupport.drainTasks()
    }

    @After fun tearDown() { this.controller.close() }

    private fun homeIntent(): Intent =
        Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .setClass(this.controller.get(), HomeActivity::class.java)

    private fun llDash() = this.controller.get().findViewById<View>(R.id.llDash)

    @Test fun pressingHomeWhileDashOpenClosesIt() {
        this.controller.get().openDash()
        assertEquals(View.VISIBLE, this.llDash().visibility)

        this.controller.newIntent(this.homeIntent())
        ActivityTestSupport.drainTasks() // let the close animation finish //

        assertEquals(View.GONE, this.llDash().visibility)
    }

    /* Home dismisses a folder popup along with the dash it was opened from. */
    @Test fun pressingHomeWhileFolderOpenClosesFolderAndDash() {
        val activity = this.controller.get()
        activity.openDash()
        ActivityTestSupport.openDashFolder(activity)
        assertTrue(FolderOverlay.isShowingIn(activity))

        this.controller.newIntent(this.homeIntent())
        ActivityTestSupport.drainTasks() // let the close animations finish //

        assertFalse(FolderOverlay.isShowingIn(activity))
        assertEquals(View.GONE, this.llDash().visibility)
    }

    /* Home also dismisses the launcher-bar folder popover. */
    @Test fun pressingHomeWhileLauncherFolderOpenClosesIt() {
        val activity = this.controller.get()
        ActivityTestSupport.openLauncherFolder(activity)
        assertTrue(FolderOverlay.isShowingIn(activity))

        this.controller.newIntent(this.homeIntent())
        ActivityTestSupport.drainTasks()

        assertFalse(FolderOverlay.isShowingIn(activity))
    }

    /* Home also dismisses the desktop folder popover. */
    @Test fun pressingHomeWhileDesktopFolderOpenClosesIt() {
        val activity = this.controller.get()
        ActivityTestSupport.openDesktopFolder(activity)
        assertTrue(FolderOverlay.isShowingIn(activity))

        this.controller.newIntent(this.homeIntent())
        ActivityTestSupport.drainTasks()

        assertFalse(FolderOverlay.isShowingIn(activity))
    }

    @Test fun pressingHomeWithDashClosedLeavesItClosed() {
        assertEquals(View.GONE, this.llDash().visibility)

        this.controller.newIntent(this.homeIntent())
        ActivityTestSupport.drainTasks()

        assertEquals(View.GONE, this.llDash().visibility)
    }
}
