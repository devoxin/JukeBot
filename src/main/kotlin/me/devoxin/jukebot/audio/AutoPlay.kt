package me.devoxin.jukebot.audio

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.devoxin.jukebot.Database
import me.devoxin.jukebot.JukeBot
import me.devoxin.jukebot.utils.trimToSize

class AutoPlay(private val guildId: Long) {
    private val previousTracks = mutableSetOf<AudioTrack>()

    val enabled: Boolean
        get() = Database.getIsPremiumServer(guildId) && Database.getIsAutoPlayEnabled(guildId)

    val hasSufficientData: Boolean
        get() = previousTracks.size > 0

    fun store(track: AudioTrack) {
        previousTracks.add(track)
        previousTracks.trimToSize(MAX_SET_SIZE)
    }

    fun getRelatedTrack(): AudioTrack? {
        if (!enabled) {
            return null
        }

        val provider = JukeBot.playerManager.source(YoutubeAudioSourceManager::class.java)
            ?: return null

        val seedTrack = previousTracks.lastOrNull() ?: return null

        if (seedTrack.sourceManager.sourceName != provider.sourceName) {
            val reference = AudioReference("ytsearch:${seedTrack.info.title} ${seedTrack.info.author}", null)
            val equivalentTrack = runCatching { provider.loadItem(JukeBot.playerManager, reference) }.getOrNull() as? AudioTrack
                ?: return null

            return getRandomMixTrack(equivalentTrack.identifier, provider)
        }

        return getRandomMixTrack(seedTrack.identifier, provider)
    }

    private fun getRandomMixTrack(trackId: String, provider: AudioSourceManager): AudioTrack? {
        val mixList = runCatching { provider.loadItem(JukeBot.playerManager, AudioReference(MIX_URL.format(trackId), null)) }.getOrNull() as? AudioPlaylist
            ?: return null

        val uniqueTracks = mixList.tracks.filter { previousTracks.none { pt -> pt.identifier == it.identifier } }

        if (uniqueTracks.isNotEmpty()) {
            return uniqueTracks.random().also { it.userData = JukeBot.selfId }
        }

        return mixList.tracks.takeIf { it.isNotEmpty() }?.random()?.also { it.userData = JukeBot.selfId }
    }

    companion object {
        private const val MAX_SET_SIZE = 10
        private const val MIX_URL = "https://www.youtube.com/watch?v=%1\$s&list=RD%1\$s"
    }
}
