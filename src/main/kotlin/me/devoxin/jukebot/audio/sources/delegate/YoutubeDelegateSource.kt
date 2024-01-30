package me.devoxin.jukebot.audio.sources.delegate

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlin.math.abs
import kotlin.math.exp

class YoutubeDelegateSource(private val apm: AudioPlayerManager,
                            private val sm: YoutubeAudioSourceManager) : DelegateSource {
    override val supportsIsrcSearch = true

    override fun findByIsrc(isrc: String): AudioTrack? {
        val results = sm.loadItem(apm, AudioReference("ytmsearch:\"$isrc\"", null)) as? AudioPlaylist
            ?: return null

        return results.tracks.elementAtOrNull(0)
    }

    override fun findBySearch(query: String, original: AudioTrack): AudioTrack? {
        val results = sm.loadItem(apm, AudioReference("ytsearch:$query", null)) as? AudioPlaylist
            ?: return null

        return results.tracks.minByOrNull { scoreTrack(it, original) }
    }

    /**
     * Scores an [AudioTrack] based on its likeness to the original track.
     * A higher score means the [track] is a better candidate.
     */
    private fun scoreTrack(track: AudioTrack, original: AudioTrack): Long {
        val titleMatches = track.info.title == original.info.title

        val durationScore = (minOf(track.duration, original.duration) - maxOf(track.duration, original.duration)) / 500
        val authorScore = if (track.info.author == original.info.author) 10 else 0
        val titleScore = if (titleMatches) 10 else 0

        var extraScore = 0

        if (!titleMatches) {
            val lower = track.info.title.lowercase()

            if ("lyrics" in lower) {
                extraScore += 5
            }

            if ("radio edit" in lower) {
                extraScore += 5
            }
        }

        return durationScore + authorScore + titleScore + extraScore
    }

    private fun scoreExponentially(value: Long, desired: Long, decayRate: Double = 0.00001): Int {
        return (exp(-decayRate * abs(desired - value)) * 10.0).toInt()
    }
}
