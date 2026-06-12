package be.robinj.distrohopper.preferences

import android.app.Application
import android.app.role.RoleManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowRoleManager

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class PreferencesDefaultLauncherOptionTest {
    private fun preference(activity: PreferencesActivity): androidx.preference.Preference =
        (activity.supportFragmentManager.findFragmentById(R.id.preferences_container)
            as PreferencesActivity.PreferencesFragment)
            .findPreference("dummy_set_default_launcher")!!

    @Test fun shownWhileNotTheDefaultLauncher() {
        ActivityScenario.launch(PreferencesActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(preference(activity).isVisible)
            }
        }
    }

    @Test fun hiddenOnceTheHomeRoleIsHeld() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val roleManager = application.getSystemService(RoleManager::class.java)
        Shadow.extract<ShadowRoleManager>(roleManager).addHeldRole(RoleManager.ROLE_HOME)

        ActivityScenario.launch(PreferencesActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(preference(activity).isVisible)
            }
        }
    }
}
