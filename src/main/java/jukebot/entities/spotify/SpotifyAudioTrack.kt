package jukebot.entities.spotify

import jukebot.JukeBot

class SpotifyAudioTrack(
        val artist: String,
        val title: String
) {
    fun toYoutubeAudioTrack() = JukeBot.youTubeApi.searchVideo("$title $artist")
}
