package jukebot

import com.grack.nanojson.JsonParser
import com.grack.nanojson.JsonWriter
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.FunctionalResultHandler
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist
import jukebot.audio.AudioHandler
import jukebot.audio.SongResultHandler
import jukebot.framework.Context
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.CompletableFuture

class CustomAudioPlayerManager(val dapm: DefaultAudioPlayerManager) : AudioPlayerManager by dapm {
    constructor() : this(DefaultAudioPlayerManager())

    fun toBase64String(track: AudioTrack): String {
        return ByteArrayOutputStream().use {
            encodeTrack(MessageOutput(it), track)
            Base64.getEncoder().encodeToString(it.toByteArray())
        }
    }

    fun toAudioTrack(encoded: String): AudioTrack {
        val b64 = Base64.getDecoder().decode(encoded)
        return ByteArrayInputStream(b64).use {
            decodeTrack(MessageInput(it)).decodedTrack
        }
    }

    fun toJsonString(playlist: AudioPlaylist): String {
        val selectedIndex = playlist.selectedTrack?.let(playlist.tracks::indexOf) ?: -1

        return JsonWriter.string()
            .`object`()
            .value("name", playlist.name)
            .value("search", playlist.isSearchResult)
            .value("selected", selectedIndex)
            .array("tracks", playlist.tracks.map(::toBase64String))
            .end()
            .done()
    }

    fun toPlaylist(encoded: String): BasicAudioPlaylist {
        val obj = JsonParser.`object`().from(encoded)

        val name = obj.getString("name")
        val tracks = obj.getArray("tracks").map { toAudioTrack(it as String) }
        val index = obj.getInt("selected")

        val selectedTrack = if (index > -1) tracks[index] else null
        val search = obj.getBoolean("search")

        return BasicAudioPlaylist(name, tracks, selectedTrack, search)
    }

    fun loadIdentifier(identifier: String, ctx: Context,
                       handler: AudioHandler, useSelection: Boolean, playNext: Boolean = false) {
        loadItem(identifier, SongResultHandler(ctx, identifier, handler, useSelection, playNext))
    }

    fun searchYoutube(query: String): CompletableFuture<AudioTrack> {
        val future = CompletableFuture<AudioTrack>()

        val resultHandler = FunctionalResultHandler(
            future::complete,
            { future.complete(it.tracks.first()) },
            {
                val ex = IllegalStateException("No results found")
                future.completeExceptionally(ex)
            },
            future::completeExceptionally
        )

        loadItem(query, resultHandler)
        return future
    }
}
