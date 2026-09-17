package be.robinj.distrohopper

import android.content.Context
import android.os.Process
import android.os.UserManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Profile labels come from the system profile *type* (not a name Android does
 * not expose), mapped to our own strings; verify each type maps correctly.
 */
@RunWith(RobolectricTestRunner::class)
class ProfilesLabelTest {
	@Test fun profileTypeMapsToItsOwnLabel() {
		assertEquals(R.string.profile_work,
			Profiles.labelRes(UserManager.USER_TYPE_PROFILE_MANAGED))
		assertEquals(R.string.profile_private,
			Profiles.labelRes(UserManager.USER_TYPE_PROFILE_PRIVATE))
		assertEquals(R.string.profile_clone,
			Profiles.labelRes(UserManager.USER_TYPE_PROFILE_CLONE))
	}

	@Test fun unknownTypeFallsBackToWork() {
		// Without a readable type (no getLauncherUserInfo before API 35) the only
		// non-personal profile a launcher sees is a managed/work one; an
		// unrecognised type is generic.
		assertEquals(R.string.profile_work, Profiles.labelRes(null))
		assertEquals(R.string.profile_other, Profiles.labelRes("android.os.usertype.profile.FUTURE"))
	}

	@Test fun personalProfileIsLabelledPersonal() {
		val context = ApplicationProvider.getApplicationContext<Context>()
		assertEquals(context.getString(R.string.profile_personal), Profiles.label(context, null))
	}

	// getLauncherUserInfo is API 35, not 34: an API 34 guard threw on Android 14.
	@Test
	@Config(sdk = [34])
	fun labellingAProfileBelowApi35DoesNotCallGetLauncherUserInfo() {
		val context = ApplicationProvider.getApplicationContext<Context>()

		assertEquals(context.getString(R.string.profile_work),
			Profiles.label(context, Process.myUserHandle()))
	}
}
