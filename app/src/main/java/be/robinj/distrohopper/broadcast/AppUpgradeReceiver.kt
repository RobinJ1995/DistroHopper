package be.robinj.distrohopper.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.PreferencesRepository

/** Prevents an APK update from being mistaken for an eligible first install. */
class AppUpgradeReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
			PreferencesRepository(context).edit {
				putBoolean(Preference.DEFAULT_PINS_AUTO_INELIGIBLE.getName(), true)
				remove(Preference.DEFAULT_PINS_PENDING.getName())
			}
		}
	}
}
