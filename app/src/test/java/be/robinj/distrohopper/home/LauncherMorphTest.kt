package be.robinj.distrohopper.home

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.App
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The pure slot maths of the per-desktop launcher morph. */
@RunWith(RobolectricTestRunner::class)
class LauncherMorphTest {
	private val context: Context = ApplicationProvider.getApplicationContext()

	private fun app(packageName: String): App =
		App(this.context, null,
			ActivityTestSupport.resolveInfo(packageName, packageName.uppercase(), packageName))

	@Test fun unionKeepsFromOrderThenAppendsToOnlyApps() {
		val a = this.app("a")
		val b = this.app("b")
		val c = this.app("c")

		assertEquals(listOf(a, b, c), LauncherMorph.union(listOf(a, b), listOf(a, c)))
	}

	@Test fun sharedAppsSlideFromTheirOldSlotToTheirNewOne() {
		val a = this.app("a")
		val b = this.app("b")
		val from = listOf(a, b)
		val to = listOf(b, a) // swapped //

		fun positionOf(app: App, fraction: Float) =
			LauncherMorph.slots(from, to, fraction).first { it.app == app }.position

		assertEquals(0F, positionOf(a, 0F), 0.001F) // a: slot 0 -> 1 //
		assertEquals(0.5F, positionOf(a, 0.5F), 0.001F)
		assertEquals(1F, positionOf(a, 1F), 0.001F)
		assertEquals(1F, positionOf(b, 0F), 0.001F) // b: slot 1 -> 0 //
		assertEquals(0F, positionOf(b, 1F), 0.001F)
	}

	@Test fun aLeavingAppHoldsItsSlotAndFadesAndShrinksOut() {
		val a = this.app("a")
		val b = this.app("b")

		val slot = LauncherMorph.slots(listOf(a, b), listOf(a), 1F).first { it.app == b }

		assertEquals(1F, slot.position, 0.001F)
		assertEquals(0F, slot.alpha, 0.001F)
		assertEquals(LauncherMorph.EXIT_SCALE, slot.scale, 0.001F)
	}

	@Test fun anArrivingAppHoldsItsSlotAndFadesAndGrowsIn() {
		val a = this.app("a")
		val b = this.app("b")

		val closed = LauncherMorph.slots(listOf(a), listOf(a, b), 0F).first { it.app == b }
		assertEquals(1F, closed.position, 0.001F)
		assertEquals(0F, closed.alpha, 0.001F)
		assertEquals(LauncherMorph.EXIT_SCALE, closed.scale, 0.001F)

		val open = LauncherMorph.slots(listOf(a), listOf(a, b), 1F).first { it.app == b }
		assertEquals(1F, open.alpha, 0.001F)
		assertEquals(1F, open.scale, 0.001F)
	}
}
