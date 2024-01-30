package me.devoxin.jukebot.audio.sources.delegate

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack

class SoundcloudDelegateSource(private val apm: AudioPlayerManager,
                               private val sm: SoundCloudAudioSourceManager) : DelegateSource {
    override val supportsIsrcSearch = false

    override fun findByIsrc(isrc: String): AudioTrack? {
        throw UnsupportedOperationException()
    }

    override fun findBySearch(query: String, original: AudioTrack): AudioTrack? {
        val results = sm.loadItem(apm, AudioReference("scsearch:$query", null)) as? AudioPlaylist
            ?: return null

        return results.tracks.elementAtOrNull(0)
    }
}
