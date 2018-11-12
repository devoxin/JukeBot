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

    public fun searchVideo(query: String, callback: (AudioTrack?) -> Unit) {
        val request = Request.Builder()
                .url("https://www.googleapis.com/youtube/v3/search?q=$query&key=$key&type=video&maxResults=3&part=id,snippet")
                .addHeader("User-Agent", "JukeBot/v${JukeBot.VERSION} (https://www.jukebot.serux.pro)")
                .get()
                .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                JukeBot.LOG.error("YouTube API request failed!", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                JukeBot.LOG.debug("YouTube API response:\n\tStatus Code: ${response.code()}\n\tResponse Message: ${response.message()}")

                val json = response.json() ?: return callback(null)

                val results = json.getJSONArray("items")

                if (results.length() == 0) {
                    callback(null)
                } else {
                    val result: JSONObject = results.getJSONObject(0)
                    val videoId: String = result.getJSONObject("id").getString("videoId")
                    val title: String = result.getJSONObject("snippet").getString("title")
                    val uploader: String = result.getJSONObject("snippet").getString("channelTitle")
                    val isStream: Boolean = result.getJSONObject("snippet").getString("liveBroadcastContent") != "none"
                    val duration: Long = if (isStream) Long.MAX_VALUE else Long.MIN_VALUE // TODO: Stuffs

                    JukeBot.LOG.debug("Found YouTubeTrack for identifier $query")
                    callback(toYouTubeAudioTrack(videoId, title, uploader, isStream, duration))
                }
            }
        })
    }

    public fun searchVideoBlocking(query: String): AudioTrack? {
        val promise = CompletableFuture<AudioTrack?>()

        searchVideo(query) {
            promise.complete(it)
        }

        return promise.get(15, TimeUnit.SECONDS)
    }

    private fun toYouTubeAudioTrack(videoId: String, title: String, uploader: String, isStream: Boolean, duration: Long): YoutubeAudioTrack {
        return source.buildTrackObject(videoId, title, uploader, isStream, duration)
    }

    private fun ISO8601toMillis(duration: String): Long {
        return Long.MAX_VALUE // TODO: Fix
    }

}