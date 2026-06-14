package be.robinj.distrohopper.home

import android.app.Activity
import android.content.res.ColorStateList
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.ViewCompat
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.ViewFinder
import be.robinj.distrohopper.desktop.dash.DashGrid
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import be.robinj.distrohopper.theme.Location
import be.robinj.distrohopper.theme.Theme
import kotlin.math.max
import kotlin.math.min

/**
 * The customise mode UI inside the dash: seekbars for the launcher/dash icon
 * sizes and spinners for the launcher/panel edges. Extracted from
 * HomeActivity's onCreate(). Edge changes relaunch the activity in customise
 * mode via [relaunchInCustomiseMode].
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

		// Launcher Icon Size // Writing the preference is enough: HomeStateBinder
		// observes it and re-initialises the launcher icons //
		val sbCustomiseLauncherIconSize = this.viewFinder.get<SeekBar>(R.id.sbCustomiseLauncherIconSize)
		sbCustomiseLauncherIconSize.progress = prefs.getInt(Preference.LAUNCHERICON_WIDTH.getName(), 36)
		sbCustomiseLauncherIconSize.setOnSeekBarChangeListener(
			object : SeekBar.OnSeekBarChangeListener {
				override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
					this.update(i)
				}

				override fun onStartTrackingTouch(seekBar: SeekBar) {}

				override fun onStopTrackingTouch(seekBar: SeekBar) {
					this.update(seekBar.progress)
				}

				private fun update(value: Int) {
					prefsEdit.putInt(Preference.LAUNCHERICON_WIDTH.getName(), value)
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
					this.update(i)
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

		// Launcher Edge //
		val edgeNames = res.getStringArray(R.array.edges)
		val spiCustomiseSpinnerTextColour = res.getColor(this.theme.dash_customise_spinner_text_colour)

		val spiCustomiseLauncherEdge = this.viewFinder.get<Spinner>(R.id.spiCustomiseLauncherEdge)
		val supportedLauncherEdges = res.getIntArray(this.theme.launcher_location_supported)
		val currentLauncherEdge = prefs.getInt(Preference.LAUNCHER_EDGE.getName(), this.theme.launcher_location)
		this.initEdgeSpinner(spiCustomiseLauncherEdge, edgeNames, supportedLauncherEdges,
			currentLauncherEdge, spiCustomiseSpinnerTextColour, Preference.LAUNCHER_EDGE)

		// Panel Edge //
		val spiCustomisePanelEdge = this.viewFinder.get<Spinner>(R.id.spiCustomisePanelEdge)
		/*
		 * When the panel's edge is dictated by the launcher's, only the edge
		 * the current launcher position allows is offered (plus None).
		 */
		val supportedPanelEdges = if (res.getIntArray(this.theme.panel_location_supported)
				.contains(Location.BOTTOM.n)) {
			intArrayOf(
				this.complementaryPanelEdge(prefs.getInt(Preference.LAUNCHER_EDGE.getName(),
					this.theme.launcher_location)),
				Location.NONE.n)
		} else {
			res.getIntArray(this.theme.panel_location_supported)
		}
		if (supportedPanelEdges.size > 1) {
			val currentPanelEdge = prefs.getInt(Preference.PANEL_EDGE.getName(), this.theme.panel_location)
			this.initEdgeSpinner(spiCustomisePanelEdge, edgeNames, supportedPanelEdges,
				currentPanelEdge, spiCustomiseSpinnerTextColour, Preference.PANEL_EDGE)
		} else {
			this.viewFinder.get<View>(llDashCustomise, R.id.llCustomisePanelEdge).visibility = View.GONE
		}

		// Menu button (BFB) // Only themes that allow it (Pantheon, COSMIC) offer
		// the toggle; the choice is applied by re-running the theme on relaunch.
		val llCustomiseMenuButton =
			this.viewFinder.get<View>(llDashCustomise, R.id.llCustomiseMenuButton)
		if (res.getBoolean(this.theme.launcher_bfb_user_toggleable)) {
			llCustomiseMenuButton.visibility = View.VISIBLE
			val cbCustomiseMenuButton = this.viewFinder.get<CheckBox>(R.id.cbCustomiseMenuButton)
			cbCustomiseMenuButton.isChecked = prefs.getBoolean(
				Preference.LAUNCHER_MENU_BUTTON_VISIBLE.getName(),
				res.getBoolean(this.theme.launcher_bfb_visible_by_default))
			cbCustomiseMenuButton.setOnCheckedChangeListener { _, checked ->
				prefsEdit.putBoolean(Preference.LAUNCHER_MENU_BUTTON_VISIBLE.getName(), checked)
				prefsEdit.commit()
				this.relaunchInCustomiseMode.run()
			}
		} else {
			llCustomiseMenuButton.visibility = View.GONE
		}
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

	private fun initEdgeSpinner(
		spinner: Spinner,
		edgeNames: Array<String>,
		supportedEdges: IntArray,
		currentEdge: Int,
		textColour: Int,
		pref: Preference,
	) {
		val supportedEdgeNames = supportedEdges.map { edgeNames[it] }.toTypedArray()
		val currentEdgeIndex = supportedEdges.indexOf(currentEdge)

		val adapter = ArrayAdapter(this.activity,
			android.R.layout.simple_spinner_dropdown_item, supportedEdgeNames)
		spinner.adapter = adapter
		spinner.setSelection(currentEdgeIndex)
		ViewCompat.setBackgroundTintList(spinner, ColorStateList.valueOf(textColour))
		spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
			override fun onItemSelected(adapterView: AdapterView<*>, view: View?, i: Int, l: Long) {
				var edge = supportedEdges[i]

				if (currentEdge != edge) {
					val prefs = Preferences.getSharedPreferences(this@CustomiseModeUi.activity)
					val prefsEdit = prefs.edit()
					if (pref == Preference.PANEL_EDGE && edge != Location.NONE.n) {
						edge = this@CustomiseModeUi.complementaryPanelEdge(
							prefs.getInt(Preference.LAUNCHER_EDGE.getName(),
								this@CustomiseModeUi.activity.resources.getInteger(
									this@CustomiseModeUi.theme.launcher_location)))
					}
					prefsEdit.putInt(pref.getName(), edge)
					if (pref == Preference.LAUNCHER_EDGE) {
						val panelEdge = prefs.getInt(Preference.PANEL_EDGE.getName(),
							this@CustomiseModeUi.activity.resources.getInteger(
								this@CustomiseModeUi.theme.panel_location))
						if (panelEdge != Location.NONE.n) {
							prefsEdit.putInt(Preference.PANEL_EDGE.getName(),
								this@CustomiseModeUi.complementaryPanelEdge(edge))
						}
					}
					prefsEdit.commit()

					this@CustomiseModeUi.relaunchInCustomiseMode.run()
				}

				// Apply spinner text colour
				(adapterView.getChildAt(0) as? TextView)?.setTextColor(textColour)
			}

			override fun onNothingSelected(adapterView: AdapterView<*>) {}
		}
	}
}
