package me.devoxin.jukebot.audio.sources.spotify.loaders

import com.grack.nanojson.JsonArray
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

class SpotifyAlbumLoader : Loader {
    override val pattern = "^(?:https?://(?:open\\.)?spotify\\.com|spotify)([/:])album\\1([a-zA-Z\\d]+)".toPattern()

    override fun load(sourceManager: SpotifyAudioSourceManager, matcher: Matcher): AudioItem {
        val albumId = matcher.group(2)
        val albumInfo = fetchAlbumInfo(sourceManager, albumId)

        val trackObj = albumInfo.getObject("tracks")
        check(trackObj.has("items")) { "Album $albumId is missing track items!" }
        val trackList = trackObj.getArray("items")
        check(trackList.isNotEmpty()) { "Album $albumId track list is empty!" }

        val albumTracks = parseAlbumTracks(sourceManager, trackList, albumInfo)
        val albumName = albumInfo.getString("name")

        return BasicAudioPlaylist(albumName, albumTracks, null, false)
    }

    private fun fetchAlbumInfo(sourceManager: SpotifyAudioSourceManager, albumId: String): JsonObject {
        return sourceManager.request(HttpGet("https://api.spotify.com/v1/albums/$albumId")).use {
            check(it.statusLine.statusCode == HttpStatus.SC_OK) {
                "Received code ${it.statusLine.statusCode} from Spotify while fetching album tracks"
            }

            JsonParser.`object`().from(it.entity.content)
        }
    }

    private fun parseAlbumTracks(sourceManager: SpotifyAudioSourceManager, jsonTracks: JsonArray, album: JsonObject): List<AudioTrack> {
        val tracks = mutableListOf<AudioTrack>()

        for (jTrack in jsonTracks) {
            val track = (jTrack as JsonObject)

            if (track.getBoolean("is_local", false)) {
                continue
            }

            val title = track.getString("name")
            val artist = track.getArray("artists").getObject(0).getString("name")
            val duration = track.getLong("duration_ms")
            val identifier = track.getString("id")
            val uri = SpotifyAudioSourceManager.TRACK_URL_FORMAT.format(identifier)
            val artworkUrl = album.takeIf { it.has("images") }?.getArray("images")?.getObject(0)?.getString("url")

            tracks.add(SpotifyAudioTrack(sourceManager, AudioTrackInfo(title, artist, duration, identifier, false, uri), null, artworkUrl))
        }

        return tracks
    }
}
