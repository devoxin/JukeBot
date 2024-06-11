package me.devoxin.jukebot.audio.sources.spotify.loaders

import com.grack.nanojson.JsonArray
import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import com.sedmelluq.discord.lavaplayer.track.*
import me.devoxin.jukebot.audio.sources.spotify.SpotifyAudioSourceManager
import me.devoxin.jukebot.audio.sources.spotify.SpotifyAudioTrack
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import java.util.regex.Matcher

class SpotifySearchLoader : Loader {
    override val pattern = "spsearch:(.+)".toPattern()

    override fun load(sourceManager: SpotifyAudioSourceManager, matcher: Matcher): AudioItem {
        val query = matcher.group(1)

        if (query.startsWith("http://") || query.startsWith("https://")) {
            return AudioReference(query, null)
        }

        if (query.trim().isEmpty()) {
            return AudioReference.NO_TRACK
        }

        val searchResult = fetchTracksFromSearch(sourceManager, query)

        val trackObj = searchResult.getObject("tracks")

        if (!trackObj.has("items")) {
            return AudioReference.NO_TRACK
        }

        val trackList = trackObj.getArray("items")

        if (trackList.isEmpty()) {
            return AudioReference.NO_TRACK
        }

        return BasicAudioPlaylist("Search results for $query", parseTrackList(sourceManager, trackList), null, true)
    }

    private fun parseTrackList(sourceManager: SpotifyAudioSourceManager, trackList: JsonArray): List<AudioTrack> {
        val tracks = mutableListOf<AudioTrack>()

        for (jTrack in trackList) {
            val track = jTrack as? JsonObject ?: continue

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

            tracks.add(SpotifyAudioTrack(sourceManager,  AudioTrackInfo(title, artist, duration, identifier, false, uri), isrc, artworkUrl))
        }

        return tracks
    }

    private fun fetchTracksFromSearch(sourceManager: SpotifyAudioSourceManager, query: String): JsonObject {
        val uri = URIBuilder("https://api.spotify.com/v1/search")
            .addParameter("q", query.take(100))
            .addParameter("type", "track")
            .addParameter("limit", "10")
            .build()

        return sourceManager.request(HttpGet(uri)).use {
            check(it.statusLine.statusCode == HttpStatus.SC_OK) {
                "Received code ${it.statusLine.statusCode} from Spotify while fetching search tracks"
            }

            JsonParser.`object`().from(it.entity.content)
        }
    }
}
