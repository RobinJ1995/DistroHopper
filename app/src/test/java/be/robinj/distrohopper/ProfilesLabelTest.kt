package be.robinj.distrohopper

import android.content.Context
import android.os.UserManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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

	@Test fun unknownOrPre34TypeFallsBackToWork() {
		// Pre-API-34 (no getLauncherUserInfo) the only non-personal profile a
		// launcher sees is a managed/work one; an unrecognised type is generic.
		assertEquals(R.string.profile_work, Profiles.labelRes(null))
		assertEquals(R.string.profile_other, Profiles.labelRes("android.os.usertype.profile.FUTURE"))
	}

	@Test fun personalProfileIsLabelledPersonal() {
		val context = ApplicationProvider.getApplicationContext<Context>()
		assertEquals(context.getString(R.string.profile_personal), Profiles.label(context, null))
	}
}
