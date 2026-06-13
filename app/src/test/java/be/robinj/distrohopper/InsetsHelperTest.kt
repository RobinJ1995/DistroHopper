package be.robinj.distrohopper

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InsetsHelperTest {
    private fun insets(systemBars: Insets, cutout: Insets = Insets.NONE): WindowInsetsCompat =
        WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.systemBars(), systemBars)
            .setInsets(WindowInsetsCompat.Type.displayCutout(), cutout)
            .build()

    private fun view(): View {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        return View(activity).apply { setPadding(4, 8, 12, 16) }
    }

    @Test
    fun paddingIsAdditiveToOriginalPaddingAndInsetsAreConsumed() {
        val view = view()
        InsetsHelper.applySystemBarsPadding(view)

        val result = ViewCompat.dispatchApplyWindowInsets(
            view, insets(Insets.of(10, 20, 30, 40)))

        assertTrue(result!!.isConsumed)
        assertEquals(4 + 10, view.paddingLeft)
        assertEquals(8 + 20, view.paddingTop)
        assertEquals(12 + 30, view.paddingRight)
        assertEquals(16 + 40, view.paddingBottom)
    }

    @Test
    fun cutoutInsetsAreIncluded() {
        val view = view()
        InsetsHelper.applySystemBarsPadding(view)

        // getInsets() over (systemBars | displayCutout) yields the max per edge.
        ViewCompat.dispatchApplyWindowInsets(
            view, insets(Insets.of(10, 0, 0, 0), Insets.of(0, 25, 0, 0)))

        assertEquals(4 + 10, view.paddingLeft)
        assertEquals(8 + 25, view.paddingTop)
        assertEquals(12, view.paddingRight)
        assertEquals(16, view.paddingBottom)
    }

    @Test
    fun dispatchingTwiceDoesNotCompoundPadding() {
        val view = view()
        InsetsHelper.applySystemBarsPadding(view)

        ViewCompat.dispatchApplyWindowInsets(view, insets(Insets.of(10, 20, 30, 40)))
        ViewCompat.dispatchApplyWindowInsets(view, insets(Insets.of(10, 20, 30, 40)))

        assertEquals(4 + 10, view.paddingLeft)
        assertEquals(8 + 20, view.paddingTop)
        assertEquals(12 + 30, view.paddingRight)
        assertEquals(16 + 40, view.paddingBottom)
    }

    @Test
    fun smallerSubsequentInsetsShrinkPaddingBackTowardsOriginal() {
        val view = view()
        InsetsHelper.applySystemBarsPadding(view)

        ViewCompat.dispatchApplyWindowInsets(view, insets(Insets.of(10, 20, 30, 40)))
        ViewCompat.dispatchApplyWindowInsets(view, insets(Insets.NONE))

        assertEquals(4, view.paddingLeft)
        assertEquals(8, view.paddingTop)
        assertEquals(12, view.paddingRight)
        assertEquals(16, view.paddingBottom)
    }

    @Test
    fun activityOverloadPadsTheContentView() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val content = activity.findViewById<View>(android.R.id.content)
        val pl = content.paddingLeft
        val pt = content.paddingTop

        InsetsHelper.applySystemBarsPadding(activity)
        ViewCompat.dispatchApplyWindowInsets(content, insets(Insets.of(5, 15, 0, 0)))

        assertEquals(pl + 5, content.paddingLeft)
        assertEquals(pt + 15, content.paddingTop)
    }

    private fun appCompatActivity(): AppCompatActivity {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.PreferencesTheme)
        return controller.setup().get()
    }

    private fun decor(activity: Activity): ViewGroup =
        activity.window.decorView as ViewGroup

    @Test
    fun appCompatActivityGetsAStatusBarScrimSizedToTheTopInset() {
        val activity = appCompatActivity()
        InsetsHelper.applySystemBarsPadding(activity)

        // The scrim is added as the first decor child so it draws behind the action bar.
        val scrim = decor(activity).getChildAt(0)
        ViewCompat.dispatchApplyWindowInsets(scrim, insets(Insets.of(0, 40, 0, 0)))

        assertEquals(40, scrim.layoutParams.height)
    }

    @Test
    fun statusBarScrimHeightIncludesTheDisplayCutout() {
        val activity = appCompatActivity()
        InsetsHelper.applySystemBarsPadding(activity)

        val scrim = decor(activity).getChildAt(0)
        ViewCompat.dispatchApplyWindowInsets(
            scrim, insets(Insets.of(0, 30, 0, 0), Insets.of(0, 50, 0, 0)))

        assertEquals(50, scrim.layoutParams.height)
    }

    @Test
    fun statusBarScrimDoesNotConsumeInsets() {
        // Consuming would stop the content view below the action bar from being padded.
        val activity = appCompatActivity()
        InsetsHelper.applySystemBarsPadding(activity)

        val scrim = decor(activity).getChildAt(0)
        val result = ViewCompat.dispatchApplyWindowInsets(scrim, insets(Insets.of(0, 40, 0, 0)))

        assertFalse(result!!.isConsumed)
    }

    @Test
    fun plainActivityGetsNoStatusBarScrim() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val before = decor(activity).childCount

        InsetsHelper.applySystemBarsPadding(activity)

        assertEquals(before, decor(activity).childCount)
    }
}
