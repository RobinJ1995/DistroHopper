package be.robinj.distrohopper.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Typed, observable access to the main "prefs" file. The pinned-apps, lenses,
 * and widgets preference files are not covered here; they belong to their
 * respective owners (AppManager, LensManager, DesktopLayoutStorage).
 */
class PreferencesRepository(context: Context) {
	private val prefs: SharedPreferences = Preferences.getSharedPreferences(context)

	fun getBoolean(pref: Preference, default: Boolean): Boolean =
		this.prefs.getBoolean(pref.getName(), default)

	fun getInt(pref: Preference, default: Int): Int =
		this.prefs.getInt(pref.getName(), default)

	fun getString(pref: Preference, default: String?): String? =
		this.prefs.getString(pref.getName(), default)

	fun edit(block: SharedPreferences.Editor.() -> Unit) {
		this.prefs.edit().apply(block).apply()
	}

	/** The selected icon-mask shape preference value (e.g. "system", "circle"). */
	fun iconShape(): String =
		this.getString(Preference.ICON_SHAPE, Preference.ICON_SHAPE.getDefault())
			?: Preference.ICON_SHAPE.getDefault()

	/** Whether monochrome icons are recoloured to a single tint colour. */
	fun tintedIcons(): Boolean =
		this.getBoolean(Preference.TINTED_ICONS, Preference.TINTED_ICONS.getDefault())

	/** The selected tint source/colour token (e.g. "wallpaper", "accent", "theme", "#RRGGBB"). */
	fun iconTint(): String =
		this.getString(Preference.ICON_TINT, Preference.ICON_TINT.getDefault())
			?: Preference.ICON_TINT.getDefault()

	/**
	 * Emits the current value immediately, then again whenever [pref] changes.
	 * Consecutive equal values are collapsed.
	 */
	fun <T> valueFlow(pref: Preference, read: (SharedPreferences) -> T): Flow<T> =
		callbackFlow {
			val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
				if (key == pref.getName()) {
					trySend(read(this@PreferencesRepository.prefs))
				}
			}

			this@PreferencesRepository.prefs.registerOnSharedPreferenceChangeListener(listener)
			trySend(read(this@PreferencesRepository.prefs))

			awaitClose {
				this@PreferencesRepository.prefs
					.unregisterOnSharedPreferenceChangeListener(listener)
			}
		}.distinctUntilChanged()
}
