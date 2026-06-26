package be.robinj.distrohopper.desktop.dash.profile

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The GNOME pill profile indicator: a [ProfilePillView] shown only while the
 * dash is open and more than one profile exists, with the pager driving the
 * pill's (fractional) position.
 */
@RunWith(RobolectricTestRunner::class)
class GnomeProfilePillIndicatorTest {
    private lateinit var context: Context

    @Before fun setUp() { context = ApplicationProvider.getApplicationContext() }

    private fun pill(container: FrameLayout) = container.getChildAt(0) as ProfilePillView

    @Test fun bindSetsPillCountAndSelectedPosition() {
        val container = FrameLayout(context)
        GnomeProfilePillIndicator(context, container) {}.bind(listOf(null, null), selected = 1)

        assertEquals(2, pill(container).count)
        assertEquals(1f, pill(container).position, 0.001f)
    }

    @Test fun visibleOnlyWithMultipleProfilesAndDashOpen() {
        val container = FrameLayout(context)
        val indicator = GnomeProfilePillIndicator(context, container) {}

        // Single profile, dash open → hidden.
        indicator.bind(listOf(null), selected = 0)
        indicator.onDashOpenChanged(true)
        assertEquals(View.GONE, container.visibility)

        // Multiple profiles, dash open → shown.
        indicator.bind(listOf(null, null), selected = 0)
        assertEquals(View.VISIBLE, container.visibility)

        // Dash closes → hidden again.
        indicator.onDashOpenChanged(false)
        assertEquals(View.GONE, container.visibility)
    }

    @Test fun scrollAndSelectDriveThePillPosition() {
        val container = FrameLayout(context)
        val indicator = GnomeProfilePillIndicator(context, container) {}
        indicator.bind(listOf(null, null, null), selected = 0)

        indicator.onPageScrolled(1, 0.25f)
        assertEquals(1.25f, pill(container).position, 0.001f)

        indicator.onPageSelected(2)
        assertEquals(2f, pill(container).position, 0.001f)
    }

    @Test fun tappingASlotForwardsTheIndex() {
        val container = FrameLayout(context)
        var selected = -1
        val indicator = GnomeProfilePillIndicator(context, container) { selected = it }
        indicator.bind(listOf(null, null), selected = 0)

        pill(container).onSlotClick?.invoke(1)

        assertEquals(1, selected)
    }

    @Test fun clearHidesTheIndicator() {
        val container = FrameLayout(context)
        val indicator = GnomeProfilePillIndicator(context, container) {}
        indicator.bind(listOf(null, null), selected = 0)
        indicator.onDashOpenChanged(true)
        assertEquals(View.VISIBLE, container.visibility)

        indicator.clear()
        assertEquals(View.GONE, container.visibility)
    }
}
