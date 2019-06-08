package jukebot.apis

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import jukebot.JukeBot
import jukebot.entities.youtube.YoutubeTrackInformation
import jukebot.utils.json
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.CompletableFuture

class YouTubeAPI(private val key: String, private val source: YoutubeAudioSourceManager) {

    private val httpClient = OkHttpClient()

    fun search(query: String): CompletableFuture<AudioTrack> {
        val request = Request.Builder()
                .url("https://www.googleapis.com/youtube/v3/search?q=$query&key=$key&type=video&maxResults=3&part=id")
                .addHeader("User-Agent", "JukeBot/v${JukeBot.VERSION} (https://www.jukebot.serux.pro)")
                .get()
                .build()

        val fut = CompletableFuture<AudioTrack>()

        makeRequest(request)
                .thenApply { it.getJSONArray("items").getJSONObject(0).getJSONObject("id").getString("videoId") }
                .thenApply { getVideoInfo(it) }
                .thenAccept { yti ->
                    yti.thenApply { toYouTubeAudioTrack(it) }
                            .thenAccept { fut.complete(it) }
                }
                .exceptionally {
                    fut.completeExceptionally(it)
                    return@exceptionally null
                }

        return fut
    }

    fun getVideoInfo(id: String): CompletableFuture<YoutubeTrackInformation> {
        val request = Request.Builder()
                .url("https://www.googleapis.com/youtube/v3/videos?id=$id&key=$key&type=video&maxResults=3&part=snippet,contentDetails")
                .addHeader("User-Agent", "JukeBot/v${JukeBot.VERSION} (https://www.jukebot.serux.pro)")
                .get()
                .build()

        val fut = CompletableFuture<YoutubeTrackInformation>()

        makeRequest(request)
                .thenAccept {
                    val results = it.getJSONArray("items")

                    if (results.length() == 0) {
                        fut.completeExceptionally(Exception("No tracks found related to the given query"))
                        return@thenAccept
                    }

                    val yti = YoutubeTrackInformation.fromJson(results.getJSONObject(0))
                    fut.complete(yti)
                }
                .exceptionally {
                    fut.completeExceptionally(it)
                    return@exceptionally null
                }

        return fut
    }

    fun makeRequest(request: Request): CompletableFuture<JSONObject> {
        val fut = CompletableFuture<JSONObject>()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                JukeBot.LOG.error("YouTube API request failed!", e)
                fut.completeExceptionally(e)
            }

            override fun onResponse(call: Call, response: Response) {
                JukeBot.LOG.debug("YouTube API response:\n\tStatus Code: ${response.code()}\n\tResponse Message: ${response.message()}")

                val json = response.json()

                if (json == null) {
                    fut.completeExceptionally(Exception("Expected JSON response, got null!"))
                    return
                }

                fut.complete(json)
            }
        })

        return fut
    }

    private fun toYouTubeAudioTrack(trackInformation: YoutubeTrackInformation): YoutubeAudioTrack {
        val (videoId, title, uploader, isStream, duration) = trackInformation
        return source.buildTrackObject(videoId, title, uploader, isStream, duration)
    }

}