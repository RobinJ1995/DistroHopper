package be.robinj.distrohopper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class ExceptionHandlerTest {
    private lateinit var context: Context

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ShadowAlertDialog.reset()
    }

    @Test fun showFromMainThreadDisplaysDialog() {
        ExceptionHandler(IllegalStateException("noot")).show(context)

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertNotNull(dialog)
        assertTrue(dialog.isShowing)
    }

    @Test fun showFromBackgroundThreadDisplaysDialog() {
        val background = Thread { ExceptionHandler(IllegalStateException("noot")).show(context) }
        background.start()
        background.join(5000)

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertNotNull(
            "the error dialog must also be shown when show() is called from a background thread",
            ShadowAlertDialog.getLatestAlertDialog(),
        )
    }

    @Test fun dialogMessageDescribesTheException() {
        ExceptionHandler(IllegalStateException("rush b no stop")).show(context)

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        val message = Shadows.shadowOf(ShadowAlertDialog.getLatestAlertDialog()).message.toString()
        assertTrue(message.contains("IllegalStateException"))
        assertTrue(message.contains("rush b no stop"))
    }

    @Test fun showWithNullContextDoesNotThrow() {
        ExceptionHandler(IllegalStateException("noot")).show(null)
    }
}
