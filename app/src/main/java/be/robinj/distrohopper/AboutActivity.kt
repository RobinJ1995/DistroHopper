package be.robinj.distrohopper

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class AboutActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		this.setContentView(R.layout.activity_about)
		InsetsHelper.applySystemBarsPadding(this)

		try {
			val pkgInfo = this.packageManager.getPackageInfo(this.packageName, 0)

			val tvDevUrl = this.findViewById<TextView>(R.id.tvDevUrl)
			val tvDevEmail = this.findViewById<TextView>(R.id.tvDevEmail)
			val tvVersion = this.findViewById<TextView>(R.id.tvVersion)
			val ivLogo = this.findViewById<ImageView>(R.id.ivLogo)

			tvDevUrl.text = Html.fromHtml("<a href=\"http://robinj.be/\">RobinJ.be</a>", Html.FROM_HTML_MODE_LEGACY)
			tvDevEmail.text = Html.fromHtml("<a href=\"mailto:distrohopper@robinj.be\">distrohopper@robinj.be</a>", Html.FROM_HTML_MODE_LEGACY)
			tvVersion.text = "v" + pkgInfo.versionName

			tvDevUrl.movementMethod = LinkMovementMethod.getInstance()
			tvDevEmail.movementMethod = LinkMovementMethod.getInstance()

			this.findViewById<View>(R.id.linkGithub).setOnClickListener {
				this.openUrl(this.getString(R.string.about_github_url))
			}
			this.findViewById<View>(R.id.linkTransifex).setOnClickListener {
				this.openUrl(this.getString(R.string.about_transifex_url))
			}
			this.findViewById<View>(R.id.linkLicenses).setOnClickListener {
				this.startActivity(Intent(this, LicensesActivity::class.java))
			}

			this.findViewById<TextView>(R.id.tvCopyright).text = this.getString(
				R.string.about_copyright,
				Calendar.getInstance().get(Calendar.YEAR).toString())

			val context = this.baseContext

			ivLogo.setOnClickListener(object : View.OnClickListener {
				private var clicked = 0
				private var player: MediaPlayer? = null

				override fun onClick(view: View) {
					if (++this.clicked % 3 == 0) {
						val player = this.player
							?: MediaPlayer.create(context, R.raw.ubuntu).also { this.player = it }

						player.seekTo(0)
						player.start()
					}
				}
			})
		} catch (ex: Exception) {
			val exh = ExceptionHandler(ex)
			exh.show(this)
		}
	}

	private fun openUrl(url: String) {
		try {
			this.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
		} catch (ex: Exception) {
			val exh = ExceptionHandler(ex)
			exh.show(this)
		}
	}
}
