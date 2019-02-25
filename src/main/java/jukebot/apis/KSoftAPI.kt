package jukebot.apis

import jukebot.JukeBot
import jukebot.utils.createHeaders
import jukebot.utils.json
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

public class KSoftAPI(private val key: String) {

    private val baseUrl = "https://api.ksoft.si"
    private val headers = createHeaders(Pair("Authorization", "Bearer $key"))

    private val JSON = MediaType.parse("application/json")

    public fun credentialsProvided(): Boolean {
        return key.isNotEmpty()
    }

    public fun getLyrics(title: String, callback: (LyricResult?) -> Unit) {
        if (!credentialsProvided()) {
            return callback(null)
        }

        val req = Request.Builder()
                .url("$baseUrl/lyrics/search?q=$title&limit=1&clean_up=true")
                .headers(headers)
                .get()
                .build()

        JukeBot.httpClient.makeRequest(req).queue({
            val json = it.json() ?: return@queue callback(null)
            val results = json.getJSONArray("data")

            if (results.length() == 0) {
                return@queue callback(null)
            }

            val selected = results.getJSONObject(0)
            val lyrics = selected.getString("lyrics")
            val artist = selected.getString("artist")
            val track = selected.getString("name")
            val score = selected.getDouble("search_score")
            callback(LyricResult(lyrics, artist, track, score))
        }, {
            callback(null)
        })
    }

    public fun getMusicRecommendations(vararg tracks: String): CompletableFuture<TrackRecommendation?> {
        val fut = CompletableFuture<TrackRecommendation?>()

        if (!credentialsProvided()) {
            fut.complete(null)
            return fut
        }

        val obj = JSONObject()
        val trackList = JSONArray()
        tracks.forEach { trackList.put(it) }

        obj.put("provider", "youtube_ids")
        obj.put("tracks", tracks)

        val req = Request.Builder()
                .url("$baseUrl/music/recommendations")
                .headers(headers)
                .post(RequestBody.create(JSON, obj.toString()))
                .build()

        JukeBot.httpClient.makeRequest(req).queue({
            val json = it.json()

            if (json == null) {
                fut.complete(null)
                return@queue
            }

            val results = json.getJSONArray("tracks")

            if (results.length() == 0) {
                fut.complete(null)
                return@queue
            }

            val selected = results.getJSONObject(0).getJSONObject("youtube")
            val id = selected.getString("id")
            val link = selected.getString("link")
            val title = selected.getString("title")
            val thumbnail = selected.getString("thumbnail")
            val description = selected.getString("description")
            fut.complete(TrackRecommendation(id, link, title, thumbnail, description))
        }, {
            fut.complete(null)
        })

        return fut
    }

}

class TrackRecommendation(
        public val id: String,
        public val url: String,
        public val title: String,
        public val thumbnail: String,
        public val description: String
)

class LyricResult(
        public val lyrics: String,
        public val artist: String,
        public val track: String,
        public val score: Double
)
