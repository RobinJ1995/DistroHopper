package be.robinj.distrohopper.desktop.dash.lens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import be.robinj.distrohopper.R
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import java.net.URLEncoder
import java.util.regex.Pattern

/**
 * Searches the Google Play Store for apps.
 *
 * Google Play has no official public search API, so this lens does what the
 * established third-party tooling does: it requests Play's own web search page
 * and parses the JSON embedded in its HTML (the `AF_initDataCallback` script
 * blocks). The page format is undocumented and can change at any time, so
 * parsing is deliberately tolerant and degrades gracefully (no results simply
 * means no results) instead of throwing.
 */
open class GooglePlayStore(context: Context) : AppStoreLens(context) {
    private val api = "https://play.google.com/store/search?c=apps&q={:QUERY:}&hl=en&gl=us"

    private val lensIcon: Drawable = context.resources.getDrawable(R.drawable.dash_search_lens_googleplay)

    override fun getIcon(): Drawable = lensIcon

    override fun getName(): String = "Google Play Store"

    override fun getDescription(): String = "Google Play Store app search results"

    @Throws(IOException::class, JSONException::class)
    override suspend fun search(query: String, maxResults: Int, emitter: LensResultEmitter) {
        val html = fetchSearchHtml(api.replace("{:QUERY:}", URLEncoder.encode(query, "UTF-8")))

        // packageName -> app, in first-seen order.
        val apps = LinkedHashMap<String, AppResult>()

        for (dataArray in extractDataArrays(html)) {
            try {
                analyze(JSONArray(dataArray), apps)
            } catch (ex: JSONException) {
                // A block we can't parse is simply skipped; never fatal.
            }

            if (apps.size >= maxResults) {
                break
            }
        }

        var emitted = 0

        for (app in apps.values) {
            if (isInstalled(app.packageName)) {
                continue
            }

            emitter.emit(
                LensSearchResult(context, app.title, "market://details?id=${app.packageName}", iconFor(app.iconUrl))
            )

            if (++emitted >= maxResults) {
                break
            }
        }
    }

    /**
     * Downloads the icon for a single app, falling back to the lens icon if the
     * download fails so that one bad image never drops the whole result list.
     */
    private fun iconFor(iconUrl: String?): Drawable {
        if (iconUrl == null) {
            return lensIcon
        }

        return try {
            // Request a small icon to keep the download cheap.
            val sized = if (iconUrl.contains("=")) iconUrl else "$iconUrl=s64"
            downloadImage(sized)
        } catch (ex: IOException) {
            lensIcon
        }
    }

    /**
     * Seam for tests: fetches the raw search HTML. Overridable so tests can feed
     * a canned fixture without hitting the network.
     */
    @Throws(IOException::class)
    protected open fun fetchSearchHtml(url: String): String = downloadStr(url)

    /**
     * Recursively walks the parsed JSON, emitting one app per minimal subtree
     * that contains both an app detail URL and an icon URL. This is
     * position-independent: it does not depend on the exact array indices Google
     * uses (which drift over time), only on the structural pairing of a detail
     * link with an icon.
     */
    private fun analyze(node: Any?, apps: MutableMap<String, AppResult>): Aggregate {
        if (node is String) {
            return fromString(node)
        }

        if (node !is JSONArray) {
            return Aggregate()
        }

        val agg = Aggregate()

        for (i in 0 until node.length()) {
            val child = analyze(node.opt(i), apps)

            agg.emitted = agg.emitted || child.emitted
            agg.hasDetail = agg.hasDetail || child.hasDetail
            agg.hasIcon = agg.hasIcon || child.hasIcon

            if (agg.packageName == null) agg.packageName = child.packageName
            if (agg.iconUrl == null) agg.iconUrl = child.iconUrl
            if (agg.title == null) agg.title = child.title
        }

        // This node is the minimal one containing both a detail link and an
        // icon iff no descendant already emitted an app from them.
        val packageName = agg.packageName
        if (!agg.emitted && agg.hasDetail && agg.hasIcon && packageName != null) {
            if (!apps.containsKey(packageName)) {
                apps[packageName] = AppResult(packageName, agg.title ?: packageName, agg.iconUrl)
            }

            agg.emitted = true
        }

        return agg
    }

