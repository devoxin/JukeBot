package jukebot.entities.spotify

import jukebot.JukeBot
import org.json.JSONObject

data class SpotifyAudioTrack(
        val title: String,
        val artist: String
) {
    fun toYoutubeAudioTrack() = JukeBot.youTubeApi.search("$title $artist")

    companion object {
        fun fromJson(json: JSONObject): SpotifyAudioTrack {
            val title = json.getString("name")
            val artist = json.getJSONArray("artists").getJSONObject(0).getString("name")

            return SpotifyAudioTrack(title, artist)
        }
    }
}
