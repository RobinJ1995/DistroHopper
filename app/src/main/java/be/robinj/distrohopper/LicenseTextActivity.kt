package be.robinj.distrohopper

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException

/** Shows the full text of one licence, read from the APK's bundled assets. */
class LicenseTextActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		this.setContentView(R.layout.activity_license_text)
		InsetsHelper.applySystemBarsPadding(this)

		this.supportActionBar?.setDisplayHomeAsUpEnabled(true)

		val title = this.intent.getStringExtra(EXTRA_TITLE)
		if (title != null) {
			this.title = title
		}

		val asset = this.intent.getStringExtra(EXTRA_ASSET)

		this.findViewById<TextView>(R.id.tvLicenseText).text = this.readLicense(asset)
	}

	/**
	 * The bundled licences are a few tens of kilobytes at most, so this reads on
	 * the main thread rather than dragging in a loader for a one-shot screen.
	 */
	private fun readLicense(asset: String?): String {
		if (asset == null) {
			return this.getString(R.string.licenses_read_error)
		}

		return try {
			this.assets.open("$ASSET_DIR/$asset").use { it.readBytes().toString(Charsets.UTF_8) }
		} catch (ex: IOException) {
			this.getString(R.string.licenses_read_error)
		}
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == android.R.id.home) {
			this.finish()

			return true
		}

		return super.onOptionsItemSelected(item)
	}

	companion object {
		const val EXTRA_TITLE = "be.robinj.distrohopper.LICENSE_TITLE"
		const val EXTRA_ASSET = "be.robinj.distrohopper.LICENSE_ASSET"

		private const val ASSET_DIR = "licenses"
	}
}
