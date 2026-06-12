package be.robinj.distrohopper.preferences

import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.R
import com.mobeta.android.dslv.DragSortListView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LensPreferencesActivityTest {
    @Test fun lensPreferencesLayoutInflates() {
        val context = android.view.ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(), R.style.LensPreferencesTheme)
        val view = LayoutInflater.from(context).inflate(R.layout.activity_lens_preferences, FrameLayout(context), false)
        assertNotNull(view.findViewById<android.view.View>(R.id.lvList))
    }

    @Test fun dragControllerUsesTheRowHandles() {
        // The vendored DSLV misreads its XML attributes (stale compiled-in
        // styleable indices), so the controller is configured in code; this
        // pins the handle wiring.
        ActivityScenario.launch(LensPreferencesActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val dslv = activity.findViewById<DragSortListView>(R.id.lvList)
                val managerField = DragSortListView::class.java.getDeclaredField("mFloatViewManager")
                managerField.isAccessible = true
                val controller = managerField.get(dslv)
                val handleField = controller.javaClass.getDeclaredField("mDragHandleId")
                handleField.isAccessible = true

                assertEquals(R.id.ivDragHandle, handleField.get(controller))
            }
        }
    }

    @Test fun lensRowLayoutInflates() {
        val context = android.view.ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(), R.style.LensPreferencesTheme)
        val view = LayoutInflater.from(context).inflate(R.layout.widget_lens_preferences_list_item, FrameLayout(context), false)
        assertNotNull(view.findViewById<android.view.View>(R.id.cbEnabled))
    }
}
