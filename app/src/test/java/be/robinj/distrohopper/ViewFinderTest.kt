package be.robinj.distrohopper

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ViewFinderTest {
    private val rootChildId = View.generateViewId()
    private val nestedChildId = View.generateViewId()
    private val unknownId = View.generateViewId()

    private fun buildActivity(): Pair<Activity, View> {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val nested = LinearLayout(activity)
        val nestedChild = View(activity).apply { id = nestedChildId }
        nested.addView(nestedChild)
        val content = FrameLayout(activity).apply {
            addView(View(activity).apply { id = rootChildId })
            addView(nested)
        }
        activity.setContentView(content)
        return activity to nestedChild
    }

    @Test
    fun getResolvesViewFromDecorRoot() {
        val (activity, _) = buildActivity()
        val finder = ViewFinder(activity)
        assertSame(activity.findViewById<View>(rootChildId), finder.get<View>(rootChildId))
    }

    @Test
    fun getCachesViewEvenAfterRemovalFromHierarchy() {
        val (activity, _) = buildActivity()
        val finder = ViewFinder(activity)
        val view = finder.get<View>(rootChildId)
        (view.parent as FrameLayout).removeView(view)
        assertNull(activity.findViewById(rootChildId))
        assertSame(view, finder.get<View>(rootChildId))
    }

    @Test
    fun unknownIdReturnsNullAndCachesTheMiss() {
        val (activity, _) = buildActivity()
        val finder = ViewFinder(activity)
        assertNull(finder.get<View>(unknownId))
        // The null result is cached: a view added later with this id is not found.
        (activity.findViewById<View>(rootChildId).parent as FrameLayout)
            .addView(View(activity).apply { id = unknownId })
        assertNull(finder.get<View>(unknownId))
    }

    @Test
    fun getWithParentViewSearchesFromRootNotTheGivenParent() {
        // Pins current behaviour: get(parentView, id) ignores parentView and
        // resolves against the decor root (ViewFinder.java:27).
        val (activity, nestedChild) = buildActivity()
        val finder = ViewFinder(activity)
        val unrelatedParent = LinearLayout(activity)
        assertSame(nestedChild, finder.get<View>(unrelatedParent, nestedChildId))
    }
}
