package be.robinj.distrohopper.onboarding

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import be.robinj.distrohopper.R
import kotlin.math.abs

/**
 * Fades pages out as they scroll away and moves their content slower than the
 * page itself (parallax), which makes the swipe feel deeper than a flat slide.
 */
class OnboardingPageTransformer : ViewPager2.PageTransformer {
	override fun transformPage(page: View, position: Float) {
		page.alpha = 1f - 0.5f * abs(position).coerceAtMost(1f)
		page.findViewById<View>(R.id.llOnboardingPageContent)
			?.translationX = -position * page.width * 0.2f
	}
}
