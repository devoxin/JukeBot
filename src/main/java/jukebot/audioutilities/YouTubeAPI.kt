package jukebot.audioutilities

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
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
                .addHeader("User-Agent", "JukeBot/v6.2 (https://www.jukebot.xyz)")
                .get()
                .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body()

                if (body == null) {
                    callback(null)
                } else {
                    val json = JSONObject(body.string())
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
                        callback(toYouTubeAudioTrack(videoId, title, uploader, isStream, duration))
                    }
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