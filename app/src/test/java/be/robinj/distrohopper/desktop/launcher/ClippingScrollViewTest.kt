package be.robinj.distrohopper.desktop.launcher

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.preferences.Preference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Real measure-pass checks that the launcher scroll viewports floor themselves to whole icon
 * slots (the no-sliver guarantee), exercising the actual framework layout — not just the pure
 * [LauncherIconGrid.viewportClipPx] arithmetic.
 */
@RunWith(RobolectricTestRunner::class)
class ClippingScrollViewTest {
	private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

	private fun setPreset(index: Int) {
		DependencyContainer.of(this.context).prefs.edit {
			putInt(Preference.LAUNCHER_ICON_PRESET.getName(), index)
		}
	}

	private fun child(): LinearLayout {
		// One oversized child so the viewport — not the content — is the limiting dimension.
		val child = LinearLayout(this.context)
		child.layoutParams = ViewGroup.LayoutParams(5000, 5000)
		return child
	}

	private fun exactly(px: Int) = View.MeasureSpec.makeMeasureSpec(px, View.MeasureSpec.EXACTLY)

	@Test fun horizontalViewportFloorsToWholeSlots() {
		this.setPreset(LauncherIconGrid.DEFAULT_PRESET)
		val slot = LauncherIconGrid.iconSizePx(this.context)
		assertTrue("precondition: positive slot", slot > 0)

		val view = ClippingHorizontalScrollView(this.context)
		view.addView(this.child())

		for (avail in intArrayOf(slot - 1, slot, slot * 3, slot * 4 + slot / 2, 2000)) {
			view.measure(this.exactly(avail), this.exactly(slot * 2))
			val w = view.measuredWidth
			assertTrue("clip must not exceed available ($w <= $avail)", w <= avail)
			assertEquals("clip must be a whole multiple of the slot", 0, w % slot)
			assertEquals("largest whole multiple", (avail / slot) * slot, w)
		}
	}

	@Test fun verticalViewportFloorsToWholeSlots() {
		this.setPreset(LauncherIconGrid.DEFAULT_PRESET)
		val slot = LauncherIconGrid.iconHeightPx(this.context) // vertical slots advance by height
		assertTrue("precondition: positive slot", slot > 0)

		val view = ClippingScrollView(this.context)
		view.addView(this.child())

		for (avail in intArrayOf(slot, slot * 5 - 1, slot * 6, 1777)) {
			view.measure(this.exactly(slot * 2), this.exactly(avail))
			val h = view.measuredHeight
			assertTrue("clip must not exceed available ($h <= $avail)", h <= avail)
			assertEquals("clip must be a whole multiple of the slot", 0, h % slot)
			assertEquals("largest whole multiple", (avail / slot) * slot, h)
		}
	}

	/** A viewport smaller than one slot collapses to zero rather than showing a partial icon. */
	@Test fun viewportSmallerThanOneSlotShowsNothing() {
		this.setPreset(LauncherIconGrid.DEFAULT_PRESET)
		val slot = LauncherIconGrid.iconSizePx(this.context)

		val view = ClippingHorizontalScrollView(this.context)
		view.addView(this.child())
		view.measure(this.exactly(slot - 1), this.exactly(slot))
		assertEquals(0, view.measuredWidth)
	}

	/**
	 * While the content FITS, the measure is left untouched even when it is not a whole slot
	 * multiple: no partial icon is possible without overflow, and PinnedAppsBar's per-desktop
	 * morph measures the bar to a fractional length — flooring it would snap the smooth
	 * auto-sizing resize (GNOME) in whole-icon steps.
	 */
	@Test fun contentThatFitsIsNotClipped() {
		this.setPreset(LauncherIconGrid.DEFAULT_PRESET)
		val slot = LauncherIconGrid.iconSizePx(this.context)
		val fractional = slot * 2 + slot / 3 // mid-morph bar length: 2⅓ slots

		val horizontal = ClippingHorizontalScrollView(this.context)
		val hChild = LinearLayout(this.context)
		hChild.layoutParams = ViewGroup.LayoutParams(fractional, slot)
		horizontal.addView(hChild)
		horizontal.measure(this.exactly(slot * 5), this.exactly(slot * 2))
		assertEquals("viewport untouched when content fits", slot * 5, horizontal.measuredWidth)

		val vertical = ClippingScrollView(this.context)
		val vChild = LinearLayout(this.context)
		vChild.layoutParams = ViewGroup.LayoutParams(slot, fractional)
		vertical.addView(vChild)
		vertical.measure(this.exactly(slot * 2), this.exactly(slot * 5))
		assertEquals("viewport untouched when content fits", slot * 5, vertical.measuredHeight)
	}
}
