package be.robinj.distrohopper.desktop.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.theme.Location
import be.robinj.distrohopper.theme.ThemeRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The pinned-icon preset maths: a preset maps to a whole, theme-independent slot count derived
 * from the screen, and the icon size is then chosen so exactly that many slots tile the
 * launcher interior — no cut-off, no sliver. See [LauncherIconGrid].
 */
@RunWith(RobolectricTestRunner::class)
class LauncherIconGridTest {

	// --- Device-adaptive count -------------------------------------------------

	/**
	 * The S24-class screen the default was tuned on reproduces the hand-picked 3·4·5·6·7 — in
	 * slider order, so Tiny (index 0) packs the most icons and Huge (index 4) the fewest.
	 */
	@Test fun s24ClassYieldsDefaultFiveAndPresetsSevenToThree() {
		val sw = 384
		assertEquals(5, LauncherIconGrid.defaultCount(sw))
		assertEquals(
			listOf(7, 6, 5, 4, 3),
			(0 until LauncherIconGrid.PRESET_COUNT).map { LauncherIconGrid.countForPreset(sw, it) })
	}

	/** The default count adapts to screen size (smaller phone → fewer, tablet → many). */
	@Test fun defaultCountAdaptsToScreen() {
		assertEquals(4, LauncherIconGrid.defaultCount(320))
		assertEquals(5, LauncherIconGrid.defaultCount(384))
		assertEquals(10, LauncherIconGrid.defaultCount(800))
		assertEquals(14, LauncherIconGrid.defaultCount(1200))
	}

	/** The default preset is always the device default count. */
	@Test fun defaultPresetIsTheDeviceDefaultCount() {
		for (sw in intArrayOf(220, 320, 384, 600, 800, 1200))
			assertEquals(LauncherIconGrid.defaultCount(sw),
				LauncherIconGrid.countForPreset(sw, LauncherIconGrid.DEFAULT_PRESET))
	}

	/** Count never drops below the cap-derived minimum or above the floor-derived maximum. */
	@Test fun countStaysWithinScreenRange() {
		for (sw in intArrayOf(180, 200, 320, 384, 600, 800, 1200, 2000)) {
			val lo = LauncherIconGrid.minCount(sw)
			val hi = LauncherIconGrid.maxCount(sw)
			assertTrue(lo in 2..hi)
			for (i in -3..LauncherIconGrid.PRESET_COUNT + 3)
				assertTrue(LauncherIconGrid.countForPreset(sw, i) in lo..hi)
		}
	}

	/** On a tiny screen the range is narrow, so the extreme presets collapse rather than crash. */
	@Test fun narrowRangeCollapsesPresetEnds() {
		val sw = 200 // minCount 2, maxCount 4
		assertEquals(2, LauncherIconGrid.minCount(sw))
		assertEquals(4, LauncherIconGrid.maxCount(sw))
		val counts = (0 until LauncherIconGrid.PRESET_COUNT).map { LauncherIconGrid.countForPreset(sw, it) }
		assertEquals(listOf(4, 3, 2, 2, 2), counts)
	}

	/** Out-of-range preset indices are clamped, not thrown. */
	@Test fun presetIndexIsClamped() {
		val sw = 384
		assertEquals(LauncherIconGrid.countForPreset(sw, 0), LauncherIconGrid.countForPreset(sw, -5))
		assertEquals(
			LauncherIconGrid.countForPreset(sw, LauncherIconGrid.PRESET_COUNT - 1),
			LauncherIconGrid.countForPreset(sw, 99))
	}

	// --- Exact fit -------------------------------------------------------------

	/** Exactly [n] slots fit the interior: n·size ≤ interior, and an (n+1)th never does. */
	@Test fun nSlotsFitExactlyAndNoMore() {
		for (interior in intArrayOf(720, 1000, 1080, 1234, 1440, 2000))
			for (n in 2..12) {
				val size = LauncherIconGrid.iconSizePx(interior, n)
				assertTrue("n*size must fit", n * size <= interior)
				assertTrue("no (n+1)th slot fits", interior - n * size < size || size == 0)
			}
	}

