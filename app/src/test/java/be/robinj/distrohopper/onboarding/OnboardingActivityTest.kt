package be.robinj.distrohopper.onboarding

import android.app.Application
import android.content.Intent
import android.widget.Button
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.viewpager2.widget.ViewPager2
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import be.robinj.distrohopper.broadcast.AppUpgradeReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class OnboardingActivityTest {
    private lateinit var application: Application

    @Before fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        application.getSharedPreferences(Preferences.PREFERENCES, 0).edit().clear().commit()
    }

    private fun launch(): ActivityScenario<OnboardingActivity> =
        ActivityScenario.launch(OnboardingActivity::class.java)
            .also { ShadowLooper.runUiThreadTasksIncludingDelayedTasks() }

    @Test fun nextAdvancesThroughThePagesAndBackStepsBack() {
        launch().use { scenario ->
            scenario.onActivity { activity ->
                // Smooth scrolls never settle under the legacy looper (the clock
                // doesn't advance); queue them instead — currentItem updates synchronously //
                Robolectric.getForegroundThreadScheduler().pause()

                val pager = activity.findViewById<ViewPager2>(R.id.vpOnboarding)
                val next = activity.findViewById<Button>(R.id.btnOnboardingNext)

                assertEquals(0, pager.currentItem)
                next.performClick()
                assertEquals(1, pager.currentItem)

                activity.onBackPressedDispatcher.onBackPressed()
                assertEquals(0, pager.currentItem)

                // Back on the first page must not finish the wizard //
                activity.onBackPressedDispatcher.onBackPressed()
                assertEquals(0, pager.currentItem)
                assertFalse(activity.isFinishing)
            }
        }
    }

    @Test fun tappingAThemeCardPersistsTheThemeAndItsEdges() {
        launch().use { scenario ->
            scenario.onActivity { activity ->
                val pager = activity.findViewById<ViewPager2>(R.id.vpOnboarding)
                pager.setCurrentItem(OnboardingPage.THEME.ordinal, false)
                ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

                val cards = activity.findViewById<LinearLayout>(R.id.llOnboardingThemeCards)
                assertNotNull("theme page should be bound", cards)
                cards.getChildAt(1).performClick() // Gnome //

                val prefs = application.getSharedPreferences(Preferences.PREFERENCES, 0)
                assertEquals("gnome", prefs.getString(Preference.THEME.getName(), null))
                assertTrue(prefs.contains(Preference.LAUNCHER_EDGE.getName()))
                assertTrue(prefs.contains(Preference.PANEL_EDGE.getName()))
            }
        }
    }

    @Test fun doneMarksSetupCompleteAndRelaunchesHome() {
        launch().use { scenario ->
            scenario.onActivity { activity ->
                val pager = activity.findViewById<ViewPager2>(R.id.vpOnboarding)
                val next = activity.findViewById<Button>(R.id.btnOnboardingNext)
                pager.setCurrentItem(pager.adapter!!.itemCount - 1, false)
                ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

                assertEquals(activity.getString(R.string.onboarding_button_done), next.text)
                next.performClick()

                assertTrue(
                    application.getSharedPreferences(Preferences.PREFERENCES, 0)
                        .getBoolean(Preference.SETUP_COMPLETED.getName(), false)
                )
                assertEquals(
                    HomeActivity::class.java.name,
                    Shadows.shadowOf(activity).nextStartedActivity.component?.className,
                )
                assertTrue(activity.isFinishing)
            }
        }
    }

    @Test fun everyPageIsShown() {
        launch().use { scenario ->
            scenario.onActivity { activity ->
                val pager = activity.findViewById<ViewPager2>(R.id.vpOnboarding)

                assertEquals(OnboardingPage.entries.size, pager.adapter!!.itemCount)
            }
        }
    }

    @Test fun launchingTheWizardRecordsThatItStarted() {
        launch().use {
            assertTrue(
                application.getSharedPreferences(Preferences.PREFERENCES, 0)
                    .getBoolean(Preference.SETUP_STARTED.getName(), false)
            )
            assertTrue(
                application.getSharedPreferences(Preferences.PREFERENCES, 0)
                    .getBoolean(Preference.DEFAULT_PINS_PENDING.getName(), false)
            )
        }
    }

    @Test fun upgradedInstallRunsOnboardingWithoutQueuingDefaultPins() {
        AppUpgradeReceiver().onReceive(application, Intent(Intent.ACTION_MY_PACKAGE_REPLACED))

        launch().use {
            val prefs = application.getSharedPreferences(Preferences.PREFERENCES, 0)
            assertTrue(prefs.getBoolean(Preference.SETUP_STARTED.getName(), false))
            assertFalse(prefs.getBoolean(Preference.DEFAULT_PINS_PENDING.getName(), false))
        }
    }
}
