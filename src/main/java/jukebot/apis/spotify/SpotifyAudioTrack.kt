package jukebot.apis.spotify

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import jukebot.JukeBot
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

data class SpotifyAudioTrack(
    val title: String,
    val artist: String
) {
    fun toYoutubeAudioTrack(): CompletableFuture<AudioTrack> {
        val append = if (title.contains("remix")) "" else "-remix"
        return JukeBot.youTubeApi.search("$title $artist $append")
    }

    companion object {
        fun fromJson(json: JSONObject): SpotifyAudioTrack {
            val title = json.getString("name")
            val artist = json.getJSONArray("artists").getJSONObject(0).getString("name")

            return SpotifyAudioTrack(title, artist)
        }
    }
}
