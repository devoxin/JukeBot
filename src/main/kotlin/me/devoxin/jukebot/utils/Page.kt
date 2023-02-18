package me.devoxin.jukebot.utils

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class Page(val content: String, val duration: String, val page: Int, val maxPages: Int) {
    companion object {
        fun paginate(tracks: List<AudioTrack>, selectedPage: Int = 1): Page {
            val queueDuration = tracks.sumOf { it.duration }.toTimeString()
            val content = StringBuilder()

            val maxPages = ceil(tracks.size.toDouble() / 10).toInt()
            val page = min(max(selectedPage, 1), maxPages)

            if (tracks.isEmpty()) {
                content.append("`No tracks.`")
            } else {
                val begin = (page - 1) * 10
                val end = min(begin + 10, tracks.size) //if (begin + 10 > tracks.size) tracks.size else begin + 10

                for (i in begin until end) {
                    val track = tracks[i]
                    content.append("`${i + 1}.` **[${track.info.title}](${track.info.uri})**\n")
                }
            }

            return Page(content.toString(), queueDuration, page, maxPages)
        }
    }
}