	@Test fun iconSizeGuardsDegenerateInputs() {
		assertEquals(0, LauncherIconGrid.iconSizePx(1080, 0))
		assertEquals(0, LauncherIconGrid.iconSizePx(1080, -3))
		assertEquals(0, LauncherIconGrid.iconSizePx(-50, 5))
	}

	/**
	 * Dragging the slider right makes icons bigger: the size never decreases as the preset index
	 * rises (Tiny → Huge), which is what stops the control feeling inverted.
	 */
	@Test fun higherPresetsMeanBiggerIcons() {
		val interior = 1080
		val sizes = (0 until LauncherIconGrid.PRESET_COUNT).map {
			LauncherIconGrid.iconSizePx(interior, LauncherIconGrid.countForPreset(384, it))
		}
		for (i in 1 until sizes.size) assertTrue(sizes[i] >= sizes[i - 1])
	}

	// --- Physical-size consistency (the point of the rework) -------------------
	// These assert the bare-screen ideal (interior = short edge). A theme's margins
	// and 9-patch insets shave a few dp more; exactlyCountSlotsFitEveryTheme covers
	// that the exact-fit division still holds there.

	/** The default icon stays a comfortable PHYSICAL size across devices — not screen-proportional. */
	@Test fun defaultIconSizeIsPhysicallyConsistent() {
		for (sw in intArrayOf(320, 384, 600, 800, 1200)) {
			// interior ≈ sw px at density 1 ⇒ sizePx is the size in dp.
			val sizeDp = LauncherIconGrid.iconSizePx(sw, LauncherIconGrid.defaultCount(sw))
			assertTrue("$sw → ${sizeDp}dp within bounds",
				sizeDp in LauncherIconGrid.MIN_ICON_DP..LauncherIconGrid.MAX_ICON_DP)
			assertTrue("$sw → ${sizeDp}dp near target",
				sizeDp in 60..110) // never the old shortEdge/5 extremes (64dp .. 160dp)
		}
	}

	/** Clamping the COUNT keeps every preset's size within the dp bounds on any screen. */
	@Test fun everyPresetSizeStaysWithinDpBounds() {
		for (sw in intArrayOf(220, 320, 384, 600, 800, 1200, 2000))
			for (i in 0 until LauncherIconGrid.PRESET_COUNT) {
				val sizeDp = LauncherIconGrid.iconSizePx(sw, LauncherIconGrid.countForPreset(sw, i))
				assertTrue("$sw preset $i → ${sizeDp}dp",
					sizeDp in LauncherIconGrid.MIN_ICON_DP..LauncherIconGrid.MAX_ICON_DP)
			}
	}

	// --- Height + viewport clip ------------------------------------------------

	@Test fun iconHeightIsSizeMinusFourDp() {
		assertEquals(216 - 12, LauncherIconGrid.iconHeightPx(216, 3F))
		assertEquals(84 - 4, LauncherIconGrid.iconHeightPx(84, 1F))
		assertEquals(0, LauncherIconGrid.iconHeightPx(2, 3F)) // never negative
	}

	@Test fun viewportClipIsLargestWholeMultiple() {
		assertEquals(864, LauncherIconGrid.viewportClipPx(1000, 216)) // 4 whole slots
		assertEquals(864, LauncherIconGrid.viewportClipPx(864, 216))  // already a multiple
		assertEquals(0, LauncherIconGrid.viewportClipPx(100, 216))    // not even one slot
	}

	@Test fun viewportClipGuardsZeroSize() {
		assertEquals(1000, LauncherIconGrid.viewportClipPx(1000, 0))
		assertEquals(1000, LauncherIconGrid.viewportClipPx(1000, -4))
	}

	@Test fun clippedViewportShowsOnlyWholeSlots() {
		for (avail in intArrayOf(500, 999, 1000, 1080, 1441))
			for (size in intArrayOf(48, 64, 91, 216)) {
				val clipped = LauncherIconGrid.viewportClipPx(avail, size)
				assertTrue(clipped <= avail)
				assertEquals(0, clipped % size)
			}
	}

	// --- Context resolvers (Robolectric) ---------------------------------------

	private fun ctx(): Context = ApplicationProvider.getApplicationContext()

	private fun setPreset(context: Context, index: Int) {
		DependencyContainer.of(context).prefs.edit {
			putInt(Preference.LAUNCHER_ICON_PRESET.getName(), index)
		}
	}

