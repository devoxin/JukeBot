package jukebot.audio.sourcemanagers.spotify.loaders

import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import jukebot.audio.sourcemanagers.spotify.SpotifyAudioSourceManager
import org.apache.http.HttpStatus
import java.util.regex.Matcher

class SpotifyTrackLoader : Loader {
    override fun pattern() = TRACK_PATTERN

    override fun load(sourceManager: SpotifyAudioSourceManager, matcher: Matcher): AudioItem? {
        val trackId = matcher.group(2)
        val spotifyTrack = fetchTrackInfo(sourceManager, trackId)
        val trackArtists = spotifyTrack.getArray("artists")
        //val trackArtist = if (trackArtists.isEmpty()) "" else trackArtists.getObject(0).getString("name")
        //val trackTitle = spotifyTrack.getString("name")
        val isrcId = spotifyTrack.getObject("external_ids").getString("isrc")

        return sourceManager.doYoutubeSearch("ytsearch:\"$isrcId\"")
    }

    private fun fetchTrackInfo(sourceManager: SpotifyAudioSourceManager, trackId: String): JsonObject {
        return sourceManager.request("https://api.spotify.com/v1/tracks/$trackId") {
            addHeader("Authorization", "Bearer ${sourceManager.accessToken}")
        }.use {
            check(it.statusLine.statusCode == HttpStatus.SC_OK) {
                "Received code ${it.statusLine.statusCode} from Spotify while fetching track information"
            }

            JsonParser.`object`().from(it.entity.content)
        }
    }

    companion object {
        private val TRACK_PATTERN = "^(?:https?://(?:open\\.)?spotify\\.com|spotify)([/:])track\\1([a-zA-Z0-9]+)".toPattern()
    }
}
