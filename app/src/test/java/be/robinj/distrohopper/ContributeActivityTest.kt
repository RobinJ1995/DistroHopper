package be.robinj.distrohopper

import android.content.Intent
import android.view.View
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class ContributeActivityTest {
    private lateinit var activity: ContributeActivity

    @Before fun setUp() { activity = Robolectric.buildActivity(ContributeActivity::class.java).setup().get() }
    @After fun tearDown() { activity.finish() }

    @Test fun translateButtonOpensTransifex() {
        activity.btnTranslate_clicked(View(activity))
        assertViewIntent("https://www.transifex.com/projects/p/distrohopper/")
    }

    @Test fun bugsButtonOpensGitHubIssues() {
        activity.btnBugs_clicked(View(activity))
        assertViewIntent("https://github.com/RobinJ1995/DistroHopper/issues")
    }

    @Test fun donateButtonOpensPaypal() {
        activity.btnDonate_clicked(View(activity))
        val intent = Shadows.shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("www.paypal.com", intent.data?.host)
    }

    private fun assertViewIntent(url: String) {
        val intent = Shadows.shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(url, intent.dataString)
    }
}
