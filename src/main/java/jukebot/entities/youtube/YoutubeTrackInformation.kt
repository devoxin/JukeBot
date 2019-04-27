package jukebot.entities.youtube

import org.json.JSONObject
import org.jsoup.Jsoup
import java.time.Duration

data class YoutubeTrackInformation(
        val videoId: String,
        val title: String,
        val uploader: String,
        val isStream: Boolean,
        val duration: Long
) {

    companion object {
        fun fromJson(json: JSONObject): YoutubeTrackInformation {
            val snippet = json.getJSONObject("snippet")
            val garbageTitle = snippet.getString("title")

            val videoId = json.getString("id")
            val title = Jsoup.parse(garbageTitle).text()
            val uploader = snippet.getString("channelTitle")
            val isStream = snippet.getString("liveBroadcastContent") != "none"
            val duration = if (isStream) {
                Long.MAX_VALUE
            } else {
                Duration.parse(json.getJSONObject("contentDetails").getString("duration")).toMillis()
            }

            return YoutubeTrackInformation(videoId, title, uploader, isStream, duration)
        }
    }

}
