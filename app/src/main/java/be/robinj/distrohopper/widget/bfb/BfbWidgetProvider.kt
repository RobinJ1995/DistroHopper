package be.robinj.distrohopper.widget.bfb

import android.content.Context
import android.content.Intent

/**
 * The BFB widget: tapping it opens the dash (see [BfbWidgetProviderBase]).
 */
class BfbWidgetProvider : BfbWidgetProviderBase() {
	override val pendingIntentRequestCode = REQUEST_CODE

	override fun tapIntent(context: Context): Intent =
		openDashIntent(context, focusSearch = false)

	companion object {
		private const val REQUEST_CODE = 0
	}
}
