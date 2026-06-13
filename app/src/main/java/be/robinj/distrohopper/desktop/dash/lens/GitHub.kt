package be.robinj.distrohopper.desktop.dash.lens

import android.content.Context
import be.robinj.distrohopper.R
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Created by robin on 4/11/14.
 */
open class GitHub(context: Context) : Lens(context) {

    init {
        this.icon = context.resources.getDrawable(R.drawable.dash_search_lens_github)
    }

    override val type = LensType.NETWORK

    override fun getName() = "GitHub"

    override fun getDescription() = "GitHub repository search results"

    override suspend fun search(query: String, maxResults: Int, emitter: LensResultEmitter) {
        val apiResults = this.downloadStr(API.replace("{:QUERY:}", URLEncoder.encode(query, "UTF-8")))

        val items = JSONObject(apiResults).getJSONArray("items")
        var nResults = 0

        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)

            if (item.has("full_name") && item.has("html_url")) {
                emitter.emit(LensSearchResult(
                    this.context, item.getString("full_name"), item.getString("html_url"), this.icon))

                if (++nResults >= maxResults) {
                    break
                }
            }
        }
    }

    override fun onClick(url: String) = this.openInBrowser(url)

    companion object {
        private const val API = "https://api.github.com/search/repositories?q={:QUERY:}"
    }
}
