package jukebot.apis.youtube

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import jukebot.JukeBot
import jukebot.utils.json
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.CompletableFuture

class YouTubeAPI(private val key: String, private val source: YoutubeAudioSourceManager) {

    private val baseUrl = HttpUrl.parse("https://www.googleapis.com/youtube/v3")!!
    private val httpClient = OkHttpClient()

    fun search(query: String): CompletableFuture<AudioTrack> {
        val fut = CompletableFuture<AudioTrack>()

        val url = baseUrl.newBuilder()
            .addPathSegment("search")
            .setQueryParameter("q", query)
            .setQueryParameter("key", key)
            .setQueryParameter("type", "video")
            .setQueryParameter("maxResults", "3")
            .setQueryParameter("part", "id")
            .build()

        JukeBot.httpClient.request { url(url) }.submit()
            .thenApply { it.json() ?: throw IllegalStateException("YouTube API did not respond with a JSON object") }
            .thenApply { it.getJSONArray("items").getJSONObject(0).getJSONObject("id").getString("videoId") }
            .thenCompose { getVideoInfo(it) }
            .thenAccept { fut.complete(it) }
            .exceptionally {
                fut.completeExceptionally(it)
                return@exceptionally null
            }

        return fut
    }

    fun getVideoInfo(id: String): CompletableFuture<AudioTrack> {
        val request = Request.Builder()
            .url("https://www.googleapis.com/youtube/v3/videos?id=$id&key=$key&type=video&maxResults=3&part=snippet,contentDetails")
            .addHeader("User-Agent", "JukeBot/v${JukeBot.VERSION} (https://www.jukebot.serux.pro)")
            .get()
            .build()

        val fut = CompletableFuture<AudioTrack>()

        makeRequest(request)
            .thenAccept {
                val results = it.getJSONArray("items")

                if (results.length() == 0) {
                    fut.completeExceptionally(Exception("No tracks found related to the given query"))
                    return@thenAccept
                }

                val yti = YoutubeTrackInformation.fromJson(results.getJSONObject(0))
                fut.complete(toYouTubeAudioTrack(yti))
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
        val trackInfo = AudioTrackInfo(title, uploader, duration, videoId, isStream, "https://www.youtube.com/watch?v=$videoId")
        return YoutubeAudioTrack(trackInfo, source)
    }

}