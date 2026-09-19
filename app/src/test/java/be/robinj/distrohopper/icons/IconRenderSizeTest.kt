package be.robinj.distrohopper.icons

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.desktop.dash.DashGrid
import be.robinj.distrohopper.desktop.launcher.LauncherIconGrid
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IconRenderSizeTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val density = this.context.resources.displayMetrics.density
    private val canvasPx = (IconRenderSize.CANVAS_DP * this.density).toInt()
    private val minPx = (IconRenderSize.MIN_DP * this.density).toInt()

    private fun prefs() = Preferences.getSharedPreferences(this.context)
    private fun sw() = this.context.resources.configuration.smallestScreenWidthDp

    // --- Pure maths ---------------------------------------------------------

    @Test fun pxIsTheLargestSurface() {
        assertEquals(300, IconRenderSize.px(listOf(10, 300, 200), 48, 405))
    }

    @Test fun pxIsFlooredAtTheMinimum() {
        assertEquals(48, IconRenderSize.px(listOf(10, 20), 48, 405))
        assertEquals(48, IconRenderSize.px(emptyList(), 48, 405))
    }

    @Test fun pxIsCappedAtTheCanvas() {
        assertEquals(405, IconRenderSize.px(listOf(900), 48, 405))
    }

    @Test fun labelledCellSubtractsPaddingAndOneLabelLine() {
        assertEquals(74, IconRenderSize.labelledCellIconPx(96, 4, 14))
        assertEquals(0, IconRenderSize.labelledCellIconPx(10, 4, 14))
    }

    @Test fun launcherSlotSubtractsItsMargins() {
        assertEquals(76, IconRenderSize.launcherIconPx(84, 4))
        assertEquals(0, IconRenderSize.launcherIconPx(4, 4))
    }

    // --- Resolved against the screen and preferences --------------------------

    @Test fun contextSizeIsTheClampedLargestSurface() {
        val expected = IconRenderSize.px(IconRenderSize.surfacesPx(this.context), this.minPx, this.canvasPx)
        assertEquals(expected, IconRenderSize.px(this.context))
    }

    @Test fun contextSizeStaysWithinFloorAndCanvas() {
        val px = IconRenderSize.px(this.context)
        assertTrue(px >= this.minPx)
        assertTrue(px <= this.canvasPx)
    }

    @Test fun defaultsRenderBelowTheAdaptiveCanvas() {
        // The point of the whole thing: at the default grids no surface draws an
        // icon anywhere near 108dp, so the render (and every cached icon) shrinks.
        assertTrue(IconRenderSize.px(this.context) < this.canvasPx)
    }

    @Test fun fewerDashColumnsRenderLarger() {
        this.prefs().edit().putInt(Preference.DASH_GRID_COLUMNS.getName(), DashGrid.maxColumns(this.sw())).commit()
        val dense = IconRenderSize.px(this.context)

        this.prefs().edit().putInt(Preference.DASH_GRID_COLUMNS.getName(), DashGrid.minColumns(this.sw())).commit()
        val sparse = IconRenderSize.px(this.context)

        assertTrue("$sparse should exceed $dense", sparse > dense)
    }

    @Test fun theDashTermFollowsTheColumnsPreference() {
        val shortEdgePx = (this.sw() * this.density).toInt()
        val padding = this.context.resources.getDimensionPixelSize(be.robinj.distrohopper.R.dimen.dash_applauncher_padding)
        val label = this.context.resources.getDimensionPixelSize(be.robinj.distrohopper.R.dimen.dash_applauncher_textsize)
        val columns = DashGrid.minColumns(this.sw())
        this.prefs().edit().putInt(Preference.DASH_GRID_COLUMNS.getName(), columns).commit()

        assertEquals(
            IconRenderSize.labelledCellIconPx(DashGrid.cellSizePx(shortEdgePx, columns), padding, label),
            IconRenderSize.surfacesPx(this.context)[0])
    }

    @Test fun theHugeLauncherPresetIsCappedAtTheCanvas() {
        this.prefs().edit().putInt(Preference.LAUNCHER_ICON_PRESET.getName(), LauncherIconGrid.PRESET_COUNT - 1).commit()

        assertEquals(this.canvasPx, IconRenderSize.px(this.context))
    }

    @Test fun theLauncherTermIgnoresTheThemeMargins() {
        // Sized off the bare short edge, so a theme switch cannot change the size
        // and needlessly purge the icon cache.
        val shortEdgePx = (this.sw() * this.density).toInt()
        val margin = this.context.resources.getDimensionPixelSize(be.robinj.distrohopper.R.dimen.launcher_applauncher_icon_margin)

        assertEquals(
            IconRenderSize.launcherIconPx(
                LauncherIconGrid.iconSizePx(shortEdgePx, LauncherIconGrid.count(this.context)), margin),
            IconRenderSize.surfacesPx(this.context)[1])
    }
}
