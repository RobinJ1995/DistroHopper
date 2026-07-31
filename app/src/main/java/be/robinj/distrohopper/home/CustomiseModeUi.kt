package be.robinj.distrohopper.home

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.ViewFinder
import be.robinj.distrohopper.desktop.dash.DashGrid
import be.robinj.distrohopper.widgets.WidgetGrid
import be.robinj.distrohopper.desktop.launcher.LauncherIconGrid
import be.robinj.distrohopper.preferences.BfbLocation
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import be.robinj.distrohopper.theme.Location
import be.robinj.distrohopper.theme.Theme
import kotlin.math.max
import kotlin.math.min

/**
 * The customise mode UI inside the dash: seekbars for the launcher/dash icon
 * sizes and segmented rows for the launcher/panel edges and the menu button.
 * Extracted from HomeActivity's onCreate(). Edge and menu-button changes
 * relaunch the activity in customise mode via [relaunchInCustomiseMode].
 */
class CustomiseModeUi(
	private val activity: Activity,
	private val viewFinder: ViewFinder,
	private val theme: Theme,
	private val relaunchInCustomiseMode: Runnable,
) {
	private var dashGridHint: TextView? = null

	fun show() {
		val res = this.activity.resources
		val prefs = Preferences.getSharedPreferences(this.activity)
		val prefsEdit = prefs.edit()

		val llDashContent = this.viewFinder.get<LinearLayout>(R.id.llDashContent)
		val llDashCustomise = this.viewFinder.get<LinearLayout>(R.id.llDashCustomise)

		llDashContent.visibility = View.GONE
		llDashCustomise.visibility = View.VISIBLE

		// Closing the dash is what leaves customise mode (HomeActivity.closeDash
		// relaunches out of it); swipe-to-close is disabled here, so without this
		// the panel's close button is the only way out and nothing says so. //
		this.viewFinder.get<View>(llDashCustomise, R.id.tvCustomiseDone).setOnClickListener {
			(this.activity as? HomeActivity)?.closeDash()
		}

		// Pinned Icon Size // Five presets (Huge…Tiny); the actual pixel size is
		// computed at runtime by LauncherIconGrid so the chosen number of slots
		// fits the launcher on the screen's shortest edge. Writing the preference
		// is enough: HomeStateBinder observes it and re-inits the launcher icons //
		val launcherIconLabels = res.getStringArray(R.array.launcher_icon_presets)
		val launcherIconHint = this.viewFinder.get<TextView>(R.id.tvCustomiseLauncherIconHint)
		val sbCustomiseLauncherIconSize = this.viewFinder.get<SeekBar>(R.id.sbCustomiseLauncherIconSize)
		sbCustomiseLauncherIconSize.max = LauncherIconGrid.PRESET_COUNT - 1
		sbCustomiseLauncherIconSize.progress = LauncherIconGrid.preset(this.activity)
		this.updateLauncherIconHint(launcherIconHint, launcherIconLabels, sbCustomiseLauncherIconSize.progress)
		sbCustomiseLauncherIconSize.setOnSeekBarChangeListener(
			object : SeekBar.OnSeekBarChangeListener {
				override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
					this@CustomiseModeUi.updateLauncherIconHint(launcherIconHint, launcherIconLabels, i)
					if (b) this.update(i)  // see onStopTrackingTouch: ignore non-user (state-restore) changes
				}

				override fun onStartTrackingTouch(seekBar: SeekBar) {}

				override fun onStopTrackingTouch(seekBar: SeekBar) {
					this.update(seekBar.progress)
				}

				private fun update(value: Int) {
					prefsEdit.putInt(Preference.LAUNCHER_ICON_PRESET.getName(), value)
					prefsEdit.commit()
				}
			})

		// Dash Grid Size // The user picks how many icons span the short screen
		// edge; icon size and the visible rows derive from that (see DashGrid).
		// Writing the preference is enough: HomeStateBinder re-applies it live //
		val range = DashGrid.columnsRange(this.activity)
		val dashGridHint = this.viewFinder.get<TextView>(R.id.tvCustomiseDashGridHint)
		this.dashGridHint = dashGridHint
		val sbCustomiseDashColumns = this.viewFinder.get<SeekBar>(R.id.sbCustomiseDashColumns)
		sbCustomiseDashColumns.min = range.first
		sbCustomiseDashColumns.max = range.last
		sbCustomiseDashColumns.progress = DashGrid.columns(this.activity)
		this.updateDashGridHint(dashGridHint)
		sbCustomiseDashColumns.setOnSeekBarChangeListener(
			object : SeekBar.OnSeekBarChangeListener {
				override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
					if (b) this.update(i)  // see onStopTrackingTouch: ignore non-user (state-restore) changes
				}

				override fun onStartTrackingTouch(seekBar: SeekBar) {}

				override fun onStopTrackingTouch(seekBar: SeekBar) {
					this.update(seekBar.progress)
				}

				private fun update(value: Int) {
					prefsEdit.putInt(Preference.DASH_GRID_COLUMNS.getName(), value)
					prefsEdit.commit()
					this@CustomiseModeUi.updateDashGridHint(dashGridHint)
				}
			})

		// Desktop Grid Size // The user picks how many cells span the short screen
		// edge; the rows derive from that (see WidgetGrid). Unlike the dash, the
		// desktop persists absolute positions against the grid, so the change is
		// committed on release and applied by relaunching home (like the edge
		// spinners): the stored layout reloads against the new grid //
		val desktopRange = WidgetGrid.columnsRange(this.activity)
		val desktopGridHint = this.viewFinder.get<TextView>(R.id.tvCustomiseDesktopGridHint)
		val sbCustomiseDesktopColumns = this.viewFinder.get<SeekBar>(R.id.sbCustomiseDesktopColumns)
		sbCustomiseDesktopColumns.min = desktopRange.first
		sbCustomiseDesktopColumns.max = desktopRange.last
		sbCustomiseDesktopColumns.progress = WidgetGrid.columns(this.activity)
		this.updateDesktopGridHint(desktopGridHint, sbCustomiseDesktopColumns.progress)
		sbCustomiseDesktopColumns.setOnSeekBarChangeListener(
			object : SeekBar.OnSeekBarChangeListener {
				override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
					this@CustomiseModeUi.updateDesktopGridHint(desktopGridHint, i)
				}

				override fun onStartTrackingTouch(seekBar: SeekBar) {}

				override fun onStopTrackingTouch(seekBar: SeekBar) {
					if (seekBar.progress == WidgetGrid.columns(this@CustomiseModeUi.activity)) {
						return
					}

					prefsEdit.putInt(Preference.DESKTOP_GRID_COLUMNS.getName(), seekBar.progress)
					prefsEdit.commit()
					this@CustomiseModeUi.relaunchInCustomiseMode.run()
				}
			})

		// Launcher Edge //
		val edgeNames = res.getStringArray(R.array.edges)

		val llCustomiseLauncherEdge =
			this.viewFinder.get<LinearLayout>(R.id.llCustomiseLauncherEdgeSegments)
		val supportedLauncherEdges = res.getIntArray(this.theme.launcher_location_supported)
		// res.getInteger: launcher_location is a resource id, not an edge — passing
		// it as the default left the stored-nothing case matching no edge at all. //
		val currentLauncherEdge = prefs.getInt(Preference.LAUNCHER_EDGE.getName(),
			res.getInteger(this.theme.launcher_location))
		this.initEdgeSegments(llCustomiseLauncherEdge, edgeNames, supportedLauncherEdges,
			currentLauncherEdge, Preference.LAUNCHER_EDGE)

		// Panel Edge //
		val llCustomisePanelEdgeSegments =
			this.viewFinder.get<LinearLayout>(R.id.llCustomisePanelEdgeSegments)
		/*
		 * When the panel's edge is dictated by the launcher's, only the edge
		 * the current launcher position allows is offered (plus None).
		 */
		val supportedPanelEdges = if (res.getIntArray(this.theme.panel_location_supported)
				.contains(Location.BOTTOM.n)) {
			intArrayOf(
				this.complementaryPanelEdge(prefs.getInt(Preference.LAUNCHER_EDGE.getName(),
					res.getInteger(this.theme.launcher_location))),
				Location.NONE.n)
		} else {
			res.getIntArray(this.theme.panel_location_supported)
		}
		if (supportedPanelEdges.size > 1) {
			val currentPanelEdge = prefs.getInt(Preference.PANEL_EDGE.getName(),
				res.getInteger(this.theme.panel_location))
			this.initEdgeSegments(llCustomisePanelEdgeSegments, edgeNames, supportedPanelEdges,
				currentPanelEdge, Preference.PANEL_EDGE)
		} else {
			this.viewFinder.get<View>(llDashCustomise, R.id.llCustomisePanelEdge).visibility = View.GONE
		}

		// Menu button (BFB) // Only themes that offer more than one BFB position
		// show the setting; the choice is applied by re-running the theme on relaunch.
		val llCustomiseMenuButton =
			this.viewFinder.get<View>(llDashCustomise, R.id.llCustomiseMenuButton)
		if (this.theme.launcherBfbToggleable(res)) {
			llCustomiseMenuButton.visibility = View.VISIBLE
			this.initMenuButtonSegments(
				this.viewFinder.get(R.id.llCustomiseMenuButtonSegments))
		} else {
			llCustomiseMenuButton.visibility = View.GONE
		}
	}

	/*
	 * The menu-button (BFB) position, on themes that offer more than one. The
	 * options come straight from the theme's launcher_bfb_location_supported
	 * array (mapped to none/start/end sides), in the order it lists them — so a
	 * theme only ever exposes the positions it declares (Pantheon/COSMIC: Start
	 * or Hide; GNOME: Start, End or Hide), and every array ends with
	 * position_none, hiding being the absence of a position rather than one of
	 * them. The choice is stored as a named string ([BfbLocation]) and applied
	 * on relaunch.
	 */
	private fun initMenuButtonSegments(segments: LinearLayout) {
		val res = this.activity.resources
		val prefs = Preferences.getSharedPreferences(this.activity)

		val options = res.getIntArray(this.theme.launcher_bfb_location_supported)
			.map { this.theme.bfbSide(Location.of(it)) }
			.distinct()

		val stored = prefs.getString(Preference.LAUNCHER_BFB_LOCATION.getName(), null)
		val current = if (stored == null) this.theme.launcherBfbDefaultChoice(res)
			else BfbLocation.of(stored)

		val labels = options.map { this.activity.getString(this.menuButtonLabel(it)) }
		this.initSegments(segments, labels, options.indexOf(current)) { i ->
			Preferences.getSharedPreferences(this.activity).edit()
				.putString(Preference.LAUNCHER_BFB_LOCATION.getName(), options[i].value).commit()
			this.relaunchInCustomiseMode.run()
		}
	}

	private fun menuButtonLabel(location: BfbLocation): Int = when (location) {
		BfbLocation.NONE -> R.string.customise_menu_button_hide
		BfbLocation.START -> R.string.customise_menu_button_start
		BfbLocation.END -> R.string.customise_menu_button_end
	}

	/**
	 * Fills [segments] with one tappable option each, [selectedIndex] filled in.
	 *
	 * These settings are short lists of positions, which read better laid out
	 * side by side than folded into a dropdown — and unlike a Spinner, nothing
	 * fires for the selection the row starts on: [onSelected] runs only on a
	 * real change, so the callers need no "did it actually change" guard.
	 */
	private fun initSegments(
		segments: LinearLayout,
		labels: List<String>,
		selectedIndex: Int,
		onSelected: (Int) -> Unit,
	) {
		val inflater = LayoutInflater.from(this.activity)

		segments.removeAllViews()
		labels.forEachIndexed { i, label ->
			val segment = inflater.inflate(R.layout.widget_customise_segment, segments, false)
				as TextView

			segment.text = label
			segment.isSelected = i == selectedIndex
			segment.setOnClickListener {
				if (! segment.isSelected) {
					for (j in 0 until segments.childCount) {
						segments.getChildAt(j).isSelected = false
					}
					segment.isSelected = true

					onSelected(i)
				}
			}

			segments.addView(segment)
		}
	}

	/**
	 * Updates the pinned-icon preset hint: the preset's label and how many icons it shows.
	 *
	 * The count is the one for the launcher's actual edge, not the short-edge slot count the
	 * size is derived from — on a side launcher those differ, and only the former is a number
	 * the user can count off the screen. See [LauncherIconGrid.visibleCountForPreset].
	 */
	private fun updateLauncherIconHint(hint: TextView, labels: Array<String>, presetIndex: Int) {
		val label = labels.getOrElse(presetIndex.coerceIn(0, labels.size - 1)) { "" }
		val count = LauncherIconGrid.visibleCountForPreset(this.activity, presetIndex)
		hint.text = this.activity.getString(R.string.launcher_icon_hint, label, count)
	}

	/** Updates the desktop grid "cols × rows" hint for a candidate column count. */
	private fun updateDesktopGridHint(hint: TextView, cols: Int) {
		val (c, r) = WidgetGrid.dimensionsFor(this.activity, cols)
		hint.text = this.activity.getString(R.string.dash_grid_hint, c, r)
	}

	/** Re-renders the grid-size hint; called on rotation while customising. */
	fun refreshDashGridHint() {
		this.dashGridHint?.let { this.updateDashGridHint(it) }
	}

	/**
	 * Updates the "columns × rows" hint. Columns are exact (orientation-aware,
	 * via [DashGrid]); rows are how many fit the apps grid's real last-measured
	 * viewport — which reflects the current theme and orientation, since the
	 * grid itself is GONE while customising. Falls back to a screen estimate
	 * before the grid has ever been laid out. See [DashGrid].
	 */
	private fun updateDashGridHint(hint: TextView) {
		val cols = DashGrid.dashColumns(this.activity)
		val viewport = (this.activity as? HomeActivity)?.appManager?.dashGridViewport()

		val rows = if (viewport != null) {
			// Square cells fill the width, so cell size = viewportWidth / cols //
			val cell = (viewport.first / cols).coerceAtLeast(1)
			DashGrid.visibleRows(viewport.second, cell)
		} else {
			val dm = this.activity.resources.displayMetrics
			val gridHeightPx =
				if (dm.heightPixels >= dm.widthPixels) max(dm.widthPixels, dm.heightPixels)
				else min(dm.widthPixels, dm.heightPixels)
			DashGrid.visibleRows(gridHeightPx, DashGrid.cellSizePx(this.activity))
		}.coerceAtLeast(1)

		hint.text = this.activity.getString(R.string.dash_grid_hint, cols, rows)
	}

	/*
	 * For themes whose panel can sit on the bottom (MATE), the panel's edge
	 * is dictated by the launcher's: the panel takes whichever horizontal
	 * edge the launcher leaves free. The user only chooses whether the panel
	 * is shown; an incompatible stored edge is corrected whenever either
	 * spinner changes.
	 */
	private fun complementaryPanelEdge(launcherEdge: Int): Int {
		val res = this.activity.resources
		if (! res.getIntArray(this.theme.panel_location_supported)
				.contains(Location.BOTTOM.n)) {
			return res.getInteger(this.theme.panel_location)
		}

		return if (launcherEdge == Location.TOP.n) Location.BOTTOM.n else Location.TOP.n
	}

	private fun initEdgeSegments(
		segments: LinearLayout,
		edgeNames: Array<String>,
		supportedEdges: IntArray,
		currentEdge: Int,
		pref: Preference,
	) {
		val supportedEdgeNames = supportedEdges.map { edgeNames[it] }

		this.initSegments(segments, supportedEdgeNames, supportedEdges.indexOf(currentEdge)) { i ->
			var edge = supportedEdges[i]

			val prefs = Preferences.getSharedPreferences(this.activity)
			val prefsEdit = prefs.edit()
			if (pref == Preference.PANEL_EDGE && edge != Location.NONE.n) {
				edge = this.complementaryPanelEdge(
					prefs.getInt(Preference.LAUNCHER_EDGE.getName(),
						this.activity.resources.getInteger(this.theme.launcher_location)))
			}
			prefsEdit.putInt(pref.getName(), edge)
			if (pref == Preference.LAUNCHER_EDGE) {
				val panelEdge = prefs.getInt(Preference.PANEL_EDGE.getName(),
					this.activity.resources.getInteger(this.theme.panel_location))
				if (panelEdge != Location.NONE.n) {
					prefsEdit.putInt(Preference.PANEL_EDGE.getName(),
						this.complementaryPanelEdge(edge))
				}
			}
			prefsEdit.commit()

			this.relaunchInCustomiseMode.run()
		}
	}
}
