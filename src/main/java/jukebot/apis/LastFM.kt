package jukebot.apis

import jukebot.JukeBot
import jukebot.utils.json
import okhttp3.Request
import org.apache.http.client.utils.URIBuilder
import java.util.concurrent.CompletableFuture


class LastFM(private val key: String) {

    private val baseUrl = "http://ws.audioscrobbler.com/2.0"

    fun findSimilar(title: String, artist: String): CompletableFuture<List<TrackMatch>?> {
        val url = URIBuilder("$baseUrl/?method=track.getsimilar&api_key=$key&format=json")

        url.addParameter("track", title)
        url.addParameter("artist", artist)

        val req = Request.Builder()
                .url(url.build().toURL())
                .get()
                .build()

        val future = CompletableFuture<List<TrackMatch>?>()

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

            val tracks = mutableListOf<TrackMatch>()

            for (i in 0..Math.min(3, obj.length())) {
                val tr = obj.getJSONObject(i)

                val t = tr.getString("name")
                val a = tr.getJSONObject("artist").getString("name")
                tracks.add(TrackMatch(a, t))
            }

            future.complete(tracks)
        }, {
            future.complete(null)
        })

        return future
    }

    class TrackMatch(val artist: String, val title: String)
}