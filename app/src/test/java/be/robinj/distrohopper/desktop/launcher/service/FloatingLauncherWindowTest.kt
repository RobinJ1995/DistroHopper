package be.robinj.distrohopper.desktop.launcher.service

import android.app.Application
import android.os.PowerManager
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.LauncherServiceSensitivity
import be.robinj.distrohopper.preferences.LauncherServiceZone
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import be.robinj.distrohopper.theme.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowPowerManager

/**
 * The floating launcher's window: the shape of the grabbable strip, the bar it
 * builds from the theme and the pins, and the pull that brings it out.
 *
 * Battery-saver mode is on throughout so settling is instantaneous — the same
 * path the rest of the app takes there, and the one that can be asserted on
 * without driving an animator.
 */
@RunWith(RobolectricTestRunner::class)
class FloatingLauncherWindowTest {
	private lateinit var application: Application

	private val screen
		get() = this.application.getSystemService(WindowManager::class.java)
			.currentWindowMetrics.bounds

	private val items = listOf(
		FloatingLauncherItem("be.robinj.one", "be.robinj.one.Main", "One", null, -1L),
		FloatingLauncherItem("be.robinj.two", "be.robinj.two.Main", "Two", null, -1L))

	@Before fun setUp() {
		this.application = ApplicationProvider.getApplicationContext()
		Preferences.getSharedPreferences(this.application).edit().clear().commit()
		Shadow.extract<ShadowPowerManager>(
			this.application.getSystemService(PowerManager::class.java)).setIsPowerSaveMode(true)
	}

	private fun prefs(vararg pairs: Pair<Preference, String>) {
		val editor = Preferences.getSharedPreferences(this.application).edit()
		for ((preference, value) in pairs) {
			editor.putString(preference.getName(), value)
		}
		editor.commit()
	}

	private fun edge(edge: Location) {
		Preferences.getSharedPreferences(this.application).edit()
			.putInt(Preference.LAUNCHER_EDGE.getName(), edge.n)
			.commit()
	}

	private fun window(): FloatingLauncherWindow =
		FloatingLauncherWindow(this.application).also { it.show(this.items) }

	private fun touch(window: FloatingLauncherWindow, action: Int, x: Float, y: Float) {
		val now = SystemClock.uptimeMillis()
		val event = MotionEvent.obtain(now, now, action, x, y, 0)
		window.overlay.dispatchTouchEvent(event)
		event.recycle()
	}

	/** A pull from the left edge, out past the sensitivity's committing distance. */
	private fun pullOut(window: FloatingLauncherWindow, distance: Float = 500F) {
		this.touch(window, MotionEvent.ACTION_DOWN, 0F, 400F)
		this.touch(window, MotionEvent.ACTION_MOVE, distance, 400F)
		this.touch(window, MotionEvent.ACTION_UP, distance, 400F)
	}

	@Test fun theBarHoldsTheMenuButtonAndEveryPinnedApp() {
		val bar = this.window().dock

		// The menu button, then the scrolling strip of app tiles //
		assertEquals(2, bar.childCount)
		val strip = (bar.getChildAt(1) as android.view.ViewGroup).getChildAt(0) as LinearLayout
		assertEquals(this.items.size, strip.childCount)
	}

	@Test fun itStartsFullyHiddenOutsideTheEdgeItIsDockedOn() {
		val window = this.window()

		assertFalse(window.isOpen)
		assertTrue("the bar is not tucked away", window.dock.translationX < 0F)
	}

	@Test fun theStripIsAsWideAsTheSensitivityAndAsLongAsTheZone() {
		this.prefs(
			Preference.LAUNCHER_SERVICE_ZONE to LauncherServiceZone.END.value,
			Preference.LAUNCHER_SERVICE_SENSITIVITY to LauncherServiceSensitivity.LOW.value)
		this.edge(Location.LEFT)

		val params = this.window().overlay.layoutParams as WindowManager.LayoutParams
		val density = this.application.resources.displayMetrics.density

		assertEquals(LauncherServiceSensitivity.LOW.hotZonePx(density), params.width)
		assertEquals(LauncherServiceZone.END.lengthPx(this.screen.height()), params.height)
		assertEquals(LauncherServiceZone.END.offsetPx(this.screen.height()), params.y)
	}

