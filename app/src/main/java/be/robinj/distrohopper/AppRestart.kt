package be.robinj.distrohopper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Performs a full restart of the app: the launcher activity is scheduled to
 * start again and the current process is then shut down, so everything
 * re-initialises from scratch (Application.attachBaseContext/onCreate incl.
 * ACRA, the app/icon/label caches, views, theme, StartupLoader, ...).
 *
 * The shutdown is a clean [Runtime.exit] with status 0, not an uncaught
 * exception or an abort signal, so it is not recorded as a crash - neither by
 * the in-app reporter (ACRA only reports uncaught/explicitly-handled
 * exceptions) nor by Play Console / Android vitals. This is the same mechanism
 * Jake Wharton's ProcessPhoenix relies on. We deliberately avoid
 * Process.killProcess (SIGKILL): the AlarmManager-scheduled relaunch fires
 * after we exit, so the kill would add ambiguity for no benefit.
 *
 * This is the only way to "swipe away and reopen" the app while it holds the
 * HOME role, since the default home screen never shows up in the recent-apps
 * overview.
 */
object AppRestart {
	@JvmStatic
	fun restart(context: Context) {
		val app = context.applicationContext

		val intent = Intent(app, HomeActivity::class.java).apply {
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
		}

		val pending = PendingIntent.getActivity(
			app, 0, intent,
			PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)

		// An inexact alarm needs no SCHEDULE_EXACT_ALARM permission and, since the
		// user just tapped the option (the device is interactive), fires promptly.
		val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
		alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 100L, pending)

		Runtime.getRuntime().exit(0)
	}
}