    private fun fromString(s: String): Aggregate {
        val agg = Aggregate()

        val detail = DETAIL_PATTERN.matcher(s)
        if (detail.find()) {
            agg.hasDetail = true
            agg.packageName = detail.group(1)
        } else if (s.contains(ICON_HOST)) {
            agg.hasIcon = true
            agg.iconUrl = s
        } else if (isTitleLike(s)) {
            agg.title = s
        }

        return agg
    }

    private fun isTitleLike(s: String): Boolean =
        s.isNotEmpty() &&
            !s.contains("://") &&
            !s.startsWith("/") &&
            !BASE64_TOKEN.matcher(s).matches() &&
            !PACKAGE_LIKE.matcher(s).matches()

    override fun onClick(url: String) {
        if (url.startsWith("market://")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            try {
                context.startActivity(intent)
            } catch (ex: ActivityNotFoundException) {
                // No Play Store app installed (e.g. de-Googled device): fall back to the web.
                val web = url.replace(
                    "market://details?id=",
                    "https://play.google.com/store/apps/details?id=",
                )
                openInBrowser(web)
            }
        } else {
            openInBrowser(url)
        }
    }

    /** A parsed app result before it is turned into a [LensSearchResult]. */
    private class AppResult(val packageName: String, val title: String, val iconUrl: String?)

    /** Aggregated facts about a JSON subtree, bubbled up during [analyze]. */
    private class Aggregate {
        var emitted = false
        var hasDetail = false
        var hasIcon = false
        var packageName: String? = null
        var iconUrl: String? = null
        var title: String? = null
    }

    companion object {
        // The detail URL embedded for every app result, e.g. ".../details?id=com.whatsapp".
        private val DETAIL_PATTERN: Pattern = Pattern.compile("details\\?id=([A-Za-z0-9._]+)")

        // The CDN that serves app icons (and screenshots).
        private const val ICON_HOST = "play-lh.googleusercontent.com"

        // Strings that look like dotted package names, used to reject them as titles.
        private val PACKAGE_LIKE: Pattern = Pattern.compile("^[a-z0-9]+(\\.[A-Za-z0-9_]+)+$")

        // Opaque base64 tokens such as "CAE=" or "CgYKBENBRT0=" that are not titles.
        // Deliberately narrow (requires base64 "=" padding) so that legitimate
        // short/all-caps titles such as "X", "AIDE" or "VLC" are kept.
        private val BASE64_TOKEN: Pattern = Pattern.compile("^[A-Za-z0-9+/_-]+={1,2}$")

        /**
         * Extracts the `data:[...]` JSON array from every `AF_initDataCallback(...)`
         * block in the page. Uses a string-literal aware bracket scanner rather than
         * a regex so nested brackets and quotes do not trip it up.
         */
        private fun extractDataArrays(html: String?): List<String> {
            val out = ArrayList<String>()

            if (html == null) {
                return out
            }

            val marker = "AF_initDataCallback("
            var from = 0

            while (true) {
                val call = html.indexOf(marker, from)
                if (call < 0) {
                    break
                }

                from = call + marker.length

                val dataKey = html.indexOf("data:", call)
                if (dataKey < 0) {
                    break
                }

                val open = html.indexOf('[', dataKey)
                if (open < 0) {
                    continue
                }

                val close = matchingBracket(html, open)
                if (close > open) {
                    out.add(html.substring(open, close + 1))
                    from = close
                }
            }

            return out
        }

        /**
         * Returns the index of the `]` that closes the `[` at [open], skipping over
         * brackets that appear inside string literals.
         */
        private fun matchingBracket(s: String, open: Int): Int {
            var depth = 0
            var inString = false
            var i = open

            while (i < s.length) {
                val c = s[i]

                if (inString) {
                    when (c) {
                        '\\' -> i++ // Skip the escaped character.
                        '"' -> inString = false
                    }
                } else when (c) {
                    '"' -> inString = true
                    '[' -> depth++
                    ']' -> {
                        depth--
                        if (depth == 0) {
                            return i
                        }
                    }
                }

                i++
            }

            return -1
        }
    }
}
