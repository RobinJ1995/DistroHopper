package be.robinj.distrohopper.onboarding

import androidx.annotation.LayoutRes
import be.robinj.distrohopper.R

/** The wizard's pages, in order. */
enum class OnboardingPage(@param:LayoutRes val layout: Int) {
	WELCOME(R.layout.widget_onboarding_page_welcome),
	THEME(R.layout.widget_onboarding_page_theme),
	DEFAULT_LAUNCHER(R.layout.widget_onboarding_page_default_launcher),
}
