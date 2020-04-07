package jukebot

import com.grack.nanojson.JsonParser
import com.grack.nanojson.JsonWriter
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
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

class CustomAudioPlayerManager(val dapm: DefaultAudioPlayerManager) : AudioPlayerManager by dapm {

    constructor() : this(DefaultAudioPlayerManager())

    fun toBase64String(track: AudioTrack): String {
        val baos = ByteArrayOutputStream()
        encodeTrack(MessageOutput(baos), track)
        return Base64.getEncoder().encodeToString(baos.toByteArray())
    }

    fun toAudioTrack(encoded: String): AudioTrack {
        val b64 = Base64.getDecoder().decode(encoded)
        val bais = ByteArrayInputStream(b64)
        return decodeTrack(MessageInput(bais)).decodedTrack
    }

    fun toJsonString(playlist: AudioPlaylist): String {
        val selectedIndex = playlist.selectedTrack?.let {
            playlist.tracks.indexOf(playlist.selectedTrack)
        } ?: -1

        return JsonWriter.string()
            .`object`()
            .value("name", playlist.name)
            .value("search", playlist.isSearchResult)
            .value("selected", selectedIndex)
            .array("tracks", playlist.tracks.map(::toBase64String))
            .done()
    }

    fun toPlaylist(encoded: String): BasicAudioPlaylist {
        val obj = JsonParser.`object`().from(encoded)

        val name = obj.getString("name")
        val tracks = mutableListOf<AudioTrack>()

        for (track in obj.getArray("tracks")) {
            tracks.add(toAudioTrack(track as String))
        }

        val index = obj.getInt("selected")
        val selectedTrack = if (index > -1) tracks[index] else null
        val search = obj.getBoolean("search")

        return BasicAudioPlaylist(name, tracks, selectedTrack, search)
    }

    fun loadIdentifier(identifier: String, ctx: Context,
                       handler: AudioHandler, useSelection: Boolean, playNext: Boolean = false) {
        loadItem(identifier, SongResultHandler(ctx, identifier, handler, useSelection, playNext))
    }

    fun loadIdentifier(customIdentifier: String, originalIdentifier: String, ctx: Context,
                       handler: AudioHandler, useSelection: Boolean, playNext: Boolean = false) {
        loadItem(customIdentifier, SongResultHandler(ctx, originalIdentifier, handler, useSelection, playNext))
    }

}
