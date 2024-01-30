package me.devoxin.jukebot.audio.sources.delegate

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.devoxin.jukebot.audio.sources.deezer.DeezerAudioSourceManager

class DeezerDelegateSource(private val apm: AudioPlayerManager,
                           private val sm: DeezerAudioSourceManager) : DelegateSource {
    override val supportsIsrcSearch = true

    override fun findByIsrc(isrc: String): AudioTrack? {
        return sm.loadItem(apm, AudioReference("dzisrc:$isrc", null)) as? AudioTrack
    }

    override fun findBySearch(query: String, original: AudioTrack): AudioTrack? {
        val results = sm.getSearch(query) as? AudioPlaylist
            ?: return null

        return results.tracks.elementAtOrNull(0)
    }
}
