package be.robinj.distrohopper.thirdparty

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.util.AttributeSet
import android.widget.FrameLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

/**
 * The spin's lifecycle. A frame left queued while the wheel is off screen runs
 * unpaced and keeps the view's activity alive, so detaching has to drop it —
 * but the launcher re-parents the spinner's wrapper, so dropping the frame must
 * not end the spin, or the startup wheel comes back blank.
 */
@RunWith(RobolectricTestRunner::class)
class ProgressWheelSpinTest {
	/** A wheel that counts its repaints, so spin frames can be observed. */
	private class CountingWheel(context: Context, attrs: AttributeSet?) :
			ProgressWheel(context, attrs) {
		var draws = 0

		override fun invalidate() {
			super.invalidate()
			this.draws++
		}
	}

	private lateinit var looper: ShadowLooper
	private lateinit var container: FrameLayout
	private lateinit var wheel: CountingWheel

	@Before fun setUp() {
		this.looper = shadowOf(Looper.getMainLooper())

		val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
		this.container = FrameLayout(activity)
		activity.setContentView(this.container)
		this.wheel = CountingWheel(activity, Robolectric.buildAttributeSet().build())
		this.container.addView(this.wheel)
		this.looper.idle()

		// Paced so the spin can be stepped: at the shipped delay of 0 each frame is
		// re-posted due at the same instant, and pumping the looper never returns.
		// These tests are about the lifecycle, not the pacing. //
		this.wheel.setDelayMillis(FRAME_MS)
	}

	/**
	 * Frames drawn over [steps] dispatches. Steps task by task rather than idling a
	 * window, and a drained queue makes runToNextTask a no-op, so this returns
	 * whether or not the wheel is ticking.
	 */
	private fun framesOver(steps: Int): Int {
		val before = this.wheel.draws
		repeat(steps) { this.looper.runToNextTask() }

		return this.wheel.draws - before
	}

	@Test fun `an attached wheel spins`() {
		this.wheel.spin()

		assertTrue("a spinning wheel on screen should draw", this.framesOver(5) > 0)
	}

	@Test fun `detaching stops the frames`() {
		this.wheel.spin()
		this.framesOver(3)

		this.container.removeView(this.wheel)

		// The queued frame is what kept the activity alive and ran unpaced. //
		assertEquals("a detached wheel must not go on drawing", 0, this.framesOver(20))
	}

	@Test fun `re-attaching resumes the spin`() {
		this.wheel.spin()
		this.framesOver(3)
		this.container.removeView(this.wheel)
		assertEquals(0, this.framesOver(5))

		// The launcher re-parents the spinner's wrapper mid-startup; stopping the
		// spin outright on detach left the wheel blank for the rest of the load. //
		this.container.addView(this.wheel)

		assertTrue("a wheel still in spin mode resumes once it is back on screen",
			this.framesOver(5) > 0)
	}

	@Test fun `stopSpinning ends it for good`() {
		this.wheel.spin()
		this.framesOver(3)

		this.wheel.stopSpinning()

		assertFalse(this.wheel.isSpinning)
		assertEquals(0, this.framesOver(20))
	}

	@Test fun `a wheel stopped before detaching does not resume`() {
		this.wheel.spin()
		this.wheel.stopSpinning()

		this.container.removeView(this.wheel)
		this.container.addView(this.wheel)

		assertEquals("only a wheel still in spin mode resumes", 0, this.framesOver(20))
	}

	companion object {
		private const val FRAME_MS = 16
	}

	@Test fun `reporting progress ends the spin`() {
		this.wheel.spin()
		this.framesOver(3)

		// What StartupLoader does on its first progress callback. //
		this.wheel.setProgress(120)

		assertFalse(this.wheel.isSpinning)

		// The frame already queued runs once more and must then retire. //
		this.framesOver(3)
		assertEquals("the spin must not carry on", 0, this.framesOver(20))
	}
}
