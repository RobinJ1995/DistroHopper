package be.robinj.distrohopper.desktop.dash.lens

import android.content.Context
import android.content.pm.PackageManager

/**
 * Base class for lenses that search an app store catalogue (Google Play,
 * F-Droid, …). Provides the shared logic for hiding results the user already
 * has installed.
 */
abstract class AppStoreLens(context: Context) : Lens(context) {
    /** Whether an app with the given package name is installed on this device. */
    protected fun isInstalled(packageName: String): Boolean =
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (ex: PackageManager.NameNotFoundException) {
            false
        }
}
