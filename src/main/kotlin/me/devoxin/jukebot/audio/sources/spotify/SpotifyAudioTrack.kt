package me.devoxin.jukebot.audio.sources.spotify

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import me.devoxin.jukebot.audio.sources.delegate.DelegatingAudioTrack

class SpotifyAudioTrack(private val sourceManager: SpotifyAudioSourceManager,
                        info: AudioTrackInfo,
                        isrc: String?,
                        val artworkUrl: String?) : DelegatingAudioTrack(info, isrc) {
    override fun getSourceManager() = sourceManager

    override fun makeShallowClone() = SpotifyAudioTrack(sourceManager, info, isrc, artworkUrl)
}
