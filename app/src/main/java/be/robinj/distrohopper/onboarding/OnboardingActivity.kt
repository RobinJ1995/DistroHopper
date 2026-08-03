package be.robinj.distrohopper.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.viewpager2.widget.ViewPager2
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.HomeRole
import be.robinj.distrohopper.InsetsHelper
import be.robinj.distrohopper.R
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.theme.Theme
import be.robinj.distrohopper.theme.ThemeCards
import be.robinj.distrohopper.theme.ThemeRegistry
import java.util.function.Consumer

/**
 * First-run wizard: theme choice and the option to set DistroHopper as the
 * default home screen. Shown by HomeActivity (gated by [OnboardingGate]) before
 * anything else is initialised; Finish marks setup complete and relaunches
 * HomeActivity so it comes up in the chosen theme.
 */
class OnboardingActivity : AppCompatActivity() {
	private lateinit var container: DependencyContainer
	private lateinit var pager: ViewPager2
	private lateinit var indicator: OnboardingPageIndicator
	private lateinit var btnNext: Button
	private lateinit var adapter: OnboardingPagerAdapter
	private lateinit var themeCards: ThemeCards

	private val pages = OnboardingPage.entries

	private val roleRequest = this.registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) {
		// The native HOME-role dialog is broken on some OEM builds (notably
		// Samsung): it returns without ever showing a picker. When the role still
		// isn't held after it closes, fall back to the system home settings. //
		if (!HomeRole.isHeld(this)) {
			this.homeSettingsRequest.launch(HomeRole.homeSettingsIntent())
		}
		this.adapter.rebind(OnboardingPage.DEFAULT_LAUNCHER)
	}

	// A separate launcher so the home-settings result never re-triggers the
	// fallback above (which would loop). //
	private val homeSettingsRequest = this.registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { this.adapter.rebind(OnboardingPage.DEFAULT_LAUNCHER) }

	/**
	 * Blurs the wallpaper behind the wizard, like the dash does: the wallpaper
	 * lives in a separate system-owned window, so only cross-window blur can
	 * reach it. Invoked with the current state on registration and again
	 * whenever blur availability changes at runtime (e.g. battery saver);
	 * when unavailable, the scrim's darkening alone keeps the text readable.
	 */
	private val crossWindowBlurListener = Consumer<Boolean> { enabled ->
		this.window.setBackgroundBlurRadius(
			if (enabled) this.resources.getDimensionPixelSize(R.dimen.onboarding_blur_radius)
			else 0
		)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		this.setContentView(R.layout.activity_onboarding)
		// The scrim (on the layout root) must extend behind the system bars, so
		// only the content within it is padded clear of them //
		WindowCompat.setDecorFitsSystemWindows(this.window, false)
		InsetsHelper.applySystemBarsPadding(this.findViewById<View>(R.id.llOnboardingRoot))
		this.windowManager.addCrossWindowBlurEnabledListener(this.crossWindowBlurListener)

		this.container = DependencyContainer.of(this)

		// Before any other preference write, so that the gate can tell this
		// wizard run's writes apart from a pre-wizard install's //
		OnboardingGate.markStarted(this.container.prefs)

		this.pager = this.findViewById(R.id.vpOnboarding)
		this.indicator = this.findViewById(R.id.opiOnboardingDots)
		this.btnNext = this.findViewById(R.id.btnOnboardingNext)

		this.themeCards = ThemeCards(
			this.themes(),
			{ this.container.themeManager.current.getName() },
			{ theme -> ThemeCards.applyTheme(this, theme) },
		)

		this.adapter = OnboardingPagerAdapter(this.pages, this::bindPage)
		this.pager.adapter = this.adapter
		this.pager.setPageTransformer(OnboardingPageTransformer())

		this.indicator.count = this.pages.size
		this.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
			override fun onPageScrolled(position: Int, offset: Float, offsetPixels: Int) {
				this@OnboardingActivity.indicator.setPosition(position, offset)
			}

			override fun onPageSelected(position: Int) {
				this@OnboardingActivity.updateButtons(position)
			}
		})

		this.btnNext.setOnClickListener {
			if (this.pager.currentItem == this.pages.size - 1) {
				this.finishSetup()
			} else {
				this.pager.currentItem += 1
			}
		}

		// Back steps through the pages; on the first page it does nothing (the
		// wizard is the task's only activity, and Finish is the way out).
		this.onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				if (this@OnboardingActivity.pager.currentItem > 0) {
					this@OnboardingActivity.pager.currentItem -= 1
				}
			}
		})

		this.updateButtons(this.pager.currentItem)
	}

	override fun onDestroy() {
		this.windowManager.removeCrossWindowBlurEnabledListener(this.crossWindowBlurListener)
		super.onDestroy()
	}

	override fun onResume() {
		super.onResume()

		// The user may have set us as default from system Settings in the meantime //
		this.adapter.rebind(OnboardingPage.DEFAULT_LAUNCHER)
	}

	private fun bindPage(page: OnboardingPage, view: View) {
		when (page) {
			OnboardingPage.WELCOME -> Unit
			OnboardingPage.THEME ->
				this.themeCards.bind(view.findViewById<LinearLayout>(R.id.llOnboardingThemeCards))
			OnboardingPage.DEFAULT_LAUNCHER -> this.bindDefaultLauncherPage(view)
		}
	}

	private fun bindDefaultLauncherPage(view: View) {
		val isDefault = HomeRole.isHeld(this)

		view.findViewById<Button>(R.id.btnOnboardingSetDefault).apply {
			this.visibility = if (isDefault) View.GONE else View.VISIBLE
			this.setOnClickListener {
				val roleIntent = HomeRole.roleRequestIntent(this@OnboardingActivity)
				if (roleIntent != null) {
					this@OnboardingActivity.roleRequest.launch(roleIntent)
				} else {
					this@OnboardingActivity.homeSettingsRequest
						.launch(HomeRole.homeSettingsIntent())
				}
			}
		}
		view.findViewById<TextView>(R.id.tvOnboardingAlreadyDefault)
			.visibility = if (isDefault) View.VISIBLE else View.GONE
	}

	private fun themes(): List<Theme> {
		val dev = this.container.prefs.getBoolean(Preference.DEV, false)

		return ThemeRegistry.themes.values
			.map { it() }
			.filter { dev || !it.dev_only }
	}

	private fun finishSetup() {
		OnboardingGate.markCompleted(this.container.prefs)
		this.startActivity(Intent(this, HomeActivity::class.java))
		this.finish()
	}

	private fun updateButtons(position: Int) {
		val last = position == this.pages.size - 1

		this.btnNext.setText(if (last) R.string.onboarding_button_done else R.string.onboarding_button_next)
	}
}
