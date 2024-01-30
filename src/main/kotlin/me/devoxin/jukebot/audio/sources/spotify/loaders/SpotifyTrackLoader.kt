package me.devoxin.jukebot.audio.sources.spotify.loaders

import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import me.devoxin.jukebot.audio.sources.spotify.SpotifyAudioSourceManager
import me.devoxin.jukebot.audio.sources.spotify.SpotifyAudioTrack
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import java.util.regex.Matcher

class SpotifyTrackLoader : Loader {
    override val pattern = "^(?:https?://(?:open\\.)?spotify\\.com|spotify)([/:])track\\1([a-zA-Z\\d]+)".toPattern()

    override fun load(sourceManager: SpotifyAudioSourceManager, matcher: Matcher): AudioItem {
        val trackId = matcher.group(2)
        val track = fetchTrackInfo(sourceManager, trackId)

        val title = track.getString("name")
        val artist = track.getArray("artists").getObject(0).getString("name")
        val duration = track.getLong("duration_ms")
        val identifier = track.getString("id")
        val uri = SpotifyAudioSourceManager.TRACK_URL_FORMAT.format(identifier)
        val isrc = track.getObject("external_ids").getString("isrc")
        val artworkUrl = track.getObject("album").takeIf { obj -> obj.has("images") }?.getArray("images")?.getObject(0)?.getString("url")

        return SpotifyAudioTrack(sourceManager, AudioTrackInfo(title, artist, duration, identifier, false, uri), isrc, artworkUrl)
    }

    private fun fetchTrackInfo(sourceManager: SpotifyAudioSourceManager, trackId: String): JsonObject {
        return sourceManager.request(HttpGet("https://api.spotify.com/v1/tracks/$trackId")).use {
            check(it.statusLine.statusCode == HttpStatus.SC_OK) {
                "Received code ${it.statusLine.statusCode} from Spotify while fetching track information"
            }

            JsonParser.`object`().from(it.entity.content)
        }
    }
}
