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
        val fut = CompletableFuture<String>()

        val obj = JsonWriter.string()
            .`object`()
            .value("provider", "youtube_titles")
            .value("type", "youtube_id")
            .value("limit", 1)
            .value("tracks", tracks)

        if ("youtube" in JukeBot.config) {
            obj.value("youtube_token", JukeBot.config["youtube"])
        }

        val jsonString = obj.end().done()

        makeRequest("/music/recommendations") {
            post(RequestBody.create(applicationJson, jsonString))
        }.thenAccept {
            val results = it.getArray("tracks")

            if (results.size == 0) {
                fut.completeExceptionally(IllegalStateException("No recommendations were returned by KSoft API"))
                return@thenAccept
            }

            val first = results.getObject(0)
            val youtube = first["youtube"]

            if (youtube is JsonObject) {
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

    fun makeRequest(endpoint: String, requestOptions: (Request.Builder.() -> Unit)? = null): CompletableFuture<JsonObject> {
        return JukeBot.httpClient
            .request {
                url(BASE_URL + endpoint)
                header("Authorization", "Bearer $key")
                requestOptions?.let {
                    apply(requestOptions)
                }
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




