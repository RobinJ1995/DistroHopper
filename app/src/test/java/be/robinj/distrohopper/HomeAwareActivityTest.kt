package be.robinj.distrohopper

import android.app.Application
import android.app.role.RoleManager
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.ThemePreferencesActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowRoleManager

/*
 * The settings sub-screens (Theme, Icons, About, ...) sit on top of HomeActivity in
 * the launcher's task while DistroHopper is the default launcher. A device HOME press
 * should drop the user back to the home screen — but opening one of those sub-screens
 * must NOT: tapping "Theme" or "Icons" once bounced the user to the home screen,
 * because the act of launching a child activity also fires onUserLeaveHint.
 */
@RunWith(RobolectricTestRunner::class)
class HomeAwareActivityTest {
    private lateinit var controller: ActivityController<TestHomeAwareActivity>
    private val activity get() = this.controller.get()

    @Before fun setUp() {
        this.controller = Robolectric.buildActivity(TestHomeAwareActivity::class.java).setup()
    }

    @After fun tearDown() { this.controller.close() }

    private fun holdHomeRole() {
        val roleManager = ApplicationProvider.getApplicationContext<Application>()
            .getSystemService(RoleManager::class.java)
        Shadow.extract<ShadowRoleManager>(roleManager).addHeldRole(RoleManager.ROLE_HOME)
    }

    private fun startedActivities(): List<Intent> = buildList {
        while (true) add(shadowOf(activity).nextStartedActivity ?: break)
    }

    @Test fun pressingHomeReturnsToHomeScreenWhileLauncher() {
        this.holdHomeRole()

        this.activity.simulateUserLeave()

        val started = this.startedActivities()
        assertEquals(1, started.size)
        assertEquals(HomeActivity::class.java.name, started[0].component?.className)
        // CLEAR_TOP collapses any settings screens stacked above HomeActivity. //
        assertTrue(started[0].flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertTrue(started[0].flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
    }

    @Test fun pressingHomeDoesNothingWhileNotLauncher() {
        // Home role not held: HOME must leave for the real launcher undisturbed. //
        this.activity.simulateUserLeave()

        assertNull(shadowOf(this.activity).nextStartedActivity)
    }

    @Test fun openingASubScreenDoesNotReturnToHomeScreen() {
        this.holdHomeRole()

        // Mimic tapping "Theme": the activity launches a child, which itself triggers
        // onUserLeaveHint. That must not be mistaken for a HOME press. //
        this.activity.startActivity(Intent(this.activity, ThemePreferencesActivity::class.java))
        this.activity.simulateUserLeave()

        val started = this.startedActivities().map { it.component?.className }
        assertEquals(listOf(ThemePreferencesActivity::class.java.name), started)
    }

    @Test fun pressingHomeAfterReturningFromASubScreenStillReturnsToHomeScreen() {
        this.holdHomeRole()

        // Open a sub-screen, then come back to this one (onResume clears the guard) ... //
        this.activity.startActivity(Intent(this.activity, ThemePreferencesActivity::class.java))
        this.startedActivities() // drain the child launch //
        this.controller.pause().resume()

        // ... a subsequent genuine HOME press must still return to the home screen. //
        this.activity.simulateUserLeave()

        val started = this.startedActivities()
        assertEquals(1, started.size)
        assertEquals(HomeActivity::class.java.name, started[0].component?.className)
    }
}

/** Minimal concrete subject that lets the test fire the protected HOME hook. */
class TestHomeAwareActivity : HomeAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // A bare test activity has no manifest theme; AppCompatActivity needs one. //
        this.setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
    }

    fun simulateUserLeave() = this.onUserLeaveHint()
}
