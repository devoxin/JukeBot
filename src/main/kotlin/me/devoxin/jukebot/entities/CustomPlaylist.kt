package me.devoxin.jukebot.entities

import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.devoxin.jukebot.Database
import me.devoxin.jukebot.JukeBot
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

class CustomPlaylist(val title: String, val creator: Long, tracks: String) {
    val tracks = tracks.split("\n")
        .asSequence()
        .filter(String::isNotEmpty)
        .map(decoder::decode)
        .map { d ->
            ByteArrayInputStream(d).use {
                JukeBot.playerManager.decodeTrack(MessageInput(it)).decodedTrack
            }
        }
        .toMutableList()

    private fun toMessage(audioTrack: AudioTrack): String {
        return ByteArrayOutputStream().use {
            JukeBot.playerManager.encodeTrack(MessageOutput(it), audioTrack)
            encoder.encodeToString(it.toByteArray())
        }
    }

    fun save() {
        val trackList = this.tracks.joinToString("\n", transform = ::toMessage)
        Database.updatePlaylist(creator, title, trackList)
    }

    companion object {
        private val decoder = Base64.getDecoder()
        private val encoder = Base64.getEncoder()
    }
}
