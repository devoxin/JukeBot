package jukebot.commands

import jukebot.JukeBot
import jukebot.audio.SongResultHandler
import jukebot.framework.Command
import jukebot.framework.CommandCategory
import jukebot.framework.CommandProperties
import jukebot.framework.Context


@CommandProperties(description = "Enqueues a song similar to the current", aliases = ["pr"], category = CommandCategory.PLAYBACK)
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

        val trackIds = mutableListOf(ap.current!!.identifier)

        if (ap.queue.isNotEmpty()) {
            trackIds.addAll(ap.queue.take(4).map { it.identifier })
        }

        JukeBot.kSoftAPI.getMusicRecommendations(*trackIds.toTypedArray()).thenAccept {
            if (it == null) {
                return@thenAccept context.embed("Related Tracks", "No matches found.")
            }

            JukeBot.playerManager.loadItem(it.url, SongResultHandler(context, ap, false))
        }
    }

}