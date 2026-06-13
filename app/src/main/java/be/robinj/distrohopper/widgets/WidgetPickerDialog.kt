package be.robinj.distrohopper.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import java.text.Collator
import be.robinj.distrohopper.R
import be.robinj.distrohopper.desktop.FrostedGlass

/**
 * Lists the installed app widget providers, grouped by application.
 */
class WidgetPickerDialog(
	private val context: Context,
	private val widgetHost: WidgetHost,
) {
	fun show() {
		val items = this.buildItems()

		val listView = ListView(this.context)
		listView.adapter = Adapter(this.context, items)
		// A clean, edge-to-edge list: the rows carry their own ripple and spacing. //
		listView.divider = null
		listView.dividerHeight = 0
		listView.selector = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
		listView.setPadding(0, listView.paddingTop, 0, listView.resources.displayMetrics.density.times(8).toInt())
		listView.clipToPadding = false

		val dialog = AlertDialog.Builder(this.context, R.style.ModernDialogTheme)
			.setTitle(R.string.widget_picker_title)
			.setView(listView)
			.setNegativeButton(android.R.string.cancel, null)
			.create()

		listView.setOnItemClickListener { _, _, position, _ ->
			val info = items[position].info

			if (info != null) {
				dialog.dismiss()
				this.widgetHost.onProviderChosen(info)
			}
		}

		// Keep the surface legible where cross-window blur isn't available (e.g. Samsung). //
		dialog.setOnShowListener { dialog.window?.let(FrostedGlass::applyDialogFallback) }

		dialog.show()
	}

	private fun buildItems(): List<Item> {
		val pm = this.context.packageManager
		val providers = AppWidgetManager.getInstance(this.context).installedProviders
		val collator = Collator.getInstance()

		val byApp = providers.groupBy { info ->
			val packageName = info.provider.packageName

			try {
				pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
			} catch (ex: PackageManager.NameNotFoundException) {
				packageName
			}
		}

		val items = mutableListOf<Item>()

		for ((appLabel, group) in byApp.entries.sortedWith(compareBy(collator) { it.key })) {
			items.add(Item(null, appLabel, group[0].provider.packageName))

			for (info in group.sortedWith(compareBy(collator) { it.loadLabel(pm) })) {
				items.add(Item(info, info.loadLabel(pm), info.provider.packageName))
			}
		}

		return items
	}

	/** [info] is null for app header rows. */
	private class Item(
		val info: AppWidgetProviderInfo?,
		val label: String,
		val packageName: String,
	)

	private class Adapter(
		private val context: Context,
		private val items: List<Item>,
	) : BaseAdapter() {
		private val inflater = LayoutInflater.from(context)

		override fun getCount(): Int = this.items.size

		override fun getItem(position: Int): Any = this.items[position]

		override fun getItemId(position: Int): Long = position.toLong()

		override fun getViewTypeCount(): Int = 2

		override fun getItemViewType(position: Int): Int =
			if (this.items[position].info == null) VIEW_TYPE_HEADER else VIEW_TYPE_WIDGET

		override fun areAllItemsEnabled(): Boolean = false

		override fun isEnabled(position: Int): Boolean = this.items[position].info != null

		override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
			val item = this.items[position]
			val info = item.info

			val view = convertView ?: this.inflater.inflate(
				if (info == null) R.layout.widget_picker_header else R.layout.widget_picker_item,
				parent, false)

			view.findViewById<TextView>(R.id.tvName).text = item.label

			if (info == null) {
				val icon: Drawable? = try {
					this.context.packageManager.getApplicationIcon(item.packageName)
				} catch (ex: PackageManager.NameNotFoundException) {
					null
				}

				view.findViewById<ImageView>(R.id.imgIcon).setImageDrawable(icon)
			} else {
				val preview = info.loadPreviewImage(this.context, 0)
					?: info.loadIcon(this.context, 0)

				view.findViewById<ImageView>(R.id.imgPreview).setImageDrawable(preview)
			}

			return view
		}

		companion object {
			private const val VIEW_TYPE_HEADER = 0
			private const val VIEW_TYPE_WIDGET = 1
		}
	}
}
