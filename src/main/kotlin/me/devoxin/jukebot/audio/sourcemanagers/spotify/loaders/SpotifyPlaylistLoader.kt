package me.devoxin.jukebot.audio.sourcemanagers.spotify.loaders

import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist
import me.devoxin.jukebot.audio.sourcemanagers.spotify.SpotifyAudioSourceManager
import org.apache.http.HttpStatus
import java.util.concurrent.CompletableFuture
import java.util.regex.Matcher

class SpotifyPlaylistLoader : Loader {
    override val pattern = "^(?:$URL_PATTERN|spotify)([/:])playlist\\1([a-zA-Z\\d]+)".toPattern()

    override fun load(sourceManager: SpotifyAudioSourceManager, matcher: Matcher): AudioItem? {
        val playlistId = matcher.group(2)
        val playlistInfo = fetchPlaylistInfo(sourceManager, playlistId)
        val playlistTracks = fetchPlaylistTracks(sourceManager, playlistId)
        val playlistName = playlistInfo.getString("name")

        return BasicAudioPlaylist(playlistName, playlistTracks, null, false)
    }

    private fun fetchPlaylistInfo(sourceManager: SpotifyAudioSourceManager, playlistId: String): JsonObject {
        return sourceManager.request("https://api.spotify.com/v1/playlists/$playlistId") {
            addHeader("Authorization", "Bearer ${sourceManager.accessToken}")
        }.use {
            check(it.statusLine.statusCode == HttpStatus.SC_OK) {
                "Received code ${it.statusLine.statusCode} from Spotify while fetching playlist information"
            }

            JsonParser.`object`().from(it.entity.content)
        }
    }

    private fun fetchPlaylistTracks(sourceManager: SpotifyAudioSourceManager, playlistId: String): List<AudioTrack> {
        return sourceManager.request("https://api.spotify.com/v1/playlists/$playlistId/tracks") {
            addHeader("Authorization", "Bearer ${sourceManager.accessToken}")
        }.use {
            check(it.statusLine.statusCode == HttpStatus.SC_OK) {
                "Received code ${it.statusLine.statusCode} from Spotify while fetching playlist tracks"
            }

            val json = JsonParser.`object`().from(it.entity.content)

            if (!json.has("items")) {
                return emptyList()
            }

            val jsonTracks = json.getArray("items")
            val tasks = mutableListOf<CompletableFuture<AudioTrack>>()

            for (trackObj in jsonTracks) {
                val trackJ = trackObj as JsonObject

                if (trackJ.isNull("track")) {
                    continue
                }

                val track = trackJ.getObject("track")
                //val title = track.getString("name")
                //val artist = track.getArray("artists").getObject(0).getString("name")
                val isrcId = track.getObject("external_ids").getString("isrc")
                val task = sourceManager.queueAlternateSearch("\"$isrcId\"")
                    .thenApply { ai -> if (ai is AudioPlaylist) ai.tracks.first() else ai as AudioTrack }
                tasks.add(task)
            }

            try {
                CompletableFuture.allOf(*tasks.toTypedArray()).get()
            } catch (ignored: Exception) {
            }

            tasks.filterNot { t -> t.isCompletedExceptionally }.map { t -> t.get() }
        }
    }

    companion object {
        //private val URL_PATTERN = "^https?://(?:open\\.)?spotify\\.com/(?:user/[a-zA-Z0-9_]+/)?playlist/([a-zA-Z0-9]+).*".toPattern()
        private const val URL_PATTERN = "https?://(?:open\\.)?spotify\\.com(?:/user/[a-zA-Z0-9_]+)?"
    }
}
