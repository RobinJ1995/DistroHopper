package be.robinj.distrohopper.widgets

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.preferences.Preference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

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

	// ---- configuration changes ----------------------------------------------
	// True cross-config checks: snapshot under one configuration, change it
	// in-process (RuntimeEnvironment.setQualifiers updates the same application,
	// so SharedPreferences persist — like a real device would), re-init, and
	// assert the grid, ladder and selection did not move.

	private fun snapshotThenReinitUnder(qualifiers: String): Triple<Pair<Int, Int>, List<Pair<Int, Int>>, Int> {
		WidgetGrid.init(this.context)
		val grid = WidgetGrid.COLS to WidgetGrid.ROWS
		val presets = WidgetGrid.presets(this.context)
		val selected = WidgetGrid.selectedPreset(this.context)

		RuntimeEnvironment.setQualifiers(qualifiers)
		WidgetGrid.init(this.context)

		assertEquals("grid must not move", grid, WidgetGrid.COLS to WidgetGrid.ROWS)
		assertEquals("ladder must not regenerate", presets, WidgetGrid.presets(this.context))
		assertEquals("selection must not move", selected, WidgetGrid.selectedPreset(this.context))
		return Triple(grid, presets, selected)
	}

	@Test @Config(qualifiers = "w360dp-h800dp-xxhdpi")
	fun gridSurvivesOrientationChange() {
		this.snapshotThenReinitUnder("+land")
	}

	@Test @Config(qualifiers = "w360dp-h800dp-480dpi")
	fun gridSurvivesDisplaySizeChange() {
		// The same 1080×2400 px panel at a smaller "Display size" setting: dp
		// grow, density drops. Precondition: a fresh snapshot HERE would yield a
		// different ladder — proving the assertion discriminates.
		WidgetGrid.init(this.context)
		assertNotEquals("precondition: the new config must disagree with the stored ladder",
			WidgetGrid.presets(this.context), WidgetGrid.generatePresets(432, 960))

		this.snapshotThenReinitUnder("w432dp-h960dp-400dpi")
	}

	@Test @Config(qualifiers = "w360dp-h800dp-xxhdpi")
	fun gridSurvivesScreenSizeChange() {
		// A foldable unfolding to a tablet-sized inner display: everything —
		// width, height, smallest width, density — changes at once.
		WidgetGrid.init(this.context)
		assertNotEquals("precondition: the new config must disagree with the stored ladder",
			WidgetGrid.presets(this.context), WidgetGrid.generatePresets(800, 1280))

		this.snapshotThenReinitUnder("w800dp-h1280dp-xhdpi")
	}

	@Test @Config(qualifiers = "w360dp-h800dp-xxhdpi")
	fun nonDefaultSelectionSurvivesConfigurationChanges() {
		// The stability contract must hold for a user-chosen preset too, across
		// a rotation and a display-size change back to back.
		WidgetGrid.setSize(this.context, WidgetGrid.presets(this.context).first())

		this.snapshotThenReinitUnder("+land")
		this.snapshotThenReinitUnder("w432dp-h960dp-400dpi")

		assertEquals(0, WidgetGrid.selectedPreset(this.context))
	}
}
