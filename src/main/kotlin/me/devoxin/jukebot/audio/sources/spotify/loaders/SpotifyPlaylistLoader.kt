package me.devoxin.jukebot.audio.sources.spotify.loaders

import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist
import me.devoxin.jukebot.audio.sources.spotify.SpotifyAudioSourceManager
import me.devoxin.jukebot.audio.sources.spotify.SpotifyAudioTrack
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import java.util.regex.Matcher

class SpotifyPlaylistLoader : Loader {
    override val pattern = "^(?:$URL_PATTERN|spotify)([/:])playlist\\1([a-zA-Z\\d]+)".toPattern()

    override fun load(sourceManager: SpotifyAudioSourceManager, matcher: Matcher): AudioItem {
        val playlistId = matcher.group(2)
        val playlistInfo = fetchPlaylistInfo(sourceManager, playlistId)
        val playlistTracks = fetchPlaylistTracks(sourceManager, playlistId)
        val playlistName = playlistInfo.getString("name")

        return BasicAudioPlaylist(playlistName, playlistTracks, null, false)
    }

    private fun fetchPlaylistInfo(sourceManager: SpotifyAudioSourceManager, playlistId: String): JsonObject {
        return sourceManager.request(HttpGet("https://api.spotify.com/v1/playlists/$playlistId")).use {
            check(it.statusLine.statusCode == HttpStatus.SC_OK) {
                "Received code ${it.statusLine.statusCode} from Spotify while fetching playlist information"
            }

            JsonParser.`object`().from(it.entity.content)
        }
    }

    private fun fetchPlaylistTracks(sourceManager: SpotifyAudioSourceManager, playlistId: String): List<AudioTrack> {
        return sourceManager.request(HttpGet("https://api.spotify.com/v1/playlists/$playlistId/tracks")).use {
            check(it.statusLine.statusCode == HttpStatus.SC_OK) {
                "Received code ${it.statusLine.statusCode} from Spotify while fetching playlist tracks"
            }

            val json = JsonParser.`object`().from(it.entity.content)

            if (!json.has("items")) {
                return emptyList()
            }

            val jsonTracks = json.getArray("items")
            val tracks = mutableListOf<AudioTrack>()

            for (trackObj in jsonTracks) {
                val trackJ = trackObj as JsonObject

                if (trackJ.isNull("track")) {
                    continue
                }

                val track = trackJ.getObject("track")

                if (track.getBoolean("is_local", false)) {
                    continue
                }

                val title = track.getString("name")
                val artist = track.getArray("artists").getObject(0).getString("name")
                val duration = track.getLong("duration_ms")
                val identifier = track.getString("id")
                val uri = SpotifyAudioSourceManager.TRACK_URL_FORMAT.format(identifier)
                val isrc = track.getObject("external_ids").getString("isrc")
                val artworkUrl = track.getObject("album").takeIf { obj -> obj.has("images") }?.getArray("images")?.getObject(0)?.getString("url")

                tracks.add(SpotifyAudioTrack(sourceManager, AudioTrackInfo(title, artist, duration, identifier, false, uri), isrc, artworkUrl))
            }

            tracks
        }
    }

    companion object {
        //private val URL_PATTERN = "^https?://(?:open\\.)?spotify\\.com/(?:user/[a-zA-Z0-9_]+/)?playlist/([a-zA-Z0-9]+).*".toPattern()
        private const val URL_PATTERN = "https?://(?:open\\.)?spotify\\.com(?:/user/[a-zA-Z0-9_]+)?"
    }
}
