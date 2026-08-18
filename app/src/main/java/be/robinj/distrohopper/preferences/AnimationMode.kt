package be.robinj.distrohopper.preferences

import android.content.Context
import android.content.SharedPreferences
import android.os.PowerManager
import be.robinj.distrohopper.DependencyContainer

/**
 * When the launcher plays its transition animations (the dash open/close, the
 * launcher bar morph, the desktop pager swipe, the desktop menu sheet).
 *
 * Stored as the string [Preference.ANIMATIONS]. The default is
 * [UNLESS_POWER_SAVING] — the historical behaviour, where battery saver
 * bypasses every transition and applies its final state immediately — so an
 * unset (or unrecognised) preference resolves to it. [ALWAYS] keeps the
 * animations even in battery saver; [OFF] applies the battery-saver behaviour
 * permanently, whether or not battery saver is on.
 */
enum class AnimationMode(val value: String) {
	ALWAYS("always"),
	UNLESS_POWER_SAVING("unless_power_saving"),
	OFF("off");

	/** Whether animations play right now, given the device's battery-saver state. */
	fun enabled(context: Context): Boolean = when (this) {
		ALWAYS -> true
		OFF -> false
		UNLESS_POWER_SAVING ->
			context.getSystemService(PowerManager::class.java)?.isPowerSaveMode != true
	}

	companion object {
		/** Maps a stored value to a mode, defaulting to [UNLESS_POWER_SAVING]. */
		@JvmStatic
		fun of(value: String?): AnimationMode =
			entries.firstOrNull { it.value == value } ?: UNLESS_POWER_SAVING

		/** The mode in effect, reading with the default for an unset value. */
		@JvmStatic
		fun current(prefs: SharedPreferences): AnimationMode =
			of(prefs.getString(Preference.ANIMATIONS.getName(),
				Preference.ANIMATIONS.getDefault()))

		/**
		 * The single gate every animated surface asks: the chosen mode resolved
		 * against the device's current battery-saver state. Read fresh each time,
		 * so both a settings change and battery saver toggling take effect without
		 * the launcher being recreated.
		 */
		@JvmStatic
		fun animationsEnabled(context: Context): Boolean =
			DependencyContainer.of(context).prefs.animationMode().enabled(context)
	}
}
