package be.robinj.distrohopper

import android.content.Intent
import android.os.Parcel
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.cache.TestStringCache
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class AppTest {
    private lateinit var scenario: ActivityScenario<HomeActivity>

    @Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
    @After fun tearDown() { HomeActivity.modeCustomise = false; scenario.close() }

    @Test fun launchStartsMainLauncherIntentForComponent() {
        scenario.onActivity { activity ->
            val app = activity.appManager[0]; app.launch()
            val intent = Shadows.shadowOf(activity).nextStartedActivity
            assertEquals(Intent.ACTION_MAIN, intent.action)
            assertTrue(intent.categories.contains(Intent.CATEGORY_LAUNCHER))
            assertEquals(app.packageName, intent.component?.packageName)
            assertEquals(app.activityName, intent.component?.className)
        }
    }

    @Test fun launchIsBlockedWhileCustomising() {
        scenario.onActivity { activity ->
            val shadow = Shadows.shadowOf(activity)
            while (shadow.nextStartedActivity != null) { }
            HomeActivity.modeCustomise = true; activity.appManager[0].launch()
            assertNull(shadow.nextStartedActivity)
        }
    }

    @Test fun parcelRoundTripPreservesPersistedFields() {
        scenario.onActivity { activity ->
            val original = activity.appManager[0].apply { description = "description" }
            val parcel = Parcel.obtain()
            original.writeToParcel(parcel, 0); parcel.setDataPosition(0)
            val restored = App.CREATOR.createFromParcel(parcel)
            assertEquals(original.packageName, restored.packageName)
            assertEquals(original.activityName, restored.activityName)
            assertEquals(original.label, restored.label)
            assertEquals("description", restored.description)
            parcel.recycle()
        }
    }

    @Test fun creatorBuildsArrayOfRequestedSize() = assertEquals(4, App.CREATOR.newArray(4).size)

    @Test fun settingPreviouslyUnloadedLabelStoresItWithoutCrashing() {
        scenario.onActivity { activity ->
            val app = App(
                activity,
                activity.appManager,
                ActivityTestSupport.resolveInfo("com.example.unloaded", "UnloadedActivity", "Unloaded"),
            )
            val cache = TestStringCache(activity, "app_label_test").apply { clear() }
            assertFalse(app.isLabelLoaded)
            assertTrue(app.setLabel("Renamed", cache))
            assertEquals("Renamed", cache[app.packageAndActivityName])
            cache.clear()
        }
    }

    @Test fun settingSameCachedLabelDoesNotRewriteCache() {
        scenario.onActivity { activity ->
            val app = activity.appManager[0]
            val cache = TestStringCache(activity, "app_label_test").apply { clear() }
            app.setLabel("Renamed", cache)
            assertFalse(app.setLabel("Renamed", cache))
            cache.clear()
        }
    }
}
