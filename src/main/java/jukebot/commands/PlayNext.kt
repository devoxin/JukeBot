package jukebot.commands

import jukebot.JukeBot
import jukebot.audio.SongResultHandler
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context

@CommandProperties(description = "Finds a track and queues it to be played next", aliases = ["pn"], category = CommandProperties.category.CONTROLS)
class PlayNext : Command(ExecutionType.REQUIRE_MUTUAL) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        if (!player.isPlaying) {
            return context.embed("Not Playing", "Nothing is currently playing. Use the `play` command to start a song.")
        }

        JukeBot.playerManager.loadItem("ytsearch:${context.argString}", SongResultHandler(context, player, useSelection = false, playNext = true))
    }

}