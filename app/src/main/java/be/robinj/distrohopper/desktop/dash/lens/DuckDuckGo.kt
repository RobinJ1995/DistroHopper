package be.robinj.distrohopper.desktop.dash.lens

import android.content.Context
import android.graphics.drawable.Drawable
import be.robinj.distrohopper.R
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

/**
 * Created by robin on 4/11/14.
 */
open class DuckDuckGo(context: Context) : Lens(context) {
    init {
        this.icon = context.resources.getDrawable(R.drawable.dash_search_lens_duckduckgo, null)
    }

    override val key = "DuckDuckGo"

    override val type = LensType.NETWORK

    override fun getName() = "DuckDuckGo"

    override fun getDescription() = "DuckDuckGo search results"

    /**
     * Each result carries its own downloaded icon, so emitting per-result lets a
     * result appear as soon as its icon finishes rather than waiting for the
     * slowest one. Downloads stay sequential (one lens, one thread) — the win is
     * incremental display, not parallelism.
     */
    override suspend fun search(query: String, maxResults: Int, emitter: LensResultEmitter) {
        for (topic in topics(query, maxResults)) {
            val result = toResult(topic) // resultIcon() blocks on the icon download here //
            currentCoroutineContext().ensureActive()
            emitter.emit(result)
        }
    }

    /** Flattens the API's RelatedTopics (which nest under "Topics") into a capped list. */
    private fun topics(str: String, maxResults: Int): List<JSONObject> {
        val apiResults = downloadStr(API.replace("{:QUERY:}", URLEncoder.encode(str, "UTF-8")))
        val relatedTopics = JSONObject(apiResults).getJSONArray("RelatedTopics")
        val topics = mutableListOf<JSONObject>()

        for (i in 0 until relatedTopics.length()) {
            val relatedTopic = relatedTopics.getJSONObject(i)

            if (relatedTopic.has("Text") && relatedTopic.has("FirstURL")) {
                topics.add(relatedTopic)
            } else if (relatedTopic.has("Topics")) {
                val nested: JSONArray = relatedTopic.getJSONArray("Topics")

                for (j in 0 until nested.length()) {
                    val topic = nested.getJSONObject(j)

                    if (topic.has("Text") && topic.has("FirstURL")) {
                        topics.add(topic)
                    }
                }
            }

            if (topics.size >= maxResults) {
                break
            }
        }

        return if (topics.size > maxResults) topics.subList(0, maxResults) else topics
    }

    private fun toResult(topic: JSONObject) = LensSearchResult(
        context, topic.getString("Text"), topic.getString("FirstURL"), resultIcon(topic))

    private fun resultIcon(topic: JSONObject): Drawable {
        var url = topic.optJSONObject("Icon")?.optString("URL", "") ?: ""

        if (url.isEmpty()) {
            return icon!!
        }
        if (url.startsWith("/")) { // The API returns paths relative to duckduckgo.com //
            url = "https://duckduckgo.com$url"
        }

        return try {
            downloadImage(url)
        } catch (ex: IOException) {
            icon!!
        }
    }

    override fun onClick(url: String) = this.openInBrowser(url)

    companion object {
        private const val API = "https://api.duckduckgo.com/?q={:QUERY:}&format=json"
    }
}
