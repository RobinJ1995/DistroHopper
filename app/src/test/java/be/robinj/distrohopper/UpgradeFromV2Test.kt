package be.robinj.distrohopper

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.broadcast.AppUpgradeReceiver
import be.robinj.distrohopper.desktop.dash.lens.LensManager
import be.robinj.distrohopper.onboarding.OnboardingGate
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import be.robinj.distrohopper.preferences.PreferencesActivity
import be.robinj.distrohopper.preferences.PreferencesRepository
import be.robinj.distrohopper.theme.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

/**
 * Upgrading an existing 2.7.0 install to 3.x: the app must start on the
 * preferences that release wrote, keep what still exists, and not mistake the
 * upgrade for a first install.
 *
 * Everything seeded here is written exactly as 2.7.0 wrote it — same file, same
 * keys, same *types* (SharedPreferences throws a ClassCastException when a key
 * is re-read as another type, so a type change is an upgrade crash, not a lost
 * setting).
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class UpgradeFromV2Test {
	private lateinit var application: Application

	@Before fun setUp() {
		this.application = ApplicationProvider.getApplicationContext()
		listOf(Preferences.PREFERENCES, Preferences.PINNED_APPS, Preferences.LENSES,
			Preferences.DASH_LAYOUT, Preferences.LAUNCHER_LAYOUT).forEach {
			this.application.getSharedPreferences(it, 0).edit().clear().commit()
		}
	}

	/**
	 * The main "prefs" file of a 2.7.0 user who had customised the app: every
	 * key in that release's Preference enum, with the type its writer used.
	 */
	private fun seedV270Preferences(theme: String? = "cinnamon") {
		Preferences.getSharedPreferences(this.application).edit().apply {
			theme?.let { putString("theme", it) }
			putInt("panel_edge_v2", Location.BOTTOM.n)
			putInt("panel_opacity", 70)
			putInt("launcher_edge_v2", Location.RIGHT.n)
			putBoolean("launcher_running_show", true)
			putInt("launchericon_width", 48)
			putInt("launchericon_opacity", 180)
			putBoolean("launcherservice_enabled", true)
			putBoolean("dash_ready_show", true)
			putBoolean("dashsearch_full", false)
			putString("dashsearch_lenses_maxresults", "20")
			putInt("dashicon_width", 32)
			putString("unitywallpaper_blur", "blur")
			putInt("unitybackground_colour", -16776961)
			putInt("unitybackground_opacity", 60)
			putBoolean("unitybackground_dynamic", false)
			putBoolean("widgets_enabled", true)
			putString("icon_pack", "com.example.iconpack")
			putBoolean("dev", true)
			putBoolean("dev_log_toaster", true)
		}.commit()
	}

	/** 2.7.0's AppManager.savePinnedApps: flat "<index>" -> "pkg\nactivity". */
	private fun seedV270PinnedApps() {
		this.application.getSharedPreferences(Preferences.PINNED_APPS, 0).edit()
			.putString("0", "com.example.alpha\nAlphaActivity")
			.putString("1", "com.example.beta\nBetaActivity")
			.commit()
	}

	/** 2.7.0's LensManager.saveEnabledLenses: "<index>" -> simple class name. */
	private fun seedV270Lenses() {
		this.application.getSharedPreferences(Preferences.LENSES, 0).edit()
			.putString("0", "InstalledApps")
			.putString("1", "DuckDuckGo")
			.putString("2", "StackOverflow")
			.putString("3", "LocalFiles")
			.putString("4", "GitHub")
			.commit()
	}

	/** The package replacement the system broadcasts when the APK is updated. */
	private fun replacePackage() =
		AppUpgradeReceiver().onReceive(this.application, Intent(Intent.ACTION_MY_PACKAGE_REPLACED))

	private fun launchUpgradedHome(): ActivityScenario<HomeActivity> {
		ActivityTestSupport.installTestDispatchers()
		ActivityTestSupport.seedPackageManager()
		DependencyContainer.of(this.application).customiseMode.value = false

		return ActivityScenario.launch<HomeActivity>(
			Intent(this.application, HomeActivity::class.java))
			.also { ActivityTestSupport.drainTasks() }
	}

	@Test fun aThemedInstallIsGrandfatheredPastTheFirstRunWizard() {
		this.seedV270Preferences()
		this.replacePackage()

		assertFalse(OnboardingGate.shouldShow(PreferencesRepository(this.application)))
	}

	/**
	 * The wizard's only evidence of a pre-wizard install is a stored theme, and
	 * 2.7.0 wrote that key ONLY from its theme picker
	 * (ThemePreferencesButtonClickListener) — never on startup, and never for
	 * the default (Unity) theme. A 2.7.0 user who never switched theme upgrades
	 * with no "theme" key and is treated as a first run.
	 */
	@Test fun anInstallThatNeverSwitchedThemeStillGetsTheWizard() {
		this.seedV270Preferences(theme = null)
		this.replacePackage()

		assertTrue(OnboardingGate.shouldShow(PreferencesRepository(this.application)))
	}

	/** Whatever the wizard does, an upgrade must not get first-install pins. */
	@Test fun theUpgradeIsNeverTreatedAsAPinnableFirstInstall() {
		this.seedV270Preferences(theme = null)
		this.replacePackage()

		val prefs = PreferencesRepository(this.application)
		OnboardingGate.markStarted(prefs)

		assertTrue(prefs.getBoolean(Preference.DEFAULT_PINS_AUTO_INELIGIBLE, false))
		assertFalse(prefs.getBoolean(Preference.DEFAULT_PINS_PENDING, false))
	}

	@Test fun theHomeScreenStartsStraightOnTheV2Preferences() {
		this.seedV270Preferences()
		this.seedV270PinnedApps()
		this.seedV270Lenses()
		this.replacePackage()

		this.launchUpgradedHome().use { scenario ->
			scenario.onActivity { activity ->
				assertFalse(activity.isFinishing)
				assertTrue(activity.findViewById<android.view.View>(
					R.id.llLauncherAndDashContainer) != null)
			}
		}
	}

	@Test fun theSettingsThatStillExistSurviveTheUpgrade() {
		this.seedV270Preferences()
		this.seedV270PinnedApps()
		this.seedV270Lenses()
		this.replacePackage()

		this.launchUpgradedHome().use { scenario ->
			scenario.onActivity { activity ->
				val prefs = Preferences.getSharedPreferences(activity)

				assertEquals("cinnamon", prefs.getString(Preference.THEME.getName(), null))
				assertEquals("cinnamon",
					DependencyContainer.of(activity).themeManager.current.getName())
				assertEquals(Location.BOTTOM.n,
					prefs.getInt(Preference.PANEL_EDGE.getName(), -1))
				assertEquals(Location.RIGHT.n,
					prefs.getInt(Preference.LAUNCHER_EDGE.getName(), -1))
				assertEquals(70, prefs.getInt(Preference.PANEL_OPACITY.getName(), -1))
				assertTrue(prefs.getBoolean(
					Preference.LAUNCHER_SHOW_RUNNING_APPS.getName(), false))
				assertTrue(prefs.getBoolean(
					Preference.LAUNCHERSERVICE_ENABLED.getName(), false))
				assertFalse(prefs.getBoolean(Preference.DASH_SEARCH_FULL.getName(), true))
				assertEquals("20", prefs.getString(
					Preference.DASH_SEARCH_LENSES_MAX_RESULTS.getName(), null))
				assertEquals("com.example.iconpack",
					prefs.getString(Preference.ICON_PACK.getName(), null))
				assertTrue(prefs.getBoolean(Preference.DEV.getName(), false))
				assertTrue(prefs.getBoolean(Preference.DEV_LOG_TOASTER.getName(), false))
			}
		}
	}

	@Test fun thePinnedAppsAreStillPinnedAndInOrder() {
		this.seedV270Preferences()
		this.seedV270PinnedApps()
		this.replacePackage()

		this.launchUpgradedHome().use { scenario ->
			scenario.onActivity { activity ->
				assertEquals(
					listOf("com.example.alpha", "com.example.beta"),
					activity.appManager.pinned.map { it.packageName },
				)
			}
		}
	}

	/**
	 * Lenses 3.x no longer ships (StackOverflow) are skipped rather than
	 * breaking the list; LocalFiles re-keyed to LocalFiles_v2 for the SAF
	 * rewrite, so it has to be re-enabled by hand.
	 */
	@Test fun theEnabledLensesThatStillExistAreStillEnabled() {
		this.seedV270Preferences()
		this.seedV270Lenses()
		this.replacePackage()

		val enabled = LensManager(this.application, null, null, null, null)
			.enabledLenses.map { it.key }

		assertEquals(listOf("InstalledApps", "DuckDuckGo", "GitHub"), enabled)
	}

	@Test fun theMaxResultsPerLensSettingIsStillRead() {
		this.seedV270Preferences()
		this.seedV270Lenses()
		this.replacePackage()

		assertEquals(20, LensManager(this.application, null, null, null, null).maxResultsPerLens)
	}

	/** Re-reading a 2.7.0 key as the wrong type would blow up here. */
	@Test fun theSettingsScreenOpensOnTheV2Preferences() {
		this.seedV270Preferences()
		this.replacePackage()

		ActivityScenario.launch(PreferencesActivity::class.java).use { scenario ->
			scenario.onActivity { activity -> assertFalse(activity.isFinishing) }
		}
	}
}
