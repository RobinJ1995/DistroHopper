package be.robinj.distrohopper

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ResolveInfo
import android.os.UserHandle
import android.os.UserManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import kotlinx.coroutines.Dispatchers
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode
import org.robolectric.config.ConfigurationRegistry
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLauncherApps
import org.robolectric.shadows.ShadowLooper
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

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

    fun settingsShortcut(activity: HomeActivity): App =
        requireNotNull(activity.appManager.findAppByPackageAndActivityName(
            activity.packageName,
            "be.robinj.distrohopper.preferences.PreferencesActivity",
        ))

    private const val WORK_PROFILE_USER_ID = 10
    /** UserHandle.PER_USER_RANGE: uids per user, for getUserHandleForUid(). */
    private const val UIDS_PER_USER = 100_000

    fun workProfileHandle(userId: Int = WORK_PROFILE_USER_ID): UserHandle =
        UserHandle.getUserHandleForUid(userId * UIDS_PER_USER)

    /** Seeds a managed ("work") profile next to the personal one. */
    fun addWorkProfile(userId: Int = WORK_PROFILE_USER_ID): UserHandle {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val userManager = application.getSystemService(Context.USER_SERVICE) as UserManager
        Shadows.shadowOf(userManager).addProfile(
            0, userId, "Work", 0x20 /* UserInfo.FLAG_MANAGED_PROFILE (hidden API) */)

        return workProfileHandle(userId)
    }

    /** Registers a launcher activity in [user]'s profile with ShadowLauncherApps. */
    fun addWorkProfileApp(
        user: UserHandle, packageName: String, activityName: String, label: String) {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val launcherApps = application.getSystemService(LauncherApps::class.java)
        val shadow = Shadow.extract<ShadowLauncherApps>(launcherApps)
        shadow.addActivity(user, launcherActivityInfo(packageName, activityName, label, user))
    }

    /**
     * Builds a LauncherActivityInfo reflectively (its constructor is hidden);
     * the constructor chain matches SDK 36, which robolectric.properties pins.
     */
    fun launcherActivityInfo(
        packageName: String, activityName: String, label: String, user: UserHandle,
    ): LauncherActivityInfo {
        val activityInfo = ActivityInfo().apply {
            this.packageName = packageName
            name = activityName
            nonLocalizedLabel = label
            applicationInfo = ApplicationInfo().apply {
                this.packageName = packageName
                nonLocalizedLabel = label
                enabled = true
            }
        }

        val internalClass = Class.forName("android.content.pm.LauncherActivityInfoInternal")
        val statesInfoClass = Class.forName("android.content.pm.IncrementalStatesInfo")
        val internal = ReflectionHelpers.callConstructor(
            internalClass,
            ClassParameter(ActivityInfo::class.java, activityInfo),
            ClassParameter(statesInfoClass, null),
            ClassParameter(UserHandle::class.java, user),
            ClassParameter(java.lang.Boolean.TYPE, false),
        )

        return ReflectionHelpers.callConstructor(
            LauncherActivityInfo::class.java,
            ClassParameter(Context::class.java,
                ApplicationProvider.getApplicationContext<Application>()),
            ClassParameter(internalClass, internal),
        )
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
        // Fresh prefs would otherwise redirect HomeActivity to the first-run wizard //
        application.getSharedPreferences(Preferences.PREFERENCES, 0).edit()
            .putBoolean(Preference.SETUP_COMPLETED.getName(), true)
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
