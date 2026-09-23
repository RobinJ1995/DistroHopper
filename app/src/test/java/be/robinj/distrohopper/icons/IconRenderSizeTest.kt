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
import org.robolectric.RuntimeEnvironment
import kotlin.math.max

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

    // --- Both orientations -------------------------------------------------------

    @Test fun dashCellIsThePortraitCellWhenLandscapeIsNoBigger() {
        // 2:1 exactly: landscape shows 2n columns across twice the length
        assertEquals(270, IconRenderSize.dashCellPx(1080, 2160, 4))
    }

    @Test fun dashCellCoversTheCappedLandscapeColumnsOfATallScreen() {
        // 2.23:1 would want 9 landscape columns but is capped at 2n = 8, so a
        // landscape cell (2856 / 8) outgrows the portrait one (1280 / 4)
        assertEquals(357, IconRenderSize.dashCellPx(1280, 2856, 4))
    }

    @Test fun dashCellCoversLandscapeColumnsRoundedDown() {
        // 3 x 1.7 = 5.1 rounds to 5 columns: 1700 / 5 beats 1000 / 3
        assertEquals(340, IconRenderSize.dashCellPx(1000, 1700, 3))
    }

    @Test fun theLandscapeDashMeasuredOnTheEmulatorIsCovered() {
        // A 1280x2856 480dpi emulator with the launcher docked at the bottom drew
        // 276px dash icons in landscape; the portrait-only bound had rendered 254.
        val icon = IconRenderSize.labelledCellIconPx(IconRenderSize.dashCellPx(1281, 2856, 4), 12, 42)
        assertTrue("$icon should cover 276", icon >= 276)
    }

    @Test fun desktopBlockCoversTheTransposedLandscapeGrid() {
        // Portrait cells are 1000/8 = 125 wide; landscape ones 2000/14 = 142
        assertEquals(142 * 2, IconRenderSize.desktopBlockPx(1000, 2000, 8, 14))
        assertEquals(125 * 2, IconRenderSize.desktopBlockPx(1000, 1500, 8, 14))
    }

    @Test fun dashCellCoversBothOrientationsAcrossScreenShapes() {
        for (short in listOf(720, 1080, 1280, 1440, 1600, 1800)) {
            for (ratio in listOf(1.0, 1.3, 1.6, 1.78, 2.0, 2.1, 2.23, 2.4, 2.7)) {
                val long = (short * ratio).toInt()
                for (n in 2..8) {
                    val portrait = short / n
                    val landscape = long / DashGrid.dashColumns(short, long, false, n)
                    val bound = IconRenderSize.dashCellPx(short, long, n)
                    assertTrue("$short x $long, n=$n", bound >= max(portrait, landscape))
                }
            }
        }
    }

    @Test fun rotatingLeavesTheRenderSizeAlone() {
        this.prefs().edit().putInt(Preference.DASH_GRID_COLUMNS.getName(), DashGrid.minColumns(this.sw())).commit()
        val portrait = IconRenderSize.px(this.context)

        RuntimeEnvironment.setQualifiers("+land")
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(context.resources.configuration.screenWidthDp > context.resources.configuration.screenHeightDp)

        assertEquals(portrait, IconRenderSize.px(context))
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
        val config = this.context.resources.configuration
        val shortEdgePx = (this.sw() * this.density).toInt()
        val longEdgePx = (max(config.screenWidthDp, config.screenHeightDp) * this.density).toInt()
        val padding = this.context.resources.getDimensionPixelSize(be.robinj.distrohopper.R.dimen.dash_applauncher_padding)
        val label = this.context.resources.getDimensionPixelSize(be.robinj.distrohopper.R.dimen.dash_applauncher_textsize)
        val columns = DashGrid.minColumns(this.sw())
        this.prefs().edit().putInt(Preference.DASH_GRID_COLUMNS.getName(), columns).commit()

        assertEquals(
            IconRenderSize.labelledCellIconPx(IconRenderSize.dashCellPx(shortEdgePx, longEdgePx, columns), padding, label),
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
