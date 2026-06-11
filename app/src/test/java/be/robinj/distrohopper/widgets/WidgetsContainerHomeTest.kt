package be.robinj.distrohopper.widgets

import android.app.Application
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class WidgetsContainerHomeTest {
    @Test fun widgetAreaIsVisibleByDefault() {
        ActivityTestSupport.launchHome().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.vgWidgets).visibility)
            }
        }
    }

    @Test fun widgetAreaIsGoneWhenDisabled() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        listOf(Preferences.PREFERENCES, Preferences.PINNED_APPS, Preferences.LENSES).forEach {
            application.getSharedPreferences(it, 0).edit().clear().commit()
        }
        application.getSharedPreferences(Preferences.PREFERENCES, 0)
            .edit().putBoolean(Preference.WIDGETS_ENABLED.getName(), false).commit()
        DependencyContainer.of(ApplicationProvider.getApplicationContext()).customiseMode.value = false
        ActivityTestSupport.seedPackageManager()

        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            ActivityTestSupport.drainTasks()
            scenario.onActivity { activity ->
                assertEquals(View.GONE, activity.findViewById<View>(R.id.vgWidgets).visibility)
            }
        }
    }
}
