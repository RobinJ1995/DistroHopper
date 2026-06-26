package be.robinj.distrohopper.widgets

import android.view.View
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class WidgetsContainerHomeTest {
    @Test fun widgetAreaIsAlwaysVisible() {
        ActivityTestSupport.launchHome().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.vgWidgets).visibility)
            }
        }
    }
}
