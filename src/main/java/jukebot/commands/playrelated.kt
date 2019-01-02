package jukebot.commands

import jukebot.JukeBot
import jukebot.audio.SongResultHandler
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.await
import java.util.regex.Pattern


@CommandProperties(description = "Enqueues a song similar to the current", developerOnly = false, aliases = ["pr"])
class PlayRelated : Command(ExecutionType.REQUIRE_MUTUAL) {

    //val noVideoTags = Pattern.compile("(?:(?:official)? (?:(?:music|lyrics?) )?video) ?").toRegex()
    val noVideoTags = Pattern.compile("(?:official|music|lyrics?|video)").toRegex()
    val noFeaturing = Pattern.compile(" \\(?(?:ft|feat)\\.? *?.+").toRegex()

    // TODO cleanup remix tags

    override fun execute(context: Context) {
        GlobalScope.async {
            val ap = context.getAudioPlayer()
            val track = ap.player.playingTrack
            val title = track.info.title.toLowerCase()

            var cleaned = title.replace(noVideoTags, "")
                    .replace(noFeaturing, "")
                    .replace("()", "")

            if (cleaned.contains(',') && cleaned.contains('-')) { // Multiple artists
                val sliced = cleaned.split('-')
                val artists = sliced[0].split(',')

                val firstArtist = artists[0].trim()
                val unhyphenatedTitle = sliced[1].trim()

                cleaned = "$firstArtist - $unhyphenatedTitle"
            } else if (cleaned.contains('&') && cleaned.contains('-')) {
                val sliced = cleaned.split('-')
                val artists = sliced[0].split('&')

                val firstArtist = artists[0].trim()
                val unhyphenatedTitle = sliced[1].trim()

                cleaned = "$firstArtist $unhyphenatedTitle"
            }

            cleaned = cleaned.replace("-", "").trim()
            println(cleaned)

            val res = JukeBot.spotifyApi.search(cleaned).await()
                    ?: return@async context.embed("Related Tracks", "No matches found.")

            val similar = JukeBot.lastFM.findSimilar(res.title, res.artist).await()
                    ?: return@async context.embed("Related Tracks", "No matches found.")

            JukeBot.playerManager.loadItem("ytsearch:${similar.artist} - ${similar.title}", SongResultHandler(context, ap, false))
        }
    }

}