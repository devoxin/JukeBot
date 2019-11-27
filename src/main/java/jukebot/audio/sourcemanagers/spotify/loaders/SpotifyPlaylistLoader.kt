package jukebot.audio.sourcemanagers.spotify.loaders

import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist
import jukebot.JukeBot
import jukebot.audio.sourcemanagers.spotify.SpotifyAudioSourceManager
import org.apache.http.HttpStatus
import org.apache.http.util.EntityUtils
import org.json.JSONObject
import java.util.concurrent.CompletableFuture
import java.util.regex.Matcher

class SpotifyPlaylistLoader : Loader {

    override fun pattern() = PLAYLIST_PATTERN

    override fun load(sourceManager: SpotifyAudioSourceManager, matcher: Matcher): AudioItem {
        val playlistId = matcher.group(1)
        val playlistInfo = fetchPlaylistInfo(sourceManager, playlistId)
        val playlistTracks = fetchPlaylistTracks(sourceManager, playlistId)
        val playlistName = playlistInfo.optString("name")

        return BasicAudioPlaylist(playlistName, playlistTracks, null, false)
    }

    private fun fetchPlaylistInfo(sourceManager: SpotifyAudioSourceManager, playlistId: String): JSONObject {
        return sourceManager.request("https://api.spotify.com/v1/playlists/$playlistId") {
            addHeader("Authorization", "Bearer ${sourceManager.accessToken}")
        }.use {
            check(it.statusLine.statusCode == HttpStatus.SC_OK) { "Received code ${it.statusLine.statusCode} while fetching playlist information" }

            val content = EntityUtils.toString(it.entity)
            JSONObject(content)
        }
    }

    private fun fetchPlaylistTracks(sourceManager: SpotifyAudioSourceManager, playlistId: String): List<AudioTrack> {
        return sourceManager.request("https://api.spotify.com/v1/playlists/$playlistId/tracks") {
            addHeader("Authorization", "Bearer ${sourceManager.accessToken}")
        }.use {
            check(it.statusLine.statusCode == HttpStatus.SC_OK) { "Received code ${it.statusLine.statusCode} while fetching playlist tracks" }

            val content = EntityUtils.toString(it.entity)
            val json = JSONObject(content)

            if (!json.has("items")) {
                return emptyList()
            }

            val jsonTracks = json.getJSONArray("items")
            val tasks = mutableListOf<CompletableFuture<AudioTrack>>()

            for (jTrack in jsonTracks) {
                val track = (jTrack as JSONObject).getJSONObject("track")
                val title = track.getString("name")
                val artist = track.getJSONArray("artists").getJSONObject(0).getString("name")
                tasks.add(JukeBot.youTubeApi.search("$title $artist"))
            }

            try {
                CompletableFuture.allOf(*tasks.toTypedArray()).get()
            } catch (ignored: Exception) {
            }

            tasks.filterNot { t -> t.isCompletedExceptionally }
                .map { t -> t.get() }
        }
    }

    companion object {
        private val PLAYLIST_PATTERN = "^https?://(?:open\\.)?spotify\\.com/(?:user/[a-zA-Z0-9_]+/)?playlist/([a-zA-Z0-9]+).*".toPattern()
    }

}