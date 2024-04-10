package me.devoxin.jukebot.audio.sources.spotify

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor
import io.sentry.Sentry
import me.devoxin.jukebot.Launcher.playerManager

class SpotifyAudioTrack(private val sourceManager: SpotifyAudioSourceManager,
                        info: AudioTrackInfo,
                        val isrc: String?,
                        val artworkUrl: String?) : DelegatedAudioTrack(info) {
    override fun process(executor: LocalAudioTrackExecutor) {
        val track = findDelegateTrack()

        try {
            processDelegate(track as InternalAudioTrack, executor)
        } catch (t: Throwable) {
            Sentry.capture(t)
            processDelegate(findDelegateTrack(track.sourceManager.sourceName) as InternalAudioTrack, executor)
        }
    }

    private fun findDelegateTrack(vararg excluding: String): AudioTrack {
        val delegate = playerManager.delegateSource

        return isrc?.let { delegate.findByIsrc(it.replace("-", ""), *excluding) }
            ?: delegate.findBySearch("${info.title} ${info.author}", this, *excluding)
            ?: throw IllegalStateException("No available source for track!")
    }

    override fun getSourceManager() = sourceManager

    override fun makeShallowClone() = SpotifyAudioTrack(sourceManager, info, isrc, artworkUrl)
}
