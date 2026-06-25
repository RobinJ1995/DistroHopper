package be.robinj.distrohopper.desktop.dash.profile

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The Unity ribbon profile indicator: one glyph per profile with the current
 * one highlighted, the highlight interpolating as the dash pager is swiped. Two
 * personal-style handles (`listOf(null, null)`) exercise the multi-glyph
 * highlight maths without depending on a headless system profile badge.
 */
@RunWith(RobolectricTestRunner::class)
class UnityRibbonIndicatorTest {
    private lateinit var context: Context

    @Before fun setUp() { context = ApplicationProvider.getApplicationContext() }

    private fun indicator(container: LinearLayout, onSelect: (Int) -> Unit = {}) =
        UnityRibbonIndicator(context, container, R.drawable.ic_profile, onSelect)

    private fun iconAt(container: LinearLayout, i: Int) = container.getChildAt(i) as ImageView

    @Test fun bindCreatesOneGlyphPerProfileAndShowsTheContainer() {
        val container = LinearLayout(context)
        indicator(container).bind(listOf(null, null, null), selected = 0)

        assertEquals(3, container.childCount)
        assertEquals(View.VISIBLE, container.visibility)
    }

    // The glyph's own (View) alpha is the exact, reliable signal — the backing
    // ColorDrawable's getAlpha() returns the base colour's alpha modulated by the
    // set value, so the backing is only asserted relatively (full vs transparent).

    @Test fun selectedGlyphIsFullyHighlightedAndOthersDimmed() {
        val container = LinearLayout(context)
        indicator(container).bind(listOf(null, null), selected = 0)

        // i=0 selected: highlight 1 → full glyph + visible backing.
        assertEquals(1f, iconAt(container, 0).alpha, 0.001f)
        assertTrue(iconAt(container, 0).background.alpha > 0)
        // i=1 inactive: highlight 0 → INACTIVE_ALPHA glyph + transparent backing.
        assertEquals(0.55f, iconAt(container, 1).alpha, 0.001f)
        assertEquals(0, iconAt(container, 1).background.alpha)
    }

    @Test fun pageScrollInterpolatesHighlightBetweenNeighbours() {
        val container = LinearLayout(context)
        val indicator = indicator(container)
        indicator.bind(listOf(null, null), selected = 0)

        indicator.onPageScrolled(0, 0.5f)

        // Both glyphs sit halfway: glyph alpha 0.55 + 0.45*0.5 == 0.775, and their
        // backings fade equally.
        assertEquals(0.775f, iconAt(container, 0).alpha, 0.001f)
        assertEquals(0.775f, iconAt(container, 1).alpha, 0.001f)
        assertEquals(iconAt(container, 0).background.alpha, iconAt(container, 1).background.alpha)
    }

    @Test fun pageSelectedSnapsHighlightToThatPage() {
        val container = LinearLayout(context)
        val indicator = indicator(container)
        indicator.bind(listOf(null, null), selected = 0)

        indicator.onPageSelected(1)

        assertEquals(1f, iconAt(container, 1).alpha, 0.001f)
        assertTrue(iconAt(container, 1).background.alpha > 0)
        assertEquals(0.55f, iconAt(container, 0).alpha, 0.001f)
        assertEquals(0, iconAt(container, 0).background.alpha)
    }

    @Test fun tappingAGlyphInvokesOnSelectWithItsIndex() {
        val container = LinearLayout(context)
        var selected = -1
        indicator(container) { selected = it }.bind(listOf(null, null), selected = 0)

        iconAt(container, 1).performClick()

        assertEquals(1, selected)
    }

    @Test fun bindHidesTheRibbonHomeButtonAndClearRestoresIt() {
        // The home button is found via the container's parent, so wrap them together.
        val parent = FrameLayout(context)
        val container = LinearLayout(context)
        val homeButton = View(context).apply { id = R.id.ibDashLensHome }
        parent.addView(container)
        parent.addView(homeButton)

        val indicator = indicator(container)
        indicator.bind(listOf(null, null), selected = 0)
        assertEquals("the personal glyph replaces the ribbon home button", View.GONE, homeButton.visibility)

        indicator.clear()
        assertEquals(View.VISIBLE, homeButton.visibility)
        assertEquals(View.GONE, container.visibility)
        assertEquals(0, container.childCount)
    }
}
