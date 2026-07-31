package be.robinj.distrohopper.widgets

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.preferences.Preference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The desktop grid's column preference wiring: the adaptive default when unset,
 * the stored count when set (clamped to the device range), and [WidgetGrid.init]
 * applying it to COLS/ROWS. The maths themselves are covered by [WidgetGridTest];
 * these exercise the Context resolvers end-to-end.
 */
@RunWith(RobolectricTestRunner::class)
class WidgetGridColumnsTest {
	private val context: Context = ApplicationProvider.getApplicationContext()

	@Before @After fun resetGrid() {
		WidgetGrid.COLS = 8
		WidgetGrid.ROWS = 8
	}

	private fun setColumns(cols: Int) {
		DependencyContainer.of(this.context).prefs.edit {
			putInt(Preference.DESKTOP_GRID_COLUMNS.getName(), cols)
		}
	}

	@Test fun unsetPreferenceYieldsTheAdaptiveDefault() {
		val (shortEdgeDp, _) = WidgetGrid.stableEdgesDp(this.context)
		assertEquals(WidgetGrid.defaultColumns(shortEdgeDp), WidgetGrid.columns(this.context))
	}

	@Test fun storedColumnsAreRespected() {
		val range = WidgetGrid.columnsRange(this.context)
		assertTrue("range must offer a non-default choice", range.count() > 1)
		val choice = if (WidgetGrid.columns(this.context) == range.last) range.first else range.last

		this.setColumns(choice)
		assertEquals(choice, WidgetGrid.columns(this.context))
	}

	@Test fun storedColumnsAreClampedToTheDeviceRange() {
		val range = WidgetGrid.columnsRange(this.context)

		this.setColumns(99)
		assertEquals(range.last, WidgetGrid.columns(this.context))
		this.setColumns(1)
		assertEquals(range.first, WidgetGrid.columns(this.context))
	}

	@Test fun initAppliesTheStoredColumns() {
		val range = WidgetGrid.columnsRange(this.context)
		this.setColumns(range.last)

		WidgetGrid.init(this.context)

		assertEquals(range.last, WidgetGrid.COLS)
		val (shortEdgeDp, longEdgeDp) = WidgetGrid.stableEdgesDp(this.context)
		assertEquals(WidgetGrid.calculate(shortEdgeDp, longEdgeDp, range.last).second, WidgetGrid.ROWS)
	}

	@Test fun dimensionsForMatchesInitGeometry() {
		val cols = WidgetGrid.columns(this.context)
		WidgetGrid.init(this.context)
		assertEquals(WidgetGrid.COLS to WidgetGrid.ROWS, WidgetGrid.dimensionsFor(this.context, cols))
	}
}
