package be.robinj.distrohopper.preferences

import androidx.preference.CheckBoxPreference
import be.robinj.distrohopper.R
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import androidx.preference.Preference as AndroidxPreference

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class CrashReportPreferenceTest {
    private val key = Preference.CRASH_REPORTING_ENABLED.getName()

    private lateinit var activity: PreferencesActivity
    private lateinit var fragment: PreferencesActivity.PreferencesFragment

    @Before fun setUp() {
        // Only drive the activity to CREATED: that is enough for the fragment's
        // onCreatePreferences() to build the preference screen, and it avoids the
        // action-bar layout pass that Robolectric mishandles on full resume. //
        activity = Robolectric.buildActivity(PreferencesActivity::class.java).create().get()
        activity.supportFragmentManager.executePendingTransactions()
        fragment = activity.supportFragmentManager
            .findFragmentById(be.robinj.distrohopper.R.id.preferences_container)
                as PreferencesActivity.PreferencesFragment
    }

    @After fun tearDown() { activity.finish() }

    class UnconfiguredPreferencesFragment :
        PreferencesActivity.PreferencesFragment() {
        override fun isCrashReportingConfigured(): Boolean = false
    }

    /** Full screen initialisation removes the toggle when this build cannot send reports. */
    @Test fun toggleHiddenWhenAcraNotConfigured() {
        fragment = UnconfiguredPreferencesFragment()
        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.preferences_container, fragment)
            .commitNow()

        assertNull(fragment.findPreference<AndroidxPreference>(key))
    }

    private fun reAddToggle(): CheckBoxPreference {
        fragment.findPreference<AndroidxPreference>(key)?.let { existing ->
            existing.parent?.removePreference(existing)
        }
        val pref = CheckBoxPreference(fragment.requireContext())
        pref.key = key
        fragment.preferenceScreen.addPreference(pref)
        return pref
    }

    /** Unconfigured path: the toggle is removed from its parent. */
    @Test fun applyRemovesToggleWhenNotConfigured() {
        reAddToggle()
        assertNotNull("precondition: toggle present", fragment.findPreference<AndroidxPreference>(key))

        fragment.applyCrashReportsPreference(false)

        assertNull(fragment.findPreference<AndroidxPreference>(key))
    }

    /** Configured path: the toggle stays and gets a change listener that applies immediately. */
    @Test fun applyKeepsToggleAndWiresListenerWhenConfigured() {
        val pref = reAddToggle()

        fragment.applyCrashReportsPreference(true)

        assertNotNull("toggle must remain visible when configured",
            fragment.findPreference<AndroidxPreference>(key))
        val listener = pref.onPreferenceChangeListener
        assertNotNull("a change listener must be attached", listener)

        // The listener applies the new state immediately and must accept the change. //
        assertTrue(listener!!.onPreferenceChange(pref, false))
        assertTrue(listener.onPreferenceChange(pref, true))
    }
}
