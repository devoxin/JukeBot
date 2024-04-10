package me.devoxin.jukebot.audio.sources.delegate

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class YoutubeDelegateSource(private val apm: AudioPlayerManager,
                            private val sm: YoutubeAudioSourceManager) : DelegateSource {
    override val name = sm.sourceName!!

    override fun findByIsrc(isrc: String): AudioTrack? {
        val results = sm.loadItem(apm, AudioReference("ytmsearch:\"$isrc\"", null)) as? AudioPlaylist
            ?: return null

        return results.tracks.elementAtOrNull(0)
    }

    override fun findBySearch(query: String, original: AudioTrack): AudioTrack? {
        val results = sm.loadItem(apm, AudioReference("ytsearch:$query", null)) as? AudioPlaylist
            ?: return null

        return results.tracks.maxByOrNull { scoreTrack(it, original) }
    }

    /**
     * Scores an [AudioTrack] based on its likeness to the original track.
     * A higher score means the [track] is a better candidate.
     */
    private fun scoreTrack(track: AudioTrack, original: AudioTrack): Long {
        val originalArtist = original.info.author
        val originalTitle = original.info.title.replace("- Radio Edit", "")

        val artist = track.info.author.replace("- Topic", "")
        val title = track.info.title.replace(artist, "")
        val titleLower = title.lowercase()

        var score = 0L

        if (artist == originalArtist) {
            score += 7
        } else {
            score -= 3
        }

        if (title == originalTitle) {
            score += 7
        } else if (originalTitle.lowercase() in titleLower) {
            score += 5

            if (originalArtist.lowercase() in titleLower) {
                score += 3
            } else if (originalArtist.replace("-", "").lowercase() in titleLower) {
                score += 3
            }

            val cleanedTitle =
                titleLower.replace(originalArtist.lowercase(), "").replace(originalTitle.lowercase(), "").trim()
            val extraCharacters = ((cleanedTitle.length / 3) * 1.5).roundToInt()
            score -= extraCharacters
        } else {
            score -= 5
        }

        if ("mix" in titleLower) {
            if ("mix" in originalTitle.lowercase()) {
                score += 5
            } else {
                score -= 5
            }
        }

        val durationDistance = (track.duration - original.duration).absoluteValue
        val durationScore = durationDistance / 5000
        score -= durationScore
        //        if (feat_artists := self.extra['feat']):
        //            feat_score = sum(2 for fa in feat_artists if fa.lower() in title.lower())
        //            score += feat_score

        return score
    }
//        val titleMatches = track.info.title == original.info.title
//
//        val durationScore = (minOf(track.duration, original.duration) - maxOf(track.duration, original.duration)) / 500
//        val authorScore = if (track.info.author == original.info.author) 10 else 0
//        val titleScore = if (titleMatches) 10 else 0
//
//        var extraScore = 0
//
//        if (!titleMatches) {
//            val lower = track.info.title.lowercase()
//
//            if ("lyrics" in lower) {
//                extraScore += 5
//            }
//
//            if ("radio edit" in lower) {
//                extraScore += 5
//            }
//        }
//
//        return durationScore + authorScore + titleScore + extraScore
//    }

//    private fun scoreExponentially(value: Long, desired: Long, decayRate: Double = 0.00001): Int {
//        return (exp(-decayRate * abs(desired - value)) * 10.0).toInt()
//    }
}
