package be.robinj.distrohopper.desktop.dash

import android.content.ClipData
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.PopupWindow
import be.robinj.distrohopper.App
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.dev.Log
import be.robinj.distrohopper.folder.FolderGrid
import be.robinj.distrohopper.home.LauncherBarBinder
import be.robinj.distrohopper.preferences.AppSortOrder
import be.robinj.distrohopper.preferences.Preferences

/**
 * The popover shown when a dash folder is tapped: the folder's apps in the
 * adaptive [FolderGrid] grid (the spec's 1..9 layout). Tapping an app launches
 * it; long-pressing starts an extract drag ([DashDragPayload.FolderMemberDrag])
 * — the popup dismisses and the dash grid handles the drop, so releasing on the
 * dash removes the app from the folder, and pausing over another app folds them.
 * In-folder reordering is allowed only when the custom sort order is active.
 */
class FolderPopup @JvmOverloads constructor(
	private val activity: HomeActivity,
	private val folderId: String,
	private val apps: List<App>,
	private val clipLabel: String = "dashFolderMember",
	private val memberPayload: (App) -> Any = { app -> DashDragPayload.FolderMemberDrag(folderId, app) },
) {
	private val window = PopupWindow(this.activity)

	fun showAt(anchor: View) {
		val grid = GridView(this.activity).apply {
			numColumns = FolderGrid.columns(apps.size)
			val pad = dp(8)
			setPadding(pad, pad, pad, pad)
			horizontalSpacing = dp(4)
			verticalSpacing = dp(4)
			isVerticalScrollBarEnabled = false
			background = GradientDrawable().apply {
				cornerRadius = dp(16).toFloat()
				setColor(Color.argb(235, 32, 32, 32))
			}
		}

		val cell = dp(76)
		grid.adapter = GridAdapter(this.activity, apps.map { DashItem.AppItem(it) as DashItem }.toMutableList())
		grid.onItemClickListener = AdapterLaunch()
		grid.onItemLongClickListener = AdapterExtract()

		val columns = FolderGrid.columns(apps.size)
		val rows = FolderGrid.rows(apps.size)
		this.window.apply {
			contentView = grid
			width = cell * columns + dp(16)
			height = cell * rows + dp(16)
			isOutsideTouchable = true
			isFocusable = true
			elevation = dp(8).toFloat()
		}

		this.window.showAtLocation(anchor, Gravity.CENTER, 0, 0)
	}

	fun dismiss() = this.window.dismiss()

	private inner class AdapterLaunch : android.widget.AdapterView.OnItemClickListener {
		override fun onItemClick(parent: android.widget.AdapterView<*>?, view: View, position: Int, id: Long) {
			(apps.getOrNull(position))?.launch()
			dismiss()
		}
	}

	private inner class AdapterExtract : android.widget.AdapterView.OnItemLongClickListener {
		override fun onItemLongClick(parent: android.widget.AdapterView<*>?, view: View, position: Int, id: Long): Boolean {
			val app = apps.getOrNull(position) ?: return false
			// Start the drag from the decor view so it survives the popup closing,
			// matching dash.AppLauncherLongClickListener's detached-view handling.
			val source = activity.window.decorView
			val payload = memberPayload(app)
			val clip = ClipData.newPlainText(clipLabel, folderId)
			val started = source.startDragAndDrop(clip, View.DragShadowBuilder(view), payload, 0)
			if (started) {
				LauncherBarBinder.startedDragging(activity)
				dismiss()
			} else {
				Log.getInstance().w("FolderPopup", "Could not start folder-member drag")
			}

			return true
		}
	}

	private fun dp(value: Int): Int =
		(value * this.activity.resources.displayMetrics.density).toInt()

	companion object {
		/** Whether in-folder reordering is currently allowed (custom sort order). */
		@JvmStatic
		fun customOrderingEnabled(activity: HomeActivity): Boolean =
			AppSortOrder.current(Preferences.getSharedPreferences(activity)) == AppSortOrder.CUSTOM
	}
}
