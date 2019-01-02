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


@CommandProperties(description = "Enqueues a song similar to the current", developerOnly = true, aliases = ["pr"])
class PlayRelated : Command(ExecutionType.REQUIRE_MUTUAL) {

    val unshitter = Pattern.compile("\\(?(?:(?:official)? (?:music|lyrics?) video)\\)? ?").toRegex()
    val nahDidItSoloMate = Pattern.compile(" ?\\(?(?:ft|feat)\\.? *?.+").toRegex()

    // TODO cleanup remix tags

    override fun execute(context: Context) {
        GlobalScope.async {
            val ap = context.getAudioPlayer()
            val track = ap.player.playingTrack
            val title = track.info.title.toLowerCase()

            val unshit = title.replace(unshitter, "").replace(nahDidItSoloMate, "")
            val res = JukeBot.spotifyApi.search(unshit).await()
                    ?: return@async context.embed("Spotify Track Search", "No matches found.")

            context.embed("Spotify Track Search", "${res.artist} - ${res.title}")

            val similar = JukeBot.lastFM.findSimilar(res.title, res.artist).await()
                    ?: return@async context.embed("LastFM Similar Track", "No matches found.")

            context.embed("LastFM Similar Track", "${similar.artist} - ${similar.title}")

            JukeBot.playerManager.loadItem("ytsearch:${similar.artist} - ${similar.title}", SongResultHandler(context, ap, false))
        }
    }

}