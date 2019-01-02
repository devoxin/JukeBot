package jukebot.apis

import jukebot.JukeBot
import jukebot.utils.createHeaders
import jukebot.utils.json
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject

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

    public fun getMusicRecommendations(vararg tracks: String, callback: (TrackRecommendation?) -> Unit) {
        if (!credentialsProvided()) {
            return callback(null)
        }

        val obj = JSONObject()
        val trackList = JSONArray()
        tracks.forEach { trackList.put(it) }

        obj.put("provider", "spotify")
        obj.put("tracks", tracks)

        val req = Request.Builder()
                .url("$baseUrl/music/recommendations")
                .headers(headers)
                .post(RequestBody.create(JSON, obj.toString()))
                .build()

        println(obj.toString())

        JukeBot.httpClient.makeRequest(req).queue({
            println(it.code())
            val json = it.json() ?: return@queue callback(null)
            val results = json.getJSONArray("tracks")

            println(results.toString())

            if (results.length() == 0) {
                println("THIS BITCH EMPTY, YEET")
                return@queue callback(null)
            }

            val selected = results.getJSONObject(0).getJSONObject("youtube")
            val id = selected.getString("id")
            val link = selected.getString("link")
            val title = selected.getString("title")
            val thumbnail = selected.getString("thumbnail")
            val description = selected.getString("description")
            callback(TrackRecommendation(id, link, title, thumbnail, description))
        }, {
            callback(null)
        })
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