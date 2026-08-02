package be.robinj.distrohopper.desktop.launcher

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.content.ContextCompat
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import be.robinj.distrohopper.theme.Location
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pure maths (plus thin runtime resolvers) for the launcher's pinned-icon size — the
 * counterpart to [be.robinj.distrohopper.desktop.dash.DashGrid] for the dock.
 *
 * The user picks one of five presets ([PRESET_COUNT]); from that we derive, at runtime, how
 * many icon slots fit along the launcher when it sits on the screen's SHORTEST edge, and the
 * per-slot pixel size — nothing is stored but the preset index.
 *
 * Two guarantees drive the design:
 *
 *  1. The slot count for a preset is computed from the SCREEN alone
 *     ([countForPreset] off [smallestScreenWidthDp]), so it is a whole number and identical on
 *     every theme: the preset that means "6 icons" on a given screen yields 6 on GNOME,
 *     elementary, MATE, … The BFB counts as one of those slots.
 *  2. The default adapts to the device. A preset targets a comfortable *physical* size
 *     ([TARGET_ICON_DP]); the count — not the size — is what flexes with screen size, so icons
 *     stay roughly the same physical size on a small phone, a Galaxy S24, or a tablet (like the
 *     dash grid). Presets are ± steps around that device default.
 *
 * The size is then chosen so exactly [count] slots tile the launcher's usable interior of the
 * CURRENT theme ([launcherInteriorPx] subtracts the theme's launcher margins and its
 * launcher-background 9-patch insets), so no icon is cut off and no sliver of an extra one
 * shows. There is deliberately no separate size clamp: clamping the COUNT to
 * `[minCount, maxCount]` already keeps the size within `[MIN_ICON_DP, MAX_ICON_DP]` dp for the
 * bare screen edge (a theme's margins/insets can shave a few dp more) while preserving the
 * exact division.
 */
object LauncherIconGrid {
	/** Target physical icon size (dp) used to pick the device's default slot count. */
	const val TARGET_ICON_DP = 84
	/** Icons never get smaller than this (touch-target floor) — sets the MAX slot count. */
	const val MIN_ICON_DP = 48
	/** Icons never get larger than this (lets "Huge" be large) — sets the MIN slot count. */
	const val MAX_ICON_DP = 160
	/** Number of presets: Tiny · Small · Default · Large · Huge (ascending, like the slider). */
	const val PRESET_COUNT = 5
	/** Preset index of the device default ("Default", the middle of [PRESET_COUNT]). */
	const val DEFAULT_PRESET = 2

	// --- Pure count maths (device-adaptive, theme-independent) -----------------

	/** Fewest slots offered on a screen [shortEdgeDp] dp wide (icons capped at [MAX_ICON_DP]). */
	@JvmStatic
	fun minCount(shortEdgeDp: Int): Int =
		max(2, ceil(shortEdgeDp.toDouble() / MAX_ICON_DP).toInt())

	/** Most slots offered on a screen [shortEdgeDp] dp wide (icons floored at [MIN_ICON_DP]). */
	@JvmStatic
	fun maxCount(shortEdgeDp: Int): Int = max(minCount(shortEdgeDp), shortEdgeDp / MIN_ICON_DP)

	/** The device-adaptive default slot count (the middle preset) for [shortEdgeDp]. */
	@JvmStatic
	fun defaultCount(shortEdgeDp: Int): Int =
		(shortEdgeDp.toDouble() / TARGET_ICON_DP).roundToInt()
			.coerceIn(minCount(shortEdgeDp), maxCount(shortEdgeDp))

	/**
	 * The slot count for [presetIndex] (0 = Tiny … [PRESET_COUNT]-1 = Huge): an offset from the
	 * device default, clamped to the screen's `[minCount, maxCount]`. The index runs the same way
	 * round as the customise slider — dragging right makes icons BIGGER, so a higher index means
	 * fewer, larger slots.
	 */
	@JvmStatic
	fun countForPreset(shortEdgeDp: Int, presetIndex: Int): Int {
		val offset = DEFAULT_PRESET - presetIndex.coerceIn(0, PRESET_COUNT - 1)
		return (defaultCount(shortEdgeDp) + offset)
			.coerceIn(minCount(shortEdgeDp), maxCount(shortEdgeDp))
	}

	// --- Pure size maths (exact fit) -------------------------------------------

	/**
	 * Per-slot size in px so that exactly [n] slots tile [interiorPx]. Floors, so
	 * `n * size <= interiorPx` always — no slot is ever cut off.
	 */
	@JvmStatic
	fun iconSizePx(interiorPx: Int, n: Int): Int =
		if (n <= 0) 0 else interiorPx.coerceAtLeast(0) / n

	/** Icon view height: the running-strip trims 4dp off the square (see [AppLauncher.init]). */
	@JvmStatic
	fun iconHeightPx(sizePx: Int, density: Float): Int =
		(sizePx - (4F * density).toInt()).coerceAtLeast(0)

	/**
	 * The largest whole multiple of [sizePx] that fits within [availPx] — the length to clip a
	 * launcher scroll viewport to, so a partial pinned icon can never peek past the visible run.
	 */
	@JvmStatic
	fun viewportClipPx(availPx: Int, sizePx: Int): Int =
		if (sizePx <= 0) availPx else (availPx / sizePx) * sizePx

	// --- Runtime resolvers -----------------------------------------------------

	/** The stored preset index, clamped to `0..PRESET_COUNT-1`; defaults to [DEFAULT_PRESET]. */
	@JvmStatic
	fun preset(context: Context): Int =
		Preferences.getSharedPreferences(context)
			.getInt(Preference.LAUNCHER_ICON_PRESET.getName(), DEFAULT_PRESET)
			.coerceIn(0, PRESET_COUNT - 1)

	/** Slot count for the current screen + stored preset (the whole number that must fit). */
	@JvmStatic
	fun count(context: Context): Int = countForPreset(context, preset(context))

	/** Slot count for the current screen and a specific [presetIndex] (for the customise hint). */
	@JvmStatic
	fun countForPreset(context: Context, presetIndex: Int): Int =
		countForPreset(context.resources.configuration.smallestScreenWidthDp, presetIndex)

	/**
	 * Memoises the resolved interior: the resolution below inflates the theme's launcher
	 * background (a 9-patch bitmap) and two TypedArrays, and it is called from the clipping
	 * viewports' onMeasure — which runs per FRAME during the per-desktop swipe morph
	 * ([PinnedAppsBar] requests layout on every morph update). The key captures everything the
	 * result depends on; UI-thread only, like all view sizing.
	 */
	private var interiorCacheKey: Triple<String, Int, Int>? = null
	private var interiorCachePx = 0

	/**
	 * The usable along-edge length (px) of the launcher placed on the screen's SHORTEST edge:
	 * the shortest screen edge minus the current theme's launcher margins and the launcher
	 * background's 9-patch content insets. The shortest-edge launcher is a horizontal bar, so we
	 * take the left+right insets of the theme's bottom launcher background; computing from this
	 * fixed hypothetical (never the live container) keeps the icon size stable across rotation
	 * and identical wherever the launcher is actually docked.
	 *
	 * The edge is smallestScreenWidthDp × density — the same stable anchor the slot count uses —
	 * rather than raw display metrics, whose min(width, height) can drift under configuration
	 * changes the activity handles without recreating (rotation with 3-button nav, a long-axis
	 * multi-window resize). Everything this px value depends on (smallestScreenSize, density,
	 * theme) forces a recreate when it changes, so a computed size can never go stale.
	 */
	@JvmStatic
	fun launcherInteriorPx(context: Context): Int {
		val res = context.resources
		val dm = res.displayMetrics
		val shortEdgePx = (res.configuration.smallestScreenWidthDp * dm.density).toInt()
		val theme = DependencyContainer.of(context).themeManager.current

		val cacheKey = Triple(theme.getName(), shortEdgePx, dm.densityDpi)
		if (cacheKey == this.interiorCacheKey) {
			return this.interiorCachePx
		}

		// Theme launcher_margin: 4-item array [top, right, bottom, left]; a horizontal bar
		// loses the two horizontal ends (indices 1 and 3, matching LauncherEdgeController's
		// convention). Every theme uses symmetric margins, but sum both ends regardless.
		val margins = res.obtainTypedArray(theme.launcher_margin)
		val marginPx = margins.getDimensionPixelSize(1, 0) + margins.getDimensionPixelSize(3, 0)
		margins.recycle()

		// Launcher background 9-patch insets. Dynamic (solid-colour) backgrounds have none.
		var paddingPx = 0
		if (! res.getBoolean(theme.launcher_background_dynamic)) {
			val backgrounds = res.obtainTypedArray(theme.launcher_background)
			val drawableRes = backgrounds.getResourceId(Location.BOTTOM.n, 0)
			backgrounds.recycle()
			if (drawableRes != 0) {
				val rect = Rect()
				if (ContextCompat.getDrawable(context, drawableRes)?.getPadding(rect) == true)
					paddingPx = rect.left + rect.right
			}
		}

		val interior = (shortEdgePx - marginPx - paddingPx).coerceAtLeast(0)
		this.interiorCacheKey = cacheKey
		this.interiorCachePx = interior

		return interior
	}

	/** The launcher's docked edge, honouring the user's choice over the theme's default. */
	@JvmStatic
	fun launcherEdge(context: Context): Location {
		val res = context.resources
		val theme = DependencyContainer.of(context).themeManager.current

		return Location.of(Preferences.getSharedPreferences(context)
			.getInt(Preference.LAUNCHER_EDGE.getName(), res.getInteger(theme.launcher_location)))
	}

	/**
	 * The usable along-edge length (px) of the launcher where it is ACTUALLY docked:
	 * [launcherInteriorPx] for a horizontal (top/bottom) one, and the long screen edge for a
	 * vertical one — less the same theme margins and background insets, the panel a side
	 * launcher runs below, and the system bars ([verticalSystemBarPx]).
	 *
	 * The long edge is max(screenWidthDp, screenHeightDp) so that, like every other length
	 * here, it does not flip on rotation.
	 */
	@JvmStatic
	fun alongEdgeInteriorPx(context: Context): Int {
		val edge = launcherEdge(context)
		if (edge != Location.LEFT && edge != Location.RIGHT) {
			return launcherInteriorPx(context)
		}

		val res = context.resources
		val dm = res.displayMetrics
		val config = res.configuration
		val theme = DependencyContainer.of(context).themeManager.current

		val longEdgePx = (max(config.screenWidthDp, config.screenHeightDp) * dm.density).toInt()

		// A vertical bar loses the two vertical ends of launcher_margin
		// ([top, right, bottom, left]) instead of the horizontal pair.
		val margins = res.obtainTypedArray(theme.launcher_margin)
		val marginPx = margins.getDimensionPixelSize(0, 0) + margins.getDimensionPixelSize(2, 0)
		margins.recycle()

		var paddingPx = 0
		if (! res.getBoolean(theme.launcher_background_dynamic)) {
			val backgrounds = res.obtainTypedArray(theme.launcher_background)
			val drawableRes = backgrounds.getResourceId(edge.n, 0)
			backgrounds.recycle()
			if (drawableRes != 0) {
				val rect = Rect()
				if (ContextCompat.getDrawable(context, drawableRes)?.getPadding(rect) == true)
					paddingPx = rect.top + rect.bottom
			}
		}

		val panelEdge = Location.of(Preferences.getSharedPreferences(context)
			.getInt(Preference.PANEL_EDGE.getName(), res.getInteger(theme.panel_location)))
		val panelPx =
			if (panelEdge == Location.NONE) 0 else res.getDimensionPixelSize(theme.panel_height)

		return (longEdgePx - marginPx - paddingPx - panelPx - verticalSystemBarPx(context))
			.coerceAtLeast(0)
	}

	/**
	 * The vertical space the system bars take from a side launcher: the status bar strip
	 * HomeActivity reserves above the panel (`llStatusBar`), and the bottom inset it pads
	 * `llLauncherAndDashContainer` by.
	 *
	 * Both come from the window rather than from `status_bar_height`/`navigation_bar_height`,
	 * for the same reason HomeActivity uses tappableElement insets: under gesture navigation
	 * the bottom inset is zero and the launcher really does extend behind the pill, which the
	 * dimension resource would wrongly reserve anyway. Needs a visual context and yields 0
	 * without one — only the customise hint calls this, always with the activity.
	 */
	private fun verticalSystemBarPx(context: Context): Int {
		if (context !is Activity) return 0

		val insets = context.getSystemService(WindowManager::class.java)
			?.currentWindowMetrics?.windowInsets ?: return 0
		val bars = insets.getInsets(
			WindowInsets.Type.statusBars() or WindowInsets.Type.tappableElement())

		return bars.top + bars.bottom
	}

	/**
	 * How many icons the launcher actually shows for [presetIndex], along the edge it is
	 * docked on — what the customise hint reports.
	 *
	 * This is not [countForPreset]. That count is the number of slots the icon SIZE is derived
	 * from, always measured across the screen's shortest edge so the size stays put whatever
	 * the theme, the edge or the rotation (see [launcherInteriorPx]). A launcher docked on a
	 * side runs along the LONG edge, so it shows proportionally more icons than that — and the
	 * short-edge count is then a number nowhere on screen.
	 *
	 * It counts icon slots, so the menu button and the preferences icon are among them (they
	 * are icons the user can see and count); the trash and app-info targets only appear during
	 * a drag and are not on screen while this is read.
	 */
	@JvmStatic
	fun visibleCountForPreset(context: Context, presetIndex: Int): Int {
		val sizePx = iconSizePx(launcherInteriorPx(context), countForPreset(context, presetIndex))
		val edge = launcherEdge(context)

		/*
		 * An icon is laid out square-minus-4dp TALL (AppLauncher.init), so the two axes do not
		 * pitch alike: a vertical launcher stacks by that reduced height and fits an icon or
		 * two more than the width alone would suggest, while a horizontal one runs by width.
		 */
		val pitchPx = if (edge == Location.LEFT || edge == Location.RIGHT) {
			iconHeightPx(sizePx, context.resources.displayMetrics.density)
		} else {
			sizePx
		}

		return if (pitchPx <= 0) 0 else max(1, alongEdgeInteriorPx(context) / pitchPx)
	}

	/** The per-slot icon size (px) for the current screen, theme and stored preset. */
	@JvmStatic
	fun iconSizePx(context: Context): Int =
		iconSizePx(launcherInteriorPx(context), count(context))

	/** The icon view height (px) for the current screen, theme and stored preset. */
	@JvmStatic
	fun iconHeightPx(context: Context): Int =
		iconHeightPx(iconSizePx(context), context.resources.displayMetrics.density)
}
