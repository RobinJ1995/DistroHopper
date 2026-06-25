package be.robinj.distrohopper.home

import android.app.Activity
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.ViewCompat
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.ViewFinder
import be.robinj.distrohopper.desktop.dash.DashGrid
import be.robinj.distrohopper.preferences.BfbLocation
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
					if (b) this.update(i)  // see onStopTrackingTouch: ignore non-user (state-restore) changes
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

		// Launcher Edge //
		val edgeNames = res.getStringArray(R.array.edges)
		val spiCustomiseSpinnerTextColour = res.getColor(this.theme.dash_customise_spinner_text_colour, null)

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

		// Menu button (BFB) // Only themes that offer more than one BFB position
		// show the dropdown; the choice is applied by re-running the theme on relaunch.
		val llCustomiseMenuButton =
			this.viewFinder.get<View>(llDashCustomise, R.id.llCustomiseMenuButton)
		if (this.theme.launcherBfbToggleable(res)) {
			llCustomiseMenuButton.visibility = View.VISIBLE
			this.initMenuButtonSpinner(
				this.viewFinder.get(R.id.spiCustomiseMenuButton), spiCustomiseSpinnerTextColour)
		} else {
			llCustomiseMenuButton.visibility = View.GONE
		}
	}

	/*
	 * The menu-button (BFB) position dropdown, on themes that offer more than one
	 * position. The options come straight from the theme's
	 * launcher_bfb_location_supported array (mapped to none/start/end sides), so a
	 * theme only ever exposes the positions it declares (Pantheon/COSMIC: Hide or
	 * Start; GNOME: Hide, Start or End). The choice is stored as a named string
	 * ([BfbLocation]) and applied on relaunch.
	 */
	private fun initMenuButtonSpinner(spinner: Spinner, textColour: Int) {
		val res = this.activity.resources
		val prefs = Preferences.getSharedPreferences(this.activity)

		val options = res.getIntArray(this.theme.launcher_bfb_location_supported)
			.map { this.theme.bfbSide(Location.of(it)) }
			.distinct()

		val stored = prefs.getString(Preference.LAUNCHER_BFB_LOCATION.getName(), null)
		val current = if (stored == null) this.theme.launcherBfbDefaultChoice(res)
			else BfbLocation.of(stored)

		val labels = options.map { this.activity.getString(this.menuButtonLabel(it)) }.toTypedArray()
		spinner.adapter = this.colouredSpinnerAdapter(labels, textColour)
		spinner.setSelection(options.indexOf(current).coerceAtLeast(0))
		ViewCompat.setBackgroundTintList(spinner, ColorStateList.valueOf(textColour))
		spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
			override fun onItemSelected(adapterView: AdapterView<*>, view: View?, i: Int, l: Long) {
				val choice = options[i]
				if (choice != current) {
					Preferences.getSharedPreferences(this@CustomiseModeUi.activity).edit()
						.putString(Preference.LAUNCHER_BFB_LOCATION.getName(), choice.value).commit()
					this@CustomiseModeUi.relaunchInCustomiseMode.run()
				}
			}

			override fun onNothingSelected(adapterView: AdapterView<*>) {}
		}
	}

	private fun menuButtonLabel(location: BfbLocation): Int = when (location) {
		BfbLocation.NONE -> R.string.menu_button_none
		BfbLocation.START -> R.string.menu_button_start
		BfbLocation.END -> R.string.menu_button_end
	}

	/*
	 * A spinner adapter that paints the selected (closed) item in the theme's
	 * customise text colour. Doing it in getView keeps the colour correct even
	 * when no item is selected yet (e.g. setSelection(-1)), which is why the
	 * panel-edge dropdown could otherwise show its default white text.
	 */
	private fun colouredSpinnerAdapter(items: Array<String>, textColour: Int): ArrayAdapter<String> =
		object : ArrayAdapter<String>(this.activity,
			android.R.layout.simple_spinner_dropdown_item, items) {
			override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
				val view = super.getView(position, convertView, parent)
				(view as? TextView)?.setTextColor(textColour)
				return view
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

		spinner.adapter = this.colouredSpinnerAdapter(supportedEdgeNames, textColour)
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
			}

			override fun onNothingSelected(adapterView: AdapterView<*>) {}
		}
	}
}
