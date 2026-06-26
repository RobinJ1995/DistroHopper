package be.robinj.distrohopper.home

import android.content.Context
import android.graphics.Color
import be.robinj.distrohopper.desktop.Wallpaper
import be.robinj.distrohopper.theme.Theme

/**
 * Resolves the background colour of a launcher app-tile (the rounded backplate
 * behind a launcher-bar icon, including the BFB) for the active theme — the same
 * value [WallpaperColourApplier] applies live in the launcher.
 *
 * Static themes use their fixed `launcher_applauncher_backgroundcolour`; the
 * chameleonic (dynamic) theme derives it from the wallpaper's primary colour,
 * with the launcher's live-wallpaper fallback. Shared so the BFB widget renders
 * the same colour the running launcher would, without duplicating the rule.
 */
object LauncherTileColour {
	/** The chameleonic tile colour for a dynamic theme, at the given opacity. */
	fun dynamic(wallpaper: Wallpaper, opacity: Int): Int =
		if (wallpaper.isLiveWallpaper) Color.argb(40, 40, 40, 40)
		else wallpaper.getAverageColour(opacity)

	/**
	 * The resolved tile colour for [theme] in [context], usable off the home
	 * screen (e.g. from a widget). Reads the wallpaper itself for dynamic themes.
	 */
	fun resolve(context: Context, theme: Theme): Int {
		val res = context.resources

		if (!res.getBoolean(theme.launcher_applauncher_backgroundcolour_dynamic))
			return res.getColor(theme.launcher_applauncher_backgroundcolour, null)

		val wallpaper = Wallpaper(context).also { it.init() }
		val opacity = res.getInteger(theme.launcher_applauncher_backgroundcolour_opacity)

		return this.dynamic(wallpaper, opacity)
	}
}
