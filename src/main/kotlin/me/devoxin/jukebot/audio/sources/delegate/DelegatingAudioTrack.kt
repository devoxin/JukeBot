package me.devoxin.jukebot.audio.sources.delegate

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor
import io.sentry.Sentry
import me.devoxin.jukebot.Launcher.playerManager
import me.devoxin.jukebot.audio.HighQualityAudioTrack
import me.devoxin.jukebot.audio.sources.deezer.DeezerAudioTrack
import me.devoxin.jukebot.audio.sources.deezer.DeezerAudioTrack.TrackFormat
import kotlin.jvm.Throws

abstract class DelegatingAudioTrack(info: AudioTrackInfo,
                                    val isrc: String?) : DelegatedAudioTrack(info), HighQualityAudioTrack {
    private var allowHighQualityDelegate = false
    private var isDelegateTrack = false

    val preferredSource: Pair<String, Class<out DelegateSource>>
        get() = "deezer" to DeezerDelegateSource::class.java

    @Throws(Exception::class)
    override fun process(executor: LocalAudioTrackExecutor) {
        findAndPlayDelegate(executor)
    }

    protected fun findAndPlayDelegate(executor: LocalAudioTrackExecutor, vararg excluding: String) {
        if (isDelegateTrack) {
            throw RuntimeException("Delegate tracks cannot delegate")
        }

        var track = findDelegateTrack(*excluding)
            ?: throw RuntimeException("No available source for track!")

        if (allowHighQualityDelegate) {
            if (track is DeezerAudioTrack) {
                track.setAllowHighQuality(true)
                val preparedSource = track.runCatching(DeezerAudioTrack::prepareSource).getOrNull()

                if (preparedSource == null || preparedSource.format >= TrackFormat.MP3_256) {
                    // skip if we get served a 256Kbps MP3 or lower.
                    findDelegateTrack(*excluding, track.sourceManager.sourceName)?.let { track = it }
                }
            }
        }

        try {
            processDelegate(track as InternalAudioTrack, executor)
        } catch (t: Throwable) {
            Sentry.capture(t)

            val alt = findDelegateTrack(*excluding, track.sourceManager.sourceName)
                ?: throw RuntimeException("No available source for track!")

            processDelegate(alt as InternalAudioTrack, executor)
        }
    }

    private fun findDelegateTrack(vararg excluding: String): AudioTrack? {
        val delegate = playerManager.delegateSource
        val prefer = preferredSource.takeIf { it.first !in excluding }?.second

        val track = isrc?.let { delegate.findByIsrc(it.replace("-", ""), prefer, *excluding) }
            ?: delegate.findBySearch("${info.title} ${info.author}", this, prefer, *excluding)

        return track.also {
            if (it is DelegatingAudioTrack) {
                it.isDelegateTrack = true
            }
        }
    }

    override fun setAllowHighQuality(allowHighQuality: Boolean) {
        allowHighQualityDelegate = allowHighQuality
    }
}
