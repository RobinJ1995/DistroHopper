package be.robinj.distrohopper

import android.app.Application
import android.app.role.RoleManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowRoleManager

@RunWith(RobolectricTestRunner::class)
class HomeRoleTest {
    private val context: Application = ApplicationProvider.getApplicationContext()

    private fun roleManagerShadow(): ShadowRoleManager =
        Shadow.extract(context.getSystemService(RoleManager::class.java))

    @Test fun roleRequestIntentIsGivenWhenTheHomeRoleIsAvailable() {
        this.roleManagerShadow().addAvailableRole(RoleManager.ROLE_HOME)

        assertNotNull(HomeRole.roleRequestIntent(this.context))
    }

    @Test fun roleRequestIntentIsNullWhenTheHomeRoleIsUnavailable() {
        // Nothing is made available, so the broken-on-some-OEMs role dialog is
        // skipped and callers fall back to the home-settings screen //
        assertNull(HomeRole.roleRequestIntent(this.context))
    }

    @Test fun homeSettingsIntentOpensTheDefaultHomeAppPicker() {
        assertEquals(Settings.ACTION_HOME_SETTINGS, HomeRole.homeSettingsIntent().action)
    }

    @Test fun isHeldReflectsWhetherTheHomeRoleIsHeld() {
        assertFalse(HomeRole.isHeld(this.context))

        this.roleManagerShadow().addHeldRole(RoleManager.ROLE_HOME)

        assertTrue(HomeRole.isHeld(this.context))
    }
}
