package be.robinj.distrohopper

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Last-ditch fallback for a base APK installed without its density split, which has no bitmaps. */
class IncompleteInstallActivity : AppCompatActivity() {
	@SuppressLint("SetTextI18n")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val padding = this.resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)
		this.setContentView(TextView(this).apply {
			text = "DistroHopper installation incomplete\n\n" +
				"DistroHopper could not start because some application resources are missing.\n" +
				"This usually means you installed a version of the app that is for a different type of device.\n\n" +
				"If you encountered this after installing from supported sources (Google Play Store or " +
				"IzzyOnDroid), please reach out to the developer."
			gravity = Gravity.CENTER_VERTICAL
			setPadding(padding, padding, padding, padding)
		})
		InsetsHelper.applySystemBarsPadding(this)
	}

	companion object {
		/** The launcher icon's layers exist only in the density split. */
		@JvmStatic
		fun isNeeded(resources: Resources) = try {
			resources.getValue(R.drawable.ic_launcher_foreground, TypedValue(), true)
			false
		} catch (e: Resources.NotFoundException) {
			true
		}
	}
}
