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
            .map { d ->
                ByteArrayInputStream(d).use {
                    JukeBot.playerManager.decodeTrack(MessageInput(it)).decodedTrack
                }
            }
            .toMutableList()
    }

    private fun toMessage(audioTrack: AudioTrack): String {
        return ByteArrayOutputStream().use {
            val encoded = JukeBot.playerManager.encodeTrack(MessageOutput(it), audioTrack)
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
        const val TRACK_LIMIT = 100
    }
}