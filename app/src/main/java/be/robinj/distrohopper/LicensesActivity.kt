package be.robinj.distrohopper

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Lists everything DistroHopper ships that carries a licence of its own, and
 * opens the full text of each. The texts themselves are bundled as assets (see
 * the copyLicenseAssets task in app/build.gradle) because the SIL OFL and the
 * Ubuntu Font Licence both require the licence to be distributed along with the
 * fonts — a copy sitting in the git repository doesn't reach someone who
 * installed a build from the Play Store.
 */
class LicensesActivity : AppCompatActivity() {
	/**
	 * @param component Name of the bundled work. Not translated: these are proper names.
	 * @param license Name of the licence it is under. Likewise untranslated.
	 * @param asset Path of the full licence text within the APK's assets.
	 */
	private data class Entry(val component: String, val license: String, val asset: String)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		this.setContentView(R.layout.activity_licenses)
		InsetsHelper.applySystemBarsPadding(this)

		this.supportActionBar?.setDisplayHomeAsUpEnabled(true)

		val container = this.findViewById<LinearLayout>(R.id.llLicenses)
		val inflater = LayoutInflater.from(this)

		for (entry in ENTRIES) {
			val row = inflater.inflate(R.layout.view_license_entry, container, false)

			row.findViewById<TextView>(R.id.tvLicenseComponent).text = entry.component
			row.findViewById<TextView>(R.id.tvLicenseName).text =
				this.getString(R.string.licenses_entry_license, entry.license)
			row.setOnClickListener { this.openLicenseText(entry) }

			container.addView(row)
		}
	}

	private fun openLicenseText(entry: Entry) {
		val intent = Intent(this, LicenseTextActivity::class.java)
		intent.putExtra(LicenseTextActivity.EXTRA_TITLE, entry.component)
		intent.putExtra(LicenseTextActivity.EXTRA_ASSET, entry.asset)

		this.startActivity(intent)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == android.R.id.home) {
			this.finish()

			return true
		}

		return super.onOptionsItemSelected(item)
	}

	companion object {
		private const val APACHE = "apache-2.0.txt"

		private val ENTRIES = listOf(
			Entry("DistroHopper", "the GNU General Public Licence v3", "gpl-3.0.txt"),
			Entry("Ubuntu font", "the Ubuntu Font Licence 1.0", "fonts/Ubuntu-UFL.txt"),
			Entry("Oxygen font", "the SIL Open Font Licence 1.1", "fonts/Oxygen-OFL.txt"),
			Entry("OpenDyslexic font", "the SIL Open Font Licence 1.1", "fonts/OpenDyslexic-OFL.txt"),
			Entry("Ubuntu sound theme by Nathaniel McCallum",
				"the Creative Commons Attribution-ShareAlike Licence 2.0",
				"ubuntu-sound-CC-BY-SA-2.0.txt"),
			Entry("ProgressWheel by Todd Davies", "the MIT Licence", "ProgressWheel-MIT.txt"),
			Entry("Android Jetpack (AndroidX)", "the Apache Licence 2.0", APACHE),
			Entry("Kotlin and kotlinx.coroutines", "the Apache Licence 2.0", APACHE),
			Entry("ACRA", "the Apache Licence 2.0", APACHE),
			Entry("DragSortListView", "the Apache Licence 2.0", APACHE),
		)
	}
}
