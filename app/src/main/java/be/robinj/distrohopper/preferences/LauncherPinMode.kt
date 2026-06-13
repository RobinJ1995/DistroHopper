package be.robinj.distrohopper.preferences

import android.content.SharedPreferences

/**
 * Whether pinned launcher apps are shared across every widget desktop
 * ([GLOBAL]) or kept separately per desktop ([DESKTOP]).
 *
 * Stored as the string [Preference.LAUNCHER_APP_PIN_MODE] (`global`/`desktop`).
 * The default is per-desktop, so an unset preference resolves to [DESKTOP]
 * (reads pass `"desktop"` as the SharedPreferences default); any other value,
 * including an unrecognised one, resolves to [GLOBAL] as a safe fallback.
 */
enum class LauncherPinMode(val value: String) {
	GLOBAL("global"),
	DESKTOP("desktop");

	companion object {
		/** Maps a stored value to a mode; only an exact `"desktop"` is DESKTOP. */
		@JvmStatic
		fun of(value: String?): LauncherPinMode =
			if (value == DESKTOP.value) DESKTOP else GLOBAL

		/** The mode in effect, reading with the per-desktop default for an unset value. */
		@JvmStatic
		fun current(prefs: SharedPreferences): LauncherPinMode =
			of(prefs.getString(Preference.LAUNCHER_APP_PIN_MODE.getName(),
				Preference.LAUNCHER_APP_PIN_MODE.getDefault()))
	}
}
