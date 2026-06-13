package be.robinj.distrohopper.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.widget.Toast
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.RequestCode
import be.robinj.distrohopper.dev.Log

/**
 * Created by robin on 8/25/14.
 */
class WidgetHost(
	private val parent: HomeActivity,
	private val widgetManager: AppWidgetManager,
	private val vgWidgets: WidgetsPager,
) : AppWidgetHost(parent.applicationContext, HOST_ID) {
	private val persistence = WidgetPersistence(parent.applicationContext)

	private var pendingAppWidgetId = -1
	private var pendingInfo: AppWidgetProviderInfo? = null

	override fun onCreateView(context: Context, appWidgetId: Int, appWidget: AppWidgetProviderInfo?): AppWidgetHostView =
		WidgetHostView(context, this)

	/**
	 * Recreate the views for all persisted widgets, pruning any whose provider is gone.
	 */
	fun restoreWidgets() {
		var pruned = false

		for (layout in this.persistence.load()) {
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

	fun persist() {
		this.persistence.save(this.vgWidgets.collectLayouts(null))
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
		val colSpan = WidgetGrid.spanForSize(info.minWidth, pageContainer.cellWidth, WidgetGrid.COLS)
		val rowSpan = WidgetGrid.spanForSize(info.minHeight, pageContainer.cellHeight, WidgetGrid.ROWS)

		val layout = WidgetGrid.findFreeRect(pageContainer.collectLayouts(null), colSpan, rowSpan)

		if (layout == null) {
			this.deleteAppWidgetId(appWidgetId)

			Toast.makeText(this.parent, R.string.widget_no_room, Toast.LENGTH_LONG).show()

			return
		}

		layout.appWidgetId = appWidgetId
		layout.page = page

		this.addWidget(appWidgetId, layout, true)
	}

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
