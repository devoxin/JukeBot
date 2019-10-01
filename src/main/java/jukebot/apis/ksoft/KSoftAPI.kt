package jukebot.apis.ksoft

import jukebot.JukeBot
import jukebot.utils.json
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

class KSoftAPI(private val key: String) {
    fun getLyrics(title: String, callback: (LyricResult?) -> Unit) {
        makeRequest("/lyrics/search?q=$title&limit=1&clean_up=true").thenAccept {
            val results = it.getJSONArray("data")

            if (results.length() == 0) {
                return@thenAccept callback(null)
            }

            val selected = results.getJSONObject(0)
            val lyrics = selected.getString("lyrics")
            val artist = selected.getString("artist")
            val track = selected.getString("name")
            val score = selected.getDouble("search_score")
            callback(LyricResult(lyrics, artist, track, score))
        }.exceptionally {
            callback(null)
            return@exceptionally null
        }
    }

    fun getMusicRecommendations(vararg tracks: String): CompletableFuture<String> {
        val fut = CompletableFuture<String>()

        val obj = JSONObject()
            .put("provider", "youtube_titles")
            .put("tracks", tracks)
            .put("type", "youtube_id")
            .put("limit", 1)

        if (JukeBot.config.hasKey("youtube")) {
            obj.put("youtube_token", JukeBot.config.getString("youtube"))
        }

        makeRequest("/music/recommendations") {
            post(RequestBody.create(applicationJson, obj.toString()))
        }.thenAccept {
            val results = it.getJSONArray("tracks")

            if (results.length() == 0) {
                fut.completeExceptionally(IllegalStateException("No recommendations were returned by KSoft API"))
                return@thenAccept
            }

            val first = results.getJSONObject(0)
            val youtube = first.get("youtube")

            if (youtube is JSONObject) {
                fut.complete(youtube.getString("id"))
            } else {
                fut.complete(youtube as String)
            }
        }.exceptionally {
            fut.completeExceptionally(it)
            return@exceptionally null
        }

        return fut
    }

    fun makeRequest(endpoint: String, requestOptions: (Request.Builder.() -> Unit)? = null): CompletableFuture<JSONObject> {
        val fut = CompletableFuture<JSONObject>()

        JukeBot.httpClient.request {
            url(BASE_URL + endpoint)
            header("Authorization", "Bearer $key")
            requestOptions?.let {
                apply(requestOptions)
            }
        }.queue({
            JukeBot.LOG.debug("Response from KSoft API: code=${it.code()} message=${it.message()}")
            val j = it.json()

            if (j == null) {
                fut.completeExceptionally(Error("Expected json response, got null!"))
                return@queue
            }

            fut.complete(j)
        }, {
            fut.completeExceptionally(it)
        })

        return fut
    }

    companion object {
        private const val BASE_URL = "https://api.ksoft.si"
        private val applicationJson = MediaType.parse("application/json")
    }

}




