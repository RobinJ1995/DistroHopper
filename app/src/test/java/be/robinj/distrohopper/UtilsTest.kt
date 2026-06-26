package be.robinj.distrohopper

import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class UtilsTest {
    @Test
    fun runOnUiThreadPostsToMainLooperInsteadOfRunningInline() {
        // In LEGACY looper mode the main looper executes posts eagerly unless
        // paused, so pause it to observe the post-then-run sequence.
        ShadowLooper.pauseMainLooper()
        val ran = AtomicBoolean(false)
        Utils.runOnUiThread { ran.set(true) }
        assertFalse(ran.get())
        ShadowLooper.runUiThreadTasks()
        assertTrue(ran.get())
    }

    @Test
    fun runOnUiThreadWorksFromBackgroundThread() {
        val ran = AtomicBoolean(false)
        val thread = Thread { Utils.runOnUiThread { ran.set(true) } }
        thread.start()
        thread.join()
        assertFalse(ran.get())
        ShadowLooper.runUiThreadTasks()
        assertTrue(ran.get())
    }
}
