package be.robinj.distrohopper.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.view.doOnLayout
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.RequestCode
import be.robinj.distrohopper.dev.Log
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences

/**
 * Created by robin on 8/25/14.
 */
class WidgetHost(
	private val parent: HomeActivity,
	private val widgetManager: AppWidgetManager,
	private val vgWidgets: WidgetsPager,
) : AppWidgetHost(parent.applicationContext, HOST_ID) {
	private fun desktopPrefs() =
		Preferences.getSharedPreferences(this.parent.applicationContext, Preferences.DESKTOP_LAYOUT)

	private var pendingAppWidgetId = -1
	private var pendingInfo: AppWidgetProviderInfo? = null

	override fun onCreateView(context: Context, appWidgetId: Int, appWidget: AppWidgetProviderInfo?): AppWidgetHostView =
		WidgetHostView(context, this)

	/**
	 * Recreate the views for all persisted widgets, pruning any whose provider is gone.
	 */
	fun restoreWidgets() {
		var pruned = false

		for (layout in DesktopLayoutStorage.readWidgets(this.desktopPrefs())) {
			if (this.widgetManager.getAppWidgetInfo(layout.appWidgetId) == null) {
				this.deleteAppWidgetId(layout.appWidgetId)
				pruned = true

				Log.getInstance().w(this.javaClass.simpleName, "Pruned stale widget: ${layout.appWidgetId}")
			} else {
				this.addWidget(layout.appWidgetId, layout, false)
			}
		}

		if (pruned) {
			this.persist()
		}

		this.vgWidgets.pagesChanged()

		// Saved spans may predate the provider's current resize limits, or have
		// been persisted at 1x1 by the old "grid not measured yet" bug; re-clamp
		// each widget to a valid size once its page has been measured //
		this.reclampRestoredWidgets()
	}

	/** Re-clamps every restored widget to its provider's limits, once measured. */
	private fun reclampRestoredWidgets() {
		for (page in 0 until this.vgWidgets.childCount) {
			val container = this.vgWidgets.pageAt(page)
			this.whenMeasured(container) { this.reclampPage(container) }
		}
	}

	private fun reclampPage(container: WidgetsContainer) {
		val cellW = container.cellWidth
		val cellH = container.cellHeight

		if (cellW <= 0 || cellH <= 0) {
			return
		}

		// When the user has opted into unrestricted widget sizing, don't clamp
		// to the provider's limits on restore — that would undo a size they set
		// on purpose; only the grid bounds and overlaps are still enforced //
		val unrestricted = this.allowUnsupportedResize()

		// Seed with the desktop apps' cells so re-clamped widgets avoid them //
		val kept = ArrayList<WidgetLayout>(container.collectAppCells(null))
		var changed = false

		for (i in 0 until container.childCount) {
			val child = container.getChildAt(i) as? WidgetContainer ?: continue
			val info = this.widgetManager.getAppWidgetInfo(child.appWidgetId) ?: continue
			val lp = child.layoutParams as WidgetsContainer.LayoutParams

			val colSpan = WidgetGrid.clampSpan(lp.colSpan,
				if (unrestricted) 0 else minResizeWidthPx(info),
				if (unrestricted) 0 else info.maxResizeWidth, cellW, WidgetGrid.COLS)
			val rowSpan = WidgetGrid.clampSpan(lp.rowSpan,
				if (unrestricted) 0 else minResizeHeightPx(info),
				if (unrestricted) 0 else info.maxResizeHeight, cellH, WidgetGrid.ROWS)

			// Always validate against the widgets already placed this pass, not
			// just when the span changed: growing one widget can collide with a
			// neighbour whose own span is unchanged. Keep the saved position when
			// it still fits, otherwise re-pack into the first free rectangle //
			val atCurrent = WidgetLayout(child.appWidgetId, lp.col, lp.row, colSpan, rowSpan)
			val placed = if (WidgetGrid.fits(kept, atCurrent)) {
				atCurrent
			} else {
				WidgetGrid.findFreeRect(kept, colSpan, rowSpan)
					?.also { it.appWidgetId = child.appWidgetId }
			}

			if (placed == null) {
				// No room for the re-clamped size; leave the widget untouched but
				// still record it so the others avoid its cells //
				kept.add(WidgetLayout(child.appWidgetId, lp.col, lp.row, lp.colSpan, lp.rowSpan))

				continue
			}

			if (placed.col != lp.col || placed.row != lp.row ||
				placed.colSpan != lp.colSpan || placed.rowSpan != lp.rowSpan) {
				lp.col = placed.col
				lp.row = placed.row
				lp.colSpan = placed.colSpan
				lp.rowSpan = placed.rowSpan
				changed = true
			}

			kept.add(WidgetLayout(child.appWidgetId, lp.col, lp.row, lp.colSpan, lp.rowSpan))
		}

		if (changed) {
			container.requestLayout()
			this.persist()
		}
	}

	private fun addWidget(appWidgetId: Int, layout: WidgetLayout, persist: Boolean) {
		val info = this.widgetManager.getAppWidgetInfo(appWidgetId)

		if (info == null) {
			this.deleteAppWidgetId(appWidgetId)

			return
		}

		// The host view must NOT be created with the activity context: AppCompatActivity
		// installs a LayoutInflater factory that swaps TextView for AppCompatTextView, and
		// RemoteViews inflation clones that inflater. AppCompat views then read appcompat
		// attribute ids against the widget package's theme, where they resolve to other
		// types, crashing inflation ("Couldn't add widget"). The application context's
		// inflater is factory-free, so widget layouts inflate plain framework views //
		val hostView = this.createView(this.parent.applicationContext, appWidgetId, info) as WidgetHostView
		val container = WidgetContainer(this.parent, this, hostView)

		val page = layout.page.coerceIn(0, WidgetsPager.MAX_PAGES - 1)
		this.vgWidgets.pageAt(page).addView(container, WidgetsContainer.LayoutParams(layout))

		hostView.setOnLongClickListener(WidgetHostView_LongClickListener(container))

		Log.getInstance().v(this.javaClass.simpleName, "Widget added: $appWidgetId")

		if (persist) {
			this.persist()
			this.vgWidgets.pagesChanged()
		}
	}

	fun removeWidget(container: WidgetContainer) {
		this.deleteAppWidgetId(container.appWidgetId)
		(container.parent as? WidgetsContainer)?.removeView(container)
		this.persist()
		this.vgWidgets.pagesChanged()
	}

	/**
	 * Removes every widget on [page] and shifts the widgets on higher desktops
	 * down by one, for desktop deletion (the per-page index is the pager child
	 * index, so moving a widget's view into the lower page re-pages it on
	 * persist). Coordinated with the pinned apps by [be.robinj.distrohopper.home.Desktops].
	 */
	fun removeWidgetPage(page: Int) {
		if (page in 0 until this.vgWidgets.childCount) {
			val container = this.vgWidgets.pageAt(page)
			for (widget in this.widgetsOf(container)) {
				this.deleteAppWidgetId(widget.appWidgetId)
				container.removeView(widget)
			}

			for (higher in (page + 1) until this.vgWidgets.childCount) {
				val from = this.vgWidgets.pageAt(higher)
				val to = this.vgWidgets.pageAt(higher - 1)
				for (widget in this.widgetsOf(from)) {
					val layoutParams = widget.layoutParams
					from.removeView(widget)
					to.addView(widget, layoutParams)
				}
			}
		}

		this.persist()
		this.vgWidgets.pagesChanged()
	}

	private fun widgetsOf(container: WidgetsContainer): List<WidgetContainer> =
		(0 until container.childCount).mapNotNull { container.getChildAt(it) as? WidgetContainer }

	/** Highest desktop holding a widget (or -1), for the `home/Desktops` coordinator. */
	fun highestWidgetDesktop(): Int = this.vgWidgets.highestWidgetPage()

	/** Whether desktop [page] holds any widget, for the `home/Desktops` coordinator. */
	fun hasWidgetsOnDesktop(page: Int): Boolean =
		page in 0 until this.vgWidgets.childCount &&
			this.widgetsOf(this.vgWidgets.pageAt(page)).isNotEmpty()

	fun persist() {
		DesktopLayoutStorage.writeWidgets(this.desktopPrefs(), this.vgWidgets.collectLayouts(null))
	}

	fun showPicker() {
		WidgetPickerDialog(this.parent, this).show()
	}

	fun onProviderChosen(info: AppWidgetProviderInfo) {
		this.pendingAppWidgetId = this.allocateAppWidgetId()
		this.pendingInfo = info

		val bound = this.widgetManager.bindAppWidgetIdIfAllowed(
			this.pendingAppWidgetId, info.profile, info.provider, null)

		if (bound) {
			this.configurePendingWidget()
		} else {
			val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, this.pendingAppWidgetId)
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)

			this.parent.startActivityForResult(intent, RequestCode.WIDGET_BOUND)
		}
	}

	fun onBindResult(resultCode: Int, data: Intent?) {
		this.recoverPendingState(data)

		if (resultCode == Activity.RESULT_OK) {
			this.configurePendingWidget()
		} else {
			this.cancelPendingWidget()
		}
	}

	fun onConfigureResult(resultCode: Int, data: Intent?) {
		this.recoverPendingState(data)

		if (resultCode == Activity.RESULT_OK) {
			this.placePendingWidget()
		} else {
			this.cancelPendingWidget()
		}
	}

	/**
	 * The pending state only lives in memory, but the bind/configure activity can
	 * outlive this process — recover the widget id from the result, as both the
	 * system bind activity and configure activities return EXTRA_APPWIDGET_ID.
	 */
	private fun recoverPendingState(data: Intent?) {
		if (this.pendingAppWidgetId != -1) {
			return
		}

		val appWidgetId = data?.getIntExtra(
			AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1

		if (appWidgetId == -1) {
			return
		}

		this.pendingAppWidgetId = appWidgetId
		this.pendingInfo = this.widgetManager.getAppWidgetInfo(appWidgetId)

		Log.getInstance().w(this.javaClass.simpleName,
			"Recovered pending widget from result intent: $appWidgetId")
	}

	private fun configurePendingWidget() {
		if (this.pendingInfo?.configure != null) {
			Log.getInstance().v(this.javaClass.simpleName,
				"Widget requires configuration: ${this.pendingAppWidgetId}")

			this.startAppWidgetConfigureActivityForResult(
				this.parent, this.pendingAppWidgetId, 0, RequestCode.WIDGET_CONFIGURED, null)
		} else {
			this.placePendingWidget()
		}
	}

	private fun placePendingWidget() {
		val appWidgetId = this.pendingAppWidgetId
		val info = this.pendingInfo

		this.pendingAppWidgetId = -1
		this.pendingInfo = null

		if (appWidgetId == -1) {
			return
		}

		if (info == null) {
			// Allocated but unusable (e.g. recovered after process death with the
			// provider gone): don't leak the binding //
			this.deleteAppWidgetId(appWidgetId)

			return
		}

		// New widgets land on whichever desktop the user is looking at //
		val page = this.vgWidgets.currentPage
		val pageContainer = this.vgWidgets.pageAt(page)

		// Size from the provider's resize/target hints against the measured cell
		// size; defer until the page is laid out so a not-yet-measured grid can
		// never squash the widget to 1x1 //
		this.whenMeasured(pageContainer) {
			val spans = this.initialSpans(info, pageContainer)

			if (spans == null) {
				// Still unmeasured after a layout pass: give up cleanly //
				this.deleteAppWidgetId(appWidgetId)

				return@whenMeasured
			}

			val layout = WidgetGrid.findFreeRect(
				pageContainer.collectOccupied(null), spans.first, spans.second)

			if (layout == null) {
				this.deleteAppWidgetId(appWidgetId)

				Toast.makeText(this.parent, R.string.widget_no_room, Toast.LENGTH_LONG).show()

				return@whenMeasured
			}

			layout.appWidgetId = appWidgetId
			layout.page = page

			this.addWidget(appWidgetId, layout, true)
		}
	}

	/** Runs [action] once [page] has a non-zero cell size (immediately if already laid out). */
	private inline fun whenMeasured(page: WidgetsContainer, crossinline action: () -> Unit) {
		if (page.cellWidth > 0 && page.cellHeight > 0) {
			action()
		} else {
			page.doOnLayout {
				if (page.cellWidth > 0 && page.cellHeight > 0) {
					action()
				}
			}
		}
	}

	/** Initial (colSpan, rowSpan) for [info] on a measured [page], or null if not measured. */
	private fun initialSpans(info: AppWidgetProviderInfo, page: WidgetsContainer): Pair<Int, Int>? {
		val colSpan = WidgetGrid.initialSpan(targetCols(info),
			minResizeWidthPx(info), info.maxResizeWidth, page.cellWidth, WidgetGrid.COLS)
		val rowSpan = WidgetGrid.initialSpan(targetRows(info),
			minResizeHeightPx(info), info.maxResizeHeight, page.cellHeight, WidgetGrid.ROWS)

		return if (colSpan <= 0 || rowSpan <= 0) null else colSpan to rowSpan
	}

	/** The provider's smallest resizable width in px (minResizeWidth, else minWidth). */
	private fun minResizeWidthPx(info: AppWidgetProviderInfo): Int =
		if (info.minResizeWidth > 0) info.minResizeWidth else info.minWidth

	/** The provider's smallest resizable height in px (minResizeHeight, else minHeight). */
	private fun minResizeHeightPx(info: AppWidgetProviderInfo): Int =
		if (info.minResizeHeight > 0) info.minResizeHeight else info.minHeight

	/** The provider's preferred cell width (API 33 targetCellWidth), or 0. */
	private fun targetCols(info: AppWidgetProviderInfo): Int =
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) info.targetCellWidth else 0

	/** The provider's preferred cell height (API 33 targetCellHeight), or 0. */
	private fun targetRows(info: AppWidgetProviderInfo): Int =
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) info.targetCellHeight else 0

	/** Whether the user has enabled unrestricted (beyond provider limits) widget sizing. */
	private fun allowUnsupportedResize(): Boolean =
		Preferences.getSharedPreferences(this.parent)
			.getBoolean(Preference.DEV_WIDGET_RESIZE_ANY.getName(), false)

	private fun cancelPendingWidget() {
		if (this.pendingAppWidgetId != -1) {
			this.deleteAppWidgetId(this.pendingAppWidgetId)
		}

		this.pendingAppWidgetId = -1
		this.pendingInfo = null
	}

	companion object {
		/** Must stay stable across builds; never derive this from a resource id. */
		const val HOST_ID = 0xD1570
	}
}
