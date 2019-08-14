package jukebot.entities

import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import jukebot.Database
import jukebot.JukeBot
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

class CustomPlaylist(val title: String, val creator: Long, tracks: String) {
    val tracks: MutableList<AudioTrack>

    init {
        this.tracks = tracks.split("\n")
                .asSequence()
                .filter { it.isNotEmpty() }
                .map { decoder.decode(it) }
                .map { ByteArrayInputStream(it) }
                .map { MessageInput(it) }
                .map { JukeBot.playerManager.decodeTrack(it) }
                .map { it.decodedTrack }
                .toMutableList()
    }

    private fun toMessage(audioTrack: AudioTrack): String {
        val baos = ByteArrayOutputStream()
        JukeBot.playerManager.encodeTrack(MessageOutput(baos), audioTrack)
        return encoder.encodeToString(baos.toByteArray())
    }

    fun save() {
        val trackList = this.tracks.joinToString("\n") { toMessage(it) }
        Database.updatePlaylist(creator, title, trackList)
    }

    companion object {
        private val decoder = Base64.getDecoder()
        private val encoder = Base64.getEncoder()
    }
}