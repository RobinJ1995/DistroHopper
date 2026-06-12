package be.robinj.distrohopper

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.Preferences
import kotlinx.coroutines.Dispatchers
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode
import org.robolectric.config.ConfigurationRegistry
import org.robolectric.shadows.ShadowLooper

internal object ActivityTestSupport {
    private val packages = listOf(
        Triple("com.example.alpha", "AlphaActivity", "Alpha"),
        Triple("com.example.beta", "BetaActivity", "Beta"),
        Triple("com.example.gamma", "GammaActivity", "Gamma"),
        Triple("com.example.settings", "SettingsActivity", "Settings"),
        Triple("com.example.zeta", "ZetaActivity", "Zeta"),
    )

    fun seedPackageManager() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val packageManager = Shadows.shadowOf(application.packageManager)
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        packages.forEach { (packageName, activityName, label) ->
            val resolveInfo = resolveInfo(packageName, activityName, label)
            packageManager.addResolveInfoForIntent(launcherIntent, resolveInfo)
            packageManager.addResolveInfoForIntent(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(packageName),
                resolveInfo,
            )
        }
    }

    fun resolveInfo(packageName: String, activityName: String, label: String): ResolveInfo {
        val activityInfo = ActivityInfo().apply {
            this.packageName = packageName
            name = activityName
            nonLocalizedLabel = label
            applicationInfo = ApplicationInfo().apply {
                this.packageName = packageName
                enabled = true
            }
        }

        return ResolveInfo().apply {
            this.activityInfo = activityInfo
            nonLocalizedLabel = label
        }
    }

    fun launchHome(
        customise: Boolean = false,
        configurePrefs: (SharedPreferences.Editor) -> Unit = {},
    ): ActivityScenario<HomeActivity> {
        val application = ApplicationProvider.getApplicationContext<Application>()
        listOf(Preferences.PREFERENCES, Preferences.PINNED_APPS, Preferences.LENSES).forEach {
            application.getSharedPreferences(it, 0).edit().clear().commit()
        }
        application.getSharedPreferences(Preferences.PREFERENCES, 0).edit()
            .also(configurePrefs).commit()
        DependencyContainer.of(ApplicationProvider.getApplicationContext()).customiseMode.value = false
        installTestDispatchers()
        seedPackageManager()
        val intent = Intent(application, HomeActivity::class.java)
            .apply { if (customise) putExtra("customise", true) }
        return ActivityScenario.launch<HomeActivity>(intent).also { drainTasks() }
    }

    /**
     * StartupLoader's background work must run inline so that drainTasks()
     * deterministically completes it: the IO dispatcher is replaced with
     * Unconfined while the main dispatcher stays looper-backed (drained by
     * Robolectric).
     */
    fun installTestDispatchers() {
        DependencyContainer.of(ApplicationProvider.getApplicationContext<Application>())
            .dispatchers = object : DispatcherProvider {
                override val main = Dispatchers.Main
                override val io = Dispatchers.Unconfined
                override val default = Dispatchers.Unconfined
            }
    }

    fun drainTasks() {
        // The background scheduler only exists under the LEGACY looper; animator-driven
        // tests run PAUSED (animations never advance in LEGACY mode).
        if (ConfigurationRegistry.get(LooperMode.Mode::class.java) == LooperMode.Mode.LEGACY) {
            Robolectric.flushBackgroundThreadScheduler()
        }
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }
}
