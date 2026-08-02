package be.robinj.distrohopper.widgets

import android.content.Context
import be.robinj.distrohopper.preferences.Preferences

/**
 * Terse test access to the unified desktop layout store, so the widget/desktop
 * host tests can seed and read one kind at a time. This store replaced the old
 * per-kind `WidgetPersistence`/`DesktopAppPersistence`/`DesktopFolderPersistence`
 * classes.
 */
object DesktopLayoutTestStore {
	private fun prefs(context: Context) =
		Preferences.getSharedPreferences(context, Preferences.DESKTOP_LAYOUT)

	fun clear(context: Context) {
		prefs(context).edit().clear().commit()
	}

	fun widgets(context: Context): List<WidgetLayout> =
		DesktopLayoutStorage.readWidgets(prefs(context))

	fun apps(context: Context): List<DesktopAppLayout> =
		DesktopLayoutStorage.readApps(prefs(context))

	fun folders(context: Context): List<DesktopFolderLayout> =
		DesktopLayoutStorage.readFolders(prefs(context))

	fun saveWidgets(context: Context, widgets: List<WidgetLayout>) =
		DesktopLayoutStorage.writeWidgets(prefs(context), widgets)

	fun saveApps(context: Context, apps: List<DesktopAppLayout>) =
		DesktopLayoutStorage.writeApps(prefs(context), apps)

	fun saveFolders(context: Context, folders: List<DesktopFolderLayout>) =
		DesktopLayoutStorage.writeFolders(prefs(context), folders)
}
