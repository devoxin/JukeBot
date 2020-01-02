package jukebot.apis

import jukebot.JukeBot
import jukebot.utils.json
import org.apache.http.client.utils.URIBuilder
import java.util.concurrent.CompletableFuture
import kotlin.math.min

class LastFM(private val key: String) {
    private val baseUrl = "http://ws.audioscrobbler.com/2.0"

    fun findSimilar(title: String, artist: String): CompletableFuture<List<TrackMatch>> {
        val url = URIBuilder("$baseUrl/?method=track.getsimilar&api_key=$key&format=json")
            .addParameter("track", title)
            .addParameter("artist", artist)

        val future = CompletableFuture<List<TrackMatch>?>()

        return JukeBot.httpClient.request { url(url.build().toURL()) }
            .submit()
            .thenApply {
                it.json() ?: throw IllegalStateException("Response was not successful, or was not a json object!")
            }
            .thenApply {
                val tracks = mutableListOf<TrackMatch>()
                val obj = it.getJSONObject("similartracks").getJSONArray("track")

                if (obj.length() > 0) {
                    for (i in 0..min(3, obj.length())) {
                        val tr = obj.getJSONObject(i)

                        val t = tr.getString("name")
                        val a = tr.getJSONObject("artist").getString("name")
                        tracks.add(TrackMatch(a, t))
                    }
                }

                tracks
            }
    }

    inner class TrackMatch(val artist: String, val title: String)
}
