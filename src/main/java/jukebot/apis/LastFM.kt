package jukebot.apis

import jukebot.JukeBot
import jukebot.utils.json
import okhttp3.Request
import org.apache.http.client.utils.URIBuilder
import java.util.concurrent.CompletableFuture


public class LastFM(private val key: String) {

    private val baseUrl = "http://ws.audioscrobbler.com/2.0"

    public fun findSimilar(title: String, artist: String): CompletableFuture<TrackMatch?> {
        val url = URIBuilder("$baseUrl/?method=track.getsimilar&api_key=$key&format=json")

        url.addParameter("track", title)
        url.addParameter("artist", artist)

        val req = Request.Builder()
                .url(url.build().toURL())
                .get()
                .build()

        val future = CompletableFuture<TrackMatch?>()

        JukeBot.httpClient.makeRequest(req).queue({
            val json = it.json()

            if (json == null) {
                future.complete(null)
                return@queue
            }

            val obj = json.getJSONObject("similartracks").getJSONArray("track")

            if (obj.length() == 0) {
                future.complete(null)
                return@queue
            }

            val first = obj.getJSONObject(0)

            val t = first.getString("name")
            val a = first.getJSONObject("artist").getString("name")

            future.complete(TrackMatch(a, t))
        }, {
            future.complete(null)
        })

        return future
    }

    class TrackMatch(val artist: String, val title: String)
}