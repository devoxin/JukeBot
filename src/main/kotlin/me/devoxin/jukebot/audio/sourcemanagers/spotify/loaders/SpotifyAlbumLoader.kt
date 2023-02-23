package me.devoxin.jukebot.audio.sourcemanagers.spotify.loaders

import com.grack.nanojson.JsonArray
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

class SpotifyAlbumLoader : Loader {
    override val pattern = "^(?:https?://(?:open\\.)?spotify\\.com|spotify)([/:])album\\1([a-zA-Z\\d]+)".toPattern()

    override fun load(sourceManager: SpotifyAudioSourceManager, matcher: Matcher): AudioItem {
        val albumId = matcher.group(2)
        val albumInfo = fetchAlbumInfo(sourceManager, albumId)

        val trackObj = albumInfo.getObject("tracks")
        check(trackObj.has("items")) { "Album $albumId is missing track items!" }
        val trackList = trackObj.getArray("items")
        check(trackList.isNotEmpty()) { "Album $albumId track list is empty!" }

        val albumTracks = fetchAlbumTracks(sourceManager, trackList)
        val albumName = albumInfo.getString("name")

        return BasicAudioPlaylist(albumName, albumTracks, null, false)
    }

    private fun fetchAlbumInfo(sourceManager: SpotifyAudioSourceManager, albumId: String): JsonObject {
        return sourceManager.request("https://api.spotify.com/v1/albums/$albumId") {
            addHeader("Authorization", "Bearer ${sourceManager.accessToken}")
        }.use {
            check(it.statusLine.statusCode == HttpStatus.SC_OK) {
                "Received code ${it.statusLine.statusCode} from Spotify while fetching album tracks"
            }

            JsonParser.`object`().from(it.entity.content)
        }
    }

    private fun fetchAlbumTracks(sourceManager: SpotifyAudioSourceManager, jsonTracks: JsonArray): List<AudioTrack> {
        val tasks = mutableListOf<CompletableFuture<AudioTrack>>()

        for (jTrack in jsonTracks) {
            val track = (jTrack as JsonObject)
            val title = track.getString("name")
            val artist = track.getArray("artists").getObject(0).getString("name")

            val task = sourceManager.queueAlternateSearch("$title $artist")
                .thenApply { ai -> if (ai is AudioPlaylist) ai.tracks.first() else ai as AudioTrack }
            tasks.add(task)

            // Consider refactoring this to load at play-time due to the fact album tracks do not contain ISRC.
        }

        try {
            CompletableFuture.allOf(*tasks.toTypedArray()).get()
        } catch (ignored: Exception) {
        }

        return tasks.filterNot { t -> t.isCompletedExceptionally }
            .mapNotNull { t -> t.get() }
    }
}
