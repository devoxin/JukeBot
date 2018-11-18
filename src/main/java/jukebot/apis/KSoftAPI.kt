package jukebot.apis

import jukebot.JukeBot
import jukebot.utils.createHeaders
import jukebot.utils.json
import okhttp3.Request

public class KSoftAPI(private val key: String) {

    private val baseUrl = "https://api.ksoft.si"

    public fun credentialsProvided(): Boolean {
        return key.isNotEmpty()
    }

    public fun getLyrics(title: String, callback: (LyricResult?) -> Unit) {
        if (!credentialsProvided()) {
            return callback(null)
        }

        val req = Request.Builder()
                .url("$baseUrl/lyrics/search?q=$title&limit=1&clean_up=true")
                .headers(createHeaders(Pair("Authorization", "Bearer $key")))
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

}

class LyricResult(
        public val lyrics: String,
        public val artist: String,
        public val track: String,
        public val score: Double
)