package be.robinj.distrohopper.widgets

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.preferences.Preference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The desktop grid's persisted presets and size: snapshotted on first launch
 * (middle preset = the device default), then read back verbatim forever — the
 * stored values, not any live-screen calculation, are what [WidgetGrid.init]
 * applies. The generation maths are covered by [WidgetGridTest]; these
 * exercise the persistence contract end-to-end.
 */
@RunWith(RobolectricTestRunner::class)
class WidgetGridColumnsTest {
	private val context: Context = ApplicationProvider.getApplicationContext()

	@Before @After fun resetGrid() {
		WidgetGrid.COLS = 8
		WidgetGrid.ROWS = 8
	}

	private fun stored(pref: Preference): String? =
		be.robinj.distrohopper.preferences.Preferences.getSharedPreferences(this.context)
			.getString(pref.getName(), null)

	@Test fun firstLaunchSnapshotsPresetsAndDefaultSize() {
		val presets = WidgetGrid.presets(this.context)
		val size = WidgetGrid.size(this.context)

		assertEquals(WidgetGrid.PRESET_COUNT, presets.size)
		assertEquals("middle preset is the default selection",
			presets[WidgetGrid.DEFAULT_PRESET], size)
		// Both snapshots are persisted, not just computed.
		assertNotNull(this.stored(Preference.DESKTOP_GRID_PRESETS))
		assertEquals(WidgetGrid.formatSize(size), this.stored(Preference.DESKTOP_GRID_SIZE))
	}

	/**
	 * The core stability contract: init applies the STORED size verbatim —
	 * whatever the current screen or today's formula would say. A future
	 * calculation change (or any density/orientation game) can therefore never
	 * move a persisted grid.
	 */
	@Test fun initAppliesTheStoredSizeVerbatim() {
		DependencyContainer.of(this.context).prefs.edit {
			putString(Preference.DESKTOP_GRID_SIZE.getName(), "3x9")
		}

		WidgetGrid.init(this.context)

		assertEquals(3, WidgetGrid.COLS)
		assertEquals(9, WidgetGrid.ROWS)
	}

	/** Stored presets are read back verbatim too — never regenerated. */
	@Test fun storedPresetsAreNeverRegenerated() {
		DependencyContainer.of(this.context).prefs.edit {
			putString(Preference.DESKTOP_GRID_PRESETS.getName(), "2x4,3x6,4x8,5x10,6x12")
		}

		assertEquals(
			listOf(2 to 4, 3 to 6, 4 to 8, 5 to 10, 6 to 12),
			WidgetGrid.presets(this.context))
	}

	@Test fun selectedPresetMatchesTheStoredSize() {
		DependencyContainer.of(this.context).prefs.edit {
			putString(Preference.DESKTOP_GRID_PRESETS.getName(), "2x4,3x6,4x8,5x10,6x12")
			putString(Preference.DESKTOP_GRID_SIZE.getName(), "5x10")
		}

		assertEquals(3, WidgetGrid.selectedPreset(this.context))
	}

	@Test fun sizeOutsideTheLadderFallsBackToTheMiddleIndex() {
		DependencyContainer.of(this.context).prefs.edit {
			putString(Preference.DESKTOP_GRID_PRESETS.getName(), "2x4,3x6,4x8,5x10,6x12")
			putString(Preference.DESKTOP_GRID_SIZE.getName(), "9x9")
		}

		// The grid itself keeps the stored size; only the slider position falls back.
		assertEquals(WidgetGrid.DEFAULT_PRESET, WidgetGrid.selectedPreset(this.context))
		WidgetGrid.init(this.context)
		assertEquals(9, WidgetGrid.COLS)
	}

	@Test fun malformedStoredValuesAreReplacedByAFreshSnapshot() {
		DependencyContainer.of(this.context).prefs.edit {
			putString(Preference.DESKTOP_GRID_PRESETS.getName(), "garbage")
			putString(Preference.DESKTOP_GRID_SIZE.getName(), "alsoxgarbage")
		}

		val presets = WidgetGrid.presets(this.context)
		assertEquals(WidgetGrid.PRESET_COUNT, presets.size)
		assertEquals(presets[WidgetGrid.DEFAULT_PRESET], WidgetGrid.size(this.context))
	}

	@Test fun setSizePersistsAndInitApplies() {
		val presets = WidgetGrid.presets(this.context)
		val finest = presets.last()

		WidgetGrid.setSize(this.context, finest)
		WidgetGrid.init(this.context)

		assertEquals(finest, WidgetGrid.COLS to WidgetGrid.ROWS)
		assertEquals(WidgetGrid.PRESET_COUNT - 1, WidgetGrid.selectedPreset(this.context))
	}
}
