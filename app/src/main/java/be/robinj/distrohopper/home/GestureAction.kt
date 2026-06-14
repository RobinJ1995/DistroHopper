package be.robinj.distrohopper.home

/**
 * What a home-screen swipe-up or swipe-down gesture does. The [value] is the
 * string persisted in SharedPreferences (and used as the ListPreference entry
 * value), kept stable across releases.
 *
 * Only [OPEN_DASH] and [OPEN_DASH_SEARCH] open the dash; [reconcileOther] uses
 * that to enforce the no-lockout rule — at least one of the two gestures must
 * keep the dash reachable.
 */
enum class GestureAction(val value: String) {
	/** Do nothing (the swipe is inert). */
	NONE("none"),
	/** Open the dash. */
	OPEN_DASH("open_dash"),
	/** Open the dash and focus the search field, raising the keyboard. */
	OPEN_DASH_SEARCH("open_dash_search"),
	/** Open the system notification tray (needs the accessibility service). */
	NOTIFICATIONS("notifications_tray");

	val opensDash: Boolean
		get() = this == OPEN_DASH || this == OPEN_DASH_SEARCH

	companion object {
		@JvmStatic
		fun fromValue(value: String?): GestureAction =
			entries.firstOrNull { it.value == value } ?: NONE

		/**
		 * No-lockout reconciliation: given that one gesture was just set to
		 * [changed], returns the value the OTHER gesture must take so the dash
		 * stays reachable, or null when no change is needed. (At least one of the
		 * two gestures must open the dash.)
		 */
		@JvmStatic
		fun reconcileOther(changed: GestureAction, other: GestureAction): GestureAction? =
			if (! changed.opensDash && ! other.opensDash) OPEN_DASH else null
	}
}
