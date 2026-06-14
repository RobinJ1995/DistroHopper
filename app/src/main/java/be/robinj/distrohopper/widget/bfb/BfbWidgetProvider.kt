package be.robinj.distrohopper.widget.bfb

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import kotlin.math.roundToInt

/**
 * Provides the BFB home-screen widget: the launcher's menu button rendered in
 * the active theme (see [BfbWidgetRenderer]), placeable on DistroHopper's own
 * widget desktops or any third-party launcher.
 *
 * Tapping it starts [HomeActivity] with `openDash=true`: already foregrounded
 * (singleTop) it just opens the dash; otherwise DistroHopper launches and opens
 * the dash. The provider is stateless — every render re-derives from current
 * preferences — so [requestUpdate] (on theme/wallpaper change) and the system's
 * own `onUpdate` both produce an up-to-date tile.
 */
class BfbWidgetProvider : AppWidgetProvider() {
	override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
		for (id in ids)
			renderWidget(context, manager, id)
	}

	override fun onAppWidgetOptionsChanged(
		context: Context,
		manager: AppWidgetManager,
		id: Int,
		newOptions: Bundle,
	) {
		renderWidget(context, manager, id)
	}

	companion object {
		/** Fallback edge (dp) before a host reports the widget's real size; matches the info's min. */
		private const val DEFAULT_SIZE_DP = 60

		/** The tap target: launch/foreground home and open the dash (handled in HomeActivity). */
		@JvmStatic
		fun tapIntent(context: Context): Intent =
			Intent(context, HomeActivity::class.java)
				.putExtra("openDash", true)
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

		/** Re-render every placed BFB widget; no-op when none are placed. */
		@JvmStatic
		fun requestUpdate(context: Context) {
			val manager = AppWidgetManager.getInstance(context) ?: return
			val ids = manager.getAppWidgetIds(ComponentName(context, BfbWidgetProvider::class.java))

			if (ids.isEmpty())
				return

			for (id in ids)
				renderWidget(context, manager, id)
		}

		private fun renderWidget(context: Context, manager: AppWidgetManager, id: Int) {
			val theme = DependencyContainer.of(context).themeManager.current
			val (widthPx, heightPx) = sizePx(context, manager, id)
			val bitmap = BfbWidgetRenderer.render(context, theme, widthPx, heightPx)

			val views = RemoteViews(context.packageName, R.layout.widget_bfb)
			views.setImageViewBitmap(R.id.imgBfbWidget, bitmap)
			views.setOnClickPendingIntent(R.id.imgBfbWidget, pendingTapIntent(context))

			manager.updateAppWidget(id, views)
		}

		private fun pendingTapIntent(context: Context): PendingIntent =
			PendingIntent.getActivity(
				context, 0, tapIntent(context),
				PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
			)

		/**
		 * The widget's render size in px from the host's options (portrait: min
		 * width, max height), falling back to [DEFAULT_SIZE_DP] when a host hasn't
		 * reported a size yet (e.g. the very first update on add).
		 */
		private fun sizePx(context: Context, manager: AppWidgetManager, id: Int): Pair<Int, Int> {
			val density = context.resources.displayMetrics.density
			val defaultPx = (DEFAULT_SIZE_DP * density).roundToInt()

			val options = manager.getAppWidgetOptions(id)
			val widthDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
			val heightDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0) ?: 0

			val widthPx = if (widthDp > 0) (widthDp * density).roundToInt() else defaultPx
			val heightPx = if (heightDp > 0) (heightDp * density).roundToInt() else defaultPx

			return Pair(widthPx, heightPx)
		}
	}
}
