package be.robinj.distrohopper.desktop.dash.profile

import android.content.Context
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The GNOME pill's geometry: its measured width is constant as the current shape
 * elongates (so neighbours slide rather than the row reflowing), and a tap
 * resolves to the slot under it. The drawing itself is left untested (pixels).
 */
@RunWith(RobolectricTestRunner::class)
class ProfilePillViewTest {
    private lateinit var context: Context

    @Before fun setUp() { context = ApplicationProvider.getApplicationContext() }

    private val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

    private fun measured(pill: ProfilePillView): Int {
        pill.measure(this.unspecified, this.unspecified)
        return pill.measuredWidth
    }

    private fun laidOut(count: Int): ProfilePillView {
        val pill = ProfilePillView(context).apply { this.count = count }
        pill.measure(this.unspecified, this.unspecified)
        pill.layout(0, 0, pill.measuredWidth, pill.measuredHeight)
        return pill
    }

    private fun tapAt(pill: ProfilePillView, x: Float) {
        val up = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_UP, x, 0f, 0)
        try { pill.onTouchEvent(up) } finally { up.recycle() }
    }

    @Test fun measuredWidthIsConstantAsTheCurrentShapeMoves() {
        val pill = ProfilePillView(context).apply { count = 3 }

        pill.position = 0f
        val atStart = this.measured(pill)
        pill.position = 1.5f
        val midSwipe = this.measured(pill)

        assertTrue(atStart > 0)
        assertEquals(atStart, midSwipe)
    }

    @Test fun zeroCountMeasuresToZeroWidth() {
        assertEquals(0, this.measured(ProfilePillView(context).apply { count = 0 }))
    }

    @Test fun tapNearTheStartSelectsTheFirstSlot() {
        val pill = this.laidOut(count = 3)
        var selected = -1
        pill.onSlotClick = { selected = it }

        this.tapAt(pill, 1f)

        assertEquals(0, selected)
    }

    @Test fun tapNearTheEndSelectsTheLastSlot() {
        val pill = this.laidOut(count = 3)
        var selected = -1
        pill.onSlotClick = { selected = it }

        this.tapAt(pill, pill.width - 1f)

        assertEquals(2, selected)
    }

    @Test fun tapPastTheLastIndicatorFallsThroughToIt() {
        val pill = this.laidOut(count = 3)
        var selected = -1
        pill.onSlotClick = { selected = it }

        this.tapAt(pill, pill.width + 500f)

        assertEquals(2, selected)
    }
}
