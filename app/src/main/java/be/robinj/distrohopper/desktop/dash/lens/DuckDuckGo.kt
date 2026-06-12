package be.robinj.distrohopper.desktop.dash.lens

import android.content.Context
import android.graphics.drawable.Drawable
import be.robinj.distrohopper.R
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

/**
 * Created by robin on 4/11/14.
 */
class DuckDuckGo(context: Context) : Lens(context) {
    init {
        icon = context.resources.getDrawable(R.drawable.dash_search_lens_duckduckgo)
    }

    override fun getName() = "DuckDuckGo"

    override fun getDescription() = "DuckDuckGo search results"

    override fun search(str: String, maxResults: Int): List<LensSearchResult> {
        val apiResults = downloadStr(API.replace("{:QUERY:}", URLEncoder.encode(str, "UTF-8")))

        val relatedTopics = JSONObject(apiResults).getJSONArray("RelatedTopics")
        val results = mutableListOf<LensSearchResult>()

        for (i in 0 until relatedTopics.length()) {
            val relatedTopic = relatedTopics.getJSONObject(i)

            if (relatedTopic.has("Text") && relatedTopic.has("FirstURL")) {
                results.add(toResult(relatedTopic))
            } else if (relatedTopic.has("Topics")) {
                val topics = relatedTopic.getJSONArray("Topics")

                for (j in 0 until topics.length()) {
                    val topic = topics.getJSONObject(j)

                    if (topic.has("Text") && topic.has("FirstURL")) {
                        results.add(toResult(topic))
                    }
                }
            }

            if (results.size >= maxResults) {
                break
            }
        }

        return results
    }

    private fun toResult(topic: JSONObject) = LensSearchResult(
        context, topic.getString("Text"), topic.getString("FirstURL"), resultIcon(topic))

    private fun resultIcon(topic: JSONObject): Drawable {
        var url = topic.optJSONObject("Icon")?.optString("URL", "") ?: ""

        if (url.isEmpty()) {
            return icon
        }
        if (url.startsWith("/")) { // The API returns paths relative to duckduckgo.com //
            url = "https://duckduckgo.com$url"
        }

        return try {
            downloadImage(url)
        } catch (ex: IOException) {
            icon
        }
    }

    companion object {
        private const val API = "https://api.duckduckgo.com/?q={:QUERY:}&format=json"
    }
}
