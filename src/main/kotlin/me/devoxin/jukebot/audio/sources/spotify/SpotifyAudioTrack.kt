package me.devoxin.jukebot.audio.sources.spotify

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import me.devoxin.jukebot.audio.sources.delegate.DelegatingAudioTrack
import me.devoxin.jukebot.audio.track.ArtworkProvider

class SpotifyAudioTrack(private val sourceManager: SpotifyAudioSourceManager,
                        info: AudioTrackInfo,
                        isrc: String?,
                        override val artworkUrl: String?) : DelegatingAudioTrack(info, isrc), ArtworkProvider {
    override fun getSourceManager() = sourceManager

    override fun makeShallowClone() = SpotifyAudioTrack(sourceManager, info, isrc, artworkUrl)
}
