package be.robinj.distrohopper.widget.bfb

import android.content.Context
import android.content.Intent

/**
 * The BFB search widget: visually identical to [BfbWidgetProvider], but tapping
 * it opens the dash *and* focuses the search field, raising the keyboard (see
 * [BfbWidgetProviderBase]).
 */
class BfbSearchWidgetProvider : BfbWidgetProviderBase() {
	override val pendingIntentRequestCode = REQUEST_CODE

	override fun tapIntent(context: Context): Intent =
		openDashIntent(context, focusSearch = true)

	companion object {
		private const val REQUEST_CODE = 1
	}
}
