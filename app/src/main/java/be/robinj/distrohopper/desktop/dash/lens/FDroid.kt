package be.robinj.distrohopper.desktop.dash.lens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import be.robinj.distrohopper.R
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.regex.Pattern

/**
 * Searches the F-Droid catalogue of free and open source Android apps.
 *
 * Uses F-Droid's official full-text search API
 * (`https://search.f-droid.org/api/search_apps`), which returns a tidy JSON
 * list of apps (`name`, `summary`, `icon`, `url`). Tapping a result opens the
 * app's F-Droid page in the F-Droid client when it is installed, otherwise in
 * the browser.
 */
open class FDroid(context: Context) : Lens(context) {
    private val api = "https://search.f-droid.org/api/search_apps?q={:QUERY:}&lang=en"

    private val lensIcon: Drawable = context.resources.getDrawable(R.drawable.dash_search_lens_fdroid)

    override fun getIcon(): Drawable = lensIcon

    override fun getName(): String = "F-Droid"

    override fun getDescription(): String = "F-Droid free and open source app search results"

    @Throws(IOException::class, JSONException::class)
    override fun search(str: String, maxResults: Int): List<LensSearchResult> {
        val apiResults = fetchSearch(api.replace("{:QUERY:}", URLEncoder.encode(str, "UTF-8")))

        val apps = JSONObject(apiResults).getJSONArray("apps")
        val results = ArrayList<LensSearchResult>()

        for (i in 0 until apps.length()) {
            val app = apps.getJSONObject(i)

            if (!app.has("name") || !app.has("url")) {
                continue
            }

            val pkg = PACKAGE_PATTERN.matcher(app.getString("url"))
            if (!pkg.find()) {
                continue
            }

            val resultIcon = if (app.has("icon")) iconFor(app.getString("icon")) else lensIcon

            // Normalise to the language-neutral package page so the F-Droid
            // client's deep link recognises it.
            results.add(
                LensSearchResult(context, app.getString("name"), "https://f-droid.org/packages/${pkg.group(1)}/", resultIcon)
            )

            if (results.size >= maxResults) {
                break
            }
        }

        return results
    }

    /**
     * Downloads an app's icon, falling back to the lens icon if it fails so that
     * one bad image never drops the whole result list.
     */
    private fun iconFor(iconUrl: String): Drawable =
        try {
            downloadImage(iconUrl)
        } catch (ex: IOException) {
            lensIcon
        }

    /**
     * Seam for tests: fetches the raw search API response. Overridable so tests
     * can feed a canned fixture without hitting the network.
     */
    @Throws(IOException::class)
    protected open fun fetchSearch(url: String): String = downloadStr(url)

    override fun onClick(url: String) {
        // Open in the F-Droid client if it is installed, otherwise the browser.
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.setPackage(FDROID_PACKAGE)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        try {
            context.startActivity(intent)
        } catch (ex: ActivityNotFoundException) {
            super.onClick(url)
        }
    }

    companion object {
        private const val FDROID_PACKAGE = "org.fdroid.fdroid"

        // The package name lives in the result url, e.g. ".../packages/com.wire".
        private val PACKAGE_PATTERN: Pattern = Pattern.compile("packages/([^/?]+)")
    }
}