	private fun setTheme(context: Context, name: String) {
		DependencyContainer.of(context).prefs.edit {
			putString(Preference.THEME.getName(), name)
		}
	}

	@Test fun presetDefaultsToDefaultIndexWhenUnset() {
		assertEquals(LauncherIconGrid.DEFAULT_PRESET, LauncherIconGrid.preset(this.ctx()))
	}

	@Test fun presetIsClampedWhenStoredOutOfRange() {
		val context = this.ctx()
		this.setPreset(context, 99)
		assertEquals(LauncherIconGrid.PRESET_COUNT - 1, LauncherIconGrid.preset(context))
		this.setPreset(context, -7)
		assertEquals(0, LauncherIconGrid.preset(context))
	}

	/** The same preset yields the same whole slot count on every theme (guarantee #1). */
	@Test fun countIsIdenticalAcrossAllThemes() {
		val context = this.ctx()
		this.setPreset(context, 3)
		val counts = ThemeRegistry.themes.keys.map {
			this.setTheme(context, it)
			LauncherIconGrid.count(context)
		}
		assertEquals(1, counts.distinct().size)
	}

	/** For every theme, exactly `count` slots fill the resolved interior — none cut off, none extra. */
	@Test fun exactlyCountSlotsFitEveryTheme() {
		val context = this.ctx()
		this.setPreset(context, LauncherIconGrid.DEFAULT_PRESET)
		for (name in ThemeRegistry.themes.keys) {
			this.setTheme(context, name)
			val interior = LauncherIconGrid.launcherInteriorPx(context)
			val count = LauncherIconGrid.count(context)
			val size = LauncherIconGrid.iconSizePx(context)
			assertTrue("$name: positive size", size > 0)
			assertTrue("$name: count fits", count * size <= interior)
			assertTrue("$name: no extra slot", interior - count * size < size)
		}
	}

	/** A theme with launcher margins has a smaller interior than the margin-less default. */
	@Test fun themeMarginsShrinkTheInterior() {
		val context = this.ctx()
		this.setTheme(context, "default") // 0dp launcher margin
		val noMargin = LauncherIconGrid.launcherInteriorPx(context)
		this.setTheme(context, "elementary") // 8dp launcher margin
		val withMargin = LauncherIconGrid.launcherInteriorPx(context)
		assertTrue("elementary ($withMargin) < default ($noMargin)", withMargin < noMargin)
	}

	@Test fun iconHeightContextIsSizeMinusFourDp() {
		val context = this.ctx()
		val density = context.resources.displayMetrics.density
		assertEquals(
			LauncherIconGrid.iconSizePx(context) - (4F * density).toInt(),
			LauncherIconGrid.iconHeightPx(context))
	}

	/**
	 * The interior derives from smallestScreenWidthDp × density — the same stable anchor the
	 * count uses — not raw window metrics: every configuration input the size depends on
	 * (smallestScreenSize, density, theme) recreates the activity when it changes (they are not
	 * in the manifest's configChanges), so a computed size can never go stale under a handled
	 * screenSize change.
	 */
	@Test @Config(qualifiers = "sw400dp-w400dp-h800dp") fun interiorAnchoredToSmallestWidth() {
		val context = this.ctx()
		this.setTheme(context, "default") // no margins, dynamic background: interior == bare edge
		val density = context.resources.displayMetrics.density
		assertEquals((400 * density).toInt(), LauncherIconGrid.launcherInteriorPx(context))
	}

	/*
	 * Anchored to the shortest screen edge: the same device portrait or landscape (w/h swapped,
	 * smallest-width 400dp either way) yields the same count — so the icon size never jumps on
	 * rotation. The landscape case is the discriminating one: there screenWidthDp (800) differs
	 * from smallestScreenWidthDp (400), so a count() bound to the wrong Configuration field
	 * fails loudly; the preconditions pin the config Robolectric actually produced.
	 */
	@Test @Config(qualifiers = "sw400dp-w400dp-h800dp-port") fun countAnchoredToShortestEdgePortrait() {
		val context = this.ctx()
		assertEquals(400, context.resources.configuration.smallestScreenWidthDp)
		assertEquals(LauncherIconGrid.countForPreset(400, LauncherIconGrid.preset(context)),
			LauncherIconGrid.count(context))
	}

