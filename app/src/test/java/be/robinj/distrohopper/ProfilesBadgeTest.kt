package be.robinj.distrohopper

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.UserHandle
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowApplicationPackageManager

/**
 * Not every profile a launcher can see has a system badge, and the platform is
 * inconsistent about saying so: AOSP hands the drawable back unbadged, while
 * some vendor builds look up badge resource 0 and throw. Either way the badge
 * is simply absent — never a crash on the icon-loading path.
 */
@RunWith(RobolectricTestRunner::class)
class ProfilesBadgeTest {
	private val context = ApplicationProvider.getApplicationContext<Context>()
	private val user = ActivityTestSupport.workProfileHandle()
	private val icon: Drawable get() = ColorDrawable(Color.RED).apply { setBounds(0, 0, 64, 64) }

	@Test @Config(shadows = [ThrowingBadgeShadow::class])
	fun glyphIsAbsentWhenTheSystemBadgeLookupThrows() {
		// Funtouch OS on a vivo V2111 (Android 12) crashed here: the badge //
		// resource for the profile is 0, and looking it up throws. //
		assertNull(Profiles.profileGlyph(this.context, this.user, 32))
	}

	@Test @Config(shadows = [ThrowingBadgeShadow::class])
	fun iconIsLeftUnbadgedWhenTheSystemBadgeLookupThrows() {
		val unbadged = this.icon

		assertSame(unbadged, Profiles.badgedIcon(this.context, unbadged, this.user))
	}

	@Test @Config(shadows = [UnbadgedShadow::class])
	fun glyphIsAbsentWhenTheSystemLeavesTheDrawableUnbadged() {
		assertNull(Profiles.profileGlyph(this.context, this.user, 32))
	}

	@Test @Config(shadows = [UnbadgedShadow::class])
	fun iconIsLeftUnbadgedWhenTheSystemLeavesTheDrawableUnbadged() {
		val unbadged = this.icon

		assertSame(unbadged, Profiles.badgedIcon(this.context, unbadged, this.user))
	}

	@Test @Config(shadows = [BadgingShadow::class])
	fun badgedProfileStillGetsItsGlyphAndBadgedIcon() {
		assertNotNull(Profiles.profileGlyph(this.context, this.user, 32))

		val unbadged = this.icon
		assertNotSame(unbadged, Profiles.badgedIcon(this.context, unbadged, this.user))
	}

	/** A vendor package manager that throws on a profile with no badge resource. */
	@Implements(className = "android.app.ApplicationPackageManager", isInAndroidSdk = false)
	class ThrowingBadgeShadow : ShadowApplicationPackageManager() {
		@Implementation
		protected fun getUserBadgedDrawableForDensity(
			drawable: Drawable, user: UserHandle, badgeLocation: Rect?, badgeDensity: Int): Drawable =
			throw Resources.NotFoundException("Resource ID #0x0")
	}

	/** AOSP's answer for an unbadged profile: the drawable it was handed. */
	@Implements(className = "android.app.ApplicationPackageManager", isInAndroidSdk = false)
	class UnbadgedShadow : ShadowApplicationPackageManager() {
		@Implementation
		protected fun getUserBadgedDrawableForDensity(
			drawable: Drawable, user: UserHandle, badgeLocation: Rect?, badgeDensity: Int): Drawable =
			drawable
	}

	/** A profile that does have a badge (the usual work-profile case). */
	@Implements(className = "android.app.ApplicationPackageManager", isInAndroidSdk = false)
	class BadgingShadow : ShadowApplicationPackageManager() {
		@Implementation
		protected fun getUserBadgedDrawableForDensity(
			drawable: Drawable, user: UserHandle, badgeLocation: Rect?, badgeDensity: Int): Drawable =
			ColorDrawable(Color.BLUE)
	}
}
