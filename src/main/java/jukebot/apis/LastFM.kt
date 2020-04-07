package jukebot.apis

import jukebot.JukeBot
import jukebot.utils.json
import okhttp3.HttpUrl
import java.util.concurrent.CompletableFuture
import kotlin.math.min

class LastFM(private val key: String) {
    private val baseUrl = HttpUrl.get("http://ws.audioscrobbler.com/2.0")

    fun findSimilar(title: String, artist: String): CompletableFuture<List<TrackMatch>> {
        val url = baseUrl.newBuilder()
            .setQueryParameter("method", "track.getsimilar")
            .setQueryParameter("api_key", key)
            .setQueryParameter("format", "json")
            .setQueryParameter("track", title)
            .setQueryParameter("artist", artist)
            .build()

        return JukeBot.httpClient.request { url(url) }
            .submit()
            .thenApply {
                it.json() ?: throw IllegalStateException("Response was not successful, or was not a json object!")
            }
            .thenApply {
                val tracks = mutableListOf<TrackMatch>()
                val obj = it.getObject("similartracks").getArray("track")

                if (obj.size > 0) {
                    for (i in 0..min(3, obj.size)) {
                        val tr = obj.getObject(i)

                        val t = tr.getString("name")
                        val a = tr.getObject("artist").getString("name")
                        tracks.add(TrackMatch(a, t))
                    }
                }

                tracks
            }
    }

    inner class TrackMatch(val artist: String, val title: String)
}
