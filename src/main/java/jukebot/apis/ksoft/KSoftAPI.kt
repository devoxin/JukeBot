package jukebot.apis.ksoft

import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonWriter
import jukebot.JukeBot
import jukebot.utils.json
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import java.util.concurrent.CompletableFuture

class KSoftAPI(private val key: String) {
    fun getMusicRecommendations(vararg tracks: String): CompletableFuture<String> {
        val obj = JsonWriter.string()
            .`object`()
            .value("provider", "youtube_titles")
            .value("recommend_type", "youtube_id")
            .value("limit", 1)
            .value("tracks", tracks)

        val jsonString = obj.end().done()

        return makeRequest("/music/recommendations") { post(RequestBody.create(applicationJson, jsonString)) }
            .thenApply {
                val results = it.getArray("tracks")

                if (results.size == 0) {
                    throw IllegalStateException("No recommendations were returned by KSoft API")
                }

                val first = results.getObject(0)
                val youtube = first["youtube"]

                if (youtube is String) youtube else (youtube as JsonObject).getString("id")
            }
    }

    fun makeRequest(endpoint: String, requestOptions: (Request.Builder.() -> Unit)? = null): CompletableFuture<JsonObject> {
        return JukeBot.httpClient
            .request {
                url(BASE_URL + endpoint)
                header("Authorization", "Bearer $key")
                requestOptions?.let { this.apply(it) }
            }
            .submit()
            .thenApply {
                it.json()
                    ?: throw IllegalStateException("KSoft.Si didn't respond with valid status code and/or JSON, " +
                        "code=${it.code()}, message=${it.message()}")
            }
    }

    companion object {
        private const val BASE_URL = "https://api.ksoft.si"
        private val applicationJson = MediaType.parse("application/json")
    }
}
