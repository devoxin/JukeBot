package me.devoxin.jukebot.audio.sources.spotify

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor
import me.devoxin.jukebot.Launcher.playerManager

class SpotifyAudioTrack(private val sourceManager: SpotifyAudioSourceManager,
                        info: AudioTrackInfo,
                        val isrc: String?,
                        val artworkUrl: String?) : DelegatedAudioTrack(info) {
    override fun process(executor: LocalAudioTrackExecutor) {
        val delegate = playerManager.delegateSource
        var track: AudioTrack? = null

        if (isrc != null && delegate.supportsIsrcSearch) {
            track = delegate.findByIsrc(isrc.replace("-", ""))
        }

        if (track == null) {
            track = delegate.findBySearch("${info.title} ${info.author}", this)
        }

        if (track == null) {
            throw IllegalStateException("No available source for track!")
        }

        processDelegate(track as InternalAudioTrack, executor)
    }

    override fun getSourceManager() = sourceManager

    override fun makeShallowClone() = SpotifyAudioTrack(sourceManager, info, isrc, artworkUrl)
}
