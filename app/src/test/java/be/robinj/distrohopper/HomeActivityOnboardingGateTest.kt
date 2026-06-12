package be.robinj.distrohopper

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.onboarding.OnboardingActivity
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class HomeActivityOnboardingGateTest {
    @Before fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        listOf(Preferences.PREFERENCES, Preferences.PINNED_APPS, Preferences.LENSES).forEach {
            application.getSharedPreferences(it, 0).edit().clear().commit()
        }
    }

    @Test fun firstRunRedirectsToTheWizardWithoutInitialisingTheHomeScreen() {
        val activity = Robolectric.buildActivity(HomeActivity::class.java).create().get()

        assertTrue(activity.isFinishing)
        assertEquals(
            OnboardingActivity::class.java.name,
            Shadows.shadowOf(activity).nextStartedActivity.component?.className,
        )
    }

    @Test fun completedSetupLaunchesTheHomeScreenNormally() {
        // launchHome() seeds SETUP_COMPLETED, like any device that finished the wizard //
        ActivityTestSupport.launchHome().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(false, activity.isFinishing)
                assertTrue(activity.findViewById<android.view.View>(R.id.llLauncherAndDashContainer) != null)
            }
        }
    }
}