	@Test fun aPullPastTheCommittingDistanceOpensTheBar() {
		val window = this.window()

		this.pullOut(window)

		assertTrue(window.isOpen)
		assertEquals(0F, window.dock.translationX, 0F)
	}

	@Test fun aStrayTouchThatBarelyMovesLeavesItClosed() {
		val window = this.window()

		this.touch(window, MotionEvent.ACTION_DOWN, 0F, 400F)
		this.touch(window, MotionEvent.ACTION_MOVE, 3F, 400F)
		this.touch(window, MotionEvent.ACTION_UP, 3F, 400F)

		assertFalse(window.isOpen)
		assertTrue(window.dock.translationX < 0F)
	}

	@Test fun aCancelledPullNeverOpensIt() {
		val window = this.window()

		this.touch(window, MotionEvent.ACTION_DOWN, 0F, 400F)
		this.touch(window, MotionEvent.ACTION_MOVE, 500F, 400F)
		this.touch(window, MotionEvent.ACTION_CANCEL, 500F, 400F)

		assertFalse(window.isOpen)
	}

	@Test fun theWindowFillsTheScreenWhileOpenAndShrinksBackWhenClosed() {
		val window = this.window()
		this.pullOut(window)

		val open = window.overlay.layoutParams as WindowManager.LayoutParams
		assertEquals(WindowManager.LayoutParams.MATCH_PARENT, open.width)
		assertEquals(WindowManager.LayoutParams.MATCH_PARENT, open.height)

		// Tapping outside — the scrim — puts it away again //
		val scrim = (window.overlay as android.view.ViewGroup).getChildAt(0)
		assertTrue(scrim.isClickable)
		scrim.performClick()

		assertFalse(window.isOpen)
		val closed = window.overlay.layoutParams as WindowManager.LayoutParams
		assertTrue(closed.width < this.screen.width())
	}

	/** Home coming to the front takes it away mid-pull, and it comes back closed. */
	@Test fun hidingAnOpenBarAndShowingItAgainStartsOverClosed() {
		val window = this.window()
		this.pullOut(window)

		window.hide()
		assertFalse(window.isOpen)

		window.show(this.items)

		assertFalse(window.isOpen)
		assertTrue(window.dock.translationX < 0F)
		val params = window.overlay.layoutParams as WindowManager.LayoutParams
		assertTrue(params.width < this.screen.width())
	}

	@Test fun aTopEdgeLauncherIsAHorizontalBarPulledDownFromTheTop() {
		this.edge(Location.TOP)

		val window = this.window()
		assertEquals(LinearLayout.HORIZONTAL, window.dock.orientation)
		assertTrue("the bar is not tucked away", window.dock.translationY < 0F)
		assertEquals(0F, window.dock.translationX, 0F)

		this.touch(window, MotionEvent.ACTION_DOWN, 400F, 0F)
		this.touch(window, MotionEvent.ACTION_MOVE, 400F, 500F)
		this.touch(window, MotionEvent.ACTION_UP, 400F, 500F)

		assertTrue(window.isOpen)
		assertEquals(0F, window.dock.translationY, 0F)
	}

	@Test fun aRightEdgeLauncherIsPulledInFromTheRight() {
		this.edge(Location.RIGHT)

		val window = this.window()
		assertTrue("the bar is not tucked away", window.dock.translationX > 0F)

		val right = this.screen.width().toFloat()
		this.touch(window, MotionEvent.ACTION_DOWN, right, 400F)
		this.touch(window, MotionEvent.ACTION_MOVE, right - 500F, 400F)
		this.touch(window, MotionEvent.ACTION_UP, right - 500F, 400F)

		assertTrue(window.isOpen)
		assertEquals(0F, window.dock.translationX, 0F)
	}

	@Test fun theTilesAreLaidOutForTheEdgeTheLauncherIsDockedOn() {
		this.edge(Location.BOTTOM)

		val window = this.window()

		assertEquals(LinearLayout.HORIZONTAL, window.dock.orientation)
		assertEquals(View.VISIBLE, window.dock.visibility)
	}
}
