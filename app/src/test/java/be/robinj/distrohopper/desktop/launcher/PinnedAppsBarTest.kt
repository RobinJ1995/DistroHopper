package be.robinj.distrohopper.desktop.launcher

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.App
import be.robinj.distrohopper.home.LauncherMorph
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The morph-mode measure/layout of the pinned-apps bar (slot positions + size). */
@RunWith(RobolectricTestRunner::class)
class PinnedAppsBarTest {
	private val context: Context = ApplicationProvider.getApplicationContext()

	private fun app(): App =
		App(this.context, null, ActivityTestSupport.resolveInfo("a", "A", "Alpha"))

	private fun child(width: Int, height: Int): View =
		View(this.context).apply { layoutParams = LinearLayout.LayoutParams(width, height) }

	private fun measureAndLayout(bar: PinnedAppsBar, size: Int) {
		bar.measure(
			View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.AT_MOST),
			View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.AT_MOST))
		bar.layout(0, 0, bar.measuredWidth, bar.measuredHeight)
	}

	@Test fun morphModePositionsIconsAtSlotsAndSizesToTheInterpolatedLength() {
		val bar = PinnedAppsBar(this.context)
		bar.orientation = LinearLayout.VERTICAL
		bar.addView(this.child(100, 80))
		bar.addView(this.child(100, 80))
		val app = this.app()

		// Slot 0 at rest, slot 1 sliding through the middle; length 1.5 slots //
		bar.setMorph(
			listOf(LauncherMorph.Slot(app, 0F, 1F, 1F), LauncherMorph.Slot(app, 0.5F, 0.5F, 0.8F)),
			stride = 80F, lengthSlots = 1.5F)
		this.measureAndLayout(bar, 1000)

		assertEquals(0, bar.getChildAt(0).top) // slot 0 -> y 0 //
		assertEquals(40, bar.getChildAt(1).top) // slot 0.5 -> y 40 //
		assertEquals(120, bar.measuredHeight) // 1.5 slots * 80 //
		assertEquals(100, bar.measuredWidth) // cross = icon width //

		assertEquals(0.5F, bar.getChildAt(1).alpha, 0.001F)
		assertEquals(0.8F, bar.getChildAt(1).scaleX, 0.001F)
	}

	@Test fun clearMorphRestoresNormalLinearLayoutFlow() {
		val bar = PinnedAppsBar(this.context)
		bar.orientation = LinearLayout.VERTICAL
		bar.addView(this.child(100, 80))
		bar.addView(this.child(100, 80))
		val app = this.app()
		bar.setMorph(
			listOf(LauncherMorph.Slot(app, 0F, 0.3F, 0.5F), LauncherMorph.Slot(app, 1F, 1F, 1F)),
			stride = 80F, lengthSlots = 2F)
		this.measureAndLayout(bar, 1000)

		bar.clearMorph()
		this.measureAndLayout(bar, 1000)

		// Back to sequential layout and full opacity/scale //
		assertEquals(0, bar.getChildAt(0).top)
		assertEquals(80, bar.getChildAt(1).top)
		assertEquals(1F, bar.getChildAt(0).alpha, 0.001F)
		assertEquals(1F, bar.getChildAt(0).scaleX, 0.001F)
	}
}
