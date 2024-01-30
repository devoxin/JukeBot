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

class SpotifyRecommendationsLoader : Loader {
    override val pattern = "sprec:(.+)".toPattern()

    override fun load(sourceManager: SpotifyAudioSourceManager, matcher: Matcher): AudioItem {
        val seedTracks = matcher.group(1).split(',').toSet()
        val searchResult = fetchRecommendations(sourceManager, seedTracks.take(5))

        val trackObj = searchResult.getArray("tracks")

        if (trackObj.isEmpty()) {
            return AudioReference.NO_TRACK
        }

        return BasicAudioPlaylist("Recommended Tracks", parseTrackList(sourceManager, trackObj), null, true)
    }

    private fun parseTrackList(sourceManager: SpotifyAudioSourceManager, trackList: JsonArray): List<AudioTrack> {
        val tracks = mutableListOf<AudioTrack>()

        for (jTrack in trackList) {
            val track = (jTrack as JsonObject)

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

    private fun fetchRecommendations(sourceManager: SpotifyAudioSourceManager, seedTracks: List<String>): JsonObject {
        val uri = URIBuilder("https://api.spotify.com/v1/recommendations")
            .addParameter("seed_tracks", seedTracks.joinToString(","))
            .addParameter("market", "US")
            .addParameter("limit", "1")
            .build()

        return sourceManager.request(HttpGet(uri)).use {
            check(it.statusLine.statusCode == HttpStatus.SC_OK) {
                "Received code ${it.statusLine.statusCode} from Spotify while fetching recommendations"
            }

            JsonParser.`object`().from(it.entity.content)
        }
    }
}