	@Test @Config(qualifiers = "sw400dp-w800dp-h400dp-land") fun countAnchoredToShortestEdgeLandscape() {
		val context = this.ctx()
		val config = context.resources.configuration
		assertEquals(400, config.smallestScreenWidthDp)
		assertEquals("landscape config must make the long edge the width", 800, config.screenWidthDp)
		assertEquals(LauncherIconGrid.countForPreset(400, LauncherIconGrid.preset(context)),
			LauncherIconGrid.count(context))
	}

	// --- The visible count, which follows the docked edge ------------------------

	private fun setLauncherEdge(context: Context, edge: Location) {
		DependencyContainer.of(context).prefs.edit {
			putInt(Preference.LAUNCHER_EDGE.getName(), edge.n)
		}
	}

	/**
	 * A horizontal launcher spans the same short edge the slot count is derived from, so what
	 * the customise hint reports is exactly that count — the two must not drift apart.
	 */
	@Test @Config(qualifiers = "sw400dp-w400dp-h800dp") fun visibleCountMatchesSlotCountOnABottomLauncher() {
		val context = this.ctx()
		this.setTheme(context, "default")
		this.setLauncherEdge(context, Location.BOTTOM)

		for (i in 0 until LauncherIconGrid.PRESET_COUNT)
			assertEquals(LauncherIconGrid.countForPreset(context, i),
				LauncherIconGrid.visibleCountForPreset(context, i))
	}

	/**
	 * A side launcher runs along the LONG edge at the very same icon size, so it shows more
	 * icons than the short-edge slot count — reporting that count would name a number the user
	 * cannot find anywhere on screen.
	 */
	@Test @Config(qualifiers = "sw400dp-w400dp-h800dp") fun visibleCountFollowsALongEdgeLauncher() {
		val context = this.ctx()
		this.setTheme(context, "default")

		this.setLauncherEdge(context, Location.BOTTOM)
		val acrossTheShortEdge = LauncherIconGrid.visibleCountForPreset(context, 2)

		this.setLauncherEdge(context, Location.LEFT)
		val downTheLongEdge = LauncherIconGrid.visibleCountForPreset(context, 2)

		assertTrue("a 400×800 screen fits more icons down its side than across its bottom",
			downTheLongEdge > acrossTheShortEdge)
	}

	/**
	 * The two axes do not pitch alike. An icon view is a square minus 4dp of HEIGHT
	 * (`AppLauncher.init`), so a stacked launcher fits them by that reduced height — dividing
	 * the side by the icon's width instead reports one too few on a tall screen.
	 */
	@Test @Config(qualifiers = "sw400dp-w400dp-h800dp") fun visibleCountStacksByIconHeightNotWidth() {
		val context = this.ctx()
		this.setTheme(context, "default")
		this.setLauncherEdge(context, Location.LEFT)
		val density = context.resources.displayMetrics.density

		for (i in 0 until LauncherIconGrid.PRESET_COUNT) {
			val sizePx = LauncherIconGrid.iconSizePx(
				LauncherIconGrid.launcherInteriorPx(context),
				LauncherIconGrid.countForPreset(context, i))
			val along = LauncherIconGrid.alongEdgeInteriorPx(context)

			assertEquals("preset $i stacks by icon height",
				along / LauncherIconGrid.iconHeightPx(sizePx, density),
				LauncherIconGrid.visibleCountForPreset(context, i))
			assertTrue("preset $i must not under-report by pitching on width",
				LauncherIconGrid.visibleCountForPreset(context, i) >= along / sizePx)
		}
	}

	/** Rotating does not change how many icons the side launcher shows. */
	@Test fun visibleCountSurvivesRotation() {
		val context = this.ctx()
		this.setTheme(context, "default")
		this.setLauncherEdge(context, Location.LEFT)

		val config = context.resources.configuration
		val portrait = LauncherIconGrid.visibleCountForPreset(context, 2)

		val (w, h) = config.screenWidthDp to config.screenHeightDp
		config.screenWidthDp = h
		config.screenHeightDp = w
		try {
			assertEquals(portrait, LauncherIconGrid.visibleCountForPreset(context, 2))
		} finally {
			config.screenWidthDp = w
			config.screenHeightDp = h
		}
	}
}
