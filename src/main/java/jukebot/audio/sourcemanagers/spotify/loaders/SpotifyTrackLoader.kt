package jukebot.audio.sourcemanagers.spotify.loaders

import com.sedmelluq.discord.lavaplayer.track.AudioItem
import jukebot.audio.sourcemanagers.spotify.SpotifyAudioSourceManager
import org.apache.http.HttpStatus
import org.apache.http.util.EntityUtils
import org.json.JSONObject
import java.util.regex.Matcher

class SpotifyTrackLoader : Loader {

    override fun pattern() = PLAYLIST_PATTERN

    override fun load(sourceManager: SpotifyAudioSourceManager, matcher: Matcher): AudioItem? {
        val trackId = matcher.group(1)
        val spotifyTrack = fetchTrackInfo(sourceManager, trackId)
        val trackArtists = spotifyTrack.getJSONArray("artists")
        val trackArtist = if (trackArtists.isEmpty) "" else trackArtists.getJSONObject(0).getString("name")
        val trackTitle = spotifyTrack.getString("name")

        return sourceManager.doYoutubeSearch("ytsearch:$trackArtist $trackTitle")
    }

    private fun fetchTrackInfo(sourceManager: SpotifyAudioSourceManager, trackId: String): JSONObject {
        return sourceManager.request("https://api.spotify.com/v1/tracks/$trackId") {
            addHeader("Authorization", "Bearer ${sourceManager.accessToken}")
        }.use {
            check(it.statusLine.statusCode == HttpStatus.SC_OK) {
                "Received code ${it.statusLine.statusCode} from Spotify while fetching track information"
            }

            val content = EntityUtils.toString(it.entity)
            JSONObject(content)
        }
    }

    companion object {
        private val PLAYLIST_PATTERN = "^https?://(?:open\\.)?spotify\\.com/track/([a-zA-Z0-9]+)".toPattern()
    }

}
