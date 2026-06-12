package be.robinj.distrohopper.onboarding

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.InsetsHelper
import be.robinj.distrohopper.Permission
import be.robinj.distrohopper.R
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.theme.Theme
import be.robinj.distrohopper.theme.ThemeRegistry

/**
 * First-run wizard: theme choice, permission prompts, and the option to set
 * DistroHopper as the default home screen. Shown by HomeActivity (gated by
 * [OnboardingGate]) before anything else is initialised; Done/Skip mark setup
 * complete and relaunch HomeActivity so it comes up in the chosen theme.
 */
class OnboardingActivity : AppCompatActivity() {
	private lateinit var container: DependencyContainer
	private lateinit var pager: ViewPager2
	private lateinit var indicator: OnboardingPageIndicator
	private lateinit var btnSkip: Button
	private lateinit var btnNext: Button
	private lateinit var adapter: OnboardingPagerAdapter
	private lateinit var themeCards: OnboardingThemeCards

	/** The runtime permissions the wizard asks for; extend as the app gains new ones. */
	private val wizardPermissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

	private val permissionRequest = this.registerForActivityResult(
		ActivityResultContracts.RequestMultiplePermissions()
	) { this.adapter.rebind(OnboardingPage.PERMISSION) }

	private val roleRequest = this.registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { this.adapter.rebind(OnboardingPage.DEFAULT_LAUNCHER) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		this.setContentView(R.layout.activity_onboarding)
		InsetsHelper.applySystemBarsPadding(this)

		this.container = DependencyContainer.of(this)

		this.pager = this.findViewById(R.id.vpOnboarding)
		this.indicator = this.findViewById(R.id.opiOnboardingDots)
		this.btnSkip = this.findViewById(R.id.btnOnboardingSkip)
		this.btnNext = this.findViewById(R.id.btnOnboardingNext)

		this.themeCards = OnboardingThemeCards(
			this.themes(),
			{ this.container.themeManager.current.getName() },
			this::applyTheme,
		)

		this.adapter = OnboardingPagerAdapter(this::bindPage)
		this.pager.adapter = this.adapter
		this.pager.setPageTransformer(OnboardingPageTransformer())

		this.indicator.count = OnboardingPage.entries.size
		this.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
			override fun onPageScrolled(position: Int, offset: Float, offsetPixels: Int) {
				this@OnboardingActivity.indicator.setPosition(position, offset)
			}

			override fun onPageSelected(position: Int) {
				this@OnboardingActivity.updateButtons(position)
			}
		})

		this.btnSkip.setOnClickListener { this.finishSetup() }
		this.btnNext.setOnClickListener {
			if (this.pager.currentItem == OnboardingPage.entries.size - 1) {
				this.finishSetup()
			} else {
				this.pager.currentItem += 1
			}
		}

		// Back steps through the pages; on the first page it does nothing (the
		// wizard is the task's only activity, and Skip is the explicit way out).
		this.onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				if (this@OnboardingActivity.pager.currentItem > 0) {
					this@OnboardingActivity.pager.currentItem -= 1
				}
			}
		})

		this.updateButtons(this.pager.currentItem)
	}

	override fun onResume() {
		super.onResume()

		// The user may have granted either from system Settings in the meantime //
		this.adapter.rebind(OnboardingPage.PERMISSION)
		this.adapter.rebind(OnboardingPage.DEFAULT_LAUNCHER)
	}

	private fun bindPage(page: OnboardingPage, view: View) {
		when (page) {
			OnboardingPage.WELCOME -> Unit
			OnboardingPage.THEME ->
				this.themeCards.bind(view.findViewById<LinearLayout>(R.id.llOnboardingThemeCards))
			OnboardingPage.PERMISSION -> this.bindPermissionPage(view)
			OnboardingPage.DEFAULT_LAUNCHER -> this.bindDefaultLauncherPage(view)
		}
	}

	private fun bindPermissionPage(view: View) {
		val granted = this.missingPermissions().isEmpty()

		view.findViewById<Button>(R.id.btnOnboardingGrantPermission).apply {
			this.visibility = if (granted) View.GONE else View.VISIBLE
			this.setOnClickListener {
				this@OnboardingActivity.permissionRequest
					.launch(this@OnboardingActivity.missingPermissions())
			}
		}
		view.findViewById<TextView>(R.id.tvOnboardingPermissionGranted)
			.visibility = if (granted) View.VISIBLE else View.GONE
	}

	private fun bindDefaultLauncherPage(view: View) {
		val isDefault = this.isDefaultLauncher()

		view.findViewById<Button>(R.id.btnOnboardingSetDefault).apply {
			this.visibility = if (isDefault) View.GONE else View.VISIBLE
			this.setOnClickListener { this@OnboardingActivity.requestHomeRole() }
		}
		view.findViewById<TextView>(R.id.tvOnboardingAlreadyDefault)
			.visibility = if (isDefault) View.VISIBLE else View.GONE
	}

	private fun missingPermissions(): Array<String> =
		this.wizardPermissions
			.filterNot { Permission(this, it).check() }
			.toTypedArray()

	private fun isDefaultLauncher(): Boolean =
		this.getSystemService(RoleManager::class.java)
			?.isRoleHeld(RoleManager.ROLE_HOME) == true

	private fun requestHomeRole() {
		val roleManager = this.getSystemService(RoleManager::class.java)

		if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
			this.roleRequest.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
		} else {
			this.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
		}
	}

	private fun themes(): List<Theme> {
		val dev = this.container.prefs.getBoolean(Preference.DEV, false)

		return ThemeRegistry.themes.values
			.map { it() }
			.filter { dev || !it.dev_only }
	}

	/** Same three preferences ThemePreferencesButtonClickListener writes. */
	private fun applyTheme(theme: Theme) {
		val res = this.resources

		this.container.prefs.edit {
			this.putString(Preference.THEME.getName(), theme.getName())
			this.putInt(Preference.LAUNCHER_EDGE.getName(), res.getInteger(theme.launcher_location))
			this.putInt(Preference.PANEL_EDGE.getName(), res.getInteger(theme.panel_location))
		}
	}

	private fun finishSetup() {
		OnboardingGate.markCompleted(this.container.prefs)
		this.startActivity(Intent(this, HomeActivity::class.java))
		this.finish()
	}

	private fun updateButtons(position: Int) {
		val last = position == OnboardingPage.entries.size - 1

		this.btnNext.setText(if (last) R.string.onboarding_button_done else R.string.onboarding_button_next)
		this.btnSkip.visibility = if (last) View.INVISIBLE else View.VISIBLE
	}
}
