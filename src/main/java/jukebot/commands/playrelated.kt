package jukebot.commands

import jukebot.JukeBot
import jukebot.audio.SongResultHandler
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context


@CommandProperties(description = "Enqueues a song similar to the current", aliases = ["pr"])
class PlayRelated : Command(ExecutionType.REQUIRE_MUTUAL) {

    //val noVideoTags = Pattern.compile("(?:official|music|lyrics?|video)").toRegex()
    //val noFeaturing = Pattern.compile(" \\(?(?:ft|feat)\\.? *?.+").toRegex()
    //val emptyBrackets = Pattern.compile("(?:\\( *?\\)|\\[ *?])").toRegex()
    // TODO cleanup remix tags

    override fun execute(context: Context) {
        val ap = context.getAudioPlayer()

        if (!ap.isPlaying) {
            return context.embed("Not Playing", "Nothing is currently playing.")
        }

        JukeBot.kSoftAPI.getMusicRecommendations(ap.player.playingTrack.identifier).thenAccept {
            if (it == null) {
                return@thenAccept context.embed("Related Tracks", "No matches found.")
            }

            JukeBot.playerManager.loadItem(it.url, SongResultHandler(context, ap, false))
        }
    }

}