package jukebot.apis

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import jukebot.JukeBot
import jukebot.utils.json
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class YouTubeAPI(private val key: String, private val source: YoutubeAudioSourceManager) {

    private val httpClient = OkHttpClient()

    fun searchVideo(query: String): CompletableFuture<AudioTrack> {
        val request = Request.Builder()
                .url("https://www.googleapis.com/youtube/v3/search?q=$query&key=$key&type=video&maxResults=3&part=id,snippet")
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

                    val result = results.getJSONObject(0)
                    val videoId = result.getJSONObject("id").getString("videoId")
                    val title = result.getJSONObject("snippet").getString("title")
                    val uploader = result.getJSONObject("snippet").getString("channelTitle")
                    val isStream = result.getJSONObject("snippet").getString("liveBroadcastContent") != "none"
                    val duration = if (isStream) Long.MAX_VALUE else Long.MIN_VALUE // TODO: Stuffs

                    JukeBot.LOG.debug("Found YouTubeTrack for identifier $query")
                    fut.complete(toYouTubeAudioTrack(videoId, title, uploader, isStream, duration))
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

    private fun toYouTubeAudioTrack(videoId: String, title: String, uploader: String, isStream: Boolean, duration: Long): YoutubeAudioTrack {
        return source.buildTrackObject(videoId, title, uploader, isStream, duration)
    }

    private fun ISO8601toMillis(duration: String): Long {
        return Long.MAX_VALUE // TODO: Fix
    }

}